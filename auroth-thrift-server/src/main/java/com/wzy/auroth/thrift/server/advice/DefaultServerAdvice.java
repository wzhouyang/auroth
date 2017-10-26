package com.wzy.auroth.thrift.server.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class DefaultServerAdvice implements MethodInterceptor, ThriftServerAdvice {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        return methodInvocation.proceed();
    }
}
