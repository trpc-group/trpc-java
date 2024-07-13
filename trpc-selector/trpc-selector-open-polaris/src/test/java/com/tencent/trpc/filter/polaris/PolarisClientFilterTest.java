package com.tencent.trpc.filter.polaris;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.common.config.ConsumerConfig;
import com.tencent.trpc.core.common.config.ProtocolConfig;
import com.tencent.trpc.core.rpc.ConsumerInvoker;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.rpc.def.DefResponse;
import com.tencent.trpc.core.utils.JsonUtils;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.polaris.common.PolarisConstant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
public class PolarisClientFilterTest {

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";

    @InjectMocks
    private PolarisClientFilter polarisClientFilter;

    private Invoker<?> invoker = new ConsumerInvoker<Object>() {
        @Override
        public ConsumerConfig<Object> getConfig() {
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
        MessageMetadataContainer calleeContainer = metadataContext.getMetadataContainer(
                MetadataType.MESSAGE, false);
        calleeContainer.setHeader(TEST_KEY, TEST_VALUE, TransitiveType.PASS_THROUGH);
        RpcClientContext requestContext = new RpcClientContext();
        RpcContextUtils.putValueMapValue(requestContext,
                PolarisConstant.RPC_CONTEXT_POALRIS_METADATA, metadataContext);
        request.setContext(requestContext);

        // Call the method to be tested
        CompletionStage<Response> result = polarisClientFilter.filter(invoker, request);
        Response response = new DefResponse();
        try {
            response = result.toCompletableFuture().get();
        } catch (Exception exception) {
            assertNull(exception);
        }

        String metadataValue = RpcContextUtils.getAttachValue((Request) response.getRequest(),
                PolarisConstant.RPC_CONTEXT_TRANSITIVE_METADATA);
        Map<String, String> transitiveHeaders = JsonUtils.fromJson(metadataValue,
                new TypeReference<Map<String, String>>() {
                });
        String value = transitiveHeaders.get(MetadataContext.DEFAULT_TRANSITIVE_PREFIX + TEST_KEY);
        assertEquals(value, TEST_VALUE);
    }
}
