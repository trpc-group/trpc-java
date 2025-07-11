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
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Collection utility.
 */
public class CollectionUtils {

    /**
     * Merge collection array into the same type of collection.
     *
     * @param merger custom merge method
     * @param collections collection array
     * @param <E> element type
     * @param <C> collection type
     * @return merged collection of type C
     */
    @SafeVarargs
    public static <E, C extends Collection<E>> C merge(BinaryOperator<C> merger, C... collections) {
        if (collections == null || collections.length == 0) {
            return merger.apply(null, null);
        }

        return Arrays.stream(collections, 1, collections.length)
                .reduce(collections[0], merger);
    }

    /**
     * Merge two collections.
     *
     * @param collection1 one collection
     * @param collection2 another collection
     * @param collectionFactory factory to create collection
     * @param <E> element type
     * @param <C> collection type
     * @return merged collection of type c
     */
    @Nonnull
    public static <E, C extends Collection<E>> C merge(@Nullable C collection1, @Nullable C collection2,
            Supplier<C> collectionFactory) {
        C result = collectionFactory.get();
        if (isNotEmpty(collection1)) {
            result.addAll(collection1);
        }
        if (isNotEmpty(collection2)) {
            result.addAll(collection2);
        }

        return result;
    }

    @SafeVarargs
    public static <E> List<E> mergeList(Supplier<List<E>> factory, @Nullable List<E>... lists) {
        return merge((l1, l2) -> merge(l1, l2, factory), lists);
    }

    @SafeVarargs
    public static <E> List<E> mergeList(@Nullable List<E>... lists) {
        return merge(CollectionUtils::mergeList, lists);
    }

    public static <E> List<E> mergeList(@Nullable List<E> l1, @Nullable List<E> l2) {
        return merge(l1, l2, Lists::newArrayList);
    }

    @SafeVarargs
    public static <E> Set<E> mergeSet(Supplier<Set<E>> factory, @Nullable Set<E>... sets) {
        return merge((s1, s2) -> merge(s1, s2, factory), sets);
    }

    @SafeVarargs
    public static <E> Set<E> mergeSet(@Nullable Set<E>... sets) {
        return merge(CollectionUtils::mergeSet, sets);
    }

    public static <E> Set<E> mergeSet(@Nullable Set<E> s1, @Nullable Set<E> s2) {
        return merge(s1, s2, Sets::newHashSet);
    }

    public static int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

}