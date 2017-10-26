package com.wzy.auroth.thrift.client.advice;


import com.wzy.auroth.core.AurothConstants;
import com.wzy.auroth.thrift.annotation.TReferenceMeta;
import com.wzy.auroth.thrift.client.ThriftClientKey;
import com.wzy.auroth.thrift.client.concurrency.ReferenceMetaProvider;
import com.wzy.auroth.thrift.client.exception.ThriftClientException;
import com.wzy.auroth.thrift.client.pool.ThriftClientPool;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.thrift.TException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.transport.TTransportException;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.SocketException;

@Slf4j
public class TThriftClientAdvice implements MethodInterceptor, ThriftClientAdvice {

    @Setter
    private LoadBalancerClient loadBalancerClient;

    @Setter
    private ThriftClientPool thriftClientPool;

    @Setter
    private ReferenceMetaProvider referenceMetaProvider;

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        TReferenceMeta referenceMeta = referenceMetaProvider.getReferenceMeta();

        //获取参数
        Object[] args = methodInvocation.getArguments();
        //获取target class
        Class<? extends TServiceClient> declaringClass = (Class<? extends TServiceClient>) methodInvocation.getMethod().getDeclaringClass();
        TServiceClient thriftClient = null;
        ThriftClientKey thriftClientKey;
        //是否直连
        if (isDirect(referenceMeta)) {
            thriftClientKey = ThriftClientKey.builder()
                    .clazz(declaringClass)
                    .serviceId(referenceMeta.getServiceId())
                    .host(referenceMeta.getHost())
                    .port(referenceMeta.getPort())
                    .build();
        } else {
            //获取一个server
            ServiceInstance serviceInstance = loadBalancerClient.choose(referenceMeta.getServiceId());
            if (serviceInstance == null) {
                throw new RuntimeException("no server is running");
            }

            //获取服务所在的host和port
            String host = serviceInstance.getHost();
            String port = serviceInstance.getMetadata().get(AurothConstants.TCP_PORT);
            //built thriftClientKey
            thriftClientKey = ThriftClientKey.builder()
                    .clazz(declaringClass)
                    .serviceId(referenceMeta.getServiceId())
                    .host(host)
                    .port(port == null ? referenceMeta.getPort() : Integer.parseInt(port))
                    .build();
        }

        try {
            //get thriftClient from the pool
            log.debug("thrift client pool created {} 个, thriftClientKey: {} 的个数为: {}", thriftClientPool.getCreatedCount(), thriftClientKey, thriftClientPool.getNumActive(thriftClientKey));
            thriftClient = thriftClientPool.borrowObject(thriftClientKey);
            //invoke method
            return ReflectionUtils.invokeMethod(methodInvocation.getMethod(), thriftClient, args);
        } catch (UndeclaredThrowableException e) {
            log.error("thrift请求异常", e);
            if (TTransportException.class.isAssignableFrom(e.getUndeclaredThrowable().getClass())) {
                TTransportException innerException = (TTransportException) e.getUndeclaredThrowable();
                Throwable realException = innerException.getCause();
                //设置超时时间后，会发生中断异常 InterruptedException
                if (realException instanceof SocketException || realException instanceof InterruptedException) {
                    //关闭thriftClient
                    close(thriftClientKey, thriftClient);
                    thriftClient = null;
                    //需要重试
                    throw new ThriftClientException(innerException);
                }
                throw innerException;
            } else {
                if (TException.class.isAssignableFrom(e.getUndeclaredThrowable().getClass())) {
                    throw e.getUndeclaredThrowable();
                }
                throw e;
            }
        } finally {
            if (thriftClient != null) {
                thriftClientPool.returnObject(thriftClientKey, thriftClient);
            }
        }
    }

    private boolean isDirect(TReferenceMeta referenceMeta) {
        return !StringUtils.isEmpty(referenceMeta.getHost()) && referenceMeta.getPort() > 0;
    }

    private void close(ThriftClientKey thriftClientKey, TServiceClient thriftClient) {
        try {
            thriftClientPool.invalidateObject(thriftClientKey, thriftClient);
        } catch (Exception e) {
            log.error("invalidate Object error", e);
        }
    }
}
