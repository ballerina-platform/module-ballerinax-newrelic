// Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/observe;
import ballerina/os;
import ballerina/log;

const REPORTER_NAME = "newrelic";
const PROVIDER_NAME = "newrelic";
const NEW_RELIC_API_KEY_ENV = "BALLERINA_NEW_RELIC_API_KEY";

configurable string apiKey = "";
configurable boolean isTraceLoggingEnabled = false;
configurable boolean isPayloadLoggingEnabled = false;

function init() returns error? {
    string configurableAPIKey = apiKey;

    if (os:getEnv(NEW_RELIC_API_KEY_ENV) != "") {
        configurableAPIKey = os:getEnv(NEW_RELIC_API_KEY_ENV);
        log:printInfo("Using New Relic API key from environment variable " + NEW_RELIC_API_KEY_ENV);
    }

    if configurableAPIKey == "" {
        return error("error: cannot find API key for trace API. Please configure API key in Config.toml file.");
    } else {
        if observe:isTracingEnabled() && observe:getTracingProvider() == PROVIDER_NAME {
            startTracerProvider(configurableAPIKey);
        }
        if observe:isMetricsEnabled() && observe:getMetricsReporter() == REPORTER_NAME {
            startMetricsReporter(configurableAPIKey);
        }
    }
}
