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
 * Dummy class to test the valueOf TypeConvertor conversion
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DummyCustomClass extends BigDecimal {

 public DummyCustomClass(String s) {
   super(s);
 }

 public static DummyCustomClass valueOf(String s) {
   return new DummyCustomClass(s);
 }
}