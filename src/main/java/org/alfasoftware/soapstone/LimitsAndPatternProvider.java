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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Limits and patterns provider for properties on web service models used in the API
 */
public class LimitsAndPatternProvider {

  // Functions which are supplied with both the Field for the property and the context annotations associated with it
  // which callers can use as required to identify any constraints which should be applied to the property.  Null handling is required.
  private final Function<Pair<Field, List<Annotation>>, StringLimitAndPatternTuple> stringLimitAndPatternFromProperty;
  private final Function<Pair<Field, List<Annotation>>, NumberLimitsTuple> numberLimitsFromProperty;
  // Allows callers to provide custom logic for handling limits and patterns
  private final LimitsAndPatternsHandler limitsAndPatternsHandler;

  LimitsAndPatternProvider(Function<Pair<Field, List<Annotation>>, StringLimitAndPatternTuple> stringLimitAndPatternFromProperty,
                           Function<Pair<Field, List<Annotation>>, NumberLimitsTuple> numberLimitsFromProperty,
                           LimitsAndPatternsHandler limitsAndPatternsHandler
  ) {
    this.stringLimitAndPatternFromProperty = stringLimitAndPatternFromProperty;
    this.numberLimitsFromProperty = numberLimitsFromProperty;
    this.limitsAndPatternsHandler = limitsAndPatternsHandler;
  }


  public StringLimitAndPatternTuple getStringLimitAndPattern(Field field, List<Annotation> annotations) {
    return Optional.ofNullable(stringLimitAndPatternFromProperty)
        .map(stringFromField -> stringFromField.apply(Pair.of(field, annotations)))
        .orElseGet(StringLimitAndPatternTuple::new);
  }


  public NumberLimitsTuple getNumberLimits(Field field, List<Annotation> annotations) {
    return Optional.ofNullable(numberLimitsFromProperty)
        .map(numberFromField -> numberFromField.apply(Pair.of(field, annotations)))
        .orElseGet(NumberLimitsTuple::new);
  }


  public LimitsAndPatternsHandler getLimitsAndPatternsHandler() {
    return limitsAndPatternsHandler;
  }


  public static class StringLimitAndPatternTuple {
    private Integer maxLength;
    private String pattern;

    public Integer getMaxLength() {
      return maxLength;
    }

    public StringLimitAndPatternTuple maxLength(Integer maxLength) {
      this.maxLength = maxLength;
      return this;
    }

    public String getPattern() {
      return pattern;
    }

    public StringLimitAndPatternTuple pattern(String pattern) {
      this.pattern = pattern;
      return this;
    }
  }


  public static class NumberLimitsTuple {
    private BigDecimal max;
    private BigDecimal min;

    public BigDecimal getMax() {
      return max;
    }

    public NumberLimitsTuple max(BigDecimal max) {
      this.max = max;
      return this;
    }

    public BigDecimal getMin() {
      return min;
    }

    public NumberLimitsTuple min(BigDecimal min) {
      this.min = min;
      return this;
    }
  }
}
