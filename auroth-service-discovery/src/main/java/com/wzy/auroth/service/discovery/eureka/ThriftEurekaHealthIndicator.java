package com.wzy.auroth.service.discovery.eureka;

import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.wzy.auroth.service.discovery.ServerInfo;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ThriftEurekaHealthIndicator implements DiscoveryHealthIndicator {

    private final EurekaClient eurekaClient;

    private final EurekaInstanceConfig instanceConfig;

    private final EurekaClientConfig clientConfig;

    private final ServerInfo serverInfo;

    public ThriftEurekaHealthIndicator(EurekaClient eurekaClient, EurekaInstanceConfig instanceConfig,
                                       EurekaClientConfig clientConfig, ServerInfo serverInfo) {
        this.eurekaClient = eurekaClient;
        this.instanceConfig = instanceConfig;
        this.clientConfig = clientConfig;
        this.serverInfo = serverInfo;
    }

    @Override
    public String getName() {
        return "thriftDiscoveryClient";
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.unknown();
        Status status = getStatus(builder);
        return builder.status(status).withDetail("applications", getApplications()).build();
    }

    private Status getStatus(Health.Builder builder) {
        Status status;
        //检测服务器是否ok
        boolean running = serverInfo.isRunning();
        if (running) {
            status = new Status("UP", "thrift server is up");
        } else {
            status = new Status("DOWN", "thrift server is down");
        }

        return status;
    }

    private Map<String, Object> getApplications() {
        Applications applications = this.eurekaClient.getApplications();
        if (applications == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> result = new HashMap<>();
        for (Application application : applications.getRegisteredApplications()) {
            result.put(application.getName(), application.getInstances().size());
        }
        return result;
    }
}
