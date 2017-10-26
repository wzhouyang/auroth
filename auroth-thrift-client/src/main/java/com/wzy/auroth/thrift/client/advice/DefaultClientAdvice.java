package com.wzy.auroth.thrift.client.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class DefaultClientAdvice implements MethodInterceptor, ThriftClientAdvice {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        return methodInvocation.proceed();
    }
}
