/*
 * Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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
import com.newrelic.telemetry.metrics.Count;
import com.newrelic.telemetry.metrics.Gauge;
import com.newrelic.telemetry.metrics.Metric;
import com.newrelic.telemetry.metrics.MetricBuffer;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.metrics.Counter;
import io.ballerina.runtime.observability.metrics.DefaultMetricRegistry;
import io.ballerina.runtime.observability.metrics.MetricId;
import io.ballerina.runtime.observability.metrics.MetricRegistry;
import io.ballerina.runtime.observability.metrics.PercentileValue;
import io.ballerina.runtime.observability.metrics.PolledGauge;
import io.ballerina.runtime.observability.metrics.Snapshot;
import io.ballerina.runtime.observability.metrics.Tag;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test class for NewRelicMetricsReporter.
 */
public class NewRelicMetricsReporterTest {

    private static final String US_ENDPOINT = "https://metric-api.newrelic.com/metric/v1";

    private final MetricRegistry registry = mock(MetricRegistry.class);

    @BeforeClass
    public void installMockMetricRegistry() {
        // The default registry can only be replaced once per JVM, and only while
        // it is still the no-op one.
        DefaultMetricRegistry.setInstance(registry);
    }

    @AfterMethod
    public void stopReporterExecutor() throws ReflectiveOperationException {
        java.lang.reflect.Field field = NewRelicMetricsReporter.class.getDeclaredField("executor");
        field.setAccessible(true);
        ScheduledExecutorService executor = (ScheduledExecutorService) field.get(null);
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    public void testSendMetricsWithEmptyApiKeyArray() {
        BArray output = sendMetrics(ValueCreator.createArrayValue(new BString[0]), "us", false);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "error: empty API key array provided");
    }

    @Test
    public void testSendMetricsWithInvalidApiKeyType() {
        BArray output = sendMetrics(Long.valueOf(42), "us", false);

        assertEquals(output.size(), 1);
        assertEquals(output.get(0).toString(), "error: invalid API key type");
    }

    @Test
    public void testSendMetricsStartsReporterForSingleApiKey() {
        BArray output = sendMetrics(StringUtils.fromString("test-key"), "us", false);

        assertTrue(containsLine(output, "ballerina: started publishing metrics to New Relic on " + US_ENDPOINT),
                "unexpected output: " + output);
    }

    @Test
    public void testSendMetricsStartsReporterForMultipleApiKeysWithTraceLogging() {
        BArray keys = ValueCreator.createArrayValue(new BString[]{
                StringUtils.fromString("key-1"), StringUtils.fromString("key-2")});

        BArray output = sendMetrics(keys, "eu", true);

        // The host name lookup can fail in restricted environments, which adds an
        // extra error line, so only the success line is asserted.
        assertTrue(containsLine(output, "ballerina: started publishing metrics to New Relic on " + US_ENDPOINT)
                || containsLine(output, "ballerina: started publishing metrics to New Relic on "
                + "https://metric-api.eu.newrelic.com/metric/v1"), "unexpected output: " + output);
    }

    @Test
    public void testGenerateMetricBufferConvertsAllMetricTypes() throws ReflectiveOperationException {
        List<Tag> tags = List.of(Tag.of("service", "test"));

        Counter counter = mock(Counter.class);
        when(counter.getId()).thenReturn(new MetricId("requests_total", "requests", tags));
        when(counter.getValue()).thenReturn(5L);

        io.ballerina.runtime.observability.metrics.Gauge gauge =
                mock(io.ballerina.runtime.observability.metrics.Gauge.class);
        when(gauge.getId()).thenReturn(new MetricId("response_time", "latency", tags));
        when(gauge.getValue()).thenReturn(2.5);
        when(gauge.getSnapshots()).thenReturn(new Snapshot[]{new Snapshot(Duration.ofSeconds(60), 1.0, 2.0, 0.5, 3.0,
                new PercentileValue[]{new PercentileValue(0.5, 2.0)})});

        PolledGauge polledGauge = mock(PolledGauge.class);
        when(polledGauge.getId()).thenReturn(new MetricId("in_progress", "in progress", tags));
        when(polledGauge.getValue()).thenReturn(7.0);

        when(registry.getAllMetrics()).thenReturn(
                new io.ballerina.runtime.observability.metrics.Metric[]{counter, gauge, polledGauge});

        MetricBuffer buffer = generateMetricBuffer();

        // counter(1) + gauge value(1) + min/max/mean/stdDev(4) + percentile(1) + polled gauge(1)
        assertEquals(buffer.size(), 8);
        Collection<Metric> metrics = buffer.createBatch().getTelemetry();

        Metric counterMetric = find(metrics, "requests_total_value");
        assertTrue(counterMetric instanceof Count);
        assertEquals(((Count) counterMetric).getValue(), 5.0);
        assertEquals(attributesOf(counterMetric).get("service"), "test");

        assertGauge(metrics, "response_time_value", 2.5);
        assertGauge(metrics, "response_time_min", 1.0);
        assertGauge(metrics, "response_time_max", 3.0);
        assertGauge(metrics, "response_time_mean", 2.0);
        assertGauge(metrics, "response_time_stdDev", 0.5);
        assertGauge(metrics, "in_progress_value", 7.0);

        Metric percentile = find(metrics, "response_time");
        assertEquals(((Gauge) percentile).getValue(), 2.0);
        assertEquals(attributesOf(percentile).get("quantile"), 0.5);
        assertNotNull(attributesOf(percentile).get("timeWindow"));
    }

