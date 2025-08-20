/*
 * Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.ballerina.observe.metrics.newrelic;

import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.MetricBatchSenderFactory;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.Response;
import com.newrelic.telemetry.SenderConfiguration;
import com.newrelic.telemetry.exceptions.ResponseException;
import com.newrelic.telemetry.http.HttpPoster;
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.MetricBatch;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.newrelic.telemetry.metrics.MetricBuffer;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.metrics.Counter;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.Gauge;
import io.ballerina.runtime.observability.metrics.Metric;
import io.ballerina.runtime.observability.metrics.MetricConstants;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.PercentileValue;
import io.ballerina.runtime.observability.metrics.PolledGauge;
import io.ballerina.runtime.observability.metrics.Snapshot;
import io.ballerina.runtime.observability.metrics.Tag;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.ballerina.observe.metrics.newrelic.ObserveNativeImplConstants.EXPIRY_TAG;
import static io.ballerina.observe.metrics.newrelic.ObserveNativeImplConstants.PERCENTILE_TAG;

/**
 * This is the New Relic metric reporter class.
 */
public class NewRelicMetricsReporter {
    private static final String METRIC_REPORTER_ENDPOINT = "https://metric-api.newrelic.com/metric/v1";
    private static final int SCHEDULE_EXECUTOR_INITIAL_DELAY = 0;
    private static final Logger logger = Logger.getLogger(NewRelicMetricsReporter.class.getName());

    private static volatile ScheduledExecutorService executor;

