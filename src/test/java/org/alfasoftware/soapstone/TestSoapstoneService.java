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

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.METHOD_NOT_ALLOWED;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.OK;
import static java.util.Arrays.asList;
import static org.alfasoftware.soapstone.testsupport.WebService.Value.VALUE_1;
import static org.alfasoftware.soapstone.testsupport.WebService.Value.VALUE_2;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.jws.WebMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.MyException;
import org.alfasoftware.soapstone.testsupport.WebService.RequestObject;
import org.alfasoftware.soapstone.testsupport.WebService.ResponseObject;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;


/**
 * Integration tests for soapstone
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestSoapstoneService extends JerseyTest {


  private static final String VENDOR = "Vendor";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JaxbAnnotationModule())
    .registerModule(new JodaModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private static final ExceptionMapper EXCEPTION_MAPPER = (exception, o) ->
    Optional.ofNullable(exception instanceof MyException ? new BadRequestException() : null);

  private static final Pattern TAG_PATTERN = Pattern.compile("/?(?<tag>.*?)(?:/.*)?");
  private static final Function<String, String> TAG_PROVIDER = path -> {
    Matcher matcher = TAG_PATTERN.matcher(path);
    return matcher.matches() ? matcher.group("tag") : null;
  };


  @Override
  protected Application configure() {

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

    SoapstoneService service = new SoapstoneServiceBuilder(webServices)
      .withVendor(VENDOR)
      .withVersionNumber("main")
      .withExceptionMapper(EXCEPTION_MAPPER)
      .withSupportedGetOperations("get.*")
      .withSupportedPutOperations("put.*")
      .withSupportedDeleteOperations("delete.*")
      .withTagProvider(TAG_PROVIDER)
      .build();


    Logger logger = Logger.getLogger("test");
    logger.setLevel(Level.FINE);
    logger.setUseParentHandlers(false);
    Handler[] handlers = logger.getHandlers();
    for(Handler h : handlers) logger.removeHandler(h);
    logger.addHandler(streamHandler());

    return new ResourceConfig()
      .registerInstances(service)
      .register(
         new LoggingFeature(logger, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 8192))
          .register(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(mock(HttpServletRequest.class)).to(HttpServletRequest.class);
            }
         });
    }

  static StreamHandler streamHandler() {
    final StreamHandler sh = new StreamHandler(System.out, new SimpleFormatter()) {
      @Override
      public synchronized void publish(final LogRecord record) {
        super.publish(record);
        flush();
      }
    };
    sh.setLevel(Level.FINE);
    return sh;
  }


  /**
   * Initialise the bridge to divert all logging to the same logger.
   *
   * <p>
   * Logging is configured in /src/main/test/resources/simplelogger.properties and is probably set to log at 'warn' as
   * the test container produces quite a lot of noise at 'info' or below. Lower the priority if you need more detail.
   * </p>
   */
  @BeforeClass
  public static void initLogger() {

    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
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
   * Test that we can POST with only some required parameters passed in via the payload. All other parameter should be
   * inferred as null or the primitive equivalent.
   */
  @Test
  public void testPostWithInferredParameters() throws Exception {

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

    /*
     * When
     */
    String responseString = target()
      .path("path/doAThing")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertNull(responseObject.getHeaderString());
    assertEquals(0, responseObject.getHeaderInteger());
    assertNull(responseObject.getString());
    assertEquals(0, responseObject.getInteger());
    assertEquals(0D, responseObject.getDecimal(), 0D);
    assertFalse(responseObject.isBool());
    assertNull(responseObject.getDate());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we get a bad request response if we try to pass a non-header parameter as a header
   */
  @Test
  public void testNonHeaderParametersAsHeaders() {

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

    /*
     * When
     */
    Response response = target()
      .path("path/doAThing")
      .request()
      .header("X-Vendor-Request", "string=value")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

    /*
     * Then
     */
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a bad request response if we try to pass an unrecognised parameter
   */
  @Test
  public void testUnrecognisedParameters() {

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
    payload.put("unrecognised", "string");

    /*
     * When
     */
    Response response = target()
      .path("path/doAThing")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

    /*
     * Then
     */
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a bad request response if we omit a required parameter
   */
  @Test
  public void testMissingRequiredParameters() {

    /*
     * Given
     */
    Map<String, Object> payload = new HashMap<>();
    /*
     * When
     */
    Response response = target()
      .path("path/doAThing")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

    /*
     * Then
     */
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
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
      .header("X-Vendor-Header", "string=test@email.com;integer=62")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(Collections.singletonMap("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("test@email.com", responseObject.getHeaderString());
    assertEquals(62, responseObject.getHeaderInteger());
    assertEquals("value", responseObject.getString());
    assertEquals(65, responseObject.getInteger());
    assertEquals(33.45D, responseObject.getDecimal(), 0D);
    assertTrue(responseObject.isBool());
    assertEquals(new LocalDate("2019-03-29"), responseObject.getDate());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we can POST with multiple values for a query parameter
   */
  @Test
  public void testPostWithListQueryParams() {

    Response response = target()
      .path("path/doAListOfThings")
      .queryParam("list", VALUE_1, VALUE_2)
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity("", MediaType.APPLICATION_JSON));

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can POST with a JSON list value for a query parameter
   */
  @Test
  public void testPostWithJsonListQueryParams() {

    Response response = target()
      .path("path/doAListOfThings")
      .queryParam("list", "[ \"VALUE_1\", \"VALUE_2\" ]")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity("", MediaType.APPLICATION_JSON));

    assertEquals(OK.getStatusCode(), response.getStatus());
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
      .post(Entity.entity(Collections.singletonMap("request", requestObject), MediaType.APPLICATION_JSON), String.class);

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
      .post(Entity.entity(Collections.singletonMap("request", requestObject), MediaType.APPLICATION_JSON), String.class);

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
      .post(Entity.entity(Collections.singletonMap("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = OBJECT_MAPPER.readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertNull(responseObject.getHeaderString());
    assertEquals("value", responseObject.getString());
    assertEquals(requestObject, responseObject.getNestedObject());
  }


  /**
   * Test that we get a bad request response if we pass junk in the payload
   */
  @Test
  public void testPostJunkPayload() {

    Response response = target()
      .path("path/doASimpleThing")
      .queryParam("string", "value")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(Collections.singletonMap("request", "boogie nights"), MediaType.APPLICATION_JSON));

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a bad request response if we pass junk in a query parameter
   */
  @Test
  public void testPostJunkQueryParameter() {

    Response response = target()
      .path("path/doAListOfThings")
      .queryParam("list", "boogie nights")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity("", MediaType.APPLICATION_JSON));

    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can use GET for a method which matches the pattern provided
   */
  @Test
  public void testGet() {

    Response response = target()
      .path("path/getAThing")
      .queryParam("string", "thing")
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
      .put(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

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
      .put(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

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
   * Test that we get type information when returning a list of some supertype
   */
   @Test
   public void testGetAListOfThings() throws JsonProcessingException {

     String response = target()
       .path("path/getAListOfThings")
       .request()
       .get(String.class);


     JavaType returnType = OBJECT_MAPPER.constructType(new TypeLiteral<List<WebService.SuperClass>>() {}.getType());
     List<WebService.SuperClass> list = OBJECT_MAPPER.readerFor(returnType).readValue(response);

     assertThat(list, containsInAnyOrder(asList(
       instanceOf(WebService.SuperClass.SubClass1.class),
       instanceOf(WebService.SuperClass.SubClass2.class)
     )));
   }


  /**
   * Test that we get a 404 for requesting a method which is not exposed
   */
  @Test
  public void testExcludedMethod() {

    Response response = target()
      .path("path/doNotDoAThing")
      .request()
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can successfully invoke a method by a name specified via {@link WebMethod#operationName()}
   */
  @Test
  public void testMethodWithOperationName() {

    Response response = target()
      .path("path/doAThingWithThisName")
      .request()
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we if a method has an operation name specified via {@link WebMethod#operationName()}, we cannot invoke
   * it via the method name
   */
  @Test
  public void testMethodMaskedByOperationName() {

    Response response = target()
      .path("path/doNotDoAThingWithThisName")
      .request()
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

    assertEquals(NOT_FOUND.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we get a 404 for requesting a path without an operation
   */
  @Test
  public void testMissingOperation() {

    Response response = target()
      .path("path")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

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
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

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
      .post(Entity.entity(Collections.singletonMap("request", new RequestObject()), MediaType.APPLICATION_JSON));

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


  /**
   * Test that we can list all tags used for Open API documents.
   */
  @Test
  public void testGetOpenApiTags() {

    List<String> response = target()
      .path("openapi/tags")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .get(new GenericType<List<String>>() {});

    assertTrue(response.contains("path"));
  }


  /**
   * Test that we can request the Open API document in JSON format.
   *
   * <p>
   * Test of the document generation is handled in {@link TestSoapstoneOpenApiReader}.
   * All we will do here is confirm that we get a successful response in the correct
   * format.
   * </p>
   */
  @Test
  public void testGetOpenApiJson() {

    Response response = target()
      .path("openapi.json")
      .request()
      .accept(MediaType.APPLICATION_JSON)
      .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
  }


  /**
   * Test that we can request the Open API document in YAML format.
   *
   * <p>
   * Test of the document generation is handled in {@link TestSoapstoneOpenApiReader}.
   * All we will do here is confirm that we get a successful response in the correct
   * format.
   * </p>
   */
  @Test
  public void testGetOpenApiYaml() {

    Response response = target()
      .path("openapi.yaml")
      .request()
      .accept("text/vnd.yaml")
      .get();

    assertEquals(OK.getStatusCode(), response.getStatus());
  }
}
