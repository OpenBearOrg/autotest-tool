package org.openbear.tool.autotest.http;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.openbear.tool.autotest.core.engine.VariableResolver;
import org.openbear.tool.autotest.spi.resource.ServiceResource;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

/** Public-SPI implementation of the standard {@code http} step. */
final class PublicHttpStepHandler implements StepHandler<HttpExecutableStep> {
  private final Map<String, HttpClient> clients = new ConcurrentHashMap<>();

  @Override
  public Class<HttpExecutableStep> stepType() {
    return HttpExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(HttpExecutableStep step, StepExecutionContext context)
      throws Exception {
    ServiceResource service =
        context
            .services()
            .environment()
            .service(step.service())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "HTTP service is not configured: " + step.service()));
    Map<String, Object> request = step.request();
    String method = string(request.getOrDefault("method", "GET"), context).toUpperCase(Locale.ROOT);
    String path = string(request.get("path"), context);
    URI uri = URI.create(join(service.baseUrl(), path) + query(request.get("query"), context));
    Map<String, String> headers = new LinkedHashMap<>();
    service.defaultHeaders().forEach((key, value) -> headers.put(key, string(value, context)));
    map(request.get("headers")).forEach((key, value) -> headers.put(key, string(value, context)));
    Object body = body(request, context);
    String bodyText = body == null ? null : context.services().json().write(body);
    Duration timeout = duration(request.get("timeout"), service.requestTimeout());
    int attempts = attempts(method, request.get("retry"), service.safeRetryAttempts());
    Duration delay = retryDelay(request.get("retry"));
    Response response =
        execute(service, method, uri, headers, bodyText, timeout, attempts, delay, context);
    Map<String, Object> normalized = response.toMap();
    Map<String, Object> requestEvidence = new LinkedHashMap<>();
    requestEvidence.put("service", step.service());
    requestEvidence.put("method", method);
    requestEvidence.put("uri", uri.toString());
    requestEvidence.put("headers", headers);
    requestEvidence.put("body", body);
    Map<String, Object> evidence = new LinkedHashMap<>();
    evidence.put("request", requestEvidence);
    evidence.put("response", normalized);
    Map<String, Object> captures = captures(step.capture(), normalized, context);
    List<String> failures = new ArrayList<>();
    Object expectedStatus =
        VariableResolver.resolve(step.expect().get("status"), context.services().variables());
    if (expectedStatus instanceof Number number && response.status() != number.intValue())
      failures.add("HTTP status expected " + number.intValue() + " but was " + response.status());
    Object values = step.expect().get("values");
    if (values instanceof Map<?, ?> raw) {
      @SuppressWarnings("unchecked")
      Map<String, ?> expected = (Map<String, ?>) raw;
      try {
        context.services().assertions().verifyValues(response.body(), expected);
      } catch (AssertionError | RuntimeException failure) {
        failures.add(failure.getMessage());
      }
    }
    return failures.isEmpty()
        ? StepExecutionResult.success(captures, evidence)
        : StepExecutionResult.failure(String.join("; ", failures), evidence);
  }

  private Response execute(
      ServiceResource service,
      String method,
      URI uri,
      Map<String, String> headers,
      String body,
      Duration timeout,
      int attempts,
      Duration delay,
      StepExecutionContext context)
      throws Exception {
    Response response = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(timeout);
      headers.forEach(builder::header);
      builder.method(
          method,
          body == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
      long started = System.nanoTime();
      HttpResponse<String> raw =
          clients
              .computeIfAbsent(
                  service.name(),
                  ignored ->
                      HttpClient.newBuilder().connectTimeout(service.connectTimeout()).build())
              .send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      response =
          new Response(
              raw.statusCode(),
              raw.headers().map(),
              parse(raw.body(), context),
              (System.nanoTime() - started) / 1_000_000L);
      if (attempt == attempts || !Set.of(502, 503, 504).contains(response.status()))
        return response;
      Thread.sleep(delay.toMillis());
    }
    return response;
  }

  private static Object body(Map<String, Object> request, StepExecutionContext context) {
    Object source = request.get("body");
    Object file = request.get("bodyFile");
    if (source != null && file != null)
      throw new IllegalArgumentException("body and bodyFile are mutually exclusive");
    Object value =
        file == null
            ? source
            : context
                .services()
                .json()
                .parse(context.services().resources().readText(String.valueOf(file)));
    if (value == null) return null;
    Object copy = context.services().json().parse(context.services().json().write(value));
    Object patch = request.get("patch");
    if (patch instanceof Collection<?> operations)
      for (Object operation : operations) applyPatch(copy, map(operation), context);
    return resolve(copy, context);
  }

  @SuppressWarnings("unchecked")
  private static void applyPatch(
      Object document, Map<String, Object> operation, StepExecutionContext context) {
    String path = String.valueOf(operation.get("path"));
    if (!path.startsWith("/"))
      throw new IllegalArgumentException("JSON Patch path must be a JSON Pointer: " + path);
    String[] segments = path.substring(1).split("/");
    Object current = document;
    for (int i = 0; i < segments.length - 1; i++) {
      String segment = segments[i].replace("~1", "/").replace("~0", "~");
      current =
          current instanceof Map<?, ?> values
              ? ((Map<String, Object>) values).get(segment)
              : ((List<Object>) current).get(Integer.parseInt(segment));
      if (current == null)
        throw new IllegalArgumentException("JSON Patch path does not exist: " + path);
    }
    String last = segments[segments.length - 1].replace("~1", "/").replace("~0", "~");
    String op = String.valueOf(operation.get("op"));
    Object value = resolve(operation.get("value"), context);
    if (current instanceof Map<?, ?> raw) {
      Map<String, Object> values = (Map<String, Object>) raw;
      if ("remove".equals(op)) values.remove(last);
      else if ("add".equals(op) || "replace".equals(op)) values.put(last, value);
      else throw new IllegalArgumentException("Unsupported JSON Patch operation: " + op);
    } else {
      List<Object> values = (List<Object>) current;
      int index = "-".equals(last) ? values.size() : Integer.parseInt(last);
      if ("remove".equals(op)) values.remove(index);
      else if ("add".equals(op)) values.add(index, value);
      else if ("replace".equals(op)) values.set(index, value);
      else throw new IllegalArgumentException("Unsupported JSON Patch operation: " + op);
    }
  }

  private static Map<String, Object> captures(
      Map<String, Object> definitions, Map<String, Object> response, StepExecutionContext context) {
    Map<String, Object> values = new LinkedHashMap<>();
    definitions.forEach(
        (name, raw) -> {
          Map<String, Object> definition = map(raw);
          String from = String.valueOf(definition.getOrDefault("from", "response.body"));
          Object source =
              switch (from.toLowerCase(Locale.ROOT)) {
                case "response.headers" -> response.get("headers");
                case "response" -> response;
                default -> response.get("body");
              };
          Object captured =
              context
                  .services()
                  .json()
                  .read(source, String.valueOf(definition.get("jsonPath")))
                  .orElse(null);
          values.put(name, captured);
        });
    return values;
  }

  private static String query(Object raw, StepExecutionContext context) {
    Map<String, Object> values = map(raw);
    if (values.isEmpty()) return "";
    List<String> parts = new ArrayList<>();
    values.forEach(
        (key, value) -> {
          Object resolved = resolve(value, context);
          if (resolved instanceof Collection<?> collection)
            collection.forEach(item -> parts.add(encode(key) + "=" + encode(String.valueOf(item))));
          else if (resolved != null)
            parts.add(encode(key) + "=" + encode(String.valueOf(resolved)));
        });
    return "?" + String.join("&", parts);
  }

  @SuppressWarnings("unchecked")
  private static Object resolve(Object value, StepExecutionContext context) {
    return VariableResolver.resolve(value, context.services().variables());
  }

  private static String string(Object value, StepExecutionContext context) {
    if (value == null) return "";
    return String.valueOf(resolve(value, context));
  }

  private static int attempts(String method, Object rawRetry, int safeAttempts) {
    Map<String, Object> retry = map(rawRetry);
    if (Boolean.FALSE.equals(retry.get("enabled"))) return 1;
    if (Boolean.TRUE.equals(retry.get("enabled")))
      return Math.max(1, ((Number) retry.getOrDefault("maxAttempts", 2)).intValue());
    return Set.of("GET", "HEAD", "OPTIONS").contains(method) ? Math.max(1, safeAttempts) : 1;
  }

  private static Duration retryDelay(Object rawRetry) {
    return duration(map(rawRetry).get("delay"), Duration.ofMillis(500));
  }

  private static Duration duration(Object value, Duration fallback) {
    if (value == null) return fallback;
    String text = String.valueOf(value);
    if (text.matches("\\d+[smhd]")) {
      long amount = Long.parseLong(text.substring(0, text.length() - 1));
      return switch (text.charAt(text.length() - 1)) {
        case 's' -> Duration.ofSeconds(amount);
        case 'm' -> Duration.ofMinutes(amount);
        case 'h' -> Duration.ofHours(amount);
        default -> Duration.ofDays(amount);
      };
    }
    return Duration.parse(text);
  }

  private static Object parse(String value, StepExecutionContext context) {
    if (value == null || value.isBlank()) return null;
    try {
      return context.services().json().parse(value);
    } catch (RuntimeException ignored) {
      return value;
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object value) {
    return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
  }

  private static String join(String base, String path) {
    if (base.endsWith("/") && path.startsWith("/"))
      return base.substring(0, base.length() - 1) + path;
    return !base.endsWith("/") && !path.startsWith("/") ? base + "/" + path : base + path;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private record Response(
      int status, Map<String, List<String>> headers, Object body, long durationMs) {
    Map<String, Object> toMap() {
      Map<String, Object> value = new LinkedHashMap<>();
      value.put("status", status);
      value.put("headers", headers);
      value.put("body", body);
      value.put("durationMs", durationMs);
      return value;
    }
  }
}
