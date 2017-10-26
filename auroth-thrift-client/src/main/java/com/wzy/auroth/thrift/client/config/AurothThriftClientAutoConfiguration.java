package com.wzy.auroth.thrift.client.config;

import com.wzy.auroth.sleuth.ThriftSpanInjector;
import com.wzy.auroth.thrift.client.AurothClientPostProcessor;
import com.wzy.auroth.thrift.client.advice.ClientRequestMetricsAdvice;
import com.wzy.auroth.thrift.client.advice.DefaultClientAdvice;
import com.wzy.auroth.thrift.client.advice.LocalRequestVariableAdvice;
import com.wzy.auroth.thrift.client.advice.RetryAdvice;
import com.wzy.auroth.thrift.client.advice.TThriftClientAdvice;
import com.wzy.auroth.thrift.client.advice.ThriftClientAdvice;
import com.wzy.auroth.thrift.client.advice.TraceThriftClientAdvice;
import com.wzy.auroth.thrift.client.concurrency.DefaultReferenceMetaProvider;
import com.wzy.auroth.thrift.client.concurrency.ReferenceMetaProvider;
import com.wzy.auroth.thrift.client.pool.ThriftClientPool;
import com.wzy.auroth.thrift.client.pool.ThriftClientPooledObjectFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.RibbonClientSpecification;
import org.springframework.cloud.netflix.ribbon.RibbonLoadBalancerClient;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.web.HttpSpanInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ThriftClientProperties.class)
public class AurothThriftClientAutoConfiguration {

    @Autowired(required = false)
    private List<RibbonClientSpecification> configurations = new ArrayList<>();

    //==balance
    @Bean
    public SpringClientFactory springClientFactory() {
        SpringClientFactory factory = new SpringClientFactory();
        factory.setConfigurations(this.configurations);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(LoadBalancerClient.class)
    public RibbonLoadBalancerClient loadBalancerClient(SpringClientFactory springClientFactory) {
        return new RibbonLoadBalancerClient(springClientFactory);
    }

    //==pool
    @Bean
    @ConfigurationProperties(prefix = "thrift.client.pool")
    public GenericKeyedObjectPoolConfig genericKeyedObjectPoolConfig() {
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setJmxEnabled(false);
        return config;
    }

    @Bean(destroyMethod = "close")
    public ThriftClientPool thriftClientPool(ThriftClientPooledObjectFactory thriftClientPooledObjectFactory,
                                             GenericKeyedObjectPoolConfig config) {
        return new ThriftClientPool(thriftClientPooledObjectFactory, config);
    }

    @Bean
    public ThriftClientPooledObjectFactory thriftClientPooledObjectFactory(ThriftClientProperties clientProperties,
                                                                           TProtocolFactory protocolFactory) {
        return new ThriftClientPooledObjectFactory(clientProperties, protocolFactory);
    }

    @Bean
    public ThriftSpanInjector thriftSpanInjector(HttpSpanInjector httpSpanInjector) {
        ThriftSpanInjector thriftSpanInjector = new ThriftSpanInjector();
        thriftSpanInjector.setHttpSpanInjector(httpSpanInjector);
        return thriftSpanInjector;
    }

    @Bean
    @ConditionalOnMissingBean
    public ReferenceMetaProvider referenceMetaProvider() {
        return new DefaultReferenceMetaProvider();
    }

    @Bean
    @Order(Integer.MAX_VALUE - 1)
    public TThriftClientAdvice thriftMethodInterceptor(LoadBalancerClient loadBalancerClient,
                                                       ThriftClientPool thriftClientPool,
                                                       ReferenceMetaProvider referenceMetaProvider) {
        TThriftClientAdvice thriftClientAdvice = new TThriftClientAdvice();
        thriftClientAdvice.setLoadBalancerClient(loadBalancerClient);
        thriftClientAdvice.setThriftClientPool(thriftClientPool);
        thriftClientAdvice.setReferenceMetaProvider(referenceMetaProvider);
        return thriftClientAdvice;
    }

    @Bean
    public AurothClientPostProcessor aurothClientPostProcessor(DefaultListableBeanFactory defaultListableBeanFactory,
                                                               ThriftClientAdvice[] advisors) {
        AurothClientPostProcessor aurothClientPostProcessor = new AurothClientPostProcessor();
        aurothClientPostProcessor.setBeanFactory(defaultListableBeanFactory);
        aurothClientPostProcessor.setAdvices(advisors);
        return aurothClientPostProcessor;
    }

    @Bean
    @ConditionalOnMissingBean
    public ThriftClientAdvice thriftClientAdvice() {
        return new DefaultClientAdvice();
    }


    @Configuration
    static class LocalConfiguration {
        @Bean
        @ConditionalOnMissingBean
        @Order(Integer.MIN_VALUE + 100)
        public LocalRequestVariableAdvice requestVariableAdvice() {
            return new LocalRequestVariableAdvice();
        }
    }

    @ConditionalOnClass(Tracer.class)
    @AutoConfigureAfter(TraceAutoConfiguration.class)
    static class TracerConfiguration {
        @Autowired
        private Tracer tracer;

        @Bean
        @Order(Integer.MAX_VALUE - 100)
        public TraceThriftClientAdvice traceMethodInterceptor(TraceKeys traceKeys, ThriftSpanInjector spanInjector) {
            return new TraceThriftClientAdvice(tracer, traceKeys, spanInjector);
        }
    }

    @Configuration
    @ConditionalOnClass({RetryTemplate.class})
    static class RetryConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public RetryPolicy retryPolicy() {
            return new NeverRetryPolicy();
        }

        @Bean
        @Order(Integer.MIN_VALUE + 10)
        @ConditionalOnMissingBean
        public RetryAdvice retryAdvice(RetryPolicy retryPolicy) {
            RetryOperationsInterceptor interceptor = RetryInterceptorBuilder.stateless()
                    .label("thriftClientRetry")
                    .retryPolicy(retryPolicy)
                    .build();

            return new RetryAdvice(interceptor);
        }
    }

    @Configuration
    @ConditionalOnBean(CounterService.class)
    static class MetricsClientConfiguration {
        @Bean
        @Order(Integer.MIN_VALUE + 98)
        public ClientRequestMetricsAdvice clientRequestMetricsAdvice(CounterService counterService, GaugeService gaugeService) {
            return new ClientRequestMetricsAdvice(counterService, gaugeService);
        }

    }
}
