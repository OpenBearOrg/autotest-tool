package org.openbear.tool.autotest.core.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnvironmentConfig {
  private String environmentVersion = "1.0";
  private String name;
  private Map<String, ServiceConfig> services = new LinkedHashMap<>();
  private Map<String, DatabaseConfig> databases = new LinkedHashMap<>();
  private Map<String, MessagingConfig> messaging = new LinkedHashMap<>();
  private Map<String, Map<String, Map<String, Object>>> resources = new LinkedHashMap<>();
  private DatabasePolicy databasePolicy = new DatabasePolicy();
  private DefaultsConfig defaults = new DefaultsConfig();

  public void setServices(Map<String, ServiceConfig> services) {
    this.services = services == null ? new LinkedHashMap<>() : services;
  }

  public void setDatabases(Map<String, DatabaseConfig> databases) {
    this.databases = databases == null ? new LinkedHashMap<>() : databases;
  }

  public void setMessaging(Map<String, MessagingConfig> messaging) {
    this.messaging = messaging == null ? new LinkedHashMap<>() : messaging;
  }

  public void setResources(Map<String, Map<String, Map<String, Object>>> resources) {
    this.resources = resources == null ? new LinkedHashMap<>() : resources;
  }

  public void setDatabasePolicy(DatabasePolicy databasePolicy) {
    this.databasePolicy = databasePolicy == null ? new DatabasePolicy() : databasePolicy;
  }

  public void setDefaults(DefaultsConfig defaults) {
    this.defaults = defaults == null ? new DefaultsConfig() : defaults;
  }
}
