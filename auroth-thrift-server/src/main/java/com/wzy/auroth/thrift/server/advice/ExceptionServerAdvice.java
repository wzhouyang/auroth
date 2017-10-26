package com.wzy.auroth.thrift.server.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.thrift.TException;

public class ExceptionServerAdvice implements MethodInterceptor, ThriftServerAdvice {
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        try {
            return methodInvocation.proceed();
        } catch (Throwable e) {
            if (e instanceof TException) {
                throw e;
            } else {
                throw new TException(e);
            }
        }
    }
}
