# Scenario Authoring Interview

Ask only questions relevant to the requested transports and behavior.

## Scenario identity

Collect the business objective, scenario boundary, success outcome, name, preferred ID if any, and tags. IDs and filenames may be inferred from workspace conventions when the user does not care, but report that inference.

## Suite membership

Inspect existing suites, then ask whether the scenario should join an existing suite, create a suite, use tag selection, or combine explicit and tag selection. Determine which non-secret values the suite should supply.

## Runtime inputs

For each input, determine its name, business meaning, whether it is required at runtime, whether a safe default exists, whether the suite shares it, and whether it is secret. Never request an actual secret for storage in an artifact.

## HTTP operations

For each HTTP action collect:

- service name, method, path, query parameters, and required headers;
- request body or sample cURL;
- expected status and representative response;
- response fields to assert or capture for later steps;
- timeout and whether retry is known to be safe.

A cURL command plus a representative response is preferred. Do not enable retries for state-changing requests unless the user confirms idempotency.

## Database verification

Ask this section only when persistence verification is required. Collect the connection name, existing query or schema contract, bind parameters, expected row count and values, immediate versus eventual behavior, polling settings, and captures. Ask the user for SQL when schema details are unavailable locally.

## Message verification

Ask this section only when messaging verification is required. Collect the connection, destination, observation mode, correlation ID or selector property, polling settings, representative message, expectations, and captures. Prefer dedicated observation; explain that browse mode can miss messages on a shared queue.

## Cleanup and isolation

Determine whether created data must be cancelled or removed, whether a supported cleanup operation exists, whether concurrent executions are safe, and whether the flow shares customers, inventory, numbers, queues, or database records. Default to sequential when safety is not demonstrated.

## Readiness rule

The scenario is ready when its objective and boundary, ordered executable operations, request contracts, observable success criteria, capture dependencies, required resources, and suite strategy are known. If an essential item remains unknown, state exactly which artifact or step is blocked.