    private static ScheduledExecutorService getOrCreateExecutor() {
        if (executor == null || executor.isShutdown()) {
            synchronized (NewRelicMetricsReporter.class) {
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newScheduledThreadPool(1);
                }
            }
        }
        return executor;
    }

    public static BArray sendMetrics(BString apiKey, int metricReporterFlushInterval,
                                     int metricReporterClientTimeout, boolean isTraceLoggingEnabled,
                                     boolean isPayloadLoggingEnabled) {
        BArray output = ValueCreator.createArrayValue(TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING));

        if (isTraceLoggingEnabled) {
            logger.setLevel(Level.INFO);
            logger.info("Trace logging is enabled for New Relic metrics reporter");
        }

        logger.info("Info log");
        logger.config("Config log");
        logger.fine("Fine log");
        logger.severe("Severe log");
        logger.warning("Warning log");

        executor = getOrCreateExecutor();

        URL endpointUrl;
        try {
            endpointUrl = URI.create(METRIC_REPORTER_ENDPOINT).toURL();
        } catch (MalformedURLException e) {
            output.append(StringUtils.fromString("error: invalid endpoint URL for New Relic metrics reporter"));
            return output;
        }

        HttpPoster httpPoster = new OkHttpPoster(Duration.ofSeconds(metricReporterClientTimeout));

        MetricBatchSenderFactory factory = MetricBatchSenderFactory.fromHttpImplementation(OkHttpPoster::new);

        SenderConfiguration config = factory
                .configureWith(apiKey.getValue())
                .httpPoster(httpPoster)
                .endpoint(endpointUrl)
                .auditLoggingEnabled(isTraceLoggingEnabled)
                .build();

        MetricBatchSender sender = MetricBatchSender.create(config);

        // Create common attributes for all metrics
        Attributes commonAttributes = null;
        try {
            commonAttributes = new Attributes()
                    .put("host", InetAddress.getLocalHost().getHostName())
                    .put("language", "ballerina");
        } catch (UnknownHostException e) {
            output.append(StringUtils.fromString("error: while getting the host name of the instance"));
        }

        // Schedule a task to run every 1 second with an initial delay of 0 seconds
        Attributes finalCommonAttributes = commonAttributes;
        executor.scheduleAtFixedRate(() -> {
            MetricBuffer metricBuffer = generateMetricBuffer(finalCommonAttributes);
            int metricCount = metricBuffer.size();
            if (isPayloadLoggingEnabled) {
                logger.info("Metric buffer: " + metricBuffer);
            }
            MetricBatch batch = metricBuffer.createBatch();
            try {
                Response response = sender.sendBatch(batch);
                logger.info("New Relic metric reporter status: " + response.getStatusMessage() + ", response: "
                        + response.getBody() + ", payload metrics count: " + metricCount);
            } catch (ResponseException e) {
                logger.severe("Error sending metrics to New Relic: " + e.getMessage());
            }
        }, SCHEDULE_EXECUTOR_INITIAL_DELAY, metricReporterFlushInterval, TimeUnit.MILLISECONDS);

        output.append(StringUtils.fromString("ballerina: started publishing metrics to New Relic on " +
                METRIC_REPORTER_ENDPOINT));

        return output;
    }

    private static MetricBuffer generateMetricBuffer(Attributes commonAttributes) {
        MetricBuffer metricBuffer = new MetricBuffer(commonAttributes);
        Metric[] metrics = DefaultMetricRegistry.getInstance().getAllMetrics();

        for (Metric metric : metrics) {
            MetricId metricId = metric.getId();
            String qualifiedMetricName = metricId.getName();
            String metricReportName = getMetricName(qualifiedMetricName, "value");

            Double metricValue = null;
            String metricType = null;
            Snapshot[] snapshots = null;

            if (metric instanceof Counter counter) {
                metricValue = getMetricValue(counter.getValue());
                metricType = MetricConstants.COUNTER;
            } else if (metric instanceof Gauge gauge) {
                metricValue = getMetricValue(gauge.getValue());
                metricType = MetricConstants.GAUGE;
                snapshots = gauge.getSnapshots();
            } else if (metric instanceof PolledGauge polledGauge) {
                metricValue = getMetricValue(polledGauge.getValue());
                metricType = MetricConstants.GAUGE;
            }
            if (metricValue != null) {
                long startTimeInMillis = System.currentTimeMillis();
                Attributes tags = new Attributes();
                for (Tag tag : metricId.getTags()) {
                    tags.put(tag.getKey(), tag.getValue());
                }

                if (metricType.equals(MetricConstants.COUNTER)) {
                    Count countMetric = generateCountMetric(metricReportName, metricValue, startTimeInMillis, tags);
                    metricBuffer.addMetric(countMetric);
                } else if (metricType.equals(MetricConstants.GAUGE)) {
                    com.newrelic.telemetry.metrics.Gauge gaugeMetric = generateGaugeMetric(metricReportName,
                            metricValue, tags);
                    metricBuffer.addMetric(gaugeMetric);
                }

                if (snapshots != null) {
                    for (Snapshot snapshot : snapshots) {
                        Attributes snapshotTags = tags.copy();
                        snapshotTags.put(EXPIRY_TAG, snapshot.getTimeWindow().toString());
                        metricBuffer.addMetric(generateGaugeMetric(getMetricName(qualifiedMetricName, "min"),
                                snapshot.getMin(), snapshotTags));
                        metricBuffer.addMetric(generateGaugeMetric(getMetricName(qualifiedMetricName, "max"),
                                snapshot.getMax(), snapshotTags));
                        metricBuffer.addMetric(generateGaugeMetric(getMetricName(qualifiedMetricName, "mean"),
                                snapshot.getMean(), snapshotTags));
                        metricBuffer.addMetric(generateGaugeMetric(getMetricName(qualifiedMetricName, "stdDev"),
                                snapshot.getStdDev(), snapshotTags));
                        for (PercentileValue percentileValue : snapshot.getPercentileValues()) {
                            Attributes percentileTags = snapshotTags.copy();
                            percentileTags.put(PERCENTILE_TAG, percentileValue.getPercentile());
                            metricBuffer.addMetric(generateGaugeMetric(qualifiedMetricName, percentileValue.getValue(),
                                    percentileTags));
                        }
                    }
                }
            }
        }

        return metricBuffer;
    }

    private static String getMetricName(String metricId, String summaryType) {
        return metricId + "_" + summaryType;
    }

    private static com.newrelic.telemetry.metrics.Gauge generateGaugeMetric(String metricName, double value,
                                                                            Attributes tags) {
        return new com.newrelic.telemetry.metrics.Gauge(
                metricName,
                value,
                System.currentTimeMillis(),
                tags);
    }

    private static Count generateCountMetric(String metricName, double value, long startTime, Attributes tags) {
        return new Count(
                metricName,
                value,
                startTime,
                System.currentTimeMillis(),
                tags);
    }

    private static double getMetricValue(Object value) {
        double metricValue = 0.0;
        if (value instanceof Long) {
            metricValue = ((Long) value).doubleValue();
        } else if (value instanceof Integer) {
            metricValue = ((Integer) value).doubleValue();
        } else if (value instanceof Double) {
            metricValue = (Double) value;
        } else if (value instanceof Float) {
            metricValue = ((Float) value).doubleValue();
        }
        return metricValue;
    }
}
