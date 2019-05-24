/* Copyright 2019 Alfa Financial Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfasoftware.soapstone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the methods in the {@link Utils} class.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestUtils {


  @Rule
  public final ExpectedException exception = ExpectedException.none();


  /**
   * Tests that the {@link Utils#convertToCamelCase(String)} method functions correctly.
   */
  @Test
  public void testConvertToCamelCase() {

    // Given
    String capitalisedString = "CapitalisedStringToConvert";
    String emptyString = "";

    // When
    String resultCapitalised = Utils.convertToCamelCase(capitalisedString);
    String resultEmpty = Utils.convertToCamelCase(emptyString);

    // Then
    assertEquals("The string should now be in camelCase", "capitalisedStringToConvert", resultCapitalised);
    assertTrue("The empty string should return an empty string", resultEmpty.isEmpty());
  }


  /**
   * Tests that the {@link Utils#simplifyQueryParameters(UriInfo, ObjectMapper)} method functions
   * correctly.
   */
  @Test
  public void testSimplifyQueryParameters() {

    MultivaluedMap<String, String> multiValuedMap = new MultivaluedHashMap<>();

    UriInfo uriInfo = mock(UriInfo.class);
    when(uriInfo.getQueryParameters()).thenReturn(multiValuedMap);

    // Given
    multiValuedMap.put("key1", singletonList("value1"));
    multiValuedMap.put("key2", asList("1", "2"));

    String expectedSimplifiedResult = "{key1=value1, key2=[\"1\",\"2\"]}";

    // When
    Map<String, WebParameter> result = Utils.simplifyQueryParameters(uriInfo, new ObjectMapper());

    assertEquals("Query parameters incorrectly simplified", "value1", result.get("key1").getNode().asText());
//    assertEquals("Query parameters incorrectly simplified", "value1", result.get("key2").getNode().);
  }

}

