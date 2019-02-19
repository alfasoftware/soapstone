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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test class for {@link SoapstoneService}
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
@RunWith(MockitoJUnitRunner.class)
public class TestSoapstoneService {

  @Mock private ExceptionMapper exceptionMapper;
  @Mock private HttpHeaders headers;
  @Mock private UriInfo uriInfo;
  @Mock private WebServiceClass<?> webServiceClass;

  @Captor private ArgumentCaptor<Map<String, String>> captor;

  @Rule public final ExpectedException exception = ExpectedException.none();

  private static final String VENDOR = "Vendor";
  private static final String KEY_1 = "key1";
  private static final String KEY_2 = "key2";
  private static final String PATH = "/path/path/path";
  private static final String ENTITY = "{ \"entityIdentifier\" : { \"entityDescriptor\":\"descriptor\", \"entityNumber\" : 1 } }";
  private static final String ENTITY_IN_JSON = "{\"entityDescriptor\":\"descriptor\",\"entityNumber\":1}";
  private static final String REQUEST_IN_JSON = "{\"realmId\":\"REALM\",\"localeCode\":\"en_gb\",\"userId\":\"USER\"}";

  private final Map<String, WebServiceClass<?>> webServiceClasses = new HashMap<>();
  private final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
  private final MultivaluedMap<String, String> headersMap = new MultivaluedHashMap<>();

  private String operation;
  private SoapstoneService soapstoneService;


  /**
   * Set up the tests
   */
  @Before
  public void setUp() {
    queryParameters.put(KEY_1, singletonList("value1"));
    queryParameters.put(KEY_2, asList("value2", "value3"));

    headersMap.put("X-Vendor-Context", singletonList("userId=USER;realmId=REALM;localeCode=en_gb"));
    headersMap.put("X-NotVendor-Test", singletonList("someKey=SOME_VALUE; anotherKey=ANOTHER_VALUE"));

    webServiceClasses.put("/path/path/path", webServiceClass);

    String headerString = "X-" + VENDOR + "-Context";
    String requestContext = "userId=USER;realmId=REALM;localeCode=en_gb";

    when(uriInfo.getQueryParameters()).thenReturn(queryParameters);
    when(headers.getHeaderString(headerString)).thenReturn(requestContext);
    when(headers.getRequestHeaders()).thenReturn(headersMap);


  }


  /**
   * Tests the {@linkplain SoapstoneService#get(HttpHeaders, UriInfo)} method.
   */
  @Test
  public void testGet() {

    // Given
    operation = "loadAll";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When
    soapstoneService.get(headers, uriInfo);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());

