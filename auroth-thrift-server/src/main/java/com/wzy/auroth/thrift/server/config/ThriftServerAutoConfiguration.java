package com.wzy.auroth.thrift.server.config;

import com.wzy.auroth.service.discovery.ServerInfo;
import com.wzy.auroth.thrift.annotation.TService;
import com.wzy.auroth.thrift.server.DefaultNiftyConfigurer;
import com.wzy.auroth.thrift.server.NiftyConfigurer;
import com.wzy.auroth.thrift.server.ThriftServerRunner;
import com.wzy.auroth.thrift.server.advice.DefaultServerAdvice;
import com.wzy.auroth.thrift.server.advice.ExceptionServerAdvice;
import com.wzy.auroth.thrift.server.advice.ServerAcceptMetricsAdvice;
import com.wzy.auroth.thrift.server.advice.ThriftServerAdvice;
import com.wzy.auroth.thrift.server.advice.TraceThriftServerAdvice;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanExtractor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Configuration
@ConditionalOnClass(TService.class)
@AutoConfigureAfter({ThriftServerAutoConfiguration.TracerConfgiration.class,
        ThriftServerAutoConfiguration.MetricsConfiguration.class})
@EnableConfigurationProperties(ThriftServerProperties.class)
public class ThriftServerAutoConfiguration {

    private final ThriftServerProperties serverProperties;

    @Autowired
    public ThriftServerAutoConfiguration(ThriftServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }



    @Bean
    @ConditionalOnMissingBean
    public NiftyConfigurer niftyConfigurer(ThriftServerAdvice[] thriftAdvices) {
        DefaultNiftyConfigurer niftyConfigurer = new DefaultNiftyConfigurer();
        for (ThriftServerAdvice thriftServerAdvice : thriftAdvices) {
            niftyConfigurer.addAdvice(thriftServerAdvice);
        }
        return niftyConfigurer;
    }

    @Bean
    public ThriftServerRunner thriftServerRunner(ApplicationContext applicationContext,
                                                 TProtocolFactory protocolFactory,
                                                 NiftyConfigurer niftyConfigurer) {
        ThriftServerRunner serverRunner = new ThriftServerRunner();
        serverRunner.setApplicationContext(applicationContext);
        serverRunner.setServerProperties(serverProperties);
        serverRunner.setProtocolFactory(protocolFactory);
        serverRunner.setNiftyConfigurer(niftyConfigurer);
        return serverRunner;
    }

    @Bean
    @ConditionalOnMissingBean
    public InetUtils inetUtils() {
        return new InetUtils(new InetUtilsProperties());
    }

    @Bean
    @ConditionalOnClass(ServerInfo.class)
    public ServerInfo serverInfo(InetUtils inetUtils) {
        return new ServerInfo() {
            @Override
            public boolean isRunning() {
                Socket socket = new Socket();

                try {
                    socket.connect(new InetSocketAddress(getHost(), getPort()));
                } catch (IOException e) {
                    return false;
                } finally {
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
                return true;
            }

            @Override
            public String getHost() {
                return inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
            }

            @Override
            public int getPort() {
                return serverProperties.getPort();
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ThriftServerAdvice thriftServerAdvice() {
        return new DefaultServerAdvice();
    }

    @Bean
    @Order(Integer.MIN_VALUE + 5)
    public ExceptionServerAdvice exceptionServerAdvice() {
        return new ExceptionServerAdvice();
    }

    @Configuration
    @ConditionalOnClass(Tracer.class)
    @AutoConfigureAfter(TraceAutoConfiguration.class)
    static class TracerConfgiration {

        @Bean
        @Order(Integer.MIN_VALUE + 100)
        public TraceThriftServerAdvice thriftTraceAdvice(Tracer tracer, TraceKeys traceKeys,
                                                         HttpSpanExtractor httpSpanExtractor, SpanReporter spanReporter) {
            return new TraceThriftServerAdvice(tracer, traceKeys, httpSpanExtractor, spanReporter);
        }
    }

    @Configuration
    @ConditionalOnClass(CounterService.class)
    @AutoConfigureAfter(MetricRepositoryAutoConfiguration.class)
    static class MetricsConfiguration {

        @Bean
        @Order(Integer.MIN_VALUE + 98)
        public ServerAcceptMetricsAdvice serverAcceptMetricsAdvice(CounterService counterService, GaugeService gaugeService) {
            return new ServerAcceptMetricsAdvice(counterService, gaugeService);
        }
    }
}