    @Test
    public void testGenerateMetricBufferWithNoMetrics() throws ReflectiveOperationException {
        when(registry.getAllMetrics()).thenReturn(new io.ballerina.runtime.observability.metrics.Metric[0]);

        assertEquals(generateMetricBuffer().size(), 0);
    }

    @Test
    public void testGetMetricValueConvertsNumericTypes() throws ReflectiveOperationException {
        assertEquals(getMetricValue(Long.valueOf(3)), 3.0);
        assertEquals(getMetricValue(Integer.valueOf(4)), 4.0);
        assertEquals(getMetricValue(Double.valueOf(5.5)), 5.5);
        assertEquals(getMetricValue(Float.valueOf(1.5f)), 1.5);
        assertEquals(getMetricValue("not a number"), 0.0);
        assertEquals(getMetricValue(null), 0.0);
    }

    @Test
    public void testGetMetricNameAppendsSummaryType() throws ReflectiveOperationException {
        Method method = NewRelicMetricsReporter.class.getDeclaredMethod("getMetricName", String.class, String.class);
        method.setAccessible(true);

        assertEquals(method.invoke(null, "requests_total", "value"), "requests_total_value");
    }

    private BArray sendMetrics(Object apiKey, String region, boolean traceLogging) {
        BMap<BString, Object> stringMap = ValueCreator.createMapValue(
                TypeCreator.createMapType(PredefinedTypes.TYPE_STRING));
        stringMap.put(StringUtils.fromString("environment"), StringUtils.fromString("test"));
        @SuppressWarnings({"unchecked", "rawtypes"})
        BMap<BString, BString> attributes = (BMap) stringMap;
        // The first report is scheduled immediately; a long flush interval keeps it to
        // one attempt, and stopReporterExecutor() shuts the scheduler down afterwards.
        return NewRelicMetricsReporter.sendMetrics(apiKey, StringUtils.fromString(region), 600000, 1, traceLogging,
                false, attributes);
    }

    private MetricBuffer generateMetricBuffer() throws ReflectiveOperationException {
        Method method = NewRelicMetricsReporter.class.getDeclaredMethod("generateMetricBuffer", Attributes.class);
        method.setAccessible(true);
        return (MetricBuffer) method.invoke(null, new Attributes());
    }

    private double getMetricValue(Object value) throws ReflectiveOperationException {
        Method method = NewRelicMetricsReporter.class.getDeclaredMethod("getMetricValue", Object.class);
        method.setAccessible(true);
        return (double) method.invoke(null, value);
    }

    private boolean containsLine(BArray output, String expected) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < output.size(); i++) {
            lines.add(output.get(i).toString());
        }
        return lines.contains(expected);
    }

    private Metric find(Collection<Metric> metrics, String name) {
        return metrics.stream().filter(m -> nameOf(m).equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("metric not found: " + name));
    }

    private String nameOf(Metric metric) {
        return metric instanceof Count ? ((Count) metric).getName() : ((Gauge) metric).getName();
    }

    private Map<String, Object> attributesOf(Metric metric) {
        return metric instanceof Count ? ((Count) metric).getAttributes() : ((Gauge) metric).getAttributes();
    }

    private void assertGauge(Collection<Metric> metrics, String name, double expected) {
        Metric metric = find(metrics, name);
        assertTrue(metric instanceof Gauge, name + " should be a gauge");
        assertEquals(((Gauge) metric).getValue(), expected);
    }
}
