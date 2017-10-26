package com.wzy.auroth.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * metrics
 */
public class RequestMetricsAdvice implements MethodInterceptor {

    private static final MetricRegistry metrics = new MetricRegistry();
    private static ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).build();
    private static final Meter requests = metrics.meter("request");

    private final CounterService counterService;

    private final GaugeService gaugeService;

    public RequestMetricsAdvice(CounterService counterService, GaugeService gaugeService) {
        this.counterService = counterService;
        this.gaugeService = gaugeService;
        reporter.start(5, TimeUnit.SECONDS);
    }

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        String className = method.getDeclaringClass().getName();
        String simpleName = className.substring(className.lastIndexOf(".") + 1);
        Object result;
        try {
            result = methodInvocation.proceed();
            requests.mark();
            String metricsName = String.join(".", "meter.thrift", simpleName, method.getName(), "ok");
            counterService.increment(metricsName);
        } catch (Throwable e) {
            String metricsName = String.join(".", "thrift", simpleName, method.getName(), "error");
            counterService.increment(metricsName);
            throw e;
        }
        return result;
    }
}
