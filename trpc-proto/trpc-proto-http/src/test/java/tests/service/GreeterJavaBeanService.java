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

package tests.service;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import java.io.Serializable;

/**
 * Java Bean-based test service
 */
@TRpcService(name = "tencent.trpc.http.GreeterJavaBeanService")
public interface GreeterJavaBeanService {


    @TRpcMethod(name = "sayHello")
    ResponseBean sayHello(RpcContext context, RequestBean request);

    @TRpcMethod(name = "sayHelloWithGeneric")
    GenericResponseBean<String> sayHelloWithGeneric(RpcContext context, RequestBean request);

    @TRpcMethod(name = "getAttachment")
    ResponseBean assertAttachment(RpcContext context, RequestBean request);

    class ResponseBean implements Serializable {

        private static final long serialVersionUID = -5809782578272943999L;

        String message;

        InnerMsg innerMsg;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public InnerMsg getInnerMsg() {
            return innerMsg;
        }

        public void setInnerMsg(InnerMsg innerMsg) {
            this.innerMsg = innerMsg;
        }

    }

    class GenericResponseBean<T> {

        String message;

        InnerMsg innerMsg;

        T genericStr;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public InnerMsg getInnerMsg() {
            return innerMsg;
        }

        public void setInnerMsg(InnerMsg innerMsg) {
            this.innerMsg = innerMsg;
        }

        public T getGenericStr() {
            return genericStr;
        }

        public void setGenericStr(T genericStr) {
            this.genericStr = genericStr;
        }
    }

    class RequestBean {

        String message;

        InnerMsg innerMsg;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public InnerMsg getInnerMsg() {
            return innerMsg;
        }

        public void setInnerMsg(InnerMsg innerMsg) {
            this.innerMsg = innerMsg;
        }
    }

    class InnerMsg {

        String msg;

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }

}
