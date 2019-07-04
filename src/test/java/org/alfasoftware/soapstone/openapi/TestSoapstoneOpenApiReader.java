package org.alfasoftware.soapstone.openapi;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alfasoftware.soapstone.SoapstoneServiceConfiguration;
import org.alfasoftware.soapstone.WebServiceClass;
import org.alfasoftware.soapstone.testsupport.WebService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;

public class TestSoapstoneOpenApiReader {

  private final Path yamlOutputPath = Paths.get("soapstone_oas.yml");
  private final Path jsonOutputPath = Paths.get("soapstone_oas.json");


  @Before
  public void setup() throws Exception {

    Files.deleteIfExists(yamlOutputPath);
    Files.deleteIfExists(jsonOutputPath);
  }


  @Test
  public void test() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JaxbAnnotationModule())
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

    Set<Class<?>> classes = webServices.values().stream()
      .map(WebServiceClass::getUnderlyingClass)
      .collect(Collectors.toSet());

    SoapstoneServiceConfiguration.get().setWebServiceClasses(webServices);
    SoapstoneServiceConfiguration.get().setVendor("TestVendor");
    SoapstoneServiceConfiguration.get().setObjectMapper(objectMapper);

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(null);
    reader.setConfiguration(new SwaggerConfiguration());
    OpenAPI openAPI = reader.read(classes, null);

    Files.createFile(yamlOutputPath);

    try (FileWriter yamlWriter = new FileWriter(yamlOutputPath.toFile())) {
      Yaml.pretty().writeValue(yamlWriter, openAPI);
    }

    try (FileWriter jsonWriter = new FileWriter(jsonOutputPath.toFile())) {
      Json.pretty().writeValue(jsonWriter, openAPI);
    }
  }


  @After
  public void teardown() throws Exception {

    readFromFile(yamlOutputPath);
    readFromFile(jsonOutputPath);
  }


  private void readFromFile(Path outputPath) throws IOException {
    if (Files.exists(jsonOutputPath)) {
      try (
        FileReader fr = new FileReader(outputPath.toFile());
        BufferedReader reader = new BufferedReader(fr)
      ) {
        reader.lines().forEach(System.out::println);
      }
    }
  }
}