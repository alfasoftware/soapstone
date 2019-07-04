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

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around a class representing a web service endpoint
 *
 * @param <T> the type of the wrapped class
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class WebServiceClass<T> {

  private static final Logger LOG = LoggerFactory.getLogger(SoapstoneService.class);

  private final Class<T> klass;
  private final Supplier<T> instance;



  /**
   * Create a new WebServiceClass for a class representing a web service endpoint
   *
   * @param klass            the class for which to create
   * @param instanceSupplier a supplier of an instance of klass
   * @param <U>              type of klass
   * @return a new WebServiceClass
   */
  public static <U> WebServiceClass<U> forClass(Class<U> klass, Supplier<U> instanceSupplier) {
    return new WebServiceClass<>(klass, instanceSupplier);
  }


  private WebServiceClass(Class<T> klass, Supplier<T> instanceSupplier) {
    this.klass = klass;
    this.instance = instanceSupplier;
  }


  public Class<T> getUnderlyingClass() {
    return klass;
  }


  T getInstance() {
    return instance.get();
  }
}