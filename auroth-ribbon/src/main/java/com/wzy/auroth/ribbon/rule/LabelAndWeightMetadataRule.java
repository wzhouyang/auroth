package com.wzy.auroth.ribbon.rule;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.wzy.auroth.ribbon.RibbonConstants;
import com.wzy.auroth.ribbon.label.LabelProvider;
import lombok.Setter;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LabelAndWeightMetadataRule extends ZoneAvoidanceRule {

    @Setter
    private LabelProvider labelProvider;

    @Setter
    private ServerIntrospector serverIntrospector;

    @Override
    public Server choose(Object key) {
        List<Server> serverList = this.getPredicate().getEligibleServers(this.getLoadBalancer().getAllServers(), key);
        if (CollectionUtils.isEmpty(serverList)) {
            return null;
        }

        if (labelProvider != null) {
            String label = labelProvider.getLabel();
            if (!StringUtils.isEmpty(label)) {
                Server server = nextLabel(serverList, label);
                if (server != null) {
                    return server;
                }
            }
        }

        return nextWeighted(serverList);
    }

    private Server nextWeighted(List<Server> servers) {
        int total = 0;
        IServer best = null;
        IServer iServer;
        for (Server server : servers) {
            if (RibbonConstants.SERVERS.containsKey(server.getId())) {
                iServer = RibbonConstants.SERVERS.get(server.getId());
            } else {
                iServer = new IServer(server, getMeta(server));
                RibbonConstants.SERVERS.put(server.getId(), iServer);
            }

            iServer.currentWeight += iServer.effectiveWeight;
            total += iServer.effectiveWeight;

            if (iServer.effectiveWeight < iServer.weight) {
                iServer.effectiveWeight++;
            }

            if (best == null || iServer.currentWeight > best.currentWeight) {
                best = iServer;
            }

        }

        if (best == null) {
            return null;
        }
        best.currentWeight -= total;
        return best.server;
    }

    private Map<String, String> getMeta(Server server) {
        return serverIntrospector.getMetadata(server);
    }

    private Server nextLabel(List<Server> servers, String label) {
        List<Server> collect = servers.stream().filter(server -> {
            Map<String, String> metaData = getMeta(server);
            return metaData != null &&
                    metaData.containsKey(RibbonConstants.LABEL) &&
                    metaData.get(RibbonConstants.LABEL).equals(label);

        }).collect(Collectors.toList());

        if (collect.size() == 1) {
            return collect.get(0);
        } else {
            return nextWeighted(collect);
        }
    }

    public class IServer {
        public String ip;
        public int weight;
        public int effectiveWeight;
        public int currentWeight;
        public Server server;

        public IServer(Server server, Map<String, String> metaInfo) {
            init(server, metaInfo);
        }

        private void init(Server server, Map<String, String> metaInfo) {
            this.server = server;
            this.ip = server.getHost();
            this.weight = getWeight(metaInfo);
            this.effectiveWeight = this.weight;
            this.currentWeight = 0;
        }

        public IServer setServer(Server server) {
            this.server = server;
            return this;
        }

        public IServer setMetaInfo(Map<String, String> metaInfo) {
            this.weight = getWeight(metaInfo);
            return this;
        }

        private int getWeight(Map<String, String> metadata) {
            if (metadata != null && metadata.containsKey(RibbonConstants.WEIGHT)) {
                try {
                    return Integer.parseInt(metadata.get(RibbonConstants.WEIGHT));
                } catch (NumberFormatException e) {
                    LoggerFactory.getLogger(LabelAndWeightMetadataRule.class).warn("{}:{}:{}", server.getId(), server
                            .getHost(), server.getPort());
                }
            }
            return RibbonConstants.DEFAULT_WEIGHT;
        }

        @Override
        public String toString() {
            return "IServer{" +
                    "ip='" + ip + '\'' +
                    ", weight=" + weight +
                    ", effectiveWeight=" + effectiveWeight +
                    ", currentWeight=" + currentWeight +
                    ", server=" + server +
                    '}';
        }
    }
}
