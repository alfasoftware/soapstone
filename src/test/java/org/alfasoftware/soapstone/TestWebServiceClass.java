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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.alfasoftware.soapstone.testsupport.CustomParameterClass;
import org.alfasoftware.soapstone.testsupport.MockedClassForTestingJsonHttp;
import org.glassfish.hk2.api.TypeLiteral;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests for the {@link WebServiceClass} class.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestWebServiceClass {

  @Mock
  private ExceptionMapper exceptionMapper;
  @Mock
  private WebApplicationException webApplicationException;
  @Mock
  private ObjectMapper objectMapper;

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  private static final String OPERATION_NAME = "mockedMethod";
  private static final String EXPECTED_RESPONSE = "The method has been invoked!";

  private final Map<String, String> nonHeaderParameters = new HashMap<>();
  private final Map<String, String> headerParameters = new HashMap<>();

  private WebServiceClass<?> webServiceClass;

  /**
   * Set up the tests.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    webServiceClass = WebServiceClass.forClass(MockedClassForTestingJsonHttp.class, MockedClassForTestingJsonHttp::new);

    Mappers.INSTANCE.setObjectMapper(objectMapper);
    Mappers.INSTANCE.setExceptionMapper(exceptionMapper);
  }

  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method
   * functions correctly and invokes the appropriate operation.
   */
  @Test
  public void testInvokeOperation() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When
    Object object = webServiceClass.invokeOperation(OPERATION_NAME, nonHeaderParameters, headerParameters);

    // Then
    assertEquals("The method was not invoked", EXPECTED_RESPONSE, object.toString());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method
   * functions correctly and invokes the appropriate operation when both header and non-header parameters are included.
   */
  @Test
  public void testInvokeOperationWhenHeaderParamsPresent() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");
    headerParameters.put("headerParameter", "headerParameterValue");

    // When
    Object object = webServiceClass.invokeOperation("methodWithCorrectlyAnnotatedHeaderParam", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("The method was not invoked", EXPECTED_RESPONSE, object.toString());
  }


  /**
   * Test that headers are treated as optional. An invocation should not fail if there are missing header parameters.
   */
  @Test
  public void testInvokeOperationWhenHeaderParamsAbsent() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When
    Object object = webServiceClass.invokeOperation("methodWithCorrectlyAnnotatedHeaderParam", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("The method was not invoked", EXPECTED_RESPONSE, object.toString());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly throws a
   * relevant {@link WebApplicationException} when appropriate.
   */
  @Test
  public void testInvokeOperationThrowsWebApplicationException() {

    // Given
    nonHeaderParameters.put("parameter", "throwWebApplicationException");

    when(exceptionMapper.mapThrowable(any(Exception.class), eq(objectMapper))).thenReturn(Optional.of(webApplicationException));

    // When / Then
    exception.expect(WebApplicationException.class);
    webServiceClass.invokeOperation(OPERATION_NAME, nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly throws a
   * relevant {@link InternalServerErrorException} when appropriate if the exception mapper is not present.
   */
  @Test
  public void testInvokeOperationThrowsInternalServerErrorExceptionWhenExceptionMapperNotPresent() {

    Mappers.INSTANCE.setExceptionMapper(null);

    // Given
    nonHeaderParameters.put("parameter", "throwWebApplicationException");

    // When / Then
    exception.expect(InternalServerErrorException.class);
    webServiceClass.invokeOperation(OPERATION_NAME, nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly
   * throws an appropriate {@link NotFoundException} if the operation cannot be found.
   */
  @Test
  public void testInvokeOperationMethodNotFound() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When / Then
    exception.expect(NotFoundException.class);
    webServiceClass.invokeOperation("nonExistentMethod", nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)}
   * method functions correctly and throws an appropriate {@link NotFoundException} if the method in question is not
   * public as only public methods can be published.
   */
  @Test
  public void testInvokeOperationMethodNotPublic() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When / Then
    exception.expect(NotFoundException.class);
    webServiceClass.invokeOperation("privateMethod", nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)}
   * method functions correctly and throws an appropriate {@link NotFoundException} if the method in question has
   * as WebMethod annotation, but exclude is set to true.
   */
  @Test
  public void testInvokeOperationMethodExcluded() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When / Then
    exception.expect(NotFoundException.class);
    webServiceClass.invokeOperation("excludedMethod", nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly
   * throws an appropriate {@link BadRequestException} if unable to distinguish between two methods.
   */
  @Test
  public void testInvokeOperationUnableToDistinguishMethods() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");

    // When / Then
    exception.expect(BadRequestException.class);
    exception.expectMessage("Unable to distinguish methods");
    webServiceClass.invokeOperation("overloadedMethod", nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly
   * throws an appropriate {@link BadRequestException} if the header parameters are not correctly annotated with
   * {@code @WebParam(header = true)}.
   */
  @Test
  public void testInvokeOperationHeaderParametersIncorrectlyAnnotated() {

    // Given
    nonHeaderParameters.put("parameter", "parameterValue");
    headerParameters.put("headerParameter", "headerParameterValue");

    // When / Then
    exception.expect(BadRequestException.class);
    webServiceClass.invokeOperation("methodWithIncorrectlyAnnotatedHeaderParam", nonHeaderParameters, headerParameters);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a string.
   */
  @Test
  public void testParameterToTypeParameterString() {

    // Given
    nonHeaderParameters.put("stringParameter", "parameterValue");

    // When
    Object parameter = webServiceClass.invokeOperation("mockedMethodWithStringArg", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("Parameter type is incorrect", String.class, parameter.getClass());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a boolean.
   */
  @Test
  public void testParameterToTypeParameterBoolean() {

    // Given
    nonHeaderParameters.put("booleanParameter", "true");

    // When
    Object parameter = webServiceClass.invokeOperation("mockedMethodWithBooleanArg", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("Parameter type is incorrect", Boolean.class, parameter.getClass());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a int.
   */
  @Test
  public void testParameterToTypeParameterInt() {

    // Given
    nonHeaderParameters.put("intParameter", "34");

    // When
    Object parameter = webServiceClass.invokeOperation("mockedMethodWithIntArg", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("Parameter type is incorrect", Integer.class, parameter.getClass());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a LocalDate.
   */
  @Test
  public void testParameterToTypeParameterLocalDate() {

    // Given
    nonHeaderParameters.put("localDateParameter", "01/02/2019");

    // When
    Object parameter = webServiceClass.invokeOperation("mockedMethodWithLocalDateArg", nonHeaderParameters, headerParameters);

    // Then
    assertEquals("Parameter type is incorrect", LocalDate.class, parameter.getClass());
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a custom class: here, {@link CustomParameterClass}.
   */
  @Test
  public void testParameterToTypeParameterCustomClass() throws IOException {

    // Given
    String argumentAsString = "{\"fieldOne\":\"VALUE\",\"fieldTwo\":1}";
    nonHeaderParameters.put("customParameter", argumentAsString);

    JavaType javaType = mock(JavaType.class);
    Type type = CustomParameterClass.class;
    when(objectMapper.constructType(type)).thenReturn(javaType);

    // When
    webServiceClass.invokeOperation("mockedMethodWithCustomParameterClassArg", nonHeaderParameters, headerParameters);

    // Then
    verify(objectMapper).constructType(type);
    verify(objectMapper).readValue(argumentAsString, javaType);
  }


  /**
   * Tests that the {@link WebServiceClass#invokeOperation(String, Map, Map)} method correctly maps
   * parameters for methods that require a list of a custom class: here, {@link CustomParameterClass}.
   */
  @Test
  public void testParameterToListOfTypeParameterCustomClass() throws IOException {

    // Given
    String argumentAsString = "[ {\"fieldOne\":\"VALUE\",\"fieldTwo\":1}, {\"fieldOne\":\"ANOTHER_VALUE\",\"fieldTwo\":2} ]";
    nonHeaderParameters.put("customParameters", argumentAsString);

    JavaType javaType = mock(JavaType.class);
    Type type = new TypeLiteral<List<CustomParameterClass>>() {
    }.getType();
    when(objectMapper.constructType(type)).thenReturn(javaType);

    // When
    webServiceClass.invokeOperation("mockedMethodWithListOfCustomParameterClassArg", nonHeaderParameters, headerParameters);

    // Then
    verify(objectMapper).constructType(type);
    verify(objectMapper).readValue(argumentAsString, javaType);
  }
}

