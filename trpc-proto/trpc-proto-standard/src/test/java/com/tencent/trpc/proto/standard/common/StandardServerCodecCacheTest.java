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

package com.tencent.trpc.proto.standard.common;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.protobuf.ByteString;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.proto.standard.common.TRPCProtocol.RequestProtocol;
import com.tencent.trpc.transport.netty.NettyChannel;
import com.tencent.trpc.transport.netty.NettyChannelBuffer;
import io.netty.buffer.UnpooledByteBufAllocator;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the bounded decoding caches of {@link StandardServerCodec}, which protect the server from the memory
 * exhaustion attack: the cache keys(func/caller/callee) are fully controlled by the remote peer and are written
 * before the service/method existence check.
 */
public class StandardServerCodecCacheTest {

    private static final String FUNC_INFO_CACHE_FIELD = "FUNC_INFO_CACHE";

    private static final String CALL_INFO_CACHE_FIELD = "CALL_INFO_CACHE";

    private static final String CACHE_MAX_SIZE_FIELD = "CACHE_MAX_SIZE";

    private static final String CACHE_KEY_MAX_LENGTH_FIELD = "CACHE_KEY_MAX_LENGTH";

    private static final String GET_OR_COMPUTE_METHOD = "getOrCompute";

    private static final String PARSE_FUNC_METHOD = "parseFunc";

    private static final String SERVICE_NAME = "helloservice";

    private static final String METHOD_NAME = "sayHello";

    private static final String FUNC = "/" + SERVICE_NAME + "/" + METHOD_NAME;

    private static final String CALLER = "trpc.callerApp.callerServer.callerService";

    private static final String CALLEE = "trpc.calleeApp.calleeServer.calleeService.calleeMethod";

    private static final int BUFFER_SIZE = 65535;

    private static final int LOCAL_PORT = 125;

    private static final String LOCAL_IP = "127.0.0.1";

    private Cache<String, String[]> funcInfoCache;

    private Cache<String, CallInfo> callInfoCache;

    private int cacheMaxSize;

    private int cacheKeyMaxLength;

    @Before
    public void before() throws Exception {
        funcInfoCache = getStaticField(FUNC_INFO_CACHE_FIELD);
        callInfoCache = getStaticField(CALL_INFO_CACHE_FIELD);
        cacheMaxSize = getStaticField(CACHE_MAX_SIZE_FIELD);
        cacheKeyMaxLength = getStaticField(CACHE_KEY_MAX_LENGTH_FIELD);
        funcInfoCache.invalidateAll();
        callInfoCache.invalidateAll();
        funcInfoCache.cleanUp();
        callInfoCache.cleanUp();
    }

    @Test
    public void testParseFuncWithLegalFunc() throws Exception {
        String[] funcInfo = parseFunc(FUNC);
        Assert.assertEquals(SERVICE_NAME, funcInfo[0]);
        Assert.assertEquals(METHOD_NAME, funcInfo[1]);
        // only the last separator is used to split the service and the method
        String[] multiLevel = parseFunc("/a/b/c");
        Assert.assertEquals("a/b", multiLevel[0]);
        Assert.assertEquals("c", multiLevel[1]);
    }

    @Test
    public void testParseFuncWithIllegalFunc() throws Exception {
        String[] illegalFuncs = new String[]{StringUtils.EMPTY, "/", "//", "/abc", "abc", "/abc/"};
        for (String illegalFunc : illegalFuncs) {
            String[] funcInfo = parseFunc(illegalFunc);
            Assert.assertEquals(illegalFunc, StringUtils.EMPTY, funcInfo[0]);
            Assert.assertEquals(illegalFunc, StringUtils.EMPTY, funcInfo[1]);
        }
    }

    @Test
    public void testGetOrComputeReusesCachedValue() throws Exception {
        String[] first = getOrCompute(funcInfoCache, FUNC);
        String[] second = getOrCompute(funcInfoCache, FUNC);
        Assert.assertSame(first, second);
        Assert.assertEquals(1, funcInfoCache.estimatedSize());
        Assert.assertNotNull(funcInfoCache.getIfPresent(FUNC));
    }

