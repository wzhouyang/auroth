package com.wzy.auroth.thrift.client.advice;

import com.facebook.nifty.core.NiftyRequestContext;
import com.facebook.nifty.core.RequestContext;
import com.facebook.nifty.core.RequestContexts;
import com.wzy.auroth.sleuth.ThriftRequestMap;
import com.wzy.auroth.sleuth.ThriftSpanInjector;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;

public class TraceThriftClientAdvice implements MethodInterceptor, ThriftClientAdvice {

    private final Tracer tracer;

    private final TraceKeys traceKeys;

    private final ThriftSpanInjector spanInjector;

    public TraceThriftClientAdvice(Tracer tracer, TraceKeys traceKeys, ThriftSpanInjector spanInjector) {
        this.tracer = tracer;
        this.traceKeys = traceKeys;
        this.spanInjector = spanInjector;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final String className = invocation.getMethod().getDeclaringClass().getName();
        String simpleName = className.substring(className.lastIndexOf(".") + 1);
        String commandKey = simpleName + "-" + invocation.getMethod().getName();

        try {
            tracer.createSpan(commandKey);

            tracer.addTag(traceKeys.getMvc().getControllerClass(), className);
            tracer.addTag(traceKeys.getMvc().getControllerMethod(), invocation.getMethod().getName());
            tracer.addTag(traceKeys.getHttp().getMethod(), "thrift");
            tracer.addTag(traceKeys.getHttp().getUrl(), commandKey);
            tracer.addTag(traceKeys.getHttp().getPath(), invocation.getMethod().getName());

            RequestContext requestContext = new NiftyRequestContext(null, null, null, null);
            RequestContexts.setCurrentContext(requestContext);
            Span span = this.startSpan(commandKey);

            spanInjector.inject(span, new ThriftRequestMap());
            span.logEvent(Span.CLIENT_SEND);

            Object result = invocation.proceed();
            tracer.addTag(traceKeys.getHttp().getStatusCode(), "ok");
            return result;
        } catch (Throwable throwable) {
            tracer.addTag(traceKeys.getHttp().getStatusCode(), "error");
            tracer.addTag(Span.SPAN_ERROR_TAG_NAME, throwable.getMessage());
            throw new Exception(throwable);
        } finally {
            Span span = startSpan(commandKey);
            span.logEvent(Span.CLIENT_RECV);
            tracer.close(span);
            RequestContexts.clearCurrentContext();
        }

    }

    private Span startSpan(String commandKeyName) {
        Span span = this.tracer.getCurrentSpan();
        if (span == null) {
            return this.tracer.createSpan(commandKeyName);
        }
        return this.tracer.continueSpan(span);
    }
}
