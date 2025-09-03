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

[ballerinax.newrelic.additionalAttributes]      # Optional Configuration. Add custom attributes (key & value pair) to metrics
key1 = "<VALUE_1>"
key2 = "<VALUE_2>"
```
User can configure the environment variable `BALLERINA_NEWRELIC_API_KEY` instead of `apiKey` in `Config.toml`.
If both are configured, `apiKey` in `Config.toml` will be overwritten by the environment variable value.
