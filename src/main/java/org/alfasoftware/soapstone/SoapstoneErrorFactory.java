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

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Factory for creation of Soapstone {@link ClientErrorException} objects which will include
 * the error message in the response body.
 *
 * @author Copyright (c) Alfa Financial Software 2022
 */
public class SoapstoneErrorFactory {

  static ClientErrorException create(ObjectMapper objectMapper, Response.Status status, String message) {

    ErrorMessage errorMessage = new ErrorMessage(message);

    try {
      return new ClientErrorException(Response.status(status).entity(objectMapper.writeValueAsString(errorMessage)).build());
    } catch (JsonProcessingException e) {
      throw new InternalServerErrorException(e);
    }
  }

  static class ErrorMessage {

    private final String message;

    ErrorMessage(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }
}
