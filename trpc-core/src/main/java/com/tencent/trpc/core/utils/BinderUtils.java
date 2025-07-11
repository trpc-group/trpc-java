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

package com.tencent.trpc.core.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.tencent.trpc.core.common.config.annotation.ConfigProperty;
import com.tencent.trpc.core.exception.BinderException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;

/**
 * Data binding utility.
 */
@SuppressWarnings("unchecked")
public class BinderUtils {

    /**
     * Asserts whether the basic types int, long, double, and string are initialized.
     */
    public static final BiPredicate<Object, Boolean> INITIAL_PREDICATE = (o, moreZero) -> {
        if (null == o) {
            return true;
        }
        if (o.getClass() == Integer.class) {
            return (Integer) o <= 0 && moreZero;
        }
        if (o.getClass() == Long.class) {
            return (Long) o <= 0L && moreZero;
        }
        if (o.getClass() == Double.class) {
            return (Double) o <= 0D && moreZero;
        }
        if (o.getClass() == String.class) {
            return StringUtils.isBlank((String) o);
        }
        return false;
    };
    /**
     * Underscores to camel case function.
     */
    public static final Function<String, String> UNDERSCORES_TO_UPPERCASE = BinderUtils::underscoresToUpperCase;
    /**
     * Camel case to underscores.
     */
    public static final Function<String, String> UPPERCASE_TO_UNDERSCORES = BinderUtils::upperCaseToUnderscores;
    /**
     * Separator.
     */
    private static final String SEPARATOR = "_";

