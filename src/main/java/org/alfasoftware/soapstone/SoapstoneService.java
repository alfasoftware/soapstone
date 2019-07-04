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
import static org.alfasoftware.soapstone.WebParameter.parameter;
import static org.alfasoftware.soapstone.WebParameters.fromHeaders;
import static org.alfasoftware.soapstone.WebParameters.fromQueryParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.alfasoftware.soapstone.openapi.SoapstoneOpenApiReader;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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


  private final ObjectMapper objectMapper;
  private final Map<String, WebServiceClass<?>> webServiceClasses;
  private final String vendor;
  private final Pattern supportedGetOperations;
  private final Pattern supportedDeleteOperations;
  private final Pattern supportedPutOperations;


  /**
   */
  SoapstoneService(SoapstoneServiceConfiguration configuration) {
    this.objectMapper = configuration.getObjectMapper();
    this.webServiceClasses = configuration.getWebServiceClasses();
    this.vendor = configuration.getVendor();
    this.supportedGetOperations = configuration.getSupportedGetOperations();
    this.supportedDeleteOperations = configuration.getSupportedDeleteOperations();
    this.supportedPutOperations = configuration.getSupportedPutOperations();
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
    return process(headers, uriInfo, entity);
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
    checkIfMethodSupported(uriInfo, GET);
    return process(headers, uriInfo, null);
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
    checkIfMethodSupported(uriInfo, PUT);
    return process(headers, uriInfo, entity);
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
    checkIfMethodSupported(uriInfo, DELETE);
    return process(headers, uriInfo, entity);
  }


  @GET
  @Path("openapi.json/tags")
  @Produces(APPLICATION_JSON)
  public Set<String> getOpenApiTags() {
    LOG.info("Retrieving list of tags for Open API");
    return webServiceClasses.keySet().stream()
      .map(path -> path.split("/")[1])
      .collect(Collectors.toCollection(TreeSet::new));
  }


  @GET
  @Path("openapi.json")
  @Produces(APPLICATION_JSON)
  public String getOpenApiJson(@Context UriInfo uriInfo, @QueryParam("tag") Set<String> tags) {
    LOG.info("Retrieving Open API JSON");
    return Json.pretty(getOpenAPI(uriInfo.getBaseUri().toASCIIString(), tags));
  }


  @GET
  @Path("openapi.yaml")
  @Produces("application/yaml")
  public String getOpenApiYaml(@Context UriInfo uriInfo, @QueryParam("tag") Set<String> tags) {
    LOG.info("Retrieving Open API YAML");
    return Yaml.pretty(getOpenAPI(uriInfo.getBaseUri().toASCIIString(), tags));
  }


  /**
   * This is pretty rough and ready. Cache the openAPI definitions as they are quite expensive to
   * generate. They are also quite large, so caching may not be the cleverest thing to do...
   */
  private OpenAPI getOpenAPI(final String baseUri, final Set<String> tags) {

    synchronized (openAPIDefinitions) {

      Function<String, OpenAPI> f = str -> {
        SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(baseUri);
        reader.setConfiguration(new SwaggerConfiguration());

        return reader.read(tags);
      };

      String tagsKey = "_";
      if (tags != null && ! tags.isEmpty()) {
        tagsKey = tags.stream().sorted().collect(Collectors.joining("_"));
      }

      return openAPIDefinitions.computeIfAbsent(tagsKey, f);
    }
  }


  /*
   * Processes the request.
   */
  private String process(HttpHeaders headers, UriInfo uriInfo, String entity) {

    Collection<WebParameter> parameters = fromQueryParams(uriInfo);
    parameters.addAll(fromHeaders(headers, vendor));

    if (StringUtils.isNotBlank(entity)) {
      try {
        JsonNode jsonNode = objectMapper.readTree(entity);
        jsonNode.fields().forEachRemaining(entry ->
            parameters.add(parameter(entry.getKey(), entry.getValue()))
        );
      } catch (IOException e) {
        throw new BadRequestException(
            Response.status(Response.Status.BAD_REQUEST)
                .entity(entity)
                .build());
      }
    }

    return execute(uriInfo.getPath(), parameters);
  }


  /*
   * Returns a list of supported method types.
   */
  private List<String> getSupportedMethods(UriInfo uriInfo) {

    String path = uriInfo.getPath();
    String operationName = path.substring(path.lastIndexOf('/') + 1);

    // Find supported operations
    List<String> supportedTypes = new ArrayList<>();

    if (supportedGetOperations != null && supportedGetOperations.matcher(operationName).matches()) {
      supportedTypes.add(GET);
    }

    if (supportedDeleteOperations != null && supportedDeleteOperations.matcher(operationName).matches()) {
      supportedTypes.add(DELETE);
    }

    if (supportedPutOperations != null && supportedPutOperations.matcher(operationName).matches()) {
      supportedTypes.add(PUT);
    }

    return supportedTypes;
  }


  /*
   * Checks if the method type is supported. If not, throws a 405 Method Not Allowed exception.
   */
  private void checkIfMethodSupported(UriInfo uriInfo, String type) {

    List<String> supportedTypes = getSupportedMethods(uriInfo); // Return a list of supported operations

    if (!supportedTypes.contains(type)) { // If our method type is not supported...
      LOG.error(type + " not supported for " + uriInfo);
      throw new NotAllowedException(POST, supportedTypes.toArray(new String[0])); // ... throw a 405 Method Not Allowed, specifying which method types ARE allowed
    }
  }


  /*
   * Execute the request.
   */
  private String execute(String path, Collection<WebParameter> parameters) {

    // Check we have a legal path: path/operation
    if (path.indexOf('/') < 0) {
      LOG.error("Path " + path + "should include an operation");
      throw new NotFoundException();
    }

    // Split into path and operation
    String operationName = path.substring(path.lastIndexOf('/') + 1);
    String pathKey = path.substring(0, path.lastIndexOf('/'));

    // Check the path is mapped to a web service class
    WebServiceClass<?> webServiceClass = webServiceClasses.get(pathKey);
    if (webServiceClass == null) {
      LOG.error("No web service class mapped for " + pathKey);
      throw new NotFoundException();
    }

    // Invoke the operation
    Object object = webServiceClass.invokeOperation(operationName, parameters);
    try {
      return objectMapper.writeValueAsString(object);
    } catch (JsonProcessingException e) {
      LOG.error("Error marshalling response from " + path, e);
      throw new InternalServerErrorException();
    }
  }

}

