package com.wzy.auroth.thrift.server;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultNiftyConfigurer implements NiftyConfigurer {

    private List<Advice> adviceList = new ArrayList<>();

    public void addAdvice(Advice advice) {
        this.adviceList.add(advice);
    }

    @Override
    public void configureProxyFactory(ProxyFactory proxyFactory) {
        adviceList.forEach(proxyFactory::addAdvice);
    }
}