    /**
     * Convert underscores to camel case.
     *
     * @param source the data source to be processed
     * @return the converted string
     */
    public static String underscoresToUpperCase(String source) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char character = source.charAt(i);
            if (Objects.equals(SEPARATOR, Character.toString(character))) {
                translation.append(Character.toUpperCase(source.charAt(++i)));
            } else {
                translation.append(character);
            }
        }
        return translation.toString();
    }

    /**
     * Convert camel case to underscores.
     *
     * @param name the field name to be converted
     * @return the converted string
     */
    public static String upperCaseToUnderscores(String name) {
        StringBuilder translation = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char character = name.charAt(i);
            if (Character.isUpperCase(character) && translation.length() != 0) {
                translation.append(SEPARATOR);
            }
            translation.append(character);
        }
        return translation.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Create a func.
     *
     * @param r the actual return value
     * @return Function
     */
    public static Function<String, String> newFunction(String r) {
        return s -> r;
    }

    /**
     * Bind data.
     *
     * @param name field actual name
     * @param source binding source object
     * @param valueMap binding value map
     * @param valueKey binding value key
     */
    public static void bind(String name, Object source, Map<String, Object> valueMap, String valueKey) {
        bind(newFunction(name), source, valueMap, valueKey, Function.identity());
    }

    /**
     * Bind data.
     *
     * @param name field name
     * @param source binding source object
     * @param valueMap binding value map
     * @param valueKey binding value key
     * @param valueFunction value conversion function
     */
    public static void bind(String name, Object source, Map<String, Object> valueMap,
            String valueKey, Function<Object, Object> valueFunction) {
        bind(newFunction(name), source, valueMap, valueKey, valueFunction);
    }

    /**
     * Bind data.
     *
     * @param fieldFunc field name conversion function
     * @param source binding source object
     * @param valueMap binding value map
     * @param valueKey binding value key
     * @param valueFunction value conversion function
     */
    public static void bind(Function<String, String> fieldFunc, Object source, Map<String, Object> valueMap,
            String valueKey, Function<Object, Object> valueFunction) {
        Objects.requireNonNull(valueMap, "the value map can be null");

        Optional.ofNullable(valueMap.get(valueKey)).map(valueFunction)
                .ifPresent(value -> ReflectionUtils.getAllFields(source.getClass(),
                                field -> Optional.ofNullable(field).map(f -> Objects.equals(f.getName(),
                                        fieldFunc.apply(valueKey))).orElse(Boolean.FALSE))
                        .forEach(field -> setField(field, source, value)));
    }

    /**
     * Bind data.
     *
     * @param source binding source object
     * @param valueMap binding value map
     */
    public static void bind(Object source, Map<String, Object> valueMap) {
        bind(source, valueMap, Function.identity());
    }

    /**
     * Bind data.
     *
     * @param source binding source object
     * @param valueMap binding value map
     * @param valueFunction value conversion function
     */
    public static void bind(Object source, Map<String, Object> valueMap, Function<Object, Object> valueFunction) {
        Objects.requireNonNull(valueMap, "the value map can be null");
        ReflectionUtils.getAllFields(source.getClass()).forEach(field ->
                Optional.ofNullable(field.getAnnotation(ConfigProperty.class))
                        .flatMap(property -> {
                            if (StringUtils.isBlank(property.name())) {
                                return Optional.ofNullable(valueMap.get(
                                        UPPERCASE_TO_UNDERSCORES.apply(field.getName())));
                            }
                            return Optional.ofNullable(valueMap.get(property.name()));
                        })
                        .ifPresent(value -> setField(field, source, valueFunction.apply(value))));
    }

    /**
     * Bind data with default values. Currently supports String, Integer, Boolean, Long data types.
     *
     * @param source binding source object
     */
    public static void bind(Object source) {
        ReflectionUtils.getAllFields(source.getClass()).forEach(field -> {
            Optional.ofNullable(field.getAnnotation(ConfigProperty.class))
                    .ifPresent(value -> {
                        if (StringUtils.isNotBlank(value.value())
                                && INITIAL_PREDICATE.test(getFieldValue(field, source), value.moreZero())) {
                            if (value.type() == String.class) {
                                setField(field, source, value.value());
                            } else if (value.type() == Integer.class) {
                                setField(field, source, Integer.valueOf(value.value()));
                            } else if (value.type() == Boolean.class) {
                                setField(field, source, Boolean.valueOf(value.value()));
                            } else if (value.type() == Long.class) {
                                setField(field, source, Long.valueOf(value.value()));
                            } else if (value.type() == Double.class) {
                                setField(field, source, Double.valueOf(value.value()));
                            }
                        }
                    });
        });
    }

    /**
     * Bind data with default values. Currently supports String, Integer, Boolean, Long data types.
     *
     * @param source binding source object
     */
    public static void bind(Object target, Object source, boolean ignoreUnknownField) {
        ReflectionUtils.getAllFields(target.getClass()).forEach(field ->
                Optional.ofNullable(field.getAnnotation(ConfigProperty.class))
                        .ifPresent(value -> {
                            if (value.override()
                                    && INITIAL_PREDICATE.test(getFieldValue(field, target), value.moreZero())) {
                                Optional.ofNullable(getFieldValue(field.getName(), source, ignoreUnknownField))
                                        .ifPresent(v -> setField(field, target, v));
                            }
                        }));
    }

    /**
     * Bind property.
     * When the bound property has a value, skip setting the value.
     *
     * @param source property source
     * @param valueKey property key
     * @param value property value
     */
    public static void bind(Object source, String valueKey, Object value) {
        bind(source, valueKey, value, UNDERSCORES_TO_UPPERCASE);
    }

    /**
     * Bind property.
     *
     * @param source property source
     * @param valueKey property key
     * @param value property value
     * @param valueKeyFunc property key conversion
     */
    public static void bind(Object source, String valueKey, Object value, Function<String, String> valueKeyFunc) {
        ReflectionUtils.getAllFields(source.getClass(), field -> Optional.ofNullable(field).map(f ->
                        Objects.equals(f.getName(), valueKeyFunc.apply(valueKey))).orElse(Boolean.FALSE))
                .forEach(field -> {
                            if (INITIAL_PREDICATE.test(getFieldValue(field, source), Boolean.TRUE)) {
                                setField(field, source, value);
                            }
                        }
                );
    }

    /**
     * Lazy bind property.
     *
     * <p>When the bound property has a value, skip setting the value. When the property is uninitialized,
     * call valueFunc to get the property value.</p>
     *
     * <p>Compared to {@link BinderUtils#bind(Object, String, Object, Function)},
     * call valueFunc only when the property is null, reducing unnecessary consumption.</p>
     *
     * @param source property source
     * @param valueKey property key
     * @param value property value
     */
    public static void lazyBind(Object source, String valueKey, Object value, Function<Object, Object> valueFunc) {
        lazyBind(source, valueKey, value, UNDERSCORES_TO_UPPERCASE, valueFunc);
    }

    /**
     * Lazy bind property, when the property is uninitialized, use valueFunc to get the property value.
     *
     * @param source property source
     * @param valueKey property key
     * @param value property value
     * @param valueKeyFunc property key conversion
     * @param valueFunc property value conversion
     */
    public static void lazyBind(Object source, String valueKey, Object value, Function<String, String> valueKeyFunc,
            Function<Object, Object> valueFunc) {
        ReflectionUtils.getAllFields(source.getClass(), field -> Optional.ofNullable(field).map(f ->
                        Objects.equals(f.getName(), valueKeyFunc.apply(valueKey))).orElse(Boolean.FALSE))
                .forEach(field -> {
                            if (INITIAL_PREDICATE.test(getFieldValue(field, source), Boolean.TRUE)) {
                                setField(field, source, valueFunc.apply(value));
                            }
                        }
                );
    }

    /**
     * Merge properties. The property must have a ConfigProperty annotation and needMerged set to true, and must be
     * a List or Map collection.
     *
     * @param target the object to be merged
     * @param source the object being merged
     */
    public static void merge(Object target, Object source) {
        ReflectionUtils.getAllFields(target.getClass()).forEach(field ->
                Optional.ofNullable(field.getAnnotation(ConfigProperty.class)).ifPresent(anno -> {
                    if (anno.needMerged() && (field.getType() == List.class || field.getType() == Map.class)) {
                        mergeField(field, target, source);
                    }
                }));
    }

    /**
     * Get property.
     *
     * @param name property name
     * @param obj source object
     * @return property field. If the corresponding property is not found, it is null.
     */
    private static Field getField(String name, Object obj) {
        return ReflectionUtils.getAllFields(obj.getClass(), field -> Objects.equals(name, field.getName()))
                .stream().findFirst().orElse(null);
    }

    /**
     * Get property value.
     *
     * @param name property
     * @param source property owner object
     * @return property value
     */
    private static Object getFieldValue(String name, Object source, boolean ignoreUnknownField) {
        return Optional.ofNullable(getField(name, source))
                .map(f -> getFieldValue(f, source, ignoreUnknownField)).orElse(null);
    }

    /**
     * Get property value.
     *
     * @param field property
     * @param source property owner object
     * @return property value
     */
    private static Object getFieldValue(Field field, Object source) {
        return getFieldValue(field, source, false);
    }

    /**
     * Get property value.
     *
     * @param field property
     * @param source property owner object
     * @return property value
     */
    private static Object getFieldValue(Field field, Object source, boolean ignoreUnknownField) {
        boolean accessible = field.isAccessible();
        try {
            field.setAccessible(Boolean.TRUE);
            return field.get(source);
        } catch (IllegalAccessException e) {
            if (ignoreUnknownField) {
                return null;
            }
            throw new BinderException("get field exception", e);
        } finally {
            field.setAccessible(accessible);
        }
    }

    /**
     * Set property value.
     *
     * @param field property
     * @param source property owner object
     * @param value new value
     */
    private static void setField(Field field, Object source, Object value) {
        boolean accessible = field.isAccessible();
        try {
            field.setAccessible(Boolean.TRUE);
            field.set(source, value);
        } catch (IllegalAccessException e) {
            throw new BinderException("bind field exception", e);
        } finally {
            field.setAccessible(accessible);
        }
    }

    /**
     * Merge source object's specified property to destination object, after deduplication.
     *
     * @param targetFiled specified property. Must be List or Map type.
     * @param target destination object
     * @param source source object
     */
    private static void mergeField(Field targetFiled, Object target, Object source) {
        Field sourceField = getField(targetFiled.getName(), source);
        boolean checkFieldType = targetFiled.getType() == sourceField.getType()
                && (targetFiled.getType() == List.class || targetFiled.getType() == Map.class);
        if (!checkFieldType || !compareFieldOfCollection(targetFiled, sourceField)) {
            return;
        }

        Object targetValue = getFieldValue(targetFiled, target);
        Object sourceValue = getFieldValue(sourceField, source);

        // if both are the same object, no need to merge
        if (targetValue == sourceValue) {
            return;
        }

        if (targetFiled.getType() == List.class) {
            mergeAndSetListField(targetFiled, target, targetValue, sourceValue);
        } else if (targetFiled.getType() == Map.class) {
            mergeAndSetMapField(targetFiled, target, targetValue, sourceValue);
        }
    }

    private static void mergeAndSetListField(Field targetFiled, Object target, Object targetValue, Object sourceValue) {
        Set<Object> mergedValue = Sets.newHashSet();
        if (sourceValue != null) {
            mergedValue.addAll((Collection<?>) sourceValue);
        }
        if (targetValue != null) {
            mergedValue.addAll((Collection<?>) targetValue);
        }
        if (!mergedValue.isEmpty()) {
            setField(targetFiled, target, Lists.newArrayList(mergedValue));
        }
    }

    private static void mergeAndSetMapField(Field targetFiled, Object target, Object targetValue, Object sourceValue) {
        Map<Object, Object> mergedValue = Maps.newHashMap();
        if (sourceValue != null) {
            mergedValue.putAll((Map<?, ?>) sourceValue);
        }
        if (targetValue != null) {
            mergedValue.putAll((Map<?, ?>) targetValue);
        }
        if (!mergedValue.isEmpty()) {
            setField(targetFiled, target, mergedValue);
        }
    }

    /**
     * Check if the generics of two property bindings are the same.
     *
     * @param targetField destination property
     * @param sourceField source property
     * @return true if they are the same
     */
    private static boolean compareFieldOfCollection(Field targetField, Field sourceField) {
        Type[] targetGenericTypes = getGenericTypes(targetField);
        Type[] sourceGenericTypes = getGenericTypes(sourceField);
        if (targetGenericTypes == null || sourceGenericTypes == null
                || targetGenericTypes.length != sourceGenericTypes.length
                || targetGenericTypes.length <= 0) {
            return false;
        }

        int checkTypeNums = 0;
        if (targetField.getType() == List.class) {
            checkTypeNums = 1;
        } else if (targetField.getType() == Map.class) {
            checkTypeNums = 2;
        }
        for (int i = 0; i < checkTypeNums; i++) {
            if (targetGenericTypes[i] != sourceGenericTypes[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the actual generic type of the collection type.
     *
     * @param collectionField collection property
     * @return generic type
     */
    private static Type[] getGenericTypes(Field collectionField) {
        Type genericType = collectionField.getGenericType();
        if (genericType instanceof ParameterizedType) {
            return ((ParameterizedType) genericType).getActualTypeArguments();
        }
        return null;
    }

}
