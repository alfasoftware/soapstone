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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * Holder for the {@link ObjectMapper} and {@link ExceptionMapper} used by SOAPStone
 *
 * <p>
 * See {@link SoapstoneServiceBuilder#withObjectMapper(ObjectMapper)} and
 * {@link SoapstoneServiceBuilder#withExceptionMapper(ExceptionMapper)}.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
enum Mappers {

  MAPPERS;

  private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JaxbAnnotationModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private ExceptionMapper exceptionMapper;

  ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  synchronized void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  ExceptionMapper getExceptionMapper() {
    return exceptionMapper;
  }

  synchronized void setExceptionMapper(ExceptionMapper exceptionMapper) {
    this.exceptionMapper = exceptionMapper;
  }
}
