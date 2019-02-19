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

import com.google.common.collect.ImmutableSet;
import com.google.common.math.DoubleMath;
import ognl.OgnlOps;
import ognl.OgnlRuntime;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;

/**
 * Provides a central mechanism for converting a value to a target type.
 * Many different transformations are possible, and sensible heuristics are
 * used to get the best possible result.
 *
 * <p>This converter provides many simple conversions, but also complex
 * conversions involving locales, arrays and nulls. For full details,
 * see {@link #convertValue(Object, Class)}.</p>
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
class TypeConverter {

  /**
   * The ISO-8601 date format. This format is always accepted for {@link Date}s.
   *
   * @see <a href="http://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>
   */
  private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd";


  /**
   * The amount to increment 'bad dates' by.
   *
   * See WEB-6496 for more information.
   */
  private static final int BAD_DATE_INCREMENT = 2000;

  /**
   * The year, within a century, which is used to determine if a two-digit
   * year is in the 20th or 21st century. This value is <em>inclusive</em>,
   * that is:
   *
   * <ul>
   * <li>If <var>year</var> &lt; 1000 and <var>year</var> &gt;= {@value #CENTURY_SWITCH}, <code><var>year</var> += 1900</code>;
   * <li>If <var>year</var> &lt; 1000 and <var>year</var> &lt; {@value #CENTURY_SWITCH}, <code><var>year</var> += 2000</code>.
   * </li>
   *
   * @see SimpleDateFormat#set2DigitYearStart(Date)
   */
  private static final int CENTURY_SWITCH = 60;

  /**
   * Upper bound for the year on a converted date.
   */
  private static final int DATE_YEAR_UPPER_BOUND = 2999;

  /**
   * Lower bound for the year on a converted date. This guards against users inputting a three character year
   */
  private static final int DATE_YEAR_LOWER_BOUND = 1000;

  /**
   * The locale of the user using this type converter.
   */
  private final Locale locale;

  /**
   * A date before which we deem a date to be 'bad', and move it forward.
   *
   * See WEB-6496 for more information.
   */
  private static final Date badDateSwitchDate;

  /**
   * Date corresponding to {@value #CENTURY_SWITCH}.
   */
  private static final Date centurySwitchDate;

  /**
   * The upper bound date that is supported in conversions.
   */
  private static final Date upperBoundDate;

  /**
   * The lower bound date that is supported in conversions.
   */
  private static final Date lowerBoundDate;

  /*
   * Initialise static date values.
   */
  static {
    try {
      centurySwitchDate = new SimpleDateFormat("yyyy").parse(Integer.toString(1900 + CENTURY_SWITCH));
      badDateSwitchDate = new SimpleDateFormat("yyyy").parse("100");
      upperBoundDate = new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(DATE_YEAR_UPPER_BOUND + "-12-31");
      lowerBoundDate = new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(DATE_YEAR_LOWER_BOUND + "-01-01");
    } catch (ParseException e) {
      throw new IllegalStateException("Invalid date when calculating date constants", e);
    }
  }


  /**
   * All of the integer types.
   */
  private static final Set<Class<?>> integerTypes = ImmutableSet.of(Integer.class, Long.class, BigInteger.class);

  /**
   * A function that allows {@link LocalDate} instances to be interned where the application provides it. Otherwise it will pass through the instance.
   */
  private static Function<LocalDate, LocalDate> dateInternFunction = date -> date;


  /**
   * Create a new type converter which will use the given locale for parsing
   * locale-specific formats. This will be used whenever understanding of a locale-specific
   * format (such as a number, or date) is required.
   *
   * @param locale Locale to use when parsing locale-specific strings.
   */
  TypeConverter(Locale locale) {
    super();
    this.locale = locale;
  }


  /**
   * Convert a value of an unknown type into the best-fit for the given type. Amongst
   * others. The behaviour of this task is defined in {@code TestTypeConverter}, and
   * is inspired by the default implementation of {@link ognl.TypeConverter}.
   *
   * @param value Value to attempt to convert.
   * @param toType Class to target.
   * @return <var>value</var> in <var>toType</var>, or <code>null</code> if no conversion was possible.
   * @see #convert(Object, Class)
   */
  <T> T convertValue(Object value, final Class<T> toType) {
    try {
      return convert(value, toType);
    } catch (ParseException e) {
      // -- Ensure defaults are set for primitives...
      //
      if (toType.isPrimitive()) {
        return (T) OgnlRuntime.getPrimitiveDefaultValue(toType);
      }
      return null;
    }
  }


  /**
   * Convert a value of an unknown type into the best-fit for the given type. Amongst
   * others. The behaviour of this task is defined in {@code TestTypeConverter}, and
   * is inspired by the default implementation of {@link ognl.TypeConverter}.
   *
   * <p>This differs from {@link #convertValue(Object, Class)} in that it will throw
   * an exception if no conversion was possible, rather than returning {@code null}.
   * This allows a differentiation between a valid value which would result in null
   * (for example, {@code convert("", Integer.class)}) and an invalid value
   * (for example, {@code convert("foo", Integer.class)}).</p>
   *
   * @param value Value to attempt to convert.
   * @param toType Class to target.
   * @return <var>value</var> in <var>toType</var>, or <code>null</code> if no conversion was possible.
   * @throws ParseException if <var>value</var> could not be converted.
   */
  <T> T convert(Object value, final Class<T> toType) throws ParseException {
    Class<T> toTypeClass = objectClass(toType);
    T        result      = null;

    if (value != null && toType.isAssignableFrom(value.getClass())) {
      return (T)value;
    }

    // -- Handle nulls using the default...
    //
    if (value == null) { // it is implicit that toType is not null
      return (T)OgnlOps.convertValue(value, toType);
    }

    // -- Single element arrays should use lone value...
    //
    if (!toType.isArray() && value.getClass().isArray() && Array.getLength(value) == 1) {
      return convert(Array.get(value, 0), toType);
    }

    // -- Empty strings should be considered null unless String...
    //
    if (value.toString().equals("") && toType != String.class && !toType.isPrimitive()) {
      return null;
    }

    // -- Handle characters/bytes better...
    //
    if (!value.getClass().isArray() && !Number.class.isAssignableFrom(value.getClass()) && (toTypeClass.equals(Character.class) || toTypeClass.equals(Byte.class))) {
      String representation = value.toString();
      if (representation.length() > 0) {
        value = convertRawValue(representation.charAt(0), toType);
      }
    }

    // -- Allow numbers to contain punctuation and currency symbols...
    //
    if (!value.getClass().isArray() && Number.class.isAssignableFrom(objectClass(toType))) {

      boolean isBigDecimal = BigDecimal.class.isAssignableFrom(toType);
      boolean isInteger    = integerTypes.contains(toTypeClass);
      boolean isBigInteger = BigInteger.class.isAssignableFrom(toType);
      boolean isLong = Long.class.isAssignableFrom(toTypeClass);

      Object parsed = parseLocale(value.toString(), locale, isBigDecimal, isInteger, isBigInteger, isLong);
      if (toType.isAssignableFrom(parsed.getClass())) {
        return (T)parsed;
      } else {
        value = parsed;
      }
    }

    // -- Handle Dates...
    //
    if (toType.equals(Date.class)) {
      String valueAsTrimmedString = StringUtils.trim(value.toString());

      // Try ISO-8601 first. *IF* some locale's short date format was yyyy-dd-MM
      // this would return the wrong Date on days 1-12 of every month.
      try {
        DateFormat format = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
        format.setLenient(false);
        return (T) checkDateValue(format.parse(valueAsTrimmedString));
      } catch (ParseException e) {
        // Obviously wasn't ISO 8601!
      }

      DateFormat format = DateFormat.getDateInstance(DateFormat.SHORT, locale);
      format.setLenient(false);
      if (format instanceof SimpleDateFormat) {
        ((SimpleDateFormat)format).set2DigitYearStart(centurySwitchDate);
      }

      return (T) checkDateValue(format.parse(valueAsTrimmedString));
    }

    // -- Handle LocalDates...
    //
    if (toType.equals(LocalDate.class)) {
      String valueAsTrimmedString = StringUtils.trim(value.toString());
      try {
        return (T) checkLocalDateValue(LocalDate.parse(valueAsTrimmedString, ISODateTimeFormat.date()));
      } catch (IllegalArgumentException e) {
        // Not ISO-8601, we assume...
      }
      DateTimeFormatter formatter = DateTimeFormat.shortDate().withLocale(locale).withPivotYear(1950 + CENTURY_SWITCH);
      try {
        return (T) checkLocalDateValue(LocalDate.parse(valueAsTrimmedString, formatter));
      } catch (IllegalArgumentException e) {
        throw new ParseException("Cannot convert [" + valueAsTrimmedString + "] to LocalDate", 0); // NOPMD ParseException doesn't allow a nested exception to be provided
      }
    }

    // -- Handle LocalTimes...
    //
    if (toType.equals(LocalTime.class)) {
      String valueAsTrimmedString = StringUtils.trim(value.toString());
      try {
        DateTimeFormatter formatter = DateTimeFormat.shortTime().withLocale(locale);
        return (T) LocalTime.parse(valueAsTrimmedString, formatter);
      } catch (IllegalArgumentException e) {
        // Not short format time
      }

      try {
        DateTimeFormatter formatter = DateTimeFormat.mediumTime().withLocale(locale);
        return (T) LocalTime.parse(valueAsTrimmedString, formatter);
      } catch (IllegalArgumentException e) {
        // Not medium format time
      }

      try {
        DateTimeFormatter formatter = DateTimeFormat.longTime().withLocale(locale);
        return (T) LocalTime.parse(valueAsTrimmedString, formatter);
      } catch (IllegalArgumentException e) {
        // Not long format time
      }

      try {
        return (T) LocalTime.parse(valueAsTrimmedString, ISODateTimeFormat.time());
      } catch (IllegalArgumentException e) {
        // Not ISO-8601 format...
      }

      try {
        return (T) LocalTime.parse(valueAsTrimmedString);
      } catch (IllegalArgumentException e) {
        throw new ParseException("Cannot convert [" + value + "] to LocalTime", 0); // NOPMD ParseException doesn't allow a nested exception to be provided
      }
    }

    // -- Handle Locale
    //
    if (toType.equals(Locale.class)) {
      try {
        return (T) LocaleUtils.toLocale(value.toString().trim());
      }
      catch(IllegalArgumentException e){
        throw new ParseException("Cannot convert [" + value + "] to Locale", 0); // NOPMD ParseException doesn't allow a nested exception to be provided
      }
    }

    // -- Handle classes...
    //
    if (!value.getClass().isArray() && toType == Class.class) {
      try {
        return (T)Class.forName(value.toString().trim());
      } catch (ClassNotFoundException e) {
      }
    }

    // -- Handle arrays and not-arrays...
    //
    if (toType.isArray()) {
      result = convertArray(value, toType);
    } else {
      try {
        result = convertRawValue(value, toType);
      } catch (NumberFormatException e) {
        throw new ParseException("Cannot convert [" + value + "] to type [" + toType + "]", 0); // NOPMD ParseException doesn't allow a nested exception to be provided
      }
    }

    // -- Finally, fall back to available static methods...
    //
    if (result == null) {
      result = useMethod(value, toType);
    }

    if (result == null && toType.isAssignableFrom(value.getClass())) {
      return (T)value;
    }

    // -- Final check & error...
    //
    if (result == null) {
      throw new ParseException("Cannot convert [" + value + "] of type " + value.getClass().getName() + " to " + toType, 0);
    }

    return result;
  }


  /**
   * Checks a parsed {@link Date} to ensure it's correct.
   *
   * i.e. if the year is less 200, we can assume it's a 'bad date'
   * and push the date up by 2000.
   *
   * @param dateResult the date to check.
   * @return a validated/adjusted date.
   * @throws ParseException when the provided date is not in the supported range.
   */
  private Date checkDateValue(Date dateResult) throws ParseException {
    if (dateResult == null) {
      return null;
    }

    if (dateResult.before(badDateSwitchDate)) {
      return DateUtils.addYears(dateResult, BAD_DATE_INCREMENT);
    }

    if (dateResult.after(upperBoundDate) || dateResult.before(lowerBoundDate)) {
      throw new ParseException("Unsupported date [" + dateResult + "]", 0);
    }

    return dateResult;
  }


  /**
   * Checks a parsed {@link LocalDate} to ensure it's correct.
   *
   * @param dateResult the date to check.
   * @return a validated date.
   * @throws ParseException when the provided date is not in the supported range.
   */
  private LocalDate checkLocalDateValue(LocalDate dateResult) throws ParseException {
    if (dateResult == null) {
      return null;
    }

    if (dateResult.getYear() > DATE_YEAR_UPPER_BOUND || dateResult.getYear() < DATE_YEAR_LOWER_BOUND) {
      throw new ParseException("Unsupported date [" + dateResult + "]", 0);
    }

    return dateInternFunction.apply(dateResult);
  }


  /**
   * Evaluates the given object as a double-precision floating-point number.
   *
   * @param value an object to interpret as a double
   * @return the double value implied by the given object
   * @throws NumberFormatException if the given object can't be understood as a double
   * @see ognl.OgnlOps#doubleValue(Object)
   */
  static double doubleValue(Object value) throws NumberFormatException {
    if (value == null) {
      return 0.0;
    }
    Class c = value.getClass();
    if ( c.getSuperclass() == Number.class ) {
      return ((Number)value).doubleValue();
    }
    if ( c == Boolean.class ) {
      return (Boolean) value ? 1 : 0;
    }
    if ( c == Character.class ) {
      return (Character) value;
    }

    return Double.parseDouble(value.toString());
  }


  /**
   * Evaluates the given object as a boolean: if it is a Boolean object, it's
   * easy; if it's a Number or a Character, returns true for non-zero objects;
   * and otherwise passes the String representation to <code>Boolean.parseBoolean()</code>.
   *
   * @param value an object to interpret as a boolean
   * @return the boolean value implied by the given object
   */
  static boolean booleanValue(Object value) {
    if (value == null) {
      return false;

    } else if (value instanceof Boolean) {
      return (Boolean) value;

    } else if (value instanceof Character) {
      return (Character) value != 0;

    } else if (value instanceof Number) {
      return ((Number) value).doubleValue() != 0;

    } else {
      String text = value.toString().trim();
      return Boolean.parseBoolean(text) || text.equalsIgnoreCase("on");
    }
  }


  /**
   * Removes punctuation and currency symbols which the current user is likely
   * to enter using the given locale.
   *
   * @param value String containing tainted data.
   * @param locale Locale to use.
   * @param isBigDecimal If true, the result will be of type {@link BigDecimal} rather than {@link Double}.
   * @param isBigInteger If true, the result will be of type {@link BigInteger} rather than {@link Double}.
   * @return <var>value</var> with insignificant punctuation removed.
   * @throws ParseException if the number could not be parsed.
   */
  private static Number parseLocale(String value, Locale locale, boolean isBigDecimal, boolean isTargetInteger, boolean isBigInteger, boolean isLong) throws ParseException {
    DecimalFormat format = (DecimalFormat)NumberFormat.getInstance(locale);
    format.setParseBigDecimal(isBigDecimal);

    //Treat is as a BigDecimal with no fraction part
    if (isLong) {
      format.setParseBigDecimal(true);
      format.setMaximumFractionDigits(-1);
    }

    DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();

    // Remove the currency symbol and trim...
    String tidied = value.replaceAll(Pattern.quote(symbols.getCurrencySymbol()), "").trim();

    // Workaround for non-breaking space separators (http://bugs.sun.com/view_bug.do?bug_id=4510618)
    if (symbols.getGroupingSeparator() == '\u00a0') {
      tidied = tidied.replace(' ', '\u00a0');
    }

    // Swedish currency amounts often use '.' as a grouping separator, so purge them too
    if (isSwedishLanguage(locale) && symbols.getDecimalSeparator() != '.') {
      tidied = tidied.replace('.', symbols.getGroupingSeparator());
    }

    // Reformats negative numbers surrounded with parentheses to have a minus
    // sign if currency format for locale permits, e.g. '(777.00)' becomes
    // '-777.00'.
    if (tidied.matches("\\((.*)\\)")) {
      // Get currency formatter for locale
      DecimalFormat currencyFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
      // Check currency format had brackets for negative suffix and prefix
      if (currencyFormat.getNegativePrefix().contains("(") && currencyFormat.getNegativeSuffix().contains(")")) {
        tidied = tidied.replaceAll("\\((.*)\\)", "-$1");
      }
    }

    verifyNumberGroupingAndDecimalPart(locale, symbols, value, tidied);

    // Very big numbers or very small numbers get dealt with specially to avoid Java bugs
    // This catch .123E312, 0.123E312, 123.123E123, 123E123 and versions with -ve exponents
    String upperValue = tidied.toUpperCase(locale);
    if (upperValue.matches("(\\d*\\.)?\\d+E-?3\\d{2,}")) {
      return upperValue.contains("E-") ? Double.MIN_VALUE : Double.MAX_VALUE;
    }

    if (isLong && StringUtils.split(tidied, symbols.getDecimalSeparator()).length > 1) {
      throw new ParseException("Unparseable number: \"" + value + "\"", 0);
    }

    Number number;
    // Handle BigInteger.
    if (isBigInteger) {
      String[] splitNumber = StringUtils.split(tidied, symbols.getDecimalSeparator());

      try {
        if (splitNumber.length == 1 || parseInt(splitNumber[1]) == 0) {
          number = new BigInteger(splitNumber[0]);
        } else {
          throw new ParseException("Unparseable number: \"" + value + "\"", 0);
        }
      } catch (NumberFormatException e) {
        throw new ParseException("Unparseable number: \"" + value + "\"", 0); // NOPMD ParseException doesn't allow a nested exception to be provided
      }
    } else {
      // Use a ParsePosition to check that all of the string has been consumed
      ParsePosition position = new ParsePosition(0);
      number = format.parse(tidied, position);

      if (number != null && number.getClass().isAssignableFrom(Double.class) &&
          isTargetInteger && !DoubleMath.isMathematicalInteger((Double)number)) { // It's been parsed as a double. are we targetting an integer?
        throw new ParseException("Unparseable number: \"" + value + "\"", 0);
      }


      int errorIndex = position.getErrorIndex();
      if (errorIndex == -1 && position.getIndex() != tidied.length()) {
        errorIndex = position.getIndex();
      }

      if (errorIndex != -1) {
        throw new ParseException("Unparseable number: \"" + value + "\"", errorIndex); // Same as that in NumberFormat#parse(String)
      }

    }

    return number;
  }


  /**
   *   This is a method that checks the separators input by the user and ensures that the correct symbols are used for the selected locale.
   *   The default is that a "." is used for a decimal point and a "," is used as a thousands separator e.g. 1,000,000.00 - English Locale.
   *   For the Indian local the separation is slightly different, this method ensures that numbers follow this pattern: 1,00,00,000.00.
   */
  private static void verifyNumberGroupingAndDecimalPart(Locale locale, DecimalFormatSymbols decimalFormat, String entireValue, String partToCheck) throws ParseException {

    int currentGroupingSeparatorPosition = partToCheck.indexOf(decimalFormat.getDecimalSeparator());
    if (currentGroupingSeparatorPosition == -1) {
      currentGroupingSeparatorPosition = partToCheck.length() - 1;
    }

    boolean rightMostGrouping = true;

    // Loop through each grouping, backwards from the decimal point.
    while (true) {
      //Find the next grouping separator before the 'current' (which has already been considered)
      int nextGroupingSeparatorToTheLeftPosition = partToCheck.lastIndexOf(decimalFormat.getGroupingSeparator(), currentGroupingSeparatorPosition);

      if (nextGroupingSeparatorToTheLeftPosition == -1 || nextGroupingSeparatorToTheLeftPosition == currentGroupingSeparatorPosition)  {
        // No more separators to consider - break out of loop
        break;
      }

      int numberOfCharactersBetweenSeparators = currentGroupingSeparatorPosition - nextGroupingSeparatorToTheLeftPosition;

      if (isIndianLocale(locale)) {
        // If it is the right most grouping of numbers before the decimal point, there must be
        // 3 digits between separators
        // E.g 50,000.00
        //        ^^^
        if (rightMostGrouping && numberOfCharactersBetweenSeparators < 3) {
          throw new ParseException("Unparseable number: \"" + entireValue + "\"", nextGroupingSeparatorToTheLeftPosition);
        }
        // Otherwise all other groupings should only have 2 digits between separators.
        // E.g 50,00,000
        //        ^^
        else {
          if (numberOfCharactersBetweenSeparators < 2) {
            throw new ParseException("Unparseable number: \"" + entireValue + "\"", nextGroupingSeparatorToTheLeftPosition);
          }
        }
      }
      // All other currencies only allow 3 digits between separators.
      // E.g. 500,000,000
      else {
        if (numberOfCharactersBetweenSeparators < 3) {
          throw new ParseException("Unparseable number: \"" + entireValue + "\"", nextGroupingSeparatorToTheLeftPosition);
        }
      }

      // Update current to the character before the 'next', which was just considered
      // The index of the currentGroupingSeparatorPosition can never go below 0
      currentGroupingSeparatorPosition = max(0, nextGroupingSeparatorToTheLeftPosition - 1);

      rightMostGrouping = false;
    }
  }


  /**
   * Handle the conversion of individual array elements to the
   * correct target type.
   *
   * @param <T> Array type
   * @param value
   * @param toType
   * @return
   */
  private <T> T convertArray(Object value, Class<T> toType) {
    T     result        = null;
    Class componentType = toType.getComponentType();

    // If target is an array, copy as much as possible, otherwise create a new array containing the single value
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      result = (T) Array.newInstance(componentType, length);

      for (int i = 0; i < length; i++) {
        Array.set(result, i, convertValue(Array.get(value, i), componentType));
      }

    } else {
      result = (T) Array.newInstance(componentType, 1);
      Array.set(result, 0, convertValue(value, componentType));
    }

    return result;
  }


  /**
   * Use appropriate conversions for base classes.
   *
   * @param value
   * @param toType
   * @return
   * @throws ParseException
   */
  private <T> T convertRawValue(Object value, Class<T> toType) throws ParseException {
    Object result = null;
    toType = objectClass(toType);

    if (toType.equals(Integer.class)) {
      result = (int) OgnlOps.longValue(value);

    } else if (toType.equals(Double.class)) {
      result = doubleValue(value);

    } else if (toType.equals(Boolean.class)) {
      result = booleanValue(value);

    } else if (toType.equals(Byte.class)) {
      result = (byte) OgnlOps.longValue(value);

    } else if (toType.equals(Character.class)) {
      result = (char) OgnlOps.longValue(value);

    } else if (toType.equals(Short.class)) {
      result = (short) OgnlOps.longValue(value);

    } else if (toType.equals(Long.class)) {
      BigDecimal numberToConvert = getBigDecimal(value);
      if (isOutOfRangeValueForType(numberToConvert, toType)) {
        throw new ParseException("Cannot convert [" + value + "] to type [" + toType + "]", 0);
      }
      result = OgnlOps.longValue(value);

    } else if (toType.equals(Float.class)) {
      result = (float) doubleValue(value);

    } else if (toType.equals(BigInteger.class)) {
      result = OgnlOps.bigIntValue(value);

    } else if (toType.equals(BigDecimal.class)) {
      result = OgnlOps.bigDecValue(value);

    } else if (toType.equals(String.class)) {
      result = OgnlOps.stringValue(value);
    }

    return (T) result;
  }


  /**
   * Gets the {@link BigDecimal} value of an {@link Object}
   * @param value to transform into {@link BigDecimal}
   * @return The value as {@link BigDecimal}
   */
  private static BigDecimal getBigDecimal( Object value ) {
    BigDecimal ret = null;
    if( value != null ) {
      if( value instanceof BigDecimal ) {
        ret = (BigDecimal) value;
      } else if( value instanceof String ) {
        ret = new BigDecimal( (String) value );
      } else if( value instanceof BigInteger ) {
        ret = new BigDecimal( (BigInteger) value );
      } else if( value instanceof Number ) {
        ret = new BigDecimal( ((Number)value).doubleValue() );
      } else {
        throw new ClassCastException("Not possible to coerce ["+value+"] from class "+value.getClass()+" into a BigDecimal.");
      }
    }
    return ret;
  }


  private boolean isOutOfRangeValueForType(BigDecimal numberToConvert, Class clazz) {

    if (clazz.equals(Long.class)) {
      BigDecimal longMaxValue = new BigDecimal(Long.MAX_VALUE);
      BigDecimal longMinValue = new BigDecimal(Long.MIN_VALUE);
      if (numberToConvert.compareTo(longMaxValue) > 0 || numberToConvert.compareTo(longMinValue) < 0 || numberToConvert.scale() > 0) {
        return true;
      }
    }

    return false;
  }


  /**
   * Look for a static {@code valueOf(String)}, {@code getInstance(String)} or {@code parse(String)}
   * method which could be used to construct the object.
   *
   * @param <T> class of type to convert to.
   * @param value value to convert from.
   * @param toType type to convert to.
   * @throws RuntimeException wrapping {@link InvocationTargetException} or {@link IllegalAccessException}
   *         if either is thrown during the attempt to call the target method.
   * @return converted value.
   */
  private <T> T useMethod(Object value, Class<T> toType) {
    T result = null;

    // -- Try and find a method on toType...
    //
    Method method = getMethod(toType, toType, "(valueOf|getInstance|parse)", String.class);
    if (method != null && Modifier.isStatic(method.getModifiers()))  {
      try {
        result = (T) method.invoke(toType, value.toString());
      } catch (IllegalAccessException e) {
        throw new IllegalStateException("Method visibility error for [" + method.getName() + "] during type conversion.", e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException("Failed invocation of " + method.getName() + " during type conversion.", e);
      } catch (IllegalArgumentException ex) {
        // value is not a valid argument for the getInstance call
        // e.g. for java.util.Currency, value is not an ISO4217 (currency) code
        // In this case we'll continue to look for other bind strategies as this is unlikely to
        // be a bug like the cases above.
      }
    }

    return result;
  }


  /**
   * Checks a locale to see if it uses the Swedish language.
   *
   * @param locale the locale to check.
   * @return <var>true</var> if the locale uses Swedish.
   */
  private static boolean isSwedishLanguage(Locale locale) {
    return "sv".equals(locale.getLanguage());
  }


  /**
   * Checks a locale to see if it uses the Indian language.
   *
   * @param locale the locale to check.
   * @return <var>true</var> if the locale uses Indian.
   */
  private static boolean isIndianLocale(Locale locale){
    return "IN".equals(locale.getCountry());
  }


  /**
   * Return the base object class for the given class. If <var>clazz</var>
   * is primitive, the appropriate object class is returned. Otherwise,
   * <var>clazz</var> itself is passed back.
   *
   * @param clazz
   * @return Object class
   */
  private <T> Class<T> objectClass(Class<T> clazz) {
    if (!clazz.isPrimitive()) return clazz;
    if (clazz == Boolean.TYPE)   return (Class<T>) Boolean.class;
    if (clazz == Character.TYPE) return (Class<T>) Character.class;
    if (clazz == Byte.TYPE)      return (Class<T>) Byte.class;
    if (clazz == Short.TYPE)     return (Class<T>) Short.class;
    if (clazz == Integer.TYPE)   return (Class<T>) Integer.class;
    if (clazz == Long.TYPE)      return (Class<T>) Long.class;
    if (clazz == Float.TYPE)     return (Class<T>) Float.class;
    if (clazz == Double.TYPE)    return (Class<T>) Double.class;
    return clazz;
  }


  /**
   * Find a method in a class, taking into account superclasses in types.
   * Check each method in a class to see if it exists with the given name,
   * and a return type and parameters assignable to <var>returnType</var> and
   * <var>args</var>.
   *
   * @param toTest Class to find method on.
   * @param returnType Class representing highest point in class hierarchy for the return type of the method.
   * @param args Classes representing highest point in class hierarchy for each parameter of the method.
   * @return method which matches <var>ReturnType</var> <var>name</var>(<var>Args<sub>[0]</sub></var> arg0, <var>Args<sub>[1]</sub></var> arg1, ...)
   */
  private Method getMethod(Class toTest, Class returnType, String nameRegex, Class... args) {
    // Normalise primitive class references
    for (int i = 0; i < args.length; i++) {
      args[i] = objectClass(args[i]);
    }
    returnType = objectClass(returnType);

    // Find best fit method
    Method match = null;

    method: for (Method method : toTest.getMethods()) {
      Class[] params = method.getParameterTypes();
      if (!method.getName().matches(nameRegex) || !returnType.isAssignableFrom(objectClass(method.getReturnType())) || params.length != args.length)
        continue;

      for (int i = 0; i < args.length; i++) {
        if (!objectClass(params[i]).isAssignableFrom(objectClass(args[i])))
          continue method;
      }
      match = method;
    }

    return match; // No match found, return null
  }
}

