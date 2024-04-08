package com.tencent.trpc.core.utils;

import com.tencent.trpc.core.common.TRpcSystemProperties;
import com.tencent.trpc.core.common.config.GlobalConfig;
import com.tencent.trpc.core.common.config.constant.ConfigConstants;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.FileInputStream;
import java.util.Map;


/**
 * @author jiaxxzhang
 * @project trpc-java
 * @description 静态加载配置工具类
 * @date 2024/4/3 14:20:14
 */
public class ConfigUtils {

    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

    public static final String DEFAULT_YAML_CONFIG_FILE_NAME = "trpc_java.yaml";

    /**
     * 静态加载全局配置
     *
     * @return
     */
    public static GlobalConfig loadGlobalConfig() {
        String confPath = TRpcSystemProperties.getProperties(TRpcSystemProperties.CONFIG_PATH);
        Map<String, Object> config = null;
        try {
            if (StringUtils.isEmpty(confPath)) {
                logger.warn("warning!!, not set properties [" + TRpcSystemProperties.CONFIG_PATH
                        + "], we will use classpath:" + DEFAULT_YAML_CONFIG_FILE_NAME + "");
                if (ObjectUtils.isNotEmpty(YamlParser.class.getClassLoader().getResourceAsStream(DEFAULT_YAML_CONFIG_FILE_NAME))){
                    config = (Map<String, Object>) YamlParser.parseAsFromClassPath(DEFAULT_YAML_CONFIG_FILE_NAME, Map.class).get(ConfigConstants.GLOBAL);
                }
            } else {
                config = (Map<String, Object>) YamlParser.parseAs(new FileInputStream(confPath), Map.class).get(ConfigConstants.GLOBAL);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        GlobalConfig globalConfig = new GlobalConfig();
        if(MapUtils.isNotEmpty(config)){
            BinderUtils.bind(globalConfig, config);
            BinderUtils.bind(BinderUtils.UNDERSCORES_TO_UPPERCASE, globalConfig, config,
                    ConfigConstants.ENABLE_SET, o -> "Y".equalsIgnoreCase(String.valueOf(o)));
        }
        return globalConfig;
    }
}
