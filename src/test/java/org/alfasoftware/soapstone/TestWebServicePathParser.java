package org.alfasoftware.soapstone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;
import java.util.Map;

import org.alfasoftware.soapstone.testsupport.WebService;
import org.junit.Test;


/**
 * Unit tests for {@link WebServicePathParser}
 */
public class TestWebServicePathParser {


  @Test
  public void testGetPaths() {
    List<String> paths = WebServicePathParser.getPaths(Map.of("/path", WebService.class));

    assertThat(paths, containsInAnyOrder(
        "/path/doAThing",
        "/path/doASimpleThing",
        "/path/doAListOfThings",
        "/path/doAThingWithThisName",
        "/path/doAThingBadly",
        "/path/getAThing",
        "/path/putAThing",
        "/path/deleteAThing",
        "/path/getAListOfThings",
        "/path/doAPackageAnnotatedAdaptableThing",
        "/path/doAClassAnnotatedAdaptableThing"
    ));
  }
}
