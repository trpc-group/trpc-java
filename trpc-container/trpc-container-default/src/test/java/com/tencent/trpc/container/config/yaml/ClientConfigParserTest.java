package com.tencent.trpc.container.config.yaml;

import com.tencent.trpc.container.config.YamlUtils;
import com.tencent.trpc.core.common.config.ClientConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.utils.YamlParser;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ClientConfigParserTest{

    @Test
    public void testParseClientConfig() {
        ClientConfigParser clientConfigParser = new ClientConfigParser();
        Assert.assertNotNull(clientConfigParser);
        Map<String, Object> yamlConfigMap = YamlParser.parseAsFromClassPath("listener_default.yaml", Map.class);
        YamlUtils yamlUtils = new YamlUtils("Label[]");
        ClientConfig clientConfig = ClientConfigParser.parseClientConfig(yamlUtils.getMap(yamlConfigMap, ConfigConstants.CLIENT));
        Assert.assertNotNull(clientConfig.getNamespace());
    }
}