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

package com.tencent.trpc.opentelemetry.sdk.metrics;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import java.lang.reflect.Field;
import java.util.List;

/**
 * Trpc metrics autoconfiguration
 */
@AutoService(AutoConfigurationCustomizerProvider.class)
public class TrpcMetricAutoConfigurationCustomizerProvider implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
        autoConfigurationCustomizer.addMeterProviderCustomizer((sdkMeterProviderBuilder, configProperties) -> {
            int registeredMetricReaderCount = resetRegisteredMetricReader(sdkMeterProviderBuilder);
            // Register metric reader if it is not disabled by environment variable
            if (registeredMetricReaderCount > 0) {
                sdkMeterProviderBuilder.registerMetricReader(OpenTelemetryMetricsReader.getReader());
            }
            return sdkMeterProviderBuilder;
        });
    }

    /**
     * Reset the metric reader that has already been registered.
     *
     * @param sdkMeterProviderBuilder SdkMeterProviderBuilder
     */
    private int resetRegisteredMetricReader(SdkMeterProviderBuilder sdkMeterProviderBuilder) {
        try {
            Field metricReadersField = sdkMeterProviderBuilder.getClass().getDeclaredField("metricReaders");
            metricReadersField.setAccessible(true);
            List<MetricReader> metricReaders = (List<MetricReader>) metricReadersField.get(sdkMeterProviderBuilder);
            int registeredMetricReaderCount = metricReaders.size();
            metricReaders.clear();
            return registeredMetricReaderCount;
        } catch (NoSuchFieldException e) {
            System.err.println("Not found metricReaders field, failed to reset the registered metric reader");
        } catch (IllegalAccessException e) {
            System.err.println(
                    "Exception in accessing metricReaders field, failed to reset the registered MetricReader.");
        } catch (Exception e) {
            System.err.println(
                    "Exception in resetting metricReaders. failed to reset the registered MetricReader, Exception:"
                            + e);
        }
        return 0;
    }

}
