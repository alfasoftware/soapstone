/* Copyright 2023 Alfa Financial Software
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

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * The provider of the Error Response Documentation.
 *
 *  @author Copyright (c) Alfa Financial Software 2023
 */
public interface ErrorResponseDocumentationProvider {

    /**
     * Return a map of the HTTP response status code and the response type
     *
     * @param method a method
     * @return a map of the HTTP response status code and the response type
     */
    Map<String, Type> getErrorResponseTypesForMethod(Method method);
}
