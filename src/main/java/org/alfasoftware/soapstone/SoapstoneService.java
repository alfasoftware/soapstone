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

import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;


/**
 * Service to map incoming JSON HTTP requests to provided web service implementations
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
@Path("")
public class SoapstoneService {

  private static final Logger LOG = LoggerFactory.getLogger(SoapstoneService.class);

  private final Map<String, OpenAPI> openAPIDefinitions = new ConcurrentHashMap<>();
  private final SoapstoneConfiguration configuration;
  private final WebParameterMapper webParameterMapper;
  private final WebServiceInvoker invoker;


  /**
   *
   */
  SoapstoneService(SoapstoneConfiguration configuration) {
    this.configuration = configuration;
    this.webParameterMapper = new WebParameterMapper(configuration);
    this.invoker = new WebServiceInvoker(configuration);
  }


  /**
   * Maps incoming JSON HTTP POST requests to web service implementations and executes the relevant method.
   *
   * @param headers The HTTP header information
   * @param uriInfo The application and request URI information
   * @param entity  The entity to be parsed as a JSON object
   * @return JSON representation of the underlying web service response
   */
  @POST
  @Path("/{s:.*}")
  @Produces(APPLICATION_JSON)
  @Consumes(APPLICATION_JSON)
  public String post(@Context HttpHeaders headers, @Context UriInfo uriInfo, String entity) {
    LOG.info("POST " + uriInfo.getRequestUri());
    return process(headers, uriInfo, entity, POST);
  }


  /**
   * Maps incoming JSON HTTP GET requests to web service implementations and executes the relevant method.
   *
   * @param headers The HTTP header information
   * @param uriInfo The application and request URI information
   * @return JSON representation of the underlying web service response
   */
  @GET
  @Path("/{s:.*}")
  @Produces(APPLICATION_JSON)
  public String get(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
    LOG.info("GET " + uriInfo.getRequestUri());
    return process(headers, uriInfo, null, GET);
  }


  /**
   * Maps incoming JSON HTTP PUT requests to web service implementations and executes the relevant method.
   *
   * @param headers The HTTP header information
   * @param uriInfo The application and request URI information
   * @param entity  The entity to be parsed as a JSON object
   * @return JSON representation of the underlying web service response
   */
  @PUT
  @Path("/{s:.*}")
  @Produces(APPLICATION_JSON)
  @Consumes(APPLICATION_JSON)
  public String put(@Context HttpHeaders headers, @Context UriInfo uriInfo, String entity) {
    LOG.info("PUT " + uriInfo.getRequestUri());
    return process(headers, uriInfo, entity, PUT);
  }


  /**
   * Maps incoming JSON HTTP DELETE requests to web service implementations and executes the relevant method.
   *
   * @param headers The HTTP header information
   * @param uriInfo The application and request URI information
   * @param entity  The entity to be parsed as a JSON object
   * @return JSON representation of the underlying web service response
   */
  @DELETE
  @Path("/{s:.*}")
  @Produces(APPLICATION_JSON)
  @Consumes(APPLICATION_JSON)
  public String delete(@Context HttpHeaders headers, @Context UriInfo uriInfo, String entity) {
    LOG.info("DELETE " + uriInfo.getRequestUri());
    return process(headers, uriInfo, entity, DELETE);
  }


  /**
   * Enumerate all tags available for inclusion in Open API documents
   *
   * @return set of tags
   */
  @GET
  @Path("openapi/tags")
  @Produces(APPLICATION_JSON)
  public Set<String> getOpenApiTags() {
    LOG.info("Retrieving list of tags for Open API");
    return configuration.getWebServiceClasses().keySet().stream()
      .map(path -> path.split("/")[1])
      .collect(Collectors.toCollection(TreeSet::new));
  }


  /**
   * Get an Open API document including all provided tags in JSON format.
   *
   * <p>
   * If no tags are specified then all tags will be included.
   * </p>
   *
   * @param uriInfo The application and request URI information
   * @param tags set of tags to include in the document. If none provided then all tags will be used.
   * @return Open API document as JSON
   */
  @GET
  @Path("openapi.json")
  @Produces(APPLICATION_JSON)
  public String getOpenApiJson(@Context UriInfo uriInfo, @QueryParam("tag") Set<String> tags) {
    LOG.info("Retrieving Open API JSON");
    return Json.pretty(getOpenAPI(uriInfo.getBaseUri().toASCIIString(), tags));
  }


  /**
   * Get an Open API document including all provided tags in YAML format.
   *
   * <p>
   * If no tags are specified then all tags will be included.
   * </p>
   *
   * @param uriInfo The application and request URI information
   * @param tags set of tags to include in the document. If none provided then all tags will be used.
   * @return Open API document as YAML
   */
  @GET
  @Path("openapi.yaml")
  @Produces("text/vnd.yaml")
  public String getOpenApiYaml(@Context UriInfo uriInfo, @QueryParam("tag") Set<String> tags) {
    LOG.info("Retrieving Open API YAML");
    return Yaml.pretty(getOpenAPI(uriInfo.getBaseUri().toASCIIString(), tags));
  }


  /**
   * This is pretty rough and ready. Cache the openAPI definitions as they are quite expensive to
   * generate. They are also quite large, so caching may not be the cleverest thing to do...
   *
   * This should be superseded by some merge function.
   */
  private OpenAPI getOpenAPI(final String baseUri, final Set<String> tags) {

    synchronized (openAPIDefinitions) {

      Function<String, OpenAPI> f = str -> {
        SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(baseUri, configuration);
        reader.setConfiguration(new SwaggerConfiguration());

        return reader.read(tags);
      };

      String tagsKey = "_";
      if (tags != null && !tags.isEmpty()) {
        tagsKey = tags.stream().sorted().collect(Collectors.joining("_"));
      }

      return openAPIDefinitions.computeIfAbsent(tagsKey, f);
    }
  }


  /*
   * Processes the request.
   */
  private String process(HttpHeaders headers, UriInfo uriInfo, String entity, String method) {

    String fullPath = uriInfo.getPath();
    // Check we have a legal path: path/operation
    if (fullPath.indexOf('/') < 0) {
      LOG.error("Path " + fullPath + "should include an operation");
      throw new NotFoundException();
    }

    // Split into path and operation
    String operationName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
    String path = fullPath.substring(0, fullPath.lastIndexOf('/'));

    // Check that the method used is supported for the requested operation
    assertMethodSupported(operationName, method);

    // Locate the web service class
    WebServiceClass<?> webServiceClass = configuration.getWebServiceClasses().get(path);
    if (webServiceClass == null) {
      LOG.error("No web service class mapped for " + path);
      throw new NotFoundException();
    }

    // Construct parameters
    Collection<WebParameter> parameters = webParameterMapper.fromQueryParams(uriInfo.getQueryParameters());
    parameters.addAll(webParameterMapper.fromHeaders(headers, configuration.getVendor()));
    parameters.addAll(webParameterMapper.fromEntity(entity));

    // Invoke the operation
    return invoker.invokeOperation(webServiceClass, operationName, parameters);
  }


  /*
   * Checks if the method type is supported. If not, throws a 405 Method Not Allowed exception.
   */
  private void assertMethodSupported(String operationName, String method) {

    // POST is always supported
    if (POST.equals(method)) {
      return;
    }

    Set<String> supportedTypes = getSupportedMethods(operationName); // Return a set of supported methods

    if (!supportedTypes.contains(method)) { // If our method type is not supported...
      LOG.error(method + " not supported for " + operationName);
      throw new NotAllowedException(POST, supportedTypes.toArray(new String[0])); // ... throw a 405 Method Not Allowed, specifying which method types ARE allowed
    }
  }


  /*
   * Returns a list of supported method types.
   */
  private Set<String> getSupportedMethods(String operationName) {

    // Find supported operations
    Set<String> supportedMethods = new HashSet<>();

    Pattern supportedGetOperations = configuration.getSupportedGetOperations();
    if (supportedGetOperations != null && supportedGetOperations.matcher(operationName).matches()) {
      supportedMethods.add(GET);
    }

    Pattern supportedDeleteOperations = configuration.getSupportedDeleteOperations();
    if (supportedDeleteOperations != null && supportedDeleteOperations.matcher(operationName).matches()) {
      supportedMethods.add(DELETE);
    }

    Pattern supportedPutOperations = configuration.getSupportedPutOperations();
    if (supportedPutOperations != null && supportedPutOperations.matcher(operationName).matches()) {
      supportedMethods.add(PUT);
    }

    return supportedMethods;
  }

}

