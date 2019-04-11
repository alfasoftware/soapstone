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
package org.alfasoftware.soapstone.testsupport;


/**
 * A simple dummy class for testing custom parameter type conversion.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
@SuppressWarnings("unused")
public class CustomParameterClass {

  private final String fieldOne;
  private final int fieldTwo;

  public CustomParameterClass(String fieldOne, int fieldTwo) {
   this.fieldOne = fieldOne;
   this.fieldTwo = fieldTwo;
  }

  public String printFields() {
    return fieldOne + fieldTwo;
  }

}

