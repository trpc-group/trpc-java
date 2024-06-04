package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import java.util.Map;

public class GlobalConfigParserTest extends TestCase {

    @Test
    public void testParseGlobalConfig() {
        GlobalConfigParser globalConfigParser = new GlobalConfigParser();
        Assert.assertNotNull(globalConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        Map<String, Object> map = yamlUtils.getMap(yamlConfigMap, ConfigConstants.GLOBAL);
        GlobalConfig globalConfig = GlobalConfigParser.parseGlobalConfig(map);
        Assert.assertNotNull(globalConfig.getNamespace());
        Assert.assertNotNull(globalConfig.getEnvName());
        GlobalConfigParser.parseGlobalConfig(null);
    }
}