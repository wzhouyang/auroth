package com.wzy.auroth.ribbon;

import com.wzy.auroth.ribbon.rule.LabelAndWeightMetadataRule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface RibbonConstants {

    String SPAN_LABEL = "X-label";

    String WEIGHT = "weight";

    String LABEL = "label";

    int DEFAULT_WEIGHT = 5;

    Map<String, LabelAndWeightMetadataRule.IServer> SERVERS = new ConcurrentHashMap<>();
}
