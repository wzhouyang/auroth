package com.wzy.auroth.service.discovery.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.wzy.auroth.core.AurothConstants;
import com.wzy.auroth.service.discovery.ServerInfo;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties;
import org.springframework.cloud.consul.discovery.HeartbeatProperties;
import org.springframework.cloud.consul.discovery.TtlScheduler;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.util.StringUtils;

public class TConsulServiceRegistry extends ConsulServiceRegistry {

    private final ConsulClient client;

    private final ConsulDiscoveryProperties properties;

    private final ServerInfo serverInfo;

    public TConsulServiceRegistry(ConsulClient client, ConsulDiscoveryProperties properties, TtlScheduler ttlScheduler, HeartbeatProperties heartbeatProperties, ServerInfo serverInfo) {
        super(client, properties, ttlScheduler, heartbeatProperties);
        this.client = client;
        this.properties = properties;
        this.serverInfo = serverInfo;
    }

    @Override
    public void register(ConsulRegistration reg) {
        reg.getService().getTags().add(AurothConstants.TCP_PORT + "=" + serverInfo.getPort());
        super.register(reg);
        NewCheck check = new NewCheck();
        check.setId(reg.getInstanceId());
        check.setName(reg.getServiceId());
        check.setServiceId(reg.getInstanceId());
        String healthCheckInterval = properties.getHealthCheckInterval();
        if (StringUtils.isEmpty(healthCheckInterval)) {
            healthCheckInterval = "10s";
        }
        check.setInterval(healthCheckInterval);
        String healthCheckTimeout = properties.getHealthCheckTimeout();
        if (StringUtils.isEmpty(healthCheckTimeout)) {
            healthCheckTimeout = healthCheckInterval;
        }
        check.setTimeout(healthCheckTimeout);
        if (!StringUtils.isEmpty(properties.getHealthCheckCriticalTimeout())) {
            check.setDeregisterCriticalServiceAfter(properties.getHealthCheckCriticalTimeout());
        }
        check.setTcp(serverInfo.getHost() + ":" + serverInfo.getPort());

        client.agentCheckRegister(check);
    }
}
