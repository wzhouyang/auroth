package com.wzy.auroth.thrift.client.advice;

import com.wzy.auroth.thrift.annotation.TReferenceMeta;
import com.wzy.auroth.thrift.utils.ThriftUtils;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class LocalRequestVariableAdvice implements MethodInterceptor, ThriftClientAdvice {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class declaringClass = invocation.getMethod().getDeclaringClass();
        TReferenceMeta referenceMeta = ThriftUtils.getReferenceMeta(declaringClass);
        if (referenceMeta != null) {
            ThriftUtils.REFERENCE_META_THREAD_LOCAL.set(referenceMeta);
        }

        try {
            return invocation.proceed();
        } finally {
            ThriftUtils.REFERENCE_META_THREAD_LOCAL.remove();
        }
    }
}
