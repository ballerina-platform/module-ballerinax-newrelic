# Ballerina New Relic Observability Extension

[![Build](https://github.com/ballerina-platform/module-ballerinax-newrelic/workflows/Daily%20Build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-newrelic/actions?query=workflow%3A"Daily+Build")
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerinax-newrelic.svg)](https://github.com/ballerina-platform/module-ballerinax-newrelic/commits/master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-newrelic/branch/main/graph/badge.svg?token=5GCQ36HBEB)](https://codecov.io/gh/ballerina-platform/module-ballerinax-newrelic)

## Building from the Source

### Setting Up the Prerequisites

1. Download and install Java SE Development Kit (JDK) version 17 (from one of the following locations).

    * [Oracle](https://www.oracle.com/java/technologies/downloads/)

    * [OpenJDK](https://adoptopenjdk.net/)

      > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.

### Building the Source

Execute the commands below to build from source.

1. To build the library:

        ./gradlew clean build

2. To run the integration tests:

        ./gradlew clean test

## Configure Ballerina Project with New Relic Observability Extension

### Prerequisites

1. Sign Up and Generate an API Key in [New Relic](https://newrelic.com/)
    * To configure the API key in Newrelic:
   > Go to Profile -> API keys -> Insights Insert key -> Insert keys to create an account in New Relic.

### Configure Ballerina Project

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

3. To enable the extension and publish traces and metrics to New Relic, add the following to the `Config.toml` when running your program.
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
```

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


## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Discuss about code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* View the [Ballerina performance test results](https://github.com/ballerina-platform/ballerina-lang/blob/master/performance/benchmarks/summary.md).
