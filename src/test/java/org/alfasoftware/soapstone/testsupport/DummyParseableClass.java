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

import java.math.BigDecimal;

/**
 * Dummy class to test the parse OgnlTypeConverter conversion.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
@SuppressWarnings("unused")
public class DummyParseableClass extends BigDecimal {

  /**
   * Default constructor
   */
  public DummyParseableClass(String s) {
    super(s);
  }


  /**
   * Dummy declaration of parse.
   */
  public static DummyParseableClass parse(String s) {
    return new DummyParseableClass(s);
  }
}