package com.tencent.trpc.selector.open.polaris;

import static org.junit.Assert.assertEquals;

import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.RequestMeta;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.selector.polaris.PolarisSelector.RequestMetadataProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class RequestMetadataTest {

    private static final String TEST_SERVER = "testServer";
    private static final String CALLEE_METHOD = "testMethod";
    private static final String TEST_PATH = "testPath";
    private static final Integer PORT = 9111;


    private Request request = new DefRequest();
    private InetAddress address;

    @Before
    public void before() throws UnknownHostException {
        RequestMeta requestMeta = new RequestMeta();
        address = InetAddress.getLocalHost();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(address, PORT);
        requestMeta.setRemoteAddress(inetSocketAddress);
        request.setMeta(requestMeta);
    }

    @Test
    public void testGetRawMetadata()
            throws NoSuchMethodException, InvocationTargetException,
            InstantiationException, IllegalAccessException {
        Constructor<RequestMetadataProvider> constructor = RequestMetadataProvider.class.
                getDeclaredConstructor(Request.class);
        constructor.setAccessible(true);
        RequestMetadataProvider metadataProvider = constructor.newInstance(request);
        String callerHostName = metadataProvider.getRawMetadataStringValue(
                MessageMetadataContainer.LABEL_KEY_CALLER_IP);
        String hostName = address.getHostName();
        assertEquals(hostName, callerHostName);
        //todo
    }

}
