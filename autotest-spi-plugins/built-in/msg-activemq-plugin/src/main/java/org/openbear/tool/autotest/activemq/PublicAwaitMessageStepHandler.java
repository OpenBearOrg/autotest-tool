package org.openbear.tool.autotest.activemq;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.openbear.tool.autotest.core.engine.VariableResolver;
import org.openbear.tool.autotest.spi.resource.MessagingResource;
import org.openbear.tool.autotest.spi.service.PollRequest;
import org.openbear.tool.autotest.spi.service.PollResult;
import org.openbear.tool.autotest.spi.step.StepExecutionContext;
import org.openbear.tool.autotest.spi.step.StepExecutionResult;
import org.openbear.tool.autotest.spi.step.StepHandler;

/** Public-SPI implementation of dedicated and browse ActiveMQ observations. */
final class PublicAwaitMessageStepHandler
    implements StepHandler<AwaitMessageExecutableStep>, AutoCloseable {
  private final Map<String, Connection> connections = new ConcurrentHashMap<>();

  @Override
  public Class<AwaitMessageExecutableStep> stepType() {
    return AwaitMessageExecutableStep.class;
  }

  @Override
  public StepExecutionResult execute(
      AwaitMessageExecutableStep step, StepExecutionContext context) {
    PollResult<Optional<Map<String, Object>>> outcome =
        context
            .services()
            .polling()
            .until(
                request(step.polling()),
                () -> observe(step, context),
                found -> found.isPresent() && matches(step, found.get(), context));
    Map<String, Object> evidence = new LinkedHashMap<>();
    Optional<Map<String, Object>> lastValue =
        outcome.lastValue() == null ? Optional.empty() : outcome.lastValue();
    lastValue.ifPresent(message -> evidence.put("message", message));
    evidence.put("observations", outcome.observations());
    if (outcome.matched() && lastValue.isPresent()) {
      Map<String, Object> captures = capture(step.capture(), lastValue.get(), context);
      return StepExecutionResult.success(captures, evidence);
    }
    return StepExecutionResult.failure("Timed out waiting for a matching message", evidence);
  }

  private Optional<Map<String, Object>> observe(
      AwaitMessageExecutableStep step, StepExecutionContext context) {
    try {
      return "browse".equalsIgnoreCase(step.observationMode())
          ? browse(step, context)
          : consumeDedicated(step, context);
    } catch (JMSException failure) {
      throw new IllegalStateException(
          "ActiveMQ observation failed on '" + step.connection() + "'", failure);
    }
  }

  private Optional<Map<String, Object>> browse(
      AwaitMessageExecutableStep step, StepExecutionContext context) throws JMSException {
    try (Session session =
        connection(step, context).createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      javax.jms.Queue queue = session.createQueue(step.destination());
      try (QueueBrowser browser = session.createBrowser(queue, selector(step, context))) {
        Enumeration<?> messages = browser.getEnumeration();
        while (messages.hasMoreElements()) {
          Map<String, Object> found = normalize((Message) messages.nextElement(), context);
          if (matches(step, found, context)) return Optional.of(found);
        }
        return Optional.empty();
      }
    }
  }

  private Optional<Map<String, Object>> consumeDedicated(
      AwaitMessageExecutableStep step, StepExecutionContext context) throws JMSException {
    String selector = selector(step, context);
    if (selector == null || selector.isBlank())
      throw new IllegalArgumentException(
          "dedicated observation requires a JMS selector or correlationId match");
    try (Session session =
        connection(step, context).createSession(false, Session.AUTO_ACKNOWLEDGE)) {
      try (MessageConsumer consumer =
          session.createConsumer(session.createQueue(step.destination()), selector)) {
        Message message = consumer.receiveNoWait();
        return message == null ? Optional.empty() : Optional.of(normalize(message, context));
      }
    }
  }

  private Connection connection(AwaitMessageExecutableStep step, StepExecutionContext context)
      throws JMSException {
    Connection existing = connections.get(step.connection());
    if (existing != null) return existing;
    MessagingResource resource =
        context
            .services()
            .environment()
            .messaging(step.connection())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Messaging connection is not configured: " + step.connection()));
    if (!"activemq".equalsIgnoreCase(resource.provider()))
      throw new IllegalArgumentException("Unsupported messaging provider: " + resource.provider());
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(resource.brokerUrl());
    String username =
        resource.usernameSecret() == null
            ? null
            : context.services().secrets().require(resource.usernameSecret());
    String password =
        resource.passwordSecret() == null
            ? null
            : context.services().secrets().require(resource.passwordSecret());
    Connection connection =
        username == null
            ? factory.createConnection()
            : factory.createConnection(username, password);
    connection.start();
    connections.put(step.connection(), connection);
    return connection;
  }

  private static boolean matches(
      AwaitMessageExecutableStep step, Map<String, Object> message, StepExecutionContext context) {
    for (var entry : step.match().entrySet()) {
      Object actual =
          context.services().json().read(message, "$.headers." + entry.getKey()).orElse(null);
      if (actual == null)
        actual =
            context.services().json().read(message, "$.properties." + entry.getKey()).orElse(null);
      if (!String.valueOf(actual).equals(resolve(String.valueOf(entry.getValue()), context)))
        return false;
    }
    Object values = step.expect().get("values");
    if (values instanceof Map<?, ?> raw) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, ?> expected = (Map<String, ?>) raw;
        context.services().assertions().verifyValues(message, expected);
      } catch (AssertionError | RuntimeException failure) {
        return false;
      }
    }
    return true;
  }

  private static Map<String, Object> capture(
      Map<String, Object> definitions, Map<String, Object> message, StepExecutionContext context) {
    Map<String, Object> values = new LinkedHashMap<>();
    definitions.forEach(
        (name, raw) -> {
          if (!(raw instanceof Map<?, ?> definition)) return;
          String from =
              String.valueOf(definition.containsKey("from") ? definition.get("from") : "message");
          Object source =
              switch (from.toLowerCase(Locale.ROOT)) {
                case "message.body" -> message.get("body");
                case "message.headers" -> message.get("headers");
                case "message.properties" -> message.get("properties");
                default -> message;
              };
          values.put(
              name,
              context
                  .services()
                  .json()
                  .read(source, String.valueOf(definition.get("jsonPath")))
                  .orElse(null));
        });
    return values;
  }

  private static String selector(AwaitMessageExecutableStep step, StepExecutionContext context) {
    if (step.selector() != null && !step.selector().isBlank())
      return resolve(step.selector(), context);
    Object correlationId = step.match().get("correlationId");
    return correlationId == null
        ? null
        : "JMSCorrelationID = '"
            + resolve(String.valueOf(correlationId), context).replace("'", "''")
            + "'";
  }

  private static Map<String, Object> normalize(Message message, StepExecutionContext context)
      throws JMSException {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("messageId", message.getJMSMessageID());
    headers.put("correlationId", message.getJMSCorrelationID());
    headers.put("timestamp", message.getJMSTimestamp());
    Map<String, Object> properties = new LinkedHashMap<>();
    Enumeration<?> names = message.getPropertyNames();
    while (names.hasMoreElements()) {
      String name = String.valueOf(names.nextElement());
      properties.put(name, message.getObjectProperty(name));
    }
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("headers", headers);
    normalized.put("properties", properties);
    normalized.put("body", body(message, context));
    return normalized;
  }

  private static Object body(Message message, StepExecutionContext context) throws JMSException {
    if (message instanceof TextMessage text) return parse(text.getText(), context);
    if (message instanceof BytesMessage bytes) {
      bytes.reset();
      byte[] value = new byte[(int) bytes.getBodyLength()];
      bytes.readBytes(value);
      return parse(new String(value, StandardCharsets.UTF_8), context);
    }
    if (message instanceof MapMessage map) {
      Map<String, Object> values = new LinkedHashMap<>();
      Enumeration<?> names = map.getMapNames();
      while (names.hasMoreElements()) {
        String name = String.valueOf(names.nextElement());
        values.put(name, map.getObject(name));
      }
      return values;
    }
    return "<unsupported JMS body type: " + message.getClass().getSimpleName() + ">";
  }

  private static PollRequest request(Map<String, Object> values) {
    return new PollRequest(
        duration(values.get("timeout"), Duration.ofMinutes(2)),
        duration(values.get("interval"), Duration.ofSeconds(1)));
  }

  private static Duration duration(Object value, Duration fallback) {
    if (value == null) return fallback;
    String text = String.valueOf(value);
    if (text.matches("\\d+[smh]")) {
      long amount = Long.parseLong(text.substring(0, text.length() - 1));
      return switch (text.charAt(text.length() - 1)) {
        case 's' -> Duration.ofSeconds(amount);
        case 'm' -> Duration.ofMinutes(amount);
        default -> Duration.ofHours(amount);
      };
    }
    return Duration.parse(text);
  }

  private static String resolve(String value, StepExecutionContext context) {
    return String.valueOf(VariableResolver.resolve(value, context.services().variables()));
  }

  private static Object parse(String value, StepExecutionContext context) {
    try {
      return context.services().json().parse(value);
    } catch (RuntimeException ignored) {
      return value;
    }
  }

  @Override
  public void close() {
    connections
        .values()
        .forEach(
            connection -> {
              try {
                connection.close();
              } catch (JMSException ignored) {
              }
            });
    connections.clear();
  }
}
