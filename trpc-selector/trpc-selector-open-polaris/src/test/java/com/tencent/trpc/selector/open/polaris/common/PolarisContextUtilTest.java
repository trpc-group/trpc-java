package com.tencent.trpc.selector.open.polaris.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.trpc.core.rpc.RpcClientContext;
import com.tencent.trpc.core.rpc.def.DefRequest;
import com.tencent.trpc.core.utils.RpcContextUtils;
import com.tencent.trpc.polaris.common.PolarisConstant;
import com.tencent.trpc.polaris.common.PolarisContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.management.*"})
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
