package com.tencent.trpc.filter.polaris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.common.config.ProviderConfig;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.ProviderInvoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.polaris.common.PolarisContextUtil;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class PolarisServerFilterTest {

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";

    @InjectMocks
    private PolarisServerFilter polarisServerFilter;

    private Invoker<?> invoker = new ProviderInvoker<Object>() {

        @Override
        public Object getImpl() {
            return null;
        }

        @Override
        public ProviderConfig<Object> getConfig() {
            return null;
        }

        @Override
        public ProtocolConfig getProtocolConfig() {
            return null;
        }

        @Override
        public Class<Object> getInterface() {
            return null;
        }

        @Override
        public CompletionStage<Response> invoke(Request request) {
            Response response = new DefResponse();
            response.setRequest(request);
            return CompletableFuture.completedFuture(response);
        }
    };

    private Request request = new DefRequest();

    @Test
    public void testFilter() {
        // Prepare test data
        MetadataContext metadataContext = new MetadataContext(); // Assuming you have a constructor for MetadataContext
        MessageMetadataContainer callerContainer = metadataContext.getMetadataContainer(
                MetadataType.MESSAGE, true);
        callerContainer.setHeader(TEST_KEY, TEST_VALUE, TransitiveType.PASS_THROUGH);
        PolarisContextUtil.putAttachValue(request, metadataContext);
        request.setContext(new RpcClientContext());

        CompletionStage<Response> result = polarisServerFilter.filter(invoker, request);
        Response response = new DefResponse();
        try {
            response = result.toCompletableFuture().get();
        } catch (Exception exception) {
            assertNull(exception);
        }

        MetadataContext responseContext = PolarisContextUtil.getMetadataContext(response.getRequest());
        MessageMetadataContainer metadataContainer = responseContext.getMetadataContainer(
                MetadataType.MESSAGE, true);
        assertEquals(metadataContainer.getHeader(TEST_KEY), TEST_VALUE);
    }
}
