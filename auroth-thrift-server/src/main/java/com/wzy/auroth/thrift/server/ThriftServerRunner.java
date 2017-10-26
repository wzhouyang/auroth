package com.wzy.auroth.thrift.server;

import com.facebook.nifty.core.NettyServerConfig;
import com.facebook.nifty.core.NettyServerConfigBuilder;
import com.facebook.nifty.core.NettyServerTransport;
import com.facebook.nifty.core.ThriftServerDef;
import com.facebook.nifty.core.ThriftServerDefBuilder;
import com.wzy.auroth.thrift.annotation.TService;
import com.wzy.auroth.thrift.ext.TProcessorExt;
import com.wzy.auroth.thrift.server.config.ThriftServerProperties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TProtocolFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ThriftServerRunner implements ApplicationRunner, DisposableBean {

    @Setter
    private ApplicationContext applicationContext;

    @Setter
    private ThriftServerProperties serverProperties;

    @Setter
    private TProtocolFactory protocolFactory;

    @Setter
    private NiftyConfigurer niftyConfigurer;

    private NettyServerTransport nettyServerTransport;

    @Override
    public void destroy() throws Exception {
        if (nettyServerTransport != null) {
            nettyServerTransport.stop();
        }
    }

    @Override
    public void run(ApplicationArguments applicationArguments) throws Exception {
        TProcessor tProcessor = builtProcessor();
        NettyServerConfig nettyServerConfig = getNettyServerConfig();
        ThriftServerDef serverDef = new ThriftServerDefBuilder().protocol(protocolFactory).listen
                (serverProperties.getPort()).withProcessor(tProcessor).build();
        if (nettyServerTransport == null) {
            nettyServerTransport = new NettyServerTransport(serverDef, nettyServerConfig, new DefaultChannelGroup());
        }
        nettyServerTransport.start();
        log.info("thriftServer start sucess on port: {}", serverDef.getServerPort());
    }

    private NettyServerConfig getNettyServerConfig() {
        ExecutorService bossExecutorService;
        ExecutorService workerExecutorService;
        ThriftServerProperties.ThriftServerConfig serverConfig = serverProperties.getConfig();
        if (ThriftServerProperties.ThreadType.cache.compareTo(serverConfig.getThreadType()) == 0) {
            bossExecutorService = Executors.newCachedThreadPool();
            workerExecutorService = Executors.newCachedThreadPool();
        } else {
            bossExecutorService = Executors.newFixedThreadPool(serverConfig.getBossThreadCount());
            workerExecutorService = Executors.newFixedThreadPool(serverConfig.getWorkerThreadCount());
        }

        return NettyServerConfig.newBuilder().setWorkerThreadCount(serverConfig.getWorkerThreadCount())
                .setWorkerThreadExecutor(workerExecutorService).setBossThreadCount(serverConfig.getBossThreadCount())
                .setBossThreadExecutor(bossExecutorService).build();
    }

    @SuppressWarnings("unchecked")
    private TProcessor builtProcessor() throws NoSuchMethodException {
        TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();
        //get all beans with {@code TService}
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(TService.class);
        //if no TService then return
        if (CollectionUtils.isEmpty(beansWithAnnotation)) {
            return multiplexedProcessor;
        }

        //service class
        Class serviceClass;
        Class<TProcessor> processorClass;
        Class ifaceClass;

        //iterator beans
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            //get all interfaces
            Class<?>[] allInterfaces = ClassUtils.getAllInterfaces(entry.getValue());
            //iterator interfaces
            if (allInterfaces != null && allInterfaces.length > 0) {
                for (Class<?> handlerInterface : allInterfaces) {
                    serviceClass = handlerInterface.getDeclaringClass();
                    //check Iface
                    if (!handlerInterface.getName().endsWith("$Iface") || serviceClass == null) {
                        continue;
                    }

                    //processor
                    for (Class<?> innerClass : serviceClass.getDeclaredClasses()) {
                        if (!innerClass.getName().endsWith("$Processor")) {
                            continue;
                        }

                        if (!TProcessor.class.isAssignableFrom(innerClass)) {
                            continue;
                        }

                        ifaceClass = handlerInterface;
                        processorClass = (Class<TProcessor>) innerClass;
                        //wrapped iface
                        Object wrappedHandler = wrapHandler(ifaceClass, entry.getValue());
                        TProcessor tProcessor = createTProcessor(ifaceClass, processorClass, wrappedHandler);
                        //register processor
                        multiplexedProcessor.registerProcessor(serviceClass.getSimpleName(), tProcessor);
                        break;
                    }
                }
            }
        }

        return new TProcessorExt(multiplexedProcessor);
    }

    @SuppressWarnings("unchecked")
    private <T> T wrapHandler(final Class<T> interfaceClass, final T handler) {
        ProxyFactory proxyFactory =
                new ProxyFactory(interfaceClass, new SingletonTargetSource(handler));
        //config proxy
        niftyConfigurer.configureProxyFactory(proxyFactory);
        // TODO remove from here?
        proxyFactory.setFrozen(true);
        proxyFactory.setProxyTargetClass(true);
        return (T) proxyFactory.getProxy();
    }

    @SuppressWarnings("unchecked")
    private <T> TProcessor createTProcessor(Class<?> iFaceClass, Class<TProcessor> processorClass, T wrappedHandler)
            throws BeanInstantiationException, NoSuchMethodException, SecurityException {
        return BeanUtils.instantiateClass(processorClass.getConstructor(iFaceClass), wrappedHandler);
    }
}
