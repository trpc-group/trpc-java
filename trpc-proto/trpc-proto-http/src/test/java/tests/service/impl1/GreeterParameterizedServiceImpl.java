package tests.service.impl1;

import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.RpcContext;
import java.util.Collections;
import java.util.Map;
import tests.service.GreeterParameterizedService;

public class GreeterParameterizedServiceImpl implements GreeterParameterizedService {

    private static final Logger logger = LoggerFactory.getLogger(GreeterParameterizedServiceImpl.class);

    @Override
    public <T> Map sayHelloParameterized(RpcContext context, RequestParameterizedBean<T> request) {
        logger.info("got hello json request, request is '{}'", request);

        return Collections.singletonMap("message", "Hi:" + request.getData());
    }
}
