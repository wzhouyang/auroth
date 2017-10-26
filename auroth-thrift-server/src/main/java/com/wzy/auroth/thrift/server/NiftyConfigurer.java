package com.wzy.auroth.thrift.server;

import org.springframework.aop.framework.ProxyFactory;

public interface NiftyConfigurer {
    void configureProxyFactory(ProxyFactory proxyFactory);
}
