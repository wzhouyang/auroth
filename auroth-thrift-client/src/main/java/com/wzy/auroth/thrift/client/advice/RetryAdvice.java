package com.wzy.auroth.thrift.client.advice;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * 重试
 */
public class RetryAdvice implements MethodInterceptor, ThriftClientAdvice {

    private final RetryOperationsInterceptor delegate;

    public RetryAdvice(RetryOperationsInterceptor delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return delegate.invoke(invocation);
    }
}