    @Test
    public void testGetOrComputeCachesKeyOfMaxLength() throws Exception {
        String key = buildFunc(cacheKeyMaxLength);
        Assert.assertEquals(cacheKeyMaxLength, key.length());
        String[] value = getOrCompute(funcInfoCache, key);
        Assert.assertEquals(METHOD_NAME, value[1]);
        Assert.assertEquals(1, funcInfoCache.estimatedSize());
        Assert.assertSame(value, funcInfoCache.getIfPresent(key));
    }

    @Test
    public void testGetOrComputeNeverCachesOversizedKey() throws Exception {
        String key = buildFunc(cacheKeyMaxLength + 1);
        Assert.assertEquals(cacheKeyMaxLength + 1, key.length());
        String[] first = getOrCompute(funcInfoCache, key);
        String[] second = getOrCompute(funcInfoCache, key);
        // the value is still computed correctly, but it is never cached
        Assert.assertEquals(METHOD_NAME, first[1]);
        Assert.assertNotSame(first, second);
        Assert.assertNull(funcInfoCache.getIfPresent(key));
        Assert.assertEquals(0, funcInfoCache.estimatedSize());
    }

    @Test
    public void testCacheIsBoundedBySize() throws Exception {
        int total = cacheMaxSize * 2;
        for (int i = 0; i < total; i++) {
            getOrCompute(funcInfoCache, "/" + SERVICE_NAME + i + "/" + METHOD_NAME);
        }
        funcInfoCache.cleanUp();
        Assert.assertTrue("cache size should be bounded, but was " + funcInfoCache.estimatedSize(),
                funcInfoCache.estimatedSize() <= cacheMaxSize);
    }

    @Test
    public void testHotKeyIsKeptWhileCacheIsFlooded() throws Exception {
        String[] hot = getOrCompute(funcInfoCache, FUNC);
        // the hot key is accessed much more frequently than every flooding key
        for (int i = 0; i < cacheMaxSize * 2; i++) {
            getOrCompute(funcInfoCache, FUNC);
            getOrCompute(funcInfoCache, "/" + SERVICE_NAME + i + "/" + METHOD_NAME);
        }
        funcInfoCache.cleanUp();
        Assert.assertTrue(funcInfoCache.estimatedSize() <= cacheMaxSize);
        Assert.assertSame(hot, funcInfoCache.getIfPresent(FUNC));
    }

    @Test
    public void testDecodeParsesAndCachesFuncInfo() {
        Request request = decode(buildRequestHead(FUNC, CALLER, CALLEE));
        Assert.assertEquals(FUNC, request.getInvocation().getFunc());
        Assert.assertEquals(SERVICE_NAME, request.getInvocation().getRpcServiceName());
        Assert.assertEquals(METHOD_NAME, request.getInvocation().getRpcMethodName());
        Assert.assertNotNull(funcInfoCache.getIfPresent(FUNC));
        Assert.assertEquals(1, funcInfoCache.estimatedSize());
        // the second decoding of the same func reuses the cached entry
        Request another = decode(buildRequestHead(FUNC, CALLER, CALLEE));
        Assert.assertEquals(SERVICE_NAME, another.getInvocation().getRpcServiceName());
        Assert.assertEquals(1, funcInfoCache.estimatedSize());
    }

    @Test
    public void testDecodeParsesAndCachesCallInfo() {
        Request request = decode(buildRequestHead(FUNC, CALLER, CALLEE));
        CallInfo callInfo = request.getMeta().getCallInfo();
        Assert.assertEquals(CALLER, callInfo.getCaller());
        Assert.assertEquals("callerApp", callInfo.getCallerApp());
        Assert.assertEquals("callerServer", callInfo.getCallerServer());
        Assert.assertEquals("callerService", callInfo.getCallerService());
        Assert.assertEquals(CALLEE, callInfo.getCallee());
        Assert.assertEquals("calleeApp", callInfo.getCalleeApp());
        Assert.assertEquals("calleeServer", callInfo.getCalleeServer());
        Assert.assertEquals("calleeService", callInfo.getCalleeService());
        Assert.assertEquals("calleeMethod", callInfo.getCalleeMethod());
        Assert.assertEquals(1, callInfoCache.estimatedSize());
        // the same caller/callee/method reuses the cached entry
        Request another = decode(buildRequestHead(FUNC, CALLER, CALLEE));
        Assert.assertSame(callInfo, another.getMeta().getCallInfo());
        Assert.assertEquals(1, callInfoCache.estimatedSize());
    }

