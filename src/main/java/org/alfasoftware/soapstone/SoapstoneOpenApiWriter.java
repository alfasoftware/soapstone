/* Copyright 2022 Alfa Financial Software
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

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.WebApplicationException;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.SwaggerConfiguration;

/**
 * Builder for the {@link SoapstoneService}
 *
 * <p>
 * This requires a map of URL paths to {@link WebServiceClass}. Optionally it can also be provided with an
 * {@link ExceptionMapper} to map exceptions to {@link WebApplicationException} and a vendor name, for use
 * in custom headers.
 *
 * @author Copyright (c) Alfa Financial Software 2022
 */
public class SoapstoneOpenApiWriter {


  private final SoapstoneConfiguration configuration;
  private final String hostUrl;


  SoapstoneOpenApiWriter(SoapstoneConfiguration configuration, String hostUrl) {
    this.configuration = configuration;
    this.hostUrl = hostUrl;
  }


  public void writeJson(OutputStream outputStream) throws IOException {

    SoapstoneOpenApiReader reader = new SoapstoneOpenApiReader(hostUrl, configuration);
    reader.setConfiguration(new SwaggerConfiguration());
    Json.pretty().writeValue(outputStream, reader.read(null));
  }
}

