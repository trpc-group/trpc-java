/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 Tencent. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.limiter.sentinel.config.datasource;

import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.FileInJarReadableDataSource;
import com.alibaba.csp.sentinel.datasource.FileRefreshableDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.trpc.core.exception.LimiterDataSourceException;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Configuration class for using a local file as the flow control rule data source.
 */
public class LocalFileDatasourceConfig extends AbstractDatasourceConfig {

    private static final String CLASSPATH_PREFIX = "classpath:";
    private static final String JAR_PREFIX = "jar:";
    private static final String FILE_PREFIX = "file:";

    /**
     * Local file path.
     */
    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    protected void validate() {
        if (StringUtils.isEmpty(path)) {
            throw new LimiterDataSourceException("sentinel local file path cannot be empty");
        }
    }

    @Override
    public void register() {
        super.register();
        logger.info("start to register local file as sentinel flow rule data source, path = {}", path);
        AbstractDataSource<String, List<FlowRule>> dataSource = null;
        Converter<String, List<FlowRule>> converter = source -> JsonUtils.fromJson(source,
                new TypeReference<List<FlowRule>>() {
                });
        String externalPath = path;
        try {
            // Supports classpath format paths and absolute paths
            if (externalPath.startsWith(CLASSPATH_PREFIX)) {
                externalPath = externalPath.substring(CLASSPATH_PREFIX.length());
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                URL resource = classLoader.getResource(externalPath);
                if (resource != null) {
                    externalPath = resource.toExternalForm();
                    if (externalPath.startsWith(JAR_PREFIX)) {
                        // in jar
                        JarURLConnection conn = (JarURLConnection) resource.openConnection();
                        dataSource = new FileInJarReadableDataSource<>(conn.getJarFileURL().getPath(),
                                conn.getEntryName(), converter);
                    } else if (externalPath.startsWith(FILE_PREFIX)) {
                        // in file
                        externalPath = externalPath.substring(FILE_PREFIX.length());
                    }
                }
            }

            if (dataSource == null) {
                File file = new File(externalPath);
                if (file.exists()) {
                    dataSource = new FileRefreshableDataSource<>(file, converter);
                }
            }
        } catch (IOException e) {
            logger.error(
                    "failed to register local file as sentinel flow rule datasource, flowRulePath = {}, error = {}",
                    path, e);
            throw new LimiterDataSourceException(e);
        }
        if (dataSource == null) {
            throw new LimiterDataSourceException("file path=" + path + " not exists");
        }
        FlowRuleManager.register2Property(dataSource.getProperty());
        logger.info("succeed to register local file as sentinel flow rule data source");
    }

}
