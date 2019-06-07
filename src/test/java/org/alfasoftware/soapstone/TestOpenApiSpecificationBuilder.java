package org.alfasoftware.soapstone;

import org.alfasoftware.soapstone.testsupport.WebService;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

public class TestOpenApiSpecificationBuilder {


  private OpenApiSpecificationBuilder builder;

  @Test
  public void testBuild() throws Exception {

    Map<String, WebServiceClass<?>> webServices = new HashMap<>();
    webServices.put("/path", WebServiceClass.forClass(WebService.class, WebService::new));

    builder = new OpenApiSpecificationBuilder(
      webServices,
      null, null, null, null
    );

    String build = builder.build();
    System.out.println(build);

    fail();
  }

}