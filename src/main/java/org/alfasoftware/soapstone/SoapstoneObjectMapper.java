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

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_ENUMS_USING_TO_STRING;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Default object mapper for use in Soapstone if no other is provided when creating the service.
 *
 * @author Copyright (c) Alfa Financial Software 2022
 */
public class SoapstoneObjectMapper {


  private SoapstoneObjectMapper() {
    throw new IllegalStateException("Not to be instantiated.");
  }

  private enum Loader {

    INSTANCE;


    private final ObjectMapper objectMapper = JsonMapper.builder()
      .findAndAddModules()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
        .configure(FAIL_ON_EMPTY_BEANS, false)
        .configure(WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(WRITE_ENUMS_USING_TO_STRING, true)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();
  }


  static ObjectMapper instance() {
    return Loader.INSTANCE.objectMapper;
  }
}
