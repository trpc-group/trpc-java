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

package com.tencent.trpc.spring.exception.support;

import com.tencent.trpc.spring.exception.api.ExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link ExceptionHandler} that handles exception by invoking an exception-handling method.
 */
public class InvocableExceptionHandler implements ExceptionHandler {

    private static final ParameterNameDiscoverer DEFAULT_PARAM_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    private static final Object[] EMPTY_ARGS = new Object[0];

    private final Object bean;
    private final Class<?> beanType;
    private final Method method;
    private final MethodParameter[] parameters;

    private ParameterNameDiscoverer parameterNameDiscoverer = DEFAULT_PARAM_NAME_DISCOVERER;

    /**
     * Construct {@link InvocableExceptionHandler}
     *
     * @param bean exception handler class instance
     * @param method exception-handling method descriptor
     */
    public InvocableExceptionHandler(Object bean, Method method) {
        this.bean = bean;
        this.method = method;
        this.beanType = ClassUtils.getUserClass(bean);
        this.parameters = initMethodParameters();
    }

    /**
     * {@inheritDoc}
     *
     * @param t the thrown exception
     * @param targetMethod descriptor of the method that throw the exception
     * @param arguments method invocation arguments
     * @return handler result
     * @throws Throwable if the handler decides to re-throw the exception
     */
    @Override
    public Object handle(Throwable t, Method targetMethod, Object[] arguments) throws Throwable {
        Object[] argumentValues = getArgumentValues(t, targetMethod, arguments);
        return invokeHandleMethod(argumentValues);
    }

    public Class<?> getTargetType() {
        return beanType;
    }

    public MethodParameter[] getMethodParameters() {
        return this.parameters.clone();
    }

    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        if (parameterNameDiscoverer != null) {
            this.parameterNameDiscoverer = parameterNameDiscoverer;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InvocableExceptionHandler)) {
            return false;
        }
        InvocableExceptionHandler otherHandler = (InvocableExceptionHandler) other;
        return this.bean.equals(otherHandler.bean) && this.method.equals(otherHandler.method);
    }

    @Override
    public int hashCode() {
        return this.bean.hashCode() * 227 + this.method.hashCode();
    }

    @Override
    public String toString() {
        return "InvocableExceptionHandler bean=" + this.bean + " and method=" + this.method;
    }

    /**
     * Adapt exception and tRPC method invocation arguments to exception-handling method arguments
     *
     * @param e the thrown exception
     * @param targetMethod descriptor of invoked tRPC method
     * @param arguments tRPC method invocation arguments
     * @return arguments to invoke exception-handling method
     */
    protected Object[] getArgumentValues(Throwable e, Method targetMethod, Object[] arguments) {
        MethodParameter[] parameters = getMethodParameters();
        if (ObjectUtils.isEmpty(parameters)) {
            return EMPTY_ARGS;
        }
        Object[] args = new Object[parameters.length];
        if (ObjectUtils.isEmpty(arguments)) {
            return args;
        }

        // argument name -> argument Object
        Map<String, Object> argumentsMap = getArgumentsMapWithParamName(targetMethod, arguments);
        for (int i = 0; i < parameters.length; i++) {
            MethodParameter parameter = parameters[i];
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
            final Class<?> parameterType = parameter.getParameterType();
            final String parameterName = parameter.getParameterName();

            // try matching parameter name
            Object argument = argumentsMap.get(parameterName);
            if (parameterType.isInstance(argument)) {
                args[i] = argument;
                continue;
            }
            // try matching exception type
            if (Throwable.class.isAssignableFrom(parameterType) && parameterType.isInstance(e)) {
                args[i] = e;
                continue;
            }
            // try matching invoked method descriptor
            if (parameterType.isInstance(targetMethod)) {
                args[i] = targetMethod;
                continue;
            }
            // lastly, match by parameter type
            args[i] = Stream.of(arguments).filter(parameterType::isInstance).findFirst().orElse(null);
        }
        return args;
    }

    private Map<String, Object> getArgumentsMapWithParamName(Method targetMethod, Object[] arguments) {
        Map<String, Object> argumentsMap = new HashMap<>(parameters.length);
        String[] paramNames = this.parameterNameDiscoverer.getParameterNames(targetMethod);
        if (paramNames != null) {
            int length = Math.min(paramNames.length, arguments.length);
            for (int i = 0; i < length; i++) {
                argumentsMap.put(paramNames[i], arguments[i]);
            }
        }
        return argumentsMap;
    }

    private Object invokeHandleMethod(Object[] argumentValues) throws Throwable {
        try {
            return method.invoke(bean, argumentValues);
        } catch (InvocationTargetException ie) {
            Throwable cause = ie.getCause();
            if (cause != null) {
                throw cause;
            } else {
                throw ie;
            }
        }
    }

    private MethodParameter[] initMethodParameters() {
        int count = this.method.getParameterCount();
        MethodParameter[] result = new MethodParameter[count];
        for (int i = 0; i < count; i++) {
            result[i] = new MethodParameter(method, i);
        }
        return result;
    }

}
