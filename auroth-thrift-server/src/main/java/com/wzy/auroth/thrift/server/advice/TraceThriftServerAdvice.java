package com.wzy.auroth.thrift.server.advice;

import com.facebook.nifty.core.RequestContexts;
import com.wzy.auroth.core.AurothConstants;
import com.wzy.auroth.sleuth.ThriftRequestMap;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.thrift.TException;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanExtractor;
import org.springframework.cloud.sleuth.instrument.web.TraceRequestAttributes;
import org.springframework.cloud.sleuth.util.ExceptionUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class TraceThriftServerAdvice implements MethodInterceptor, ThriftServerAdvice {

    public static final String TRACE_REQUEST_ATTR = TraceThriftServerAdvice.class.getName()
            + ".TRACE";

    private final Tracer tracer;
    private final TraceKeys traceKeys;
    private final HttpSpanExtractor httpSpanExtractor;
    private final SpanReporter spanReporter;

    public TraceThriftServerAdvice(Tracer tracer, TraceKeys traceKeys, HttpSpanExtractor httpSpanExtractor, SpanReporter spanReporter) {
        this.tracer = tracer;
        this.traceKeys = traceKeys;
        this.httpSpanExtractor = httpSpanExtractor;
        this.spanReporter = spanReporter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = null;
        Span span = null;
        Throwable te = null;
        Method method = invocation.getMethod();
        String className = method.getDeclaringClass().getName();
        className = className.substring(className.lastIndexOf(".") + 1);
        String spanName = className + "-" + method.getName();
        try {
            //创建span
            Map<String, String> headers
                    = (Map<String, String>) RequestContexts.getCurrentContext().getContextData(AurothConstants.REQUEST_HEADERS);
            if (headers == null) {
                headers = new HashMap<>();
            }
            headers.put("X-Span-Uri", spanName);
            span = createSpan(headers, spanName);
            //服务端方法执行
            result = invocation.proceed();
        } catch (Throwable e) {
            this.tracer.addTag(Span.SPAN_ERROR_TAG_NAME, ExceptionUtils.getExceptionMessage(e));
            te = new TException(e);
            throw e;
        }  finally {
            span = createSpanIfRequestNotHandled(span, spanName);
            detachOrCloseSpans(span, te);
        }

        return result;
    }

    /**
     * create span.
     *
     * @param headers headers
     * @param name    name
     * @return new span
     */
    private Span createSpan(Map<String, String> headers, String name) {
        Span span;
        Span parent = null;

        if (headers != null) {
            parent = this.httpSpanExtractor.joinTrace(new ThriftRequestMap(headers));
        }

        if (parent != null) {
            span = parent;
            this.tracer.continueSpan(span);
            if (parent.isRemote()) {
                parent.logEvent(Span.SERVER_RECV);
            }
        } else {
            span = this.tracer.createSpan(name);
            span.logEvent(Span.SERVER_RECV);
        }
        RequestContexts.getCurrentContext().setContextData(TRACE_REQUEST_ATTR, span);

        return span;
    }

    /**
     * create span
     *
     * 这里的判断需要依靠aop中的span执行
     *
     * @see org.springframework.cloud.sleuth.instrument.web.TraceHandlerInterceptor
     *
     * @param spanFromRequest span
     * @param name name
     * @return span
     */
    private Span createSpanIfRequestNotHandled(Span spanFromRequest, String name) {
        /*RequestContext currentContext = RequestContexts.getCurrentContext();
        if (currentContext.getContextData(TraceRequestAttributes.HANDLED_SPAN_REQUEST_ATTR) == null) {
            spanFromRequest = this.tracer.createSpan(name);
            currentContext.setContextData(TRACE_REQUEST_ATTR, spanFromRequest);
        }*/

        return spanFromRequest;
    }

    /**
     * span close
     *
     * @param spanFromRequest span
     * @param e exception
     */
    private void detachOrCloseSpans(Span spanFromRequest, Throwable e) {
        Span span = spanFromRequest;
        if (span != null) {
            if (e == null) {
                this.tracer.addTag(this.traceKeys.getHttp().getStatusCode(), "200");
            } else {
                this.tracer.addTag(this.traceKeys.getHttp().getStatusCode(), "500");
            }

            if (span.hasSavedSpan() && TraceRequestAttributes.HANDLED_SPAN_REQUEST_ATTR != null) {
                Span parent = span.getSavedSpan();
                recodeParentSpan(parent);
            } else if (TraceRequestAttributes.HANDLED_SPAN_REQUEST_ATTR == null){
                span = this.tracer.close(span);
            }

            recodeParentSpan(span);
            this.tracer.close(span);
        }
    }

    private void recodeParentSpan(Span parent) {
        if (parent.isRemote()) {
            parent.stop();
            boolean match = parent.logs().stream().anyMatch(log -> Span.SERVER_SEND.equals(log.getEvent()));
            if (!match) {
                parent.logEvent(Span.SERVER_SEND);
            }

            this.spanReporter.report(parent);
        } else {
            boolean match = parent.logs().stream().anyMatch(log -> Span.SERVER_SEND.equals(log.getEvent()));
            if (!match) {
                parent.logEvent(Span.SERVER_SEND);
            }
        }
    }
}
