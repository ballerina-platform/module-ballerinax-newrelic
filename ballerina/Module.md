## Package Overview

The New Relic Observability Extension is one of the observability extensions in the <a target="_blank" href="https://ballerina.io/">Ballerina</a> language.

It provides an implementation for tracing and metrics and, publishing both metrics and traces to a <a target="_blank" href="https://newrelic.com/">New Relic</a> platform.

## Enabling New Relic Extension

To package the New Relic extension into the Jar, follow the following steps.
1. Add the following import to your program.
```ballerina
import ballerinax/newrelic as _;
```

2. Add the following to the `Ballerina.toml` when building your program.
```toml
[package]
org = "my_org"
name = "my_package"
version = "1.0.0"

[build-options]
observabilityIncluded=true
```

To enable the extension and publish traces and metrics to New Relic, add the following to the `Config.toml` when running your program.

```toml
[ballerina.observe]
tracingEnabled=true
tracingProvider="newrelic"
metricsEnabled=true
metricsReporter="newrelic"

[ballerinax.newrelic]
apiKey="<NEW_RELIC_LICENSE_KEY>"    # Mandatory Configuration.
tracingSamplerType="const"          # Optional Configuration. Default value is 'const'
tracingSamplerParam=1               # Optional Configuration. Default value is 1
tracingReporterFlushInterval=15000  # Optional Configuration. Default value is 15000 milliseconds
tracingReporterBufferSize=10000     # Optional Configuration. Default value is 10000
metricReporterFlushInterval=15000   # Optional Configuration. Default value is 15000 milliseconds
metricReporterClientTimeout=10000   # Optional Configuration. Default value is 10000 milliseconds
isTraceLoggingEnabled=false         # Optional Configuration. Default value is false
isPayloadLoggingEnabled=false       # Optional Configuration. Default value is false
```

User can configure the environment variable `BALLERINA_NEW_RELIC_API_KEY` instead of `apiKey` in `Config.toml`.
If both are configured, `apiKey` in `Config.toml` will be overwritten by the environment variable value.

### Observe Metrics in New Relic

Instead of using prometheus as an intermediate metric reporter that remote writes the metrics to New Relic,
Ballerina New Relic Observability Extension directly publishes metrics to New Relic on the following metric API `https://metric-api.newrelic.com/metric/v1`.

Instrumentation of metrics is done using the [com.newrelic.telemetry](https://github.com/newrelic/newrelic-telemetry-sdk-java).

#### Available Metrics

The exporter provides the following metrics.

|Metric Name|Description|
|---|---|
|response_time_seconds_value|Response time of a HTTP request in seconds|
|response_time_seconds_max|Maximum response time of a HTTP request|
|response_time_seconds_min|Minimum response time of a HTTP request|
|response_time_seconds_mean|Average response time of a HTTP request|
|response_time_seconds_stdDev|Standard deviation of response time of a HTTP request|
|response_time_seconds|Summary of HTTP request-response times across various time frames and quantiles|
|response_time_nanoseconds_total_value|Response time of a HTTP request in nano seconds|
|requests_total_value|Total number of requests|
|response_errors_total_value|Total number of response errors|
|inprogress_requests_value|Total number of inprogress requests|
|kafka_publishers_value|Number of publishers in kafka|
|kafka_consumers_value|Number of consumers in kafka|
|kafka_errors_value|Number of errors happened while publishing in kafka|

### Observe Traces in New Relic

Ballerina New Relic Observability Extension directly publishes traces to New Relic on the following trace API `https://otlp.nr-data.net:4317`.
Traces are published to New Relic on OpenTelemetry format.

Instrumentation of traces is done using the [io.opentelemetry](https://github.com/open-telemetry/opentelemetry-java) and `GRPC` protocol is used send traces.
