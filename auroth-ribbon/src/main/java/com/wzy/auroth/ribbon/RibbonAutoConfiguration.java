package com.wzy.auroth.ribbon;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.IRule;
import com.netflix.loadbalancer.Server;
import com.wzy.auroth.ribbon.label.DefaultLabelProvider;
import com.wzy.auroth.ribbon.label.LabelProvider;
import com.wzy.auroth.ribbon.rule.LabelAndWeightMetadataRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.discovery.ConsulServer;
import org.springframework.cloud.netflix.ribbon.DefaultServerIntrospector;
import org.springframework.cloud.netflix.ribbon.PropertiesFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RibbonAutoConfiguration {
    @Value("${ribbon.client.name:#{null}}")
    private String name;

    @Autowired(required = false)
    private IClientConfig config;

    @Autowired
    private PropertiesFactory propertiesFactory;

    @Autowired(required = false)
    private LabelProvider labelProvider;

    @Bean
    public IRule ribbonRule(ServerIntrospector serverIntrospector) {
        if (this.propertiesFactory.isSet(IRule.class, name)) {
            return this.propertiesFactory.get(IRule.class, config, name);
        }

        // 默认配置
        LabelAndWeightMetadataRule rule = new LabelAndWeightMetadataRule();
        rule.setLabelProvider(labelProvider);
        rule.setServerIntrospector(serverIntrospector);
        rule.initWithNiwsConfig(config);
        return rule;
    }

    @Configuration
    @ConditionalOnClass(Tracer.class)
    static class LabelConfiguration {

        @Autowired(required = false)
        private Tracer tracer;

        @Bean
        public LabelProvider labelProvider() {
            DefaultLabelProvider labelProvider = new DefaultLabelProvider();
            labelProvider.setTracer(tracer);
            return labelProvider;
        }
    }

    @Configuration
    @ConditionalOnConsulEnabled
    static class ConsulServerIntrospectorCongiguration {
        @Bean
        public ServerIntrospector serverIntrospector() {
            return new DefaultServerIntrospector() {
                @Override
                public Map<String, String> getMetadata(Server server) {
                    if (server instanceof ConsulServer) {
                        return ((ConsulServer) server).getMetadata();
                    }

                    return super.getMetadata(server);
                }
            };
        }
    }
}
