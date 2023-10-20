/*
 * Tencent is pleased to support the open source community by making tRPC available.
 *
 * Copyright (C) 2023 THL A29 Limited, a Tencent company. 
 * All rights reserved.
 *
 * If you have downloaded a copy of the tRPC source code from Tencent,
 * please note that tRPC source code is licensed under the Apache 2.0 License,
 * A copy of the Apache 2.0 License can be found in the LICENSE file.
 */

package com.tencent.trpc.registry.util;

import static com.tencent.trpc.registry.common.Constants.URL_SEPARATOR;

import com.tencent.trpc.core.common.NamedThreadFactory;
import com.tencent.trpc.core.logger.Logger;
import com.tencent.trpc.core.logger.LoggerFactory;
import com.tencent.trpc.core.registry.RegisterInfo;
import com.tencent.trpc.registry.common.RegistryCenterConfig;
import com.tencent.trpc.registry.common.RegistryCenterData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * The data cache of the registry center, which can enable persistence through configuration options.
 */
public class RegistryCenterCache {

    private static final Logger logger = LoggerFactory.getLogger(RegistryCenterCache.class);

    /**
     * The key for the cache update time, with a value precision of seconds.
     * There may be multiple versions at the same time, so PROPERTIES_TIME_KEY + PROPERTIES_VERSION_KEY is used to
     * determine the unique version.
     */
    private static final String UPDATE_TIME_SECS_KEY = "update_time_secs";

    /**
     * The key for the cache version number.
     */
    private static final String UPDATE_VERSION_KEY = "update_version";

    /**
     * The key for the cache expiration time.
     * When disconnected from the registry center, it can be read from the cache. If it exceeds this time, the cache
     * will expire.
     */
    private static final String EXPIRE_TIME_SECS_KEY = "expire_time_secs";

    /**
     * The latest version number of the cache.
     */
    private static final AtomicLong lastVersion = new AtomicLong();

    /**
     * The maximum number of retries for cache persistence.
     */
    private static final int MAX_SAVE_RETRY_TIMES = 3;

    /**
     * Local cache, mainly caching subscribed services and their providers.
     * Additional caching of several special key-value pairs:
     * Cache update time: UPDATE_TIME_SECS_KEY
     * Cache version number: UPDATE_VERSION_KEY
     * Cache expiration time: EXPIRE_TIME_SECS_KEY
     */
    private final Properties properties = new Properties();
    /**
     * Asynchronous persistence thread pool for cache.
     */
    private final ExecutorService executor = Executors.newFixedThreadPool(1,
            new NamedThreadFactory("TrpcRegistryCenterCache", true));
    /**
     * The file used for cache persistence.
     */
    private File persistentFile;

    /**
     * Registry center configuration items.
     */
    private RegistryCenterConfig config;

    /**
     * The number of retries for cache persistence.
     */
    private AtomicInteger saveRetryTimes = new AtomicInteger();

    /**
     * If cache persistence is enabled, create a cache persistence file and attempt to retrieve previous data from disk.
     *
     * @param config
     */
    public RegistryCenterCache(RegistryCenterConfig config) {
        this.config = config;
        String fileName = config.getCacheFilePath();
        if (config.isPersistedSaveCache() && StringUtils.isNotEmpty(fileName)) {
            this.persistentFile = buildPersistentFile(fileName);
            loadFromDisk();
        }
    }

    /**
     * Get the list of service providers for a subscribed service from the local cache.
     *
     * @param serviceName The name of the subscribed service.
     * @return The list of service providers.
     */
    public List<RegisterInfo> getRegisterInfos(String serviceName) {
        List<RegisterInfo> registerInfos = new ArrayList<>();

        if (expired()) {
            return registerInfos;
        }
        String urlStr = properties.getProperty(serviceName);
        if (StringUtils.isEmpty(urlStr)) {
            return registerInfos;
        }
        Arrays.stream(urlStr.split(URL_SEPARATOR)).map(this::getRegisterInfoDecode)
                .filter(Objects::nonNull).forEach(registerInfos::add);
        return registerInfos;
    }

    /**
     * Persist the cache to a local file.
     *
     * @param registerInfo The subscribed service.
     */
    public void save(RegisterInfo registerInfo, RegistryCenterData data) {
        try {
            String serviceName = registerInfo.getServiceName();
            if (data.isEmpty()) {
                properties.remove(serviceName);
            } else {
                String allUrls = data.getTypeToRegisterInfosMap().values().stream()
                        .flatMap(registerInfos -> registerInfos.stream().map(RegisterInfo::encode))
                        .collect(Collectors.joining(URL_SEPARATOR));
                properties.setProperty(serviceName, allUrls);
            }

            doSave();
        } catch (Throwable t) {
            logger.warn("Registry save properties error, cause: {}", t.getMessage(), t);
        }
    }

    /**
     * Set an expiration time for the cache.
     */
    public void expireCache() {
        long expireTime = getCurTimeSecs() + this.config.getCacheAliveTimeSecs();
        properties.setProperty(EXPIRE_TIME_SECS_KEY, String.valueOf(expireTime));
        doSave();
    }