    // Verify we populate the parameters with the correct query and context
    Map<String, String> capturedNonHeaderValues = captor.getAllValues().get(0);
    Map<String, String> capturedHeaderValues = captor.getAllValues().get(1);
    assertEquals("The first key and value query combination is incorrect", "value1", capturedNonHeaderValues.get(KEY_1));
    assertEquals("The second key and value query combination is incorrect", "[\"value2\",\"value3\"]", capturedNonHeaderValues.get(KEY_2));
    assertEquals("The context is incorrect", REQUEST_IN_JSON, capturedHeaderValues.get("context"));
  }


  /**
   * Tests the {@linkplain SoapstoneService#post(HttpHeaders, UriInfo)} method.
   */
  @Test
  public void testPost() {

    // Given
    operation = "anyOperation";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When
    soapstoneService.post(headers, uriInfo, ENTITY);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());

    // Verify we populate the parameters with the correct query and context
    Map<String, String> capturedNonHeaderValues = captor.getAllValues().get(0);
    Map<String, String> capturedHeaderValues = captor.getAllValues().get(1);
    assertEquals("The first key and value query combination is incorrect", "value1", capturedNonHeaderValues.get(KEY_1));
    assertEquals("The second key and value query combination is incorrect", "[\"value2\",\"value3\"]", capturedNonHeaderValues.get(KEY_2));
    assertEquals("The context is incorrect", REQUEST_IN_JSON, capturedHeaderValues.get("context"));
    assertEquals("The entity is incorrect", ENTITY_IN_JSON, capturedNonHeaderValues.get("entityIdentifier"));
  }


  /**
   * Tests the {@linkplain SoapstoneService#put(String, UriInfo, String)} method.
   */
  @Test
  public void testPut() {

    // Given
    operation = "update";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When
    soapstoneService.put(headers, uriInfo, ENTITY);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());
  }


  /**
   * Tests the {@linkplain SoapstoneService#delete(String, UriInfo, String)} method.
   */
  @Test
  public void testDelete() {

    // Given
    operation = "remove";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When
    soapstoneService.delete(headers, uriInfo, ENTITY);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());
  }


  /**
   * Tests that the appropriate {@link NotAllowedException} is thrown when a forbidden operation has been used.
   */
  @Test()
  public void testMethodNotAllowed() {

    // Given
    operation = "forbiddenMethod";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When / Then
    exception.expect(NotAllowedException.class);
    exception.expectMessage("HTTP 405 Method Not Allowed");
    soapstoneService.get(headers, uriInfo);
  }


  /**
   * Tests that the appropriate {@link NotAllowedException} is thrown when a forbidden operation has been used that is
   * supported by another method type.
   */
  @Test()
  public void testMethodNotAllowedButSupportedByDELETE() {

    // Given
    operation = "deleteFile";
    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    soapstoneService = createService();

    // When / Then
    try {
      soapstoneService.get(headers, uriInfo);
    } catch (NotAllowedException e) {
      assertEquals("Exception message is incorrect", "HTTP 405 Method Not Allowed", e.getMessage());
      assertEquals("Allowed methods is incorrect", "{Allow=[POST,DELETE]}", e.getResponse().getHeaders().toString()); // Post and Delete allowed
    }
  }


  /**
   * Tests that when processing a combined context header (e.g. of the format "X-Vendor-Context: userId=USER;realmId=REALM;localeCode=en_gb"), the
   * contained parameters are correctly mapped by the service.
   */
  @Test
  public void testProcessingOfCombinedContextHeader() {

    // Given
    operation = "loadAll";
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.put("X-Vendor-Context", singletonList("userId=USER;realmId=REALM;localeCode=en_gb"));

    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    when(headers.getRequestHeaders()).thenReturn(requestHeaders);

    soapstoneService = createService();

    // When
    soapstoneService.get(headers, uriInfo);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());

    Map<String, String> capturedHeaderValues = captor.getAllValues().get(1);
    assertEquals("The context is incorrect", REQUEST_IN_JSON, capturedHeaderValues.get("context"));
  }


  /**
   * Tests that when processing a series of individual property context headers (e.g. of the format
   * "X-Vendor-Context-UserId: USER", "X-Vendor-Context-RealmId = REALM"... etc), the contained parameters are
   * correctly mapped by the service.
   */
  @Test
  public void testProcessingOfIndividualPropertyContextHeader() {

    // Given
    operation = "loadAll";

    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.put("X-Vendor-Context-UserId", singletonList("USER"));
    requestHeaders.put("X-Vendor-Context-RealmId", singletonList("REALM"));
    requestHeaders.put("X-Vendor-Context-LocaleCode", singletonList("en_gb"));

    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    when(headers.getRequestHeaders()).thenReturn(requestHeaders);
    when(headers.getHeaderString("X-Vendor-Context-UserId")).thenReturn("USER");
    when(headers.getHeaderString("X-Vendor-Context-RealmId")).thenReturn("REALM");
    when(headers.getHeaderString("X-Vendor-Context-LocaleCode")).thenReturn("en_gb");

    soapstoneService = createService();

    // When
    soapstoneService.get(headers, uriInfo);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());

    Map<String, String> capturedHeaderValues = captor.getAllValues().get(1);
    assertEquals("The context is incorrect", REQUEST_IN_JSON, capturedHeaderValues.get("context"));
  }


  /**
   * Tests that if both a combined context header and a series of individual context headers have been given, the
   * service will correctly return a map containing only one set of the parameters (i.e. no potentially conflicting
   * duplicates).
   */
  @Test
  public void testProcessingBothCombinedAndIndividualContextHeaders() {

    // Given
    operation = "loadAll";

    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.put("X-Vendor-Context", singletonList("userId=USER;realmId=REALM;localeCode=en_gb"));
    requestHeaders.put("X-Vendor-Context-UserId", singletonList("OTHER_USER"));
    requestHeaders.put("X-Vendor-Context-RealmId", singletonList("USER"));
    requestHeaders.put("X-Vendor-Context-LocaleCode", singletonList("en_us"));

    when(uriInfo.getPath()).thenReturn(PATH + "/" + operation);
    when(headers.getRequestHeaders()).thenReturn(requestHeaders);
    when(headers.getHeaderString("X-Vendor-Context-UserId")).thenReturn("OTHER_USER");
    when(headers.getHeaderString("X-Vendor-Context-RealmId")).thenReturn("OTHER_REALM");
    when(headers.getHeaderString("X-Vendor-Context-LocaleCode")).thenReturn("en_us");

    String requestInJson = "{\"realmId\":\"OTHER_REALM\",\"localeCode\":\"en_us\",\"userId\":\"OTHER_USER\"}";

    soapstoneService = createService();

    // When
    soapstoneService.get(headers, uriInfo);

    // Then
    // Verify we invoke the operation
    verify(webServiceClass).invokeOperation(eq(operation), captor.capture(), captor.capture());

    Map<String, String> capturedHeaderValues = captor.getAllValues().get(1);
    assertEquals("The captured header values map should contain only one entry", 1, capturedHeaderValues.size());
    assertEquals("The context is incorrect", requestInJson, capturedHeaderValues.get("context"));
  }


  /*
   * Creates a SoapstoneService for testing.
   */
  private SoapstoneService createService() {

    return new SoapstoneServiceBuilder(webServiceClasses)
        .withExceptionMapper(exceptionMapper)
        .withVendor(VENDOR)
        .withSupportedGetOperations("load.*", "list.*", "get.*")
        .withSupportedDeleteOperations("delete.*", "remove.*")
      .withSupportedPutOperations("update.*")
        .build();
  }

}
