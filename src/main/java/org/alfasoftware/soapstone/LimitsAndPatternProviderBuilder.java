/* Copyright 2026 Alfa Financial Software
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

import java.lang.reflect.Field;
import java.util.function.Function;

import org.alfasoftware.soapstone.LimitsAndPatternProvider.NumberLimitsTuple;
import org.alfasoftware.soapstone.LimitsAndPatternProvider.StringLimitAndPatternTuple;

/**
 * Builder for {@link LimitsAndPatternProvider}
 *
 */
public class LimitsAndPatternProviderBuilder {

  private Function<Field, StringLimitAndPatternTuple> stringLimitAndPatternFromProperty;
  private Function<Field, NumberLimitsTuple> numberLimitsFromProperty;
  private LimitsAndPatternsHandler limitsAndPatternsHandler;


  public LimitsAndPatternProviderBuilder withStringLimitAndPatternFromProperty(Function<Field, StringLimitAndPatternTuple> stringLimitAndPatternFromProperty) {
    this.stringLimitAndPatternFromProperty = stringLimitAndPatternFromProperty;
    return this;
  }


  public LimitsAndPatternProviderBuilder withNumberLimitsFromProperty(Function<Field, NumberLimitsTuple> numberLimitsFromProperty) {
    this.numberLimitsFromProperty = numberLimitsFromProperty;
    return this;
  }


  public LimitsAndPatternProviderBuilder withLimitsAndPatternsHandler(LimitsAndPatternsHandler limitsAndPatternsHandler) {
    this.limitsAndPatternsHandler = limitsAndPatternsHandler;
    return this;
  }


  public LimitsAndPatternProvider build() {
    return new LimitsAndPatternProvider(
        stringLimitAndPatternFromProperty,
        numberLimitsFromProperty,
        limitsAndPatternsHandler == null ? LimitsAndPatternsHandler.DEFAULT : limitsAndPatternsHandler
    );
  }

}
