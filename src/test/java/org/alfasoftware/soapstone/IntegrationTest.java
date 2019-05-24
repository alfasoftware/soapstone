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

import com.google.common.collect.ImmutableMap;
import org.alfasoftware.soapstone.testsupport.WebService;
import org.alfasoftware.soapstone.testsupport.WebService.RequestObject;
import org.alfasoftware.soapstone.testsupport.WebService.ResponseObject;
import org.glassfish.jersey.test.JerseyTest;
import org.joda.time.LocalDate;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Integration tests for soapstone
 */
public class IntegrationTest extends JerseyTest {


  private static final String vendor = "Vendor";

  @Override
  protected Application configure() {
    return new Application() {

      @Override
      public Set<Object> getSingletons() {

        Map<String, WebServiceClass<?>> webServices = new HashMap<>();
        webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

        SoapstoneService service = new SoapstoneServiceBuilder(webServices)
          .withVendor(vendor)
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
      .header("X-Vendor-Header", "string=headerStringValue")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(payload, MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = Mappers.INSTANCE.getObjectMapper().readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeader());
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
      .header("X-Vendor-Header", "string=headerStringValue")
      .accept(MediaType.APPLICATION_JSON)
      .post(Entity.entity(ImmutableMap.of("request", requestObject), MediaType.APPLICATION_JSON), String.class);

    ResponseObject responseObject = Mappers.INSTANCE.getObjectMapper().readValue(responseString, ResponseObject.class);

    /*
     * Then
     */
    assertEquals("headerStringValue", responseObject.getHeader());
    assertEquals("value", responseObject.getString());
    assertEquals(65, responseObject.getInteger());
    assertEquals(33.45D, responseObject.getDecimal(), 0D);
    assertTrue(responseObject.isBool());
    assertEquals(new LocalDate("2019-03-29"), responseObject.getDate());
    assertEquals(requestObject, responseObject.getNestedObject());
  }
}
