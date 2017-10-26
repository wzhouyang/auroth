package com.wzy.auroth.thrift.client.pool;

import com.facebook.nifty.client.FramedClientConnector;
import com.facebook.nifty.client.NettyClientConfig;
import com.facebook.nifty.client.NiftyClient;
import com.facebook.nifty.client.TNiftyClientChannelTransport;
import com.wzy.auroth.thrift.client.ThriftClientKey;
import com.wzy.auroth.thrift.client.config.ThriftClientProperties;
import com.wzy.auroth.thrift.ext.TProtocolExt;
import io.airlift.units.Duration;
import lombok.Getter;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TTransport;
import org.springframework.beans.BeanUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThriftClientPooledObjectFactory extends BaseKeyedPooledObjectFactory<ThriftClientKey, TServiceClient> {

    /**
     * niftyClient cache
     */
    private static final Map<ThriftClientKey, NiftyClientDelegate> NIFTY_CLIENT_MAP = new ConcurrentHashMap<>(256);

    private final ThriftClientProperties clientProperties;

    private final TProtocolFactory protocolFactory;

    private final NiftyClientDelegate niftyClientDelegate;

    public ThriftClientPooledObjectFactory(ThriftClientProperties clientProperties, TProtocolFactory protocolFactory) {
        this.clientProperties = clientProperties;
        this.protocolFactory = protocolFactory;
        this.niftyClientDelegate = getNiftyClient();
    }

    @Override
    public TServiceClient create(ThriftClientKey key) throws Exception {
        if (!key.validate()) {
            throw new IllegalArgumentException("serviceId and service host is null");
        }

        InetSocketAddress address = new InetSocketAddress(key.getHost(), key.getPort());
        FramedClientConnector framedClientConnector = new FramedClientConnector(address);
        NiftyClient niftyClient = niftyClientDelegate.getNiftyClient();
        ThriftClientProperties.ThriftClientConfig clientConfig = niftyClientDelegate.getThriftClientConfig();


        TNiftyClientChannelTransport transport =
                niftyClient.connectSync(key.getClazz(), framedClientConnector,
                        Duration.succinctDuration(clientConfig.getConnectTimeout(), TimeUnit.MILLISECONDS),
                        Duration.succinctDuration(clientConfig.getReceiveTimeout(), TimeUnit.MILLISECONDS),
                        Duration.succinctDuration(clientConfig.getReadTimeout(), TimeUnit.MILLISECONDS),
                        Duration.succinctDuration(clientConfig.getSendTimeout(), TimeUnit.MILLISECONDS),
                        clientConfig.getMaxFrameSize()
                        );
        TProtocol tp = protocolFactory.getProtocol(transport);
        TMultiplexedProtocol multiplexedProtocol = new TMultiplexedProtocol(tp, key.getProtocolServiceName());
        TProtocol protocolExt = new TProtocolExt(multiplexedProtocol);
        return BeanUtils.instantiateClass(
                key.getClazz().getConstructor(TProtocol.class),
                (TProtocol) protocolExt
        );
    }

    @Override
    public void activateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        super.activateObject(key, p);
    }

    @Override
    public boolean validateObject(ThriftClientKey key, PooledObject<TServiceClient> p) {
        TServiceClient client = p.getObject();
        return client.getInputProtocol().getTransport().isOpen() &&
                client.getOutputProtocol().getTransport().isOpen() &&
                super.validateObject(key, p);

    }

    /**
     * Uninitialize an instance to be returned to the idle object pool.
     * <p>
     * The default implementation is a no-op.
     *
     * @param key the key used when selecting the object
     * @param p   a {@code PooledObject} wrapping the the instance to be passivated
     */
    @Override
    public void passivateObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        super.passivateObject(key, p);
    }

    @Override
    public void destroyObject(ThriftClientKey key, PooledObject<TServiceClient> p) throws Exception {
        TServiceClient client = p.getObject();
        TProtocol inputProtocol = client.getInputProtocol();
        inputProtocol.reset();
        TTransport inputTransport = inputProtocol.getTransport();
        if (inputTransport.isOpen()) {
            inputTransport.close();
        }
        TProtocol outputProtocol = client.getOutputProtocol();
        outputProtocol.reset();
        TTransport outputTransport = outputProtocol.getTransport();
        if (outputTransport.isOpen()) {
            outputTransport.close();
        }
        super.destroyObject(key, p);
    }

    @Override
    public PooledObject<TServiceClient> wrap(TServiceClient value) {
        return new ThriftClientPooledObject<>(value);
    }

    private NiftyClientDelegate getNiftyClient() {
        ThriftClientProperties.ThriftClientConfig thriftClientConfig;
        //获取全局的
        thriftClientConfig = clientProperties.getConfig();
        return new NiftyClientDelegate(thriftClientConfig);
    }

    @Getter
    static class NiftyClientDelegate {

        private final NiftyClient niftyClient;

        private final ThriftClientProperties.ThriftClientConfig thriftClientConfig;

        NiftyClientDelegate(ThriftClientProperties.ThriftClientConfig thriftClientConfig) {
            this.thriftClientConfig = thriftClientConfig;
            this.niftyClient = createNifryClient();
        }

        private NiftyClient createNifryClient() {
            ExecutorService bossExecutorService;
            ExecutorService workerExecutorService;
            if (ThriftClientProperties.ThreadType.cache.compareTo(thriftClientConfig.getThreadType()) == 0) {
                bossExecutorService = Executors.newCachedThreadPool();
                workerExecutorService = Executors.newCachedThreadPool();
            } else {
                bossExecutorService = Executors.newFixedThreadPool(thriftClientConfig.getBossThreadCount());
                workerExecutorService = Executors.newFixedThreadPool(thriftClientConfig.getWorkerThreadCount());
            }
            NettyClientConfig nettyClientConfig = NettyClientConfig.newBuilder()
                    .setBossThreadCount(thriftClientConfig.getBossThreadCount())
                    .setBossThreadExecutor(bossExecutorService)
                    .setWorkerThreadCount(thriftClientConfig.getWorkerThreadCount())
                    .setWorkerThreadExecutor(workerExecutorService)
                    .build();
            return new NiftyClient(nettyClientConfig);
        }
    }

}