    @Test
    public void testDecodeWithOversizedFuncDoesNotPolluteCache() {
        String func = buildFunc(cacheKeyMaxLength + 1);
        Request request = decode(buildRequestHead(func, CALLER, CALLEE));
        Assert.assertEquals(METHOD_NAME, request.getInvocation().getRpcMethodName());
        Assert.assertNull(funcInfoCache.getIfPresent(func));
        Assert.assertEquals(0, funcInfoCache.estimatedSize());
    }

    @Test
    public void testDecodeWithOversizedCallerDoesNotPolluteCache() {
        String caller = CALLER + StringUtils.repeat('x', cacheKeyMaxLength);
        Request request = decode(buildRequestHead(FUNC, caller, CALLEE));
        Assert.assertEquals(caller, request.getMeta().getCallInfo().getCaller());
        Assert.assertEquals(0, callInfoCache.estimatedSize());
    }

    @Test
    public void testDecodeWithBlankCallInfoIsNotCached() {
        Request request = decode(buildRequestHead(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY));
        Assert.assertEquals(StringUtils.EMPTY, request.getInvocation().getRpcServiceName());
        Assert.assertEquals(StringUtils.EMPTY, request.getInvocation().getRpcMethodName());
        Assert.assertEquals(0, callInfoCache.estimatedSize());
        Assert.assertNull(callInfoCache.getIfPresent(StringUtils.EMPTY));
    }

    @Test
    public void testDecodeWithDistinctFuncKeepsCachesBounded() {
        int total = 100;
        for (int i = 0; i < total; i++) {
            decode(buildRequestHead("/" + SERVICE_NAME + i + "/" + METHOD_NAME, CALLER + i, CALLEE));
        }
        funcInfoCache.cleanUp();
        callInfoCache.cleanUp();
        Assert.assertTrue(funcInfoCache.estimatedSize() <= Math.min(total, cacheMaxSize));
        Assert.assertTrue(callInfoCache.estimatedSize() <= Math.min(total, cacheMaxSize));
    }

    private Request decode(RequestProtocol requestHead) {
        byte[] headBytes = requestHead.toByteArray();
        StandardPackage pkg = new StandardPackage();
        pkg.setHeadBytes(headBytes);
        pkg.getFrame().setHeadSize(headBytes.length);
        pkg.getFrame().setSize(StandardFrame.FRAME_SIZE + headBytes.length);
        ProtocolConfig config = new ProtocolConfig();
        config.setIp(LOCAL_IP);
        config.setPort(LOCAL_PORT);
        config.setDefault();
        NettyChannel channel = new NettyChannel(null, config);
        NettyChannelBuffer buffer = new NettyChannelBuffer(UnpooledByteBufAllocator.DEFAULT.buffer(BUFFER_SIZE));
        pkg.write(buffer);
        return (Request) new StandardServerCodec().decode(channel, buffer);
    }

    private RequestProtocol buildRequestHead(String func, String caller, String callee) {
        return RequestProtocol.newBuilder()
                .setFunc(ByteString.copyFromUtf8(func))
                .setCaller(ByteString.copyFromUtf8(caller))
                .setCallee(ByteString.copyFromUtf8(callee))
                .build();
    }

    /**
     * Build a legal func whose total length is the given length.
     *
     * @param length the expected length of the func
     * @return the func like {@code /xxx.../sayHello}
     */
    private String buildFunc(int length) {
        int padding = length - METHOD_NAME.length() - 2;
        return "/" + StringUtils.repeat('x', padding) + "/" + METHOD_NAME;
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(String name) throws Exception {
        Field field = StandardServerCodec.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static String[] getOrCompute(Cache<String, String[]> cache, String key) throws Exception {
        Method method = StandardServerCodec.class.getDeclaredMethod(GET_OR_COMPUTE_METHOD, Cache.class,
                String.class, Function.class);
        method.setAccessible(true);
        Function<String, String[]> mappingFunction = func -> {
            try {
                return parseFunc(func);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        return (String[]) method.invoke(null, cache, key, mappingFunction);
    }

    private static String[] parseFunc(String func) throws Exception {
        Method method = StandardServerCodec.class.getDeclaredMethod(PARSE_FUNC_METHOD, String.class);
        method.setAccessible(true);
        return (String[]) method.invoke(null, func);
    }
}
