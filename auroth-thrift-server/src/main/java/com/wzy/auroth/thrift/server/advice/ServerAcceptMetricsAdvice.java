package com.wzy.auroth.thrift.server.advice;

import com.wzy.auroth.metrics.RequestMetricsAdvice;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

public class ServerAcceptMetricsAdvice extends RequestMetricsAdvice implements ThriftServerAdvice {
    public ServerAcceptMetricsAdvice(CounterService counterService, GaugeService gaugeService) {
        super(counterService, gaugeService);
    }
}
