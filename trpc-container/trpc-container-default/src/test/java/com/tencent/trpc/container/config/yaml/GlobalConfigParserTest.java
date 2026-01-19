package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.util.Map;

public class GlobalConfigParserTest {

    @Test
    public void testParseGlobalConfig() {
        GlobalConfigParser globalConfigParser = new GlobalConfigParser();
        Assertions.assertNotNull(globalConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> map = yamlUtils.getMap(yamlConfigMap, ConfigConstants.GLOBAL);
        GlobalConfig globalConfig = GlobalConfigParser.parseGlobalConfig(map);
        Assertions.assertNotNull(globalConfig.getNamespace());
        Assertions.assertNotNull(globalConfig.getEnvName());
        GlobalConfigParser.parseGlobalConfig(null);
    }
}
