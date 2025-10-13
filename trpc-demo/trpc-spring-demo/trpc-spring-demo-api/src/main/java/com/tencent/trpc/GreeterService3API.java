package com.tencent.trpc;

import com.tencent.trpc.core.rpc.RpcContext;
import com.tencent.trpc.core.rpc.anno.TRpcMethod;
import com.tencent.trpc.core.rpc.anno.TRpcService;
import java.util.Map;

@TRpcService(name = "trpc.TestApp.TestServer.GreeterService3")
public interface GreeterService3API {

    @TRpcMethod(name = "sayHelloParameterized")
    <T> Map sayHelloParameterized(RpcContext context, RequestParameterizedBean<T> request);

    class RequestParameterizedBean<T> {

        String message;
        T data;

        public static <T> RequestParameterizedBean<T> of(String message, T data) {
            RequestParameterizedBean<T> bean = new RequestParameterizedBean<>();
            bean.setMessage(message);
            bean.setData(data);
            return bean;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
    }
}
