package com.wzy.auroth.service.discovery;

import com.facebook.nifty.core.NiftyBootstrap;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.wzy.auroth.service.discovery.eureka.ThriftEurekaHealthIndicator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({EurekaClientConfig.class, NiftyBootstrap.class})
@ConditionalOnProperty(value = "eureka.client.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration")
public class EurekaAutoConfiguration {

    @Autowired
    private ServerInfo serverInfo;

    @Bean
    public ThriftEurekaHealthIndicator thriftEurekaHealthIndicator(EurekaClient eurekaClient,
                                                                   EurekaInstanceConfig instanceConfig,
                                                                   EurekaClientConfig clientConfig) {
        return new ThriftEurekaHealthIndicator(eurekaClient, instanceConfig, clientConfig, serverInfo);
    }
}
