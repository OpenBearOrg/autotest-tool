package org.openbear.tool.autotest.core.database;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JdbcValueNormalizer {
  private JdbcValueNormalizer() {}

  public static List<Map<String, Object>> normalizeRows(List<Map<String, Object>> rows)
      throws SQLException {
    List<Map<String, Object>> out = new ArrayList<>();
    if (rows == null) return out;
    for (Map<String, Object> row : rows) out.add(normalizeRow(row));
    return out;
  }

  public static Map<String, Object> normalizeRow(Map<String, Object> row) throws SQLException {
    Map<String, Object> out = new LinkedHashMap<>();
    if (row == null) return out;
    for (Map.Entry<String, Object> entry : row.entrySet())
      out.put(entry.getKey(), normalizeValue(entry.getValue()));
    return out;
  }

  public static Object normalizeValue(Object value) throws SQLException {
    if (value == null) return null;
    if (value instanceof Map<?, ?> map) return normalizeMap(map);
    if (value instanceof Iterable<?> iterable) return normalizeIterable(iterable);
    if (value instanceof byte[] bytes) return Base64.getEncoder().encodeToString(bytes);
    if (value.getClass().isArray()) return normalizeArray(value);
    if (value instanceof Clob clob) return readClob(clob);
    if (value instanceof Blob blob) return readBlob(blob);
    if (value instanceof SQLXML xml) return xml.getString();
    if (value instanceof Array array) return normalizeValue(array.getArray());
    if (value instanceof Struct struct) return normalizeStruct(struct);
    return value;
  }

  private static Map<String, Object> normalizeMap(Map<?, ?> map) throws SQLException {
    Map<String, Object> out = new LinkedHashMap<>();
    for (Map.Entry<?, ?> entry : map.entrySet())
      out.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
    return out;
  }

  private static List<Object> normalizeIterable(Iterable<?> iterable) throws SQLException {
    List<Object> out = new ArrayList<>();
    for (Object item : iterable) out.add(normalizeValue(item));
    return out;
  }

  private static Object normalizeArray(Object array) throws SQLException {
    if (array instanceof Object[] objects) {
      List<Object> out = new ArrayList<>(objects.length);
      for (Object object : objects) out.add(normalizeValue(object));
      return out;
    }
    int length = java.lang.reflect.Array.getLength(array);
    List<Object> out = new ArrayList<>(length);
    for (int i = 0; i < length; i++) out.add(normalizeValue(java.lang.reflect.Array.get(array, i)));
    return out;
  }

  private static Map<String, Object> normalizeStruct(Struct struct) throws SQLException {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("sqlType", struct.getSQLTypeName());
    Object[] attributes = struct.getAttributes();
    List<Object> values = new ArrayList<>(attributes.length);
    for (Object attribute : attributes) values.add(normalizeValue(attribute));
    out.put("attributes", values);
    return out;
  }

  private static String readClob(Clob clob) throws SQLException {
    try (Reader reader = clob.getCharacterStream()) {
      StringBuilder out = new StringBuilder();
      char[] buffer = new char[4096];
      int read;
      while ((read = reader.read(buffer)) != -1) out.append(buffer, 0, read);
      return out.toString();
    } catch (IOException e) {
      throw new SQLException("Failed to read CLOB value", e);
    }
  }

  private static String readBlob(Blob blob) throws SQLException {
    try (InputStream in = blob.getBinaryStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int read;
      while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
      return Base64.getEncoder().encodeToString(out.toByteArray());
    } catch (IOException e) {
      throw new SQLException("Failed to read BLOB value", e);
    }
  }
}
