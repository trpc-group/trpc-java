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

package com.tencent.trpc.validation.pgv;

import com.tencent.trpc.core.common.config.PluginConfig;
import com.tencent.trpc.core.exception.TRpcExtensionException;
import com.tencent.trpc.core.extension.Extension;
import com.tencent.trpc.core.extension.InitializingExtension;
import com.tencent.trpc.core.extension.PluginConfigAware;
import com.tencent.trpc.core.filter.FilterOrdered;
import com.tencent.trpc.core.filter.spi.Filter;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.rpc.Invoker;
import com.tencent.trpc.core.rpc.Request;
import com.tencent.trpc.core.rpc.Response;
import com.tencent.trpc.core.rpc.RpcInvocation;
import com.tencent.trpc.core.utils.FutureUtils;
import io.envoyproxy.pgv.ValidationException;
import io.envoyproxy.pgv.ValidatorImpl;
import io.envoyproxy.pgv.ValidatorIndex;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import org.apache.commons.lang.ArrayUtils;

/**
 * Request and response body pgv validation filter.
 */
@Extension("pgv")
public class PgvValidationFilter implements Filter, PluginConfigAware, InitializingExtension {

    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(PgvValidationFilter.class);

    /**
     * Pgv validation configuration information.
     */
    private PgvValidationConfig validationConfig;

    /**
     * Pgv validation plugin configuration.
     */
    private PluginConfig pluginConfig;

    /**
     * Pgv Validator class, encapsulating the generated Validator implementation.
     */
    private PgvValidator pgvValidator;

    /**
     * Pgv validatorIndex implementation class.
     */
    private ValidatorIndex validatorIndex;

    @Override
    public void setPluginConfig(PluginConfig pluginConfig) throws TRpcExtensionException {
        this.pluginConfig = pluginConfig;
    }

    @Override
    public void init() throws TRpcExtensionException {
        this.validationConfig = PgvValidationConfig.parse(pluginConfig);
        this.pgvValidator = new PgvValidator(validationConfig);
        this.validatorIndex = new PgvDefaultValidatorIndex(pgvValidator);
    }

    @Override
    public int getOrder() {
        return FilterOrdered.PGV_VALIDATION_ORDERED;
    }

    @Override
    public CompletionStage<Response> filter(Invoker<?> invoker, Request request) {
        try {
            validateRequest(request);
        } catch (IllegalArgumentException e) {
            return FutureUtils.failed(e);
        }
        if (!validationConfig.isEnableResponseValidation()) {
            return invoker.invoke(request);
        }
        CompletionStage<Response> result = invoker.invoke(request);
        result.whenComplete((r, t) -> {
            try {
                validateResponse(r);
            } catch (IllegalArgumentException e) {
                r.setException(e);
            }
        });
        return result;
    }

    /**
     * Validate request parameters.
     *
     * @param request request parameters
     * @throws IllegalArgumentException throws an exception if the request parameter validation fails
     */
    private void validateRequest(Request request) throws IllegalArgumentException {
        RpcInvocation inv = request.getInvocation();
        Object[] args = inv.getArguments();
        if (ArrayUtils.isEmpty(args)) {
            return;
        }
        Stream.of(args).forEach(this::doValidate);
    }

    /**
     * Validate response object.
     *
     * @param response response object
     * @throws IllegalArgumentException throws an exception if the response object validation fails
     */
    private void validateResponse(Response response) throws IllegalArgumentException {
        Object value = response.getValue();
        if (value != null) {
            doValidate(value);
        }
    }

    /**
     * Validate an object instance.
     *
     * @param needValidatedInstance the object to be validated
     * @throws IllegalArgumentException throws an exception if the parameter validation fails
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void doValidate(Object needValidatedInstance) throws IllegalArgumentException {
        ValidatorImpl validatorImpl = pgvValidator.getValidatorImpl(needValidatedInstance.getClass());
        try {
            validatorImpl.assertValid(needValidatedInstance, validatorIndex);
        } catch (ValidationException e) {
            logger.error("need validated instance validate fail, {}", e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
