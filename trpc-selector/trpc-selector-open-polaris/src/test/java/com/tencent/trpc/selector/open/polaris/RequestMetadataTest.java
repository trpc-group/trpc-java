package com.tencent.trpc.selector.open.polaris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.trpc.core.constant.proto.HttpConstants;
import com.tencent.trpc.core.rpc.CallInfo;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.rpc.common.RpcMethodInfo;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.demo.api.service.HelloServiceImpl;
import com.tencent.trpc.selector.polaris.PolarisSelector.RequestMetadataProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.http.HttpHeaders;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class RequestMetadataTest {

    private static final String CALLEE_METHOD = "testMethod";
    private static final String TEST_METHOD_NAME = "sayHello";
    private static final String TEST_HEADER_KEY = "testHeader";
    private static final String TEST_HEADER_VALUE = "testHeaderValue";
    private static final Integer PORT = 9111;
    private static final String FIELD_NAME_HEADERS = "headers";


    private Request request = new DefRequest();
    private InetAddress address;

    /**
     * The method to be run before the {@link org.junit.Test}. Init request and address.
     *
     * @throws UnknownHostException
     * @throws NoSuchMethodException
     */
    @Before
    public void before() throws UnknownHostException, NoSuchMethodException {
        // set meta
        RequestMeta requestMeta = new RequestMeta();
        address = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(address, PORT);
        requestMeta.setRemoteAddress(inetSocketAddress);
        requestMeta.setCallInfo(new CallInfo().setCalleeMethod(CALLEE_METHOD));
        request.setMeta(requestMeta);

        // set invocation
        HelloServiceImpl helloService = new HelloServiceImpl();
        Class<? extends HelloServiceImpl> helloClazz = helloService.getClass();
        RpcMethodInfo rpcMethodInfo = new RpcMethodInfo(helloClazz, helloClazz.getMethod(TEST_METHOD_NAME,
                RpcContext.class, String.class));
        RpcInvocation rpcInvocation = new RpcInvocation();
        rpcInvocation.setRpcMethodInfo(rpcMethodInfo);
        request.setInvocation(rpcInvocation);

        // set context
        RpcContext clientContext = new RpcClientContext();
        request.setContext(clientContext);
    }

    @Test
    public void testGetRawMetadata()
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider
                .class.getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        assertEquals(address.getHostName(),
                metadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_CALLER_IP));
        assertEquals(request.getMeta().getCallInfo().getCalleeMethod(),
                metadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD));
        assertEquals(request.getInvocation().getRpcMethodInfo().getServiceInterface().getCanonicalName(),
                metadataProvider.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH));
        assertNull(metadataProvider.getRawMetadataStringValue(""));
    }

    @Test
    public void testGetMapValue()
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(TEST_HEADER_KEY, TEST_HEADER_VALUE);
        request.getContext().getReqAttachMap().put(FIELD_NAME_HEADERS, httpHeaders);

        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider
                .class.getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        String value = metadataProvider.getRawMetadataMapValue(
                MessageMetadataContainer.LABEL_MAP_KEY_HEADER, TEST_HEADER_KEY);
        assertEquals(TEST_HEADER_VALUE, value);

        // test HttpServletRequest
        ConcurrentMap<String, Object> reqAttachMap = request.getContext().getReqAttachMap();
        reqAttachMap.remove(FIELD_NAME_HEADERS);
        HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getHeader(TEST_HEADER_KEY)).thenReturn(TEST_HEADER_VALUE);
        reqAttachMap.put(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST, servletRequest);
        assertEquals(TEST_HEADER_VALUE,
                metadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER,
                        TEST_HEADER_KEY));
    }

    @Test
    public void testGetNull() throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider
                .class.getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        String value = metadataProvider.getRawMetadataMapValue("", "");
        assertNull(value);
    }

    @Test
    public void testReqAttachMap() throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider
                .class.getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        ConcurrentMap<String, Object> reqAttachMap = request.getContext().getReqAttachMap();
        reqAttachMap.remove(FIELD_NAME_HEADERS);
        reqAttachMap.remove(HttpConstants.TRPC_ATTACH_SERVLET_REQUEST);
        assertNull(metadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER,
                TEST_HEADER_KEY));
    }

    @Test
    public void testException() throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider
                .class.getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        RpcContext context = request.getContext();
        context.getReqAttachMap().put(FIELD_NAME_HEADERS, "");
        String value = metadataProvider.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER,
                TEST_HEADER_KEY);
        assertNull(value);
    }
}
