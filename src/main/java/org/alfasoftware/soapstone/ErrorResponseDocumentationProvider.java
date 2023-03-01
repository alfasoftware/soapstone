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
