/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.admin;

import com.tencent.trpc.admin.custom.ShellUtils;
import com.tencent.trpc.admin.custom.TestDto;
import com.tencent.trpc.admin.dto.CommandDto;
import com.tencent.trpc.admin.dto.CommonDto;
import com.tencent.trpc.admin.dto.ConfigOverviewDto;
import com.tencent.trpc.admin.dto.LoggerLevelDto;
import com.tencent.trpc.admin.dto.LoggerLevelRevisedDto;
import com.tencent.trpc.admin.dto.VersionDto;
import com.tencent.trpc.admin.dto.rpc.RpcStatsDto;
import com.tencent.trpc.core.common.ConfigManager;
import com.tencent.trpc.core.common.Version;
import com.tencent.trpc.core.common.config.AdminConfig;
import com.tencent.trpc.core.utils.JsonUtils;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("admin")
@SpringBootTest(classes = TrpcServerApplication.class)
public class AdminIntegrationTest {

    private String adminAddress;

    @Before
    public void before() {
        final AdminConfig adminConfig = ConfigManager.getInstance().getServerConfig().getAdminConfig();
        adminAddress = new StringBuffer(adminConfig.getAdminIp()).append(":").append(adminConfig.getAdminPort())
                .toString();
    }

    @Test
    public void testVersion() {
        String[] cmd = {"curl", "http://" + adminAddress + "/version"};
        String versionInfo = ShellUtils.execute(cmd);
        VersionDto versionDto = JsonUtils.fromJson(versionInfo, VersionDto.class);
        Assert.assertEquals(Version.version(), versionDto.getVersion());
    }

    @Test
    public void testUpdateLoglevel() {
        //修改
        String[] set2DebugCmd = {"curl", "-XPUT", "http://" + adminAddress + "/cmds/loglevel/ROOT", "-d",
                "value=DEBUG"};
        String set2DebugResult = ShellUtils.execute(set2DebugCmd);
        System.out.println(set2DebugResult);
        final LoggerLevelRevisedDto levelDebugRevisedDto = JsonUtils.fromJson(set2DebugResult,
                LoggerLevelRevisedDto.class);

        Assert.assertEquals(CommonDto.SUCCESS, levelDebugRevisedDto.getErrorcode());
        Assert.assertEquals("DEBUG", (levelDebugRevisedDto.getLevel()));

        //查看修改后的结果
        String[] logLevelCmd = {"curl", "http://" + adminAddress + "/cmds/loglevel"};
        String logLevelInfo = ShellUtils.execute(logLevelCmd);
        LoggerLevelDto dto = JsonUtils.fromJson(logLevelInfo, LoggerLevelDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, dto.getErrorcode());
        Assert.assertTrue(dto.getLogger().stream()
                .anyMatch(log -> "ROOT".equals(log.getLoggerName()) && "DEBUG".equals(log.getLevel())));

        //修改
        String[] set2InfoCmd = {"curl", "-XPUT", "http://" + adminAddress + "/cmds/loglevel/ROOT", "-d", "value=INFO"};
        String set2InfoResult = ShellUtils.execute(set2InfoCmd);
        LoggerLevelRevisedDto levelInfoRevisedDto = JsonUtils.fromJson(set2InfoResult,
                LoggerLevelRevisedDto.class);

        Assert.assertEquals(CommonDto.SUCCESS, levelInfoRevisedDto.getErrorcode());
        Assert.assertEquals("INFO", levelInfoRevisedDto.getLevel());
    }

    @Test
    public void testLogLevel() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds/loglevel"};
        String logLevelInfo = ShellUtils.execute(cmd);
        LoggerLevelDto dto = JsonUtils.fromJson(logLevelInfo, LoggerLevelDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, dto.getErrorcode());
        Assert.assertTrue(dto.getLogger().stream()
                .anyMatch(log -> "ROOT".equals(log.getLoggerName()) && "INFO".equals(log.getLevel())));
    }

    @Test
    public void testWorkerPool() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds/workerpool/info"};
        String workPoolInfo = ShellUtils.execute(cmd);
        Assert.assertTrue("{\"errorcode\":\"0\",\"message\":\"\",\"workerPoolInfo\":{}}".equals(workPoolInfo));
    }

    @Test
    public void testConfig() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds/config"};
        String configInfo = ShellUtils.execute(cmd);
        ConfigOverviewDto configOverviewDto = JsonUtils.fromJson(configInfo, ConfigOverviewDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, configOverviewDto.getErrorcode());
        Assert.assertEquals("integration-test-admin", configOverviewDto.getContent().getServer().getApp());
    }

    @Test
    public void testStatRpc() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds/stats/rpc"};
        String rpcStats = ShellUtils.execute(cmd);
        RpcStatsDto rpcStatsDto = JsonUtils.fromJson(rpcStats, RpcStatsDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, rpcStatsDto.getErrorcode());
        Assert.assertEquals(Version.version(), rpcStatsDto.getRpcVersion());
        Assert.assertEquals(0, rpcStatsDto.getRpcServiceCount().intValue());
    }

    @Test
    public void testCustomAdmin() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds/test"};
        String testInfo = ShellUtils.execute(cmd);
        TestDto rpcStatsDto = JsonUtils.fromJson(testInfo, TestDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, rpcStatsDto.getErrorcode());
        Assert.assertEquals("hello world!", rpcStatsDto.getTestResult());
    }

    @Test
    public void testCmds() {
        String[] cmd = {"curl", "http://" + adminAddress + "/cmds"};
        String cmdResult = ShellUtils.execute(cmd);
        CommandDto commandDto = JsonUtils.fromJson(cmdResult, CommandDto.class);
        Assert.assertEquals(CommonDto.SUCCESS, commandDto.getErrorcode());
        List<String> commands = commandDto.getCmds();
        Assert.assertTrue(commands.contains("/cmds/loglevel") && commands.contains("/cmds/loglevel/{logname}")
                && commands.contains("/version") && commands.contains("/cmds") && commands.contains("/cmds/config")
                && commands.contains("/cmds/stats/rpc") && commands.contains("/cmds/workerpool/info")
                && commands.contains("/cmds/test"));
    }
}
