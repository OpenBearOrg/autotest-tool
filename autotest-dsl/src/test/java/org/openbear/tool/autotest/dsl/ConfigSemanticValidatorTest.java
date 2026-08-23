package org.openbear.tool.autotest.dsl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.openbear.tool.autotest.core.config.DatabaseConfig;
import org.openbear.tool.autotest.core.config.EnvironmentConfig;
import org.openbear.tool.autotest.core.config.MessagingConfig;
import org.openbear.tool.autotest.core.config.SecretRef;

class ConfigSemanticValidatorTest {
  @Test
  void acceptsGenericDatabaseAndMessagingDrivers() {
    EnvironmentConfig environment = new EnvironmentConfig();

    DatabaseConfig database = new DatabaseConfig();
    database.setDriver("mysql");
    database.setJdbcUrl("jdbc:mysql://localhost:3306/autotest");
    database.setUsername(new SecretRef("DB_USERNAME"));
    database.setPassword(new SecretRef("DB_PASSWORD"));
    environment.getDatabases().put("mysql", database);

    MessagingConfig messaging = new MessagingConfig();
    messaging.setType("rabbitmq");
    messaging.setBrokerUrl("tcp://localhost:61616");
    messaging.setUsername(new SecretRef("MQ_USERNAME"));
    messaging.setPassword(new SecretRef("MQ_PASSWORD"));
    environment.getMessaging().put("mq", messaging);

    assertDoesNotThrow(() -> ConfigSemanticValidator.validate(environment));
  }
}
