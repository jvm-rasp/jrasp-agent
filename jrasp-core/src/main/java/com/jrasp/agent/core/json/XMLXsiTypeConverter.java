package com.jrasp.agent.core.json;
/*
Public Domain.
*/

/**
 * Type conversion configuration interface to be used with xsi:type attributes.
 * <pre>
 * <b>XML Sample</b>
 * {@code
 *      <root>
 *          <asString xsi:type="string">12345</asString>
 *          <asInt xsi:type="integer">54321</asInt>
 *      </root>
 * }
 * <b>JSON Output</b>
 * {@code
 *     {
 *         "root" : {
 *             "asString" : "12345",
 *             "asInt": 54321
 *         }
 *     }
 * }
 *
 * <b>Usage</b>
 * {@code
 *      Map<String, XMLXsiTypeConverter<?>> xsiTypeMap = new HashMap<String, XMLXsiTypeConverter<?>>();
 *      xsiTypeMap.put("string", new XMLXsiTypeConverter<String>() {
 *          &#64;Override public String convert(final String value) {
 *              return value;
 *          }
 *      });
 *      xsiTypeMap.put("integer", new XMLXsiTypeConverter<Integer>() {
 *          &#64;Override public Integer convert(final String value) {
 *              return Integer.valueOf(value);
 *          }
 *      });
 * }
 * </pre>
 * @author kumar529
 * @param <T> return type of convert method
 */
public interface XMLXsiTypeConverter<T> {
    T convert(String value);
}
