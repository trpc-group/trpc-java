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

package tests.service.impl1;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.TrpcTransInfoKeys;
import com.tencent.trpc.core.utils.Charsets;
import com.tencent.trpc.core.utils.RpcContextUtils;
import java.util.Map;
import org.junit.Assert;
import tests.service.GreeterJavaBeanService;

/**
 * Java Bean-based test case.
 */
public class GreeterJavaBeanServiceImpl implements GreeterJavaBeanService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterJavaBeanServiceImpl.class);


    @Override
    public ResponseBean sayHello(RpcContext context, RequestBean request) {
        logger.info("got hello request, message is '{}'", request.getMessage());
        ResponseBean responseBean = new ResponseBean();
        responseBean.setMessage(request.getMessage());
        responseBean.setInnerMsg(request.getInnerMsg());
        return responseBean;
    }

    @Override
    public GenericResponseBean<String> sayHelloWithGeneric(RpcContext context,
            RequestBean request) {
        logger.info("got hello request, message is '{}'", request.getMessage());
        GenericResponseBean<String> genericResponseBean = new GenericResponseBean<>();
        genericResponseBean.setMessage(request.getMessage());
        genericResponseBean.setInnerMsg(request.getInnerMsg());
        genericResponseBean.setGenericStr("generic");
        return genericResponseBean;
    }

    @Override
    public ResponseBean assertAttachment(RpcContext context, RequestBean request) {
        logger.info("got hello request, message is '{}'", request.getMessage());
        ResponseBean responseBean = new ResponseBean();
        responseBean.setMessage(request.getMessage());
        responseBean.setInnerMsg(request.getInnerMsg());
        Map<String, Object> attachments = context.getReqAttachMap();
        Assert.assertNotNull(attachments);
        Assert.assertTrue(attachments.containsKey(TrpcTransInfoKeys.CALLER_CONTAINER_NAME));
        Assert.assertEquals("test-container",
                new String((byte[]) attachments.get(TrpcTransInfoKeys.CALLER_CONTAINER_NAME), Charsets.UTF_8));
        Assert.assertEquals("test-container",
                RpcContextUtils.getRequestAttachValue(context, TrpcTransInfoKeys.CALLER_CONTAINER_NAME));
        Assert.assertTrue(attachments.containsKey(TrpcTransInfoKeys.CALLER_SET_NAME));
        Assert.assertEquals("test-fullset",
                new String((byte[]) attachments.get(TrpcTransInfoKeys.CALLER_SET_NAME), Charsets.UTF_8));
        Assert.assertEquals("test-fullset",
                RpcContextUtils.getRequestAttachValue(context, TrpcTransInfoKeys.CALLER_SET_NAME));
        Assert.assertTrue(attachments.containsKey("Connection"));
        Assert.assertEquals("keep-alive", RpcContextUtils.getRequestAttachValue(context, "Connection"));
        return responseBean;
    }
}
