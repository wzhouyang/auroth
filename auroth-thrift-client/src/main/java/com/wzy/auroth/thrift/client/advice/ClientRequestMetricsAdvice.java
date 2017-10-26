package com.wzy.auroth.thrift.client.advice;

import com.wzy.auroth.metrics.RequestMetricsAdvice;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

public class ClientRequestMetricsAdvice extends RequestMetricsAdvice implements ThriftClientAdvice {

    public ClientRequestMetricsAdvice(CounterService counterService, GaugeService gaugeService) {
        super(counterService, gaugeService);
    }
}
