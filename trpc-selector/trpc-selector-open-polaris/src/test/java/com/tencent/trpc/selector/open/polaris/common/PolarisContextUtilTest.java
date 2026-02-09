package com.tencent.trpc.selector.open.polaris.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisContextUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PolarisContextUtilTest {

    @Test
    public void testMetadataEmpty() {
        DefRequest request = new DefRequest();
        RpcClientContext clientContext = new RpcClientContext();
        request.setContext(clientContext);
        RpcContextUtils.putAttachValue(request, PolarisConstant.RPC_CONTEXT_TRANSITIVE_METADATA, "");
        assertNotNull(PolarisContextUtil.getMetadataContext(request));
    }

    @Test
    public void testPutRpcContext() {
        RpcClientContext clientContext = new RpcClientContext();
        MetadataContext metadataContext = new MetadataContext();
        PolarisContextUtil.putRpcContext(clientContext, metadataContext);
        MetadataContext value = RpcContextUtils.getValueMapValue(clientContext,
                PolarisConstant.RPC_CONTEXT_POALRIS_METADATA);
        assertEquals(value, metadataContext);
    }

}
