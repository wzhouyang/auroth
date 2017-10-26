package com.wzy.auroth.service.discovery;

import com.ecwid.consul.v1.ConsulClient;
import com.wzy.auroth.service.discovery.consul.TConsulServiceRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnConsulEnabled
public class ConsulAutoConfiguration {

    @Autowired(required = false)
    private TtlScheduler ttlScheduler;

    @Bean
    public TConsulServiceRegistry consulServiceRegistry(ConsulClient client,
                                                        ConsulDiscoveryProperties properties,
                                                        HeartbeatProperties heartbeatProperties,
                                                        ServerInfo serverInfo) {
        return new TConsulServiceRegistry(client, properties, ttlScheduler, heartbeatProperties, serverInfo);
    }
}
