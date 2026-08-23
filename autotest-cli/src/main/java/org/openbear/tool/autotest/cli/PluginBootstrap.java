package org.openbear.tool.autotest.cli;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.openbear.tool.autotest.core.plugin.PluginRegistry;
import org.openbear.tool.autotest.core.plugin.ServiceLoaderPluginDiscovery;
import org.openbear.tool.autotest.spi.plugin.AutotestPlugin;

public final class PluginBootstrap {
  private static final Set<String> SHIPPED_PLUGIN_IDS =
      Set.of("autotest-core", "http", "jdbc", "oracle", "mysql", "activemq", "reporting-default");

  private PluginBootstrap() {}

  public static org.openbear.tool.autotest.core.plugin.PluginRegistry dynamicRegistry() {
    return dynamicRegistry(List.of());
  }

  public static org.openbear.tool.autotest.core.plugin.PluginRegistry dynamicRegistry(
      ClassLoader classLoader) {
    return dynamicRegistry(classLoader, List.of());
  }

  public static PluginRegistry dynamicRegistry(Collection<Path> pluginDirs) {
    ClassLoader loader = PluginBootstrap.class.getClassLoader();
    PluginRegistry registry = new PluginRegistry();
    registry.registerAll(
        new ServiceLoaderPluginDiscovery(loader)
            .discover().stream()
                .filter(plugin -> SHIPPED_PLUGIN_IDS.contains(plugin.descriptor().id()))
                .toList());
    if (pluginDirs != null)
      for (Path pluginDir : pluginDirs) loadDynamicExternalPlugins(registry, loader, pluginDir);
    return registry;
  }

  public static PluginRegistry dynamicRegistry(
      ClassLoader classLoader, Collection<Path> pluginDirs) {
    PluginRegistry registry = new PluginRegistry();
    registry.registerAll(new ServiceLoaderPluginDiscovery(classLoader).discover());
    if (pluginDirs != null)
      for (Path pluginDir : pluginDirs)
        loadDynamicExternalPlugins(registry, classLoader, pluginDir);
    return registry;
  }

  private static void loadDynamicExternalPlugins(
      PluginRegistry registry, ClassLoader parent, Path pluginDir) {
    Objects.requireNonNull(pluginDir, "pluginDir");
    Path root = pluginDir.toAbsolutePath().normalize();
    if (!Files.exists(root))
      throw new IllegalArgumentException("Plugin directory does not exist: " + root);
    List<Path> entries = new ArrayList<>();
    if (Files.isDirectory(root)) {
      try (Stream<Path> stream = Files.walk(root)) {
        stream.filter(Files::isRegularFile).filter(PluginBootstrap::isJar).forEach(entries::add);
      } catch (IOException e) {
        throw new IllegalStateException("Failed to scan plugin directory: " + root, e);
      }
      entries.sort(Comparator.comparing(Path::toString));
      if (entries.isEmpty()) entries.add(root);
    } else if (isJar(root)) entries.add(root);
    else throw new IllegalArgumentException("Plugin path must be a directory or JAR: " + root);

    URLClassLoader loader =
        new URLClassLoader(
            entries.stream().map(PluginBootstrap::toUrl).toArray(URL[]::new), parent);
    try {
      registry.registerAll(discoverDynamicPlugins(loader, entries));
      registry.addCloseable(loader);
    } catch (RuntimeException | Error failure) {
      try {
        loader.close();
      } catch (IOException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private static List<AutotestPlugin> discoverDynamicPlugins(
      ClassLoader loader, List<Path> entries) {
    Set<String> classNames = new LinkedHashSet<>();
    try {
      for (Path entry : entries)
        classNames.addAll(readServiceEntries(entry, AutotestPlugin.class.getName()));
      List<AutotestPlugin> plugins = new ArrayList<>();
      for (String className : classNames) {
        Class<?> type = Class.forName(className, true, loader);
        if (!AutotestPlugin.class.isAssignableFrom(type))
          throw new IllegalStateException("Class is not an AutotestPlugin: " + className);
        @SuppressWarnings("unchecked")
        Class<? extends AutotestPlugin> pluginType = (Class<? extends AutotestPlugin>) type;
        plugins.add(pluginType.getDeclaredConstructor().newInstance());
      }
      plugins.sort(Comparator.comparing(plugin -> plugin.descriptor().id()));
      return List.copyOf(plugins);
    } catch (ReflectiveOperationException | IOException e) {
      throw new IllegalStateException("Failed to load public SPI plugins from: " + entries, e);
    }
  }

  private static Set<String> readServiceEntries(Path entry, String serviceClassName)
      throws IOException {
    Set<String> out = new LinkedHashSet<>();
    if (Files.isDirectory(entry)) {
      Path service = entry.resolve("META-INF/services/" + serviceClassName);
      if (Files.exists(service))
        out.addAll(parseServiceFile(Files.readAllLines(service, StandardCharsets.UTF_8)));
      return out;
    }
    try (JarFile jar = new JarFile(entry.toFile())) {
      JarEntry service = jar.getJarEntry("META-INF/services/" + serviceClassName);
      if (service == null) return out;
      try (InputStream in = jar.getInputStream(service)) {
        out.addAll(
            parseServiceFile(
                new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList()));
      }
    }
    return out;
  }

  private static Set<String> parseServiceFile(List<String> lines) {
    Set<String> out = new LinkedHashSet<>();
    for (String line : lines) {
      String trimmed = line;
      int hash = trimmed.indexOf('#');
      if (hash >= 0) trimmed = trimmed.substring(0, hash);
      trimmed = trimmed.trim();
      if (!trimmed.isEmpty()) out.add(trimmed);
    }
    return out;
  }

  private static boolean isJar(Path path) {
    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
    return name.endsWith(".jar");
  }

  private static URL toUrl(Path path) {
    try {
      return path.toUri().toURL();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to convert plugin path to URL: " + path, e);
    }
  }
}
