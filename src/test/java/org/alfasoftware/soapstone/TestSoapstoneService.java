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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import com.google.common.collect.ImmutableMap;
import org.alfasoftware.soapstone.ExceptionMapper;
import org.alfasoftware.soapstone.SoapstoneService;
import org.alfasoftware.soapstone.SoapstoneServiceBuilder;
import org.alfasoftware.soapstone.WebServiceClass;
import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.MyException;
import org.alfasoftware.soapstone.testsupport.WebService.RequestObject;
import org.alfasoftware.soapstone.testsupport.WebService.ResponseObject;
import org.glassfish.jersey.test.JerseyTest;
import org.joda.time.LocalDate;
import org.junit.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Integration tests for soapstone
 */
public class TestSoapstoneService extends JerseyTest {


  private static final String VENDOR = "Vendor";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JaxbAnnotationModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final ExceptionMapper EXCEPTION_MAPPER = (exception, o) ->
    Optional.ofNullable(exception instanceof MyException ? new BadRequestException() : null);


  @Override
  protected Application configure() {
    return new Application() {

      @Override
      public Set<Object> getSingletons() {

        Map<String, WebServiceClass<?>> webServices = new HashMap<>();
        webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

        SoapstoneService service = new SoapstoneServiceBuilder(webServices)
          .withVendor(VENDOR)
          .withObjectMapper(OBJECT_MAPPER)
          .withExceptionMapper(EXCEPTION_MAPPER)
          .withSupportedGetOperations("get.*")
          .withSupportedPutOperations("put.*")
          .withSupportedDeleteOperations("delete.*")
          .build();

        return Collections.singleton(service);
      }
    };
  }


  /**
   * Test that we can POST with all required parameters (other than header) passed in
   * via the payload.
   */
  @Test
  public void testPostWithPayload() throws Exception {

    /*
     * Given
     */
    RequestObject requestObject = new RequestObject();
    requestObject.setBool(false);
    requestObject.setString("complexValue");
    requestObject.setInteger(897);
    requestObject.setDecimal(88.55D);
    requestObject.setDate("2001-12-16");

    Map<String, Object> payload = new HashMap<>();
    payload.put("request", requestObject);
    payload.put("string", "value");
    payload.put("integer", 65);
    payload.put("decimal", 33.45D);
    payload.put("bool", true);
    payload.put("date", "2019-03-29");

    /*
     * When
     */
    String responseString = target()
      .path("path/doAThing")
      .request()
      .header("X-Vendor-Header", "string=headerStringValue;integer=62")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeaderString());
    assertEquals(62, responseObject.getHeaderInteger());
    assertEquals("value", responseObject.getString());
    assertEquals(65, responseObject.getInteger());
    assertEquals(33.45D, responseObject.getDecimal(), 0D);
    assertTrue(responseObject.isBool());
    assertEquals(new LocalDate("2019-03-29"), responseObject.getDate());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can POST with all simple parameters passed in as query parameters
   */
  @Test
  public void testPostWithQueryParams() throws Exception {

    /*
     * Given
     */
    RequestObject requestObject = new RequestObject();
    requestObject.setBool(false);
    requestObject.setString("complexValue");
    requestObject.setInteger(897);
    requestObject.setDecimal(88.55D);
    requestObject.setDate("2001-12-16");

    /*
     * When
     */
    String responseString = target()
      .path("path/doAThing")
      .queryParam("string", "value")
      .queryParam("integer", 65)
      .queryParam("decimal", 33.45D)
      .queryParam("bool", true)
      .queryParam("date", "2019-03-29")
      .request()
      .header("X-Vendor-Header", "string=headerStringValue;integer=62")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeaderString());
    assertEquals(62, responseObject.getHeaderInteger());
    assertEquals("value", responseObject.getString());
    assertEquals(65, responseObject.getInteger());
    assertEquals(33.45D, responseObject.getDecimal(), 0D);
    assertTrue(responseObject.isBool());
    assertEquals(new LocalDate("2019-03-29"), responseObject.getDate());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can POST with individual header properties specified
   */
  @Test
  public void testPostWithHeaderProperties() throws Exception {

    /*
     * Given
     */
    RequestObject requestObject = new RequestObject();

    /*
     * When
     */
    String responseString = target()
      .path("path/doASimpleThing")
      .queryParam("string", "value")
      .request()
      .header("X-Vendor-Header-String", "headerStringValue")
      .header("X-Vendor-Header-Integer", 62)
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeaderString());
    assertEquals(62, responseObject.getHeaderInteger());
    assertEquals("value", responseObject.getString());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can POST with full header and individual header properties specified
   *
   * <p>
   * Individual properties should override the values on the full header
   * </p>
   */
  @Test
  public void testPostWithHeaderAndProperties() throws Exception {

    /*
     * Given
     */
    RequestObject requestObject = new RequestObject();

    /*
     * When
     */
    String responseString = target()
      .path("path/doASimpleThing")
      .queryParam("string", "value")
      .request()
      .header("X-Vendor-Header", "string=headerStringValue;integer=62")
      .header("X-Vendor-Header-Integer", 89)
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeaderString());
    assertEquals(89, responseObject.getHeaderInteger());
    assertEquals("value", responseObject.getString());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can POST and header parameters are treated as optional
   */
  @Test
  public void testPostWithNoHeader() throws Exception {

    /*
     * Given
     */
    RequestObject requestObject = new RequestObject();

    /*
     * When
     */
    String responseString = target()
      .path("path/doASimpleThing")
      .queryParam("string", "value")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertNull(responseObject.getHeaderString());
    assertEquals("value", responseObject.getString());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can use GET for a method which matches the pattern provided
   */
  @Test
  public void testGet() {

    Response response = target()
      .path("path/getAThing")
      .request()
      .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we cannot use GET for a method which does not match the pattern provided
   */
  @Test
  public void testGetNotAllowed() {

    Response response = target()
      .path("path/doAThing")
      .request()
      .get();

    assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can use PUT for a method which matches the pattern provided
   */
  @Test
  public void testPut() {

    Response response = target()
      .path("path/putAThing")
      .request()
      .put(Entity.entity(ImmutableMap.of("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we cannot use PUT for a method which does not match the pattern provided
   */
  @Test
  public void testPutNotAllowed() {

    Response response = target()
      .path("path/doAThing")
      .request()
      .put(Entity.entity(ImmutableMap.of("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can use DELETE for a method which matches the pattern provided
   */
  @Test
  public void testDelete() {

    Response response = target()
      .path("path/deleteAThing")
      .request()
      .delete();

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we cannot use DELETE for a method which does not match the pattern provided
   */
  @Test
  public void testDeleteNotAllowed() {

    Response response = target()
      .path("path/doAThing")
      .request()
      .delete();

    assertEquals(METHOD_NOT_ALLOWED.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a 404 for requesting a method which is not exposed
   */
  @Test
  public void testExcludedMethod() {

    Response response = target()
      .path("path/doNotDoAThing")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a 404 for requesting a path which does not correspond to an
   * exposed service
   */
  @Test
  public void testUnmappedService() {

    Response response = target()
      .path("not-the-path/doAThing")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get the correctly mapped error (bad request) if an exception is
   * thrown which is handled by the exception mapper
   */
  @Test
  public void testMappedException() {

    Response response = target()
      .path("path/doAThingBadly")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get an internal server error if an exception is thrown which is
   * not handled by the exception mapper
   */
  @Test
  public void testUnmappedException() {

    Map<String, Object> payload = new HashMap<>();
    payload.put("request", null);

    Response response = target()
      .path("path/doAThingBadly")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

}