    /**
     * Cancel the expiration time set for the cache.
     */
    public void cancelExpireCache() {
        properties.remove(EXPIRE_TIME_SECS_KEY);
        doSave();
    }

    /**
     * Create a new file for persistence storage.
     *
     * @param fileName The file path.
     * @return A file object. If null, persistence storage is not enabled.
     */
    private File buildPersistentFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists() && file.getParentFile() != null && !file.getParentFile()
                .exists()) {
            if (!file.getParentFile().mkdirs()) {
                throw new IllegalArgumentException(
                        "Failed to create directory of registry cache file. Check fail path:"
                                + fileName);
            }
        }
        return file;
    }

    /**
     * Read the subscribed service from the local cache file.
     */
    private void loadFromDisk() {
        try (InputStream in = new FileInputStream(persistentFile)) {
            properties.load(in);
        } catch (Throwable t) {
            logger.warn("Failed to load registry cache file: {}", persistentFile, t);
        }
    }

    /**
     * The actual file persistence operation. When saving, add two keys: UPDATE_TIME_SECS_KEY and UPDATE_VERSION_KEY.
     */
    private void doSave() {
        long curTime = getCurTimeSecs();
        long version = lastVersion.incrementAndGet();
        properties.setProperty(UPDATE_TIME_SECS_KEY, String.valueOf(curTime));
        properties.setProperty(UPDATE_VERSION_KEY, String.valueOf(version));

        if (persistentFile == null) {
            return;
        }

        if (this.config.isSyncedSaveCache()) {
            sync(version);
        } else {
            executor.execute(() -> sync(version));
        }
    }

    /**
     * Synchronize data to the local file.
     *
     * @param version The version number of the cache.
     */
    private void sync(long version) {
        if (version < lastVersion.get()) {
            return;
        }
        try {
            syncToDisk();
        } catch (Throwable e) {
            retryDoSave(version, e);
        }
    }

    /**
     * Save the properties file.
     *
     * @throws IOException IO exception.
     */
    private void syncToDisk() throws IOException {
        // 1. Build a lock file to avoid concurrent modification.
        File lockFile = new File(persistentFile.getAbsoluteFile() + ".lock");
        if (!lockFile.exists()) {
            lockFile.createNewFile();
        }
        // 2. Lock the lock file.
        try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw")) {
            FileChannel channel = raf.getChannel();
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException("Can not lock registry cache file and try again later. file: "
                        + persistentFile.getAbsolutePath());
            }
            try {
                // 3. Create the local file if it does not exist.
                if (!persistentFile.exists()) {
                    persistentFile.createNewFile();
                }
                // 4. Write to the file.
                try (FileOutputStream outputFile = new FileOutputStream(persistentFile)) {
                    properties.store(outputFile, "Trpc Registry Center Cache");
                }
            } finally {
                // 5. Release the lock on the lock file.
                lock.release();
            }
        }
    }

    /**
     * Retry saving the file.
     *
     * @param version The version number.
     * @param e The exception from the previous save attempt.
     */
    private void retryDoSave(long version, Throwable e) {
        // Retry the save operation if it fails. If the number of retries exceeds the threshold (which is set to
        // MAX_SAVE_RETRY_TIMES by default), stop retrying.
        int times = saveRetryTimes.incrementAndGet();
        if (times >= MAX_SAVE_RETRY_TIMES) {
            logger.warn("Failed to save registry cache file after retrying {} times, cause: {}",
                    times, e.getMessage(), e);
            saveRetryTimes.set(0);
            return;
        }
        // To avoid data inconsistency issues when multiple threads retry synchronously,
        if (version < lastVersion.get()) {
            saveRetryTimes.set(0);
            return;
        }

        // If synchronous persistence fails, consider switching to asynchronous retry to avoid blocking.
        executor.execute(() -> sync(lastVersion.incrementAndGet()));
        logger.warn("Ready to retry {} times to save registry cache file. last cause: {}",
                saveRetryTimes.get(), e.getMessage(), e);
    }

    /**
     * Check if the cache has expired.
     */
    private boolean expired() {
        long expireTime = Long.parseLong(properties.getProperty(EXPIRE_TIME_SECS_KEY, "0"));
        return expireTime != 0 && getCurTimeSecs() > expireTime;
    }

    /**
     * Get the current timestamp in seconds.
     *
     * @return The current timestamp in seconds.
     */
    private long getCurTimeSecs() {
        return System.currentTimeMillis() / 1000;
    }


    /**
     * Get the decoded registry information.
     *
     * @param url The string information before decoding.
     * @return The decoded registry information.
     */
    private RegisterInfo getRegisterInfoDecode(String url) {
        RegisterInfo r = null;
        try {
            r = RegisterInfo.decode(url);
        } catch (IllegalStateException e) {
            logger.warn("Decode registerInfo from cache has error, url: {}, cause: ", url, e);
        }
        return r;
    }
}
