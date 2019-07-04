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

import org.alfasoftware.soapstone.testsupport.DummyCustomClass;
import org.alfasoftware.soapstone.testsupport.DummyGetInstanceClass;
import org.alfasoftware.soapstone.testsupport.DummyParseableClass;
import org.alfasoftware.soapstone.testsupport.PoorVisibility;
import org.alfasoftware.soapstone.testsupport.ProblematicInvocation;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Test that all supported translations between different object types are
 * supported using {@link TypeConverter}.
 *
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class TestTypeConverter {

  /**
   * Poison value for {@link #testJavaDosDoubleBug()}.
   */
  private static final String DOUBLE_PARSING_POISION_VALUE = "2.2250738585072012e-308";

  private final TypeConverter ukConverter = new TypeConverter(Locale.UK);


  /**
   * Test that null inputs don't cause problems.
   */
  @Test
  public void testNull() {
    assertNull("Null value", ukConverter.convertValue(null, String.class));
  }


  /**
   * Test fix (1): (<var>toType</var> != Array) && (<var>value</var> == Object[1]) != value[1]
   */
  @Test
  public void testSingleElementArray() {
    String   expected = "xyzzy";
    String[] input    = new String[] { expected };

    Object result = ukConverter.convertValue(input, String.class);
    assertEquals("Single element array", expected, result);

    input  = new String[] { expected, "foo" };
    result = ukConverter.convertValue(input, String[].class);
    assertEquals("Multi-element array", input.length, ((String[])result).length);

    input  = new String[] { "5" };
    result = ukConverter.convertValue(input, int[].class);
    assertEquals("Array -> Array", 1, ((int[])result).length );
    assertEquals("Array -> Array", 5, ((int[])result)[0] );

    input = new String[] { null };
    Integer intResult = ukConverter.convertValue(input, Integer.TYPE);
    assertEquals("String[]{ null } -> (int)0", 0, intResult.intValue());
  }


  /**
   * Test that single element arrays can be targeted from non-array values.
   */
  @Test
  public void testSingleElementArrayTarget() {
    int[] result = ukConverter.convertValue("12345", int[].class);
    assertEquals("Result size", 1, result.length);
    assertEquals("Result value", 12345, result[0]);
  }


  /**
   * Test fix (2): (<var>toType</var> != String) && (<var>value</var> == "") != null
   */
  @Test
  public void testEmptyStringsToNull() {
    assertNull("Integer gets null", ukConverter.convertValue("", Integer.class));
    assertEquals("String gets ''", "", ukConverter.convertValue("", String.class));
  }


  /**
   * Test fix (3): (<var>toType</var> is Number) =&gt; handle currency symbols and punctuation
   */
  @Test
  public void testCommaSeparatedParsing() {
    assertEquals("Simple number", 42, ukConverter.convertValue("42", Integer.class).intValue());

    assertEquals("Commas-separated number", 12345.67, ukConverter.convertValue("12,345.67", Double.class), 0.001);

    assertEquals("Currency", 3999.99, ukConverter.convertValue('\u00a3' + "3,999.99", Double.class), 0.001);
  }


  /**
   * Test that methods with a "valueOf" method get it invoked as a fall-back.
   */
  @Test
  public void testClassesWithValueOfStringMethodCanBeConvertedTo() {
    DummyCustomClass result = ukConverter.convertValue("1234", DummyCustomClass.class);
    assertEquals("Custom class", 1234, (int)result.doubleValue());

    result = new TypeConverter(Locale.FRANCE).convertValue("12 345,67", DummyCustomClass.class);
    assertEquals("Locale-sensitive parsing", "12345.67", result.toString());

    result = new TypeConverter(Locale.FRANCE).convertValue("   12 345,67   ", DummyCustomClass.class); // now with whitespace
    assertEquals("Locale-sensitive parsing", "12345.67", result.toString());
  }


  /**
   * Test that methods with a "getInstance" method get it invoked as a fall-back.
   */
  @Test
  public void testClassesWithGetInstanceStringMethodCanBeConvertedTo() {
    DummyGetInstanceClass result = ukConverter.convertValue("1234", DummyGetInstanceClass.class);
    assertEquals("Custom getInstance class", 1234, (int)result.doubleValue());

    result = new TypeConverter(Locale.FRANCE).convertValue("12 345,67", DummyGetInstanceClass.class);

    assertEquals("Locale-sensitive parsing", "12345.67", result.toString());
  }


  /**
   * Test that methods with a "parse" method get it invoked as a fall-back.
   */
  @Test
  public void testClassesWithParseStringMethodCanBeConvertedTo() {
    DummyParseableClass result = ukConverter.convertValue("1234", DummyParseableClass.class);
    assertEquals("Custom parse class", 1234, (int)result.doubleValue());

    result = new TypeConverter(Locale.FRANCE).convertValue("12 345,67", DummyParseableClass.class);

    assertEquals("Locale-sensitive parsing", "12345.67", result.toString());
  }


  /**
   * Test that primitives get a default value.
   */
  @Test
  public void testPrimitiveDefaults() {
    assertEquals("Default int value for incorrect string", 0, (int)ukConverter.convertValue("wibble", Integer.TYPE));
    assertEquals("Default int value for empty string", 0, (int)ukConverter.convertValue("", Integer.TYPE));
  }


  /**
   * Test that the first character of a string is used when
   * getting a character.
   */
  @Test
  public void testCharactersFromStrings() {
    assertEquals("Long string", 'Y', ukConverter.convertValue("Yellow", Character.class).charValue());
  }


  /**
   * Test date parsing in multiple locales.
   */
  @Test
  public void testDates() throws ParseException {
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    assertDates(Date.class, format.parse("1977-12-04"), format.parse("2014-02-28"));
    assertDatesWithWhitespace(Date.class, format.parse("1977-12-04"), format.parse("2014-02-28"));

//    assertEquals("Century rollover", format.parse("2039-12-04"), ukConverter.convertValue("04/12/39", Date.class));
//    assertEquals("Century rollover", format.parse("2039-12-04"), ukConverter.convertValue("   04/12/39   ", Date.class)); // with whitespace

//    assertEquals("Century rollover at limit", format.parse("2059-12-04"), ukConverter.convertValue("04/12/59", Date.class));
//    assertEquals("Century rollover at limit", format.parse("2059-12-04"), ukConverter.convertValue("   04/12/59   ", Date.class)); // with whitespace

//    final String farFuture = "2999-12-31";
//    assertEquals("UK short date with 4-digit year in far future", farFuture, format.format(ukConverter.convertValue("31/12/2999", Date.class)));

//    assertNull("Badly formed for locale", ukConverter.convertValue("12/20/39", Date.class));
//    assertNull("Invalid date number - expected null but got [" + ukConverter.convertValue("12345", Date.class) + "]", ukConverter.convertValue("12345", Date.class));
  }


  /**
   * Tests parsing LocalDates in several ways.
   */
  @Test
  public void testLocalDates() {
    assertDates(LocalDate.class, new LocalDate(1977, 12, 4), new LocalDate(2014, 2, 28));

//    final LocalDate farFuture = new LocalDate(1999, 12, 31);
//    assertEquals("UK short date with 4-digit year in far future", farFuture, ukConverter.convertValue("31/12/1999", LocalDate.class));
//    assertEquals("UK short date with 4-digit year in far future", farFuture, ukConverter.convertValue("   31/12/1999   ", LocalDate.class)); // Now with whitespace

//    assertNull("Invalid date number - expected null but got [" + ukConverter.convertValue("12345", LocalDate.class) + "]", ukConverter.convertValue("12345", LocalDate.class));
//    assertNull("Badly formed for locale", ukConverter.convertValue("12/20/39", LocalDate.class));

    DateTime isoFormat = new DateTime(1977, 12, 4, 0, 0);
    assertEquals("ISO-formatted date", new LocalDate(1977, 12, 4), ukConverter.convertValue(ISODateTimeFormat.date().print(isoFormat), LocalDate.class));
  }


//  /**
//   * Test date parsing in multiple locales.
//   */
//  @Test
//  public void testBadDate() {
//    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
//    final String expected = "2002-02-02";
//
//    assertEquals(expected, format.format(ukConverter.convertValue("2/2/2", Date.class)));
//  }


  /**
   * Test Date parsing for out of range dates.
   */
  @Test(expected = ParseException.class)
  public void testDateUpperBound() throws ParseException {
    ukConverter.convert("1/1/20011", Date.class);
  }


  /**
   * Test LocalDate parsing for out of range dates.
   */
  @Test(expected = ParseException.class)
  public void testLocalDateUpperBound() throws ParseException {
    ukConverter.convert("1/1/20011", LocalDate.class);
  }


  /**
   * Test LocalDate parsing for out of range dates.
   */
  @Test(expected = ParseException.class)
  public void testLocalDateLowerBound() throws ParseException {
    ukConverter.convert("21/07/207", LocalDate.class); // based on real example!
  }


  /**
   * Test for parsing {@link LocalTime} objects correctly
   */
  @Test
  public void testLocalTimesWithWhitespace() throws ParseException {

    // Given
    final String shortTimeUk = "   10:10   ";
    final String shortTimeUs = "  10:10 AM  ";

    final String shortTime2Uk = "   9:9   ";
    final String shortTime2Us = "  9:9 AM  ";

    final String mediumTimeUk = "    10:10:10.000    ";
    final String mediumTimeUs = "    10:10:10.000    ";

    final String mediumTimeUk2 = "    09:09:09    ";
    final String mediumTimeUs2 = "    9:09:09 AM  ";

    TypeConverter typeConverterUk = new TypeConverter(Locale.UK);
    TypeConverter typeConverterUs = new TypeConverter(Locale.US);

    // When
    final LocalTime shortTimeResultUk = typeConverterUk.convert(shortTimeUk, LocalTime.class);
    final LocalTime shortTimeResultUs = typeConverterUs.convert(shortTimeUs, LocalTime.class);

    final LocalTime shortTime2ResultUk = typeConverterUk.convert(shortTime2Uk, LocalTime.class);
    final LocalTime shortTime2ResultUs = typeConverterUs.convert(shortTime2Us, LocalTime.class);

    final LocalTime mediumTimeResultUk = typeConverterUk.convert(mediumTimeUk, LocalTime.class);
    final LocalTime mediumTimeResultUs = typeConverterUk.convert(mediumTimeUs, LocalTime.class);

    final LocalTime mediumTimeResultUk2 = typeConverterUk.convert(mediumTimeUk2, LocalTime.class);
    final LocalTime mediumTimeResultUs2 = typeConverterUs.convert(mediumTimeUs2, LocalTime.class);

    // Then
    assertThat("Local time could not be converted for UK locale", shortTimeResultUk, is(new LocalTime(10,10)));
    assertThat("Local time could not be converted for US locale", shortTimeResultUs, is(new LocalTime(10,10)));

    assertThat("Local time could not be converted for UK locale", shortTime2ResultUk, is(new LocalTime(9,9)));
    assertThat("Local time could not be converted for US locale", shortTime2ResultUs, is(new LocalTime(9,9)));

    assertThat("Local time could not be converted for UK locale", mediumTimeResultUk, is(new LocalTime(10,10,10)));
    assertThat("Local time could not be converted for US locale", mediumTimeResultUs, is(new LocalTime(10,10,10)));

    assertThat("Local time could not be converted for UK locale", mediumTimeResultUk2, is(new LocalTime(9,9,9)));
    assertThat("Local time could not be converted for US locale", mediumTimeResultUs2, is(new LocalTime(9,9,9)));
  }


  /**
   * Test for parsing {@link LocalTime} objects correctly
   */
  @Test
  public void testLocalTimes() throws ParseException {

    // Given
    final LocalTime shortTime = new LocalTime(10,10);
    final LocalTime shortTime2 = new LocalTime(9,9);
    final LocalTime mediumTime = new LocalTime(10,10,10);
    final LocalTime mediumTime2 = new LocalTime(9,9,9);

    for(Locale locale : asList(Locale.UK, Locale.US, Locale.CANADA, Locale.FRANCE)) {
      TypeConverter typeConverter = new TypeConverter(locale);

      // When
      final LocalTime shortTimeResult = typeConverter.convert(shortTime.toString(DateTimeFormat.shortTime().withLocale(locale)), LocalTime.class);
      final LocalTime shortTime2Result = typeConverter.convert(shortTime2.toString(DateTimeFormat.shortTime().withLocale(locale)), LocalTime.class);
      final LocalTime mediumTimeResult = typeConverter.convert(mediumTime.toString(DateTimeFormat.mediumTime().withLocale(locale)), LocalTime.class);
      final LocalTime mediumTime2Result = typeConverter.convert(mediumTime2.toString(DateTimeFormat.mediumTime().withLocale(locale)), LocalTime.class);

      // Then
      assertThat(locale.getCountry() + " locale failed", shortTimeResult, is(shortTime));
      assertThat(locale.getCountry() + " locale failed", shortTime2Result, is(shortTime2));
      assertThat(locale.getCountry() + " locale failed", mediumTimeResult, is(mediumTime));
      assertThat(locale.getCountry() + " locale failed", mediumTime2Result, is(mediumTime2));
    }
  }


  /**
   * Test for parsing {@link LocalTime} objects correctly
   */
  @Test
  public void testISOLocalTimes() throws ParseException {

    // Given
    final LocalTime shortTime = new LocalTime(10,10);
    final LocalTime mediumTime = new LocalTime(10,10,10);
    final LocalTime longTime = new LocalTime(10,10,10,100);

    // When
    final LocalTime shortTimeResult = ukConverter.convert(shortTime.toString(ISODateTimeFormat.time()), LocalTime.class);
    final LocalTime mediumTimeResult = ukConverter.convert(mediumTime.toString(ISODateTimeFormat.time()), LocalTime.class);
    final LocalTime longTimeResult = ukConverter.convert(longTime.toString(ISODateTimeFormat.time()), LocalTime.class);

    // Then
    assertThat(shortTimeResult, is(shortTime));
    assertThat(mediumTimeResult, is(mediumTime));
    assertThat(longTimeResult, is(longTime));
  }


  /**
   * Test parsing of invalid time
   */
  @Test(expected=ParseException.class)
  public void testConvertTimeFailure() throws ParseException {
    ukConverter.convert("invalid", LocalTime.class);
  }




  /**
   * Test Date parsing for out of range dates.
   */
  @Test(expected = ParseException.class)
  public void testDateLowerBound() throws ParseException {
    ukConverter.convert("21/07/207", Date.class);
  }


  /**
   * Test Locale conversion with language and country
   */
  @Test
  public void testLocaleLangCountry() throws ParseException {
    Locale locale = ukConverter.convert("en_GB", Locale.class);
    assertEquals(new Locale("en", "GB"), locale);
  }


  /**
   * Test Locale conversion with language
   */
  @Test
  public void testLocaleLang() throws ParseException {
    Locale locale = ukConverter.convert("en", Locale.class);
    assertEquals(new Locale("en"), locale);
  }


  /**
   * Test Locale conversion with language, country and code
   */
  @Test
  public void testLocaleLangCountryCode() throws ParseException {
    Locale locale = ukConverter.convert("en_GB_xx", Locale.class);
    assertEquals(new Locale("en", "GB", "xx"), locale);
  }


  /**
   * Test Locale conversion wrong format case
   */
  @Test(expected = ParseException.class)
  public void testLocaleWrongFormat() throws ParseException {
    ukConverter.convert("wrong format", Locale.class);
  }


  /**
   * Test that classes can be retrieved.
   */
  @Test
  public void testClasses() {
    assertEquals("Simple class", this.getClass(), ukConverter.convertValue(this.getClass().getName(), Class.class));
    assertNull("Invalid class", ukConverter.convertValue(12435, Class.class));
  }


  /**
   * Test that {@link TypeConverter#booleanValue(Object)} works
   * correctly.
   */
  @Test
  public void testBooleanValue() {
    assertFalse(TypeConverter.booleanValue(null));

    assertTrue(TypeConverter.booleanValue(true));
    assertFalse(TypeConverter.booleanValue(false));

    assertTrue(TypeConverter.booleanValue(' '));
    assertFalse(TypeConverter.booleanValue('\0'));

    assertTrue(TypeConverter.booleanValue(-1));
    assertTrue(TypeConverter.booleanValue(1));
    assertTrue(TypeConverter.booleanValue(6.9));
    assertFalse(TypeConverter.booleanValue(0));

    assertTrue(TypeConverter.booleanValue("true"));
    assertTrue(TypeConverter.booleanValue("TRUE"));
    assertFalse(TypeConverter.booleanValue("false"));
    assertFalse(TypeConverter.booleanValue("FALSE"));

    assertFalse(TypeConverter.booleanValue("yes"));
    assertFalse(TypeConverter.booleanValue("no"));

    // -- Required for web browsers...
    //
    assertTrue(TypeConverter.booleanValue("on"));
    assertTrue(TypeConverter.booleanValue("ON"));
    assertFalse(TypeConverter.booleanValue("off"));
    assertFalse(TypeConverter.booleanValue("OFF"));
  }


  /**
   * Test that {@link TypeConverter#doubleValue(Object)} works
   * correctly.
   */
  @Test
  public void testDoubleValue() {
    assertEquals(0.0, TypeConverter.doubleValue(null), 0.0001);
    assertEquals(4.2, TypeConverter.doubleValue(new BigDecimal("4.2")), 0.0001);
    assertEquals(1.0, TypeConverter.doubleValue(true), 0.0001);
    assertEquals(0.0, TypeConverter.doubleValue(false), 0.0001);
    assertEquals(32.0, TypeConverter.doubleValue(' '), 0.0001);
    assertEquals(12345.67, TypeConverter.doubleValue("   12345.67  "), 0.0001);
  }


  /**
   * Test raw conversions of simple types.
   */
  @Test
  public void testBaseConversions() {
    assertEquals("Integer", 12, (int)ukConverter.convertValue("  12", Integer.TYPE));
    assertEquals("Negative integer", -12, (int)ukConverter.convertValue("-12", Integer.TYPE));
    assertEquals("Double", 12.45, ukConverter.convertValue("  12.45", Double.TYPE), 0.0001);
    assertTrue("Boolean", ukConverter.convertValue("  true", Boolean.TYPE));
    assertEquals("Character", ' ', (char)ukConverter.convertValue(32, Character.TYPE));
    assertEquals("Byte from number", 32, (byte)ukConverter.convertValue(32, Byte.TYPE));
    assertEquals("Byte from string", 32, (byte)ukConverter.convertValue(" ", Byte.TYPE));
    assertEquals("Short", 42, (short)ukConverter.convertValue(42, Short.TYPE));
    assertEquals("Long", 12345678901L, (long)ukConverter.convertValue("12345678901", Long.TYPE));
    assertEquals("Float", 12.45f, ukConverter.convertValue("12.45   ", Float.TYPE), 0.0001);
    assertEquals("BigInteger", new BigInteger("12345678901"), ukConverter.convertValue("12345678901", BigInteger.class));
    assertEquals("BigDecimal", new BigDecimal("12345678901.99"), ukConverter.convertValue("12345678901.99", BigDecimal.class));

    assertNull("Invalid integer", ukConverter.convertValue("h", Integer.class));

    assertEquals("Integer with superfluous decimals", 1, (int)ukConverter.convertValue("1.0", Integer.TYPE));
    assertEquals("BigInteger with superfluous decimals", BigInteger.valueOf(1), ukConverter.convertValue("1.0", BigInteger.class));
    assertNull("Invalid integer", ukConverter.convertValue("1.1", Integer.class));
    assertNotNull("Valid double", ukConverter.convertValue("1.1", Double.class));
    assertNull("Invalid BigInteger", ukConverter.convertValue("1.1", BigInteger.class));
    assertNull("Invalid long",    ukConverter.convertValue("1.1", Long.class));
    assertNull("Invalid double",  ukConverter.convertValue("h", Double.class));
  }


  /**
   * Test conversions of BigInteger types.
   */
  @Test
  public void testBigIntegerConversions() {
    assertEquals("Integer with leading whitespace", BigInteger.valueOf(12), ukConverter.convertValue("  12", BigInteger.class));
    assertEquals("Integer with trailing whitespace", BigInteger.valueOf(12), ukConverter.convertValue("12    ", BigInteger.class));
    assertEquals("Negative integer", BigInteger.valueOf(-12), ukConverter.convertValue("-12", BigInteger.class));
    assertEquals("Negative integer leading whitespace", BigInteger.valueOf(-12), ukConverter.convertValue("     -12", BigInteger.class));
    assertEquals("Negative integer trailing whitespace", BigInteger.valueOf(-12), ukConverter.convertValue("-12     ", BigInteger.class));
    assertEquals("Negative integer padded", BigInteger.valueOf(-12), ukConverter.convertValue("       -12     ", BigInteger.class));
    assertEquals("BigInteger", new BigInteger("1234567890156464747648647464694"), ukConverter.convertValue("1234567890156464747648647464694", BigInteger.class));
    assertNull("BigDecimal", ukConverter.convertValue("12345678901.99", BigInteger.class));
    assertNull("Invalid integer", ukConverter.convertValue("h", Integer.class));
    assertEquals("BigInteger with superfluous decimals", BigInteger.ONE, ukConverter.convertValue("1.0", BigInteger.class));
    assertNull("Invalid BigInteger", ukConverter.convertValue("1.1", BigInteger.class));
  }


  /**
   * The {@link NumberFormatException} should be always caught and re-thrown as {@link ParseException}
   */
  @Test(expected = ParseException.class)
  public void invalidCharsInBigIntegerValueShouldCauseParseException() throws ParseException {
    ukConverter.convert("x", BigInteger.class);
  }


  /**
   * Test that empty string is converted to default value of primitive types
   */
  @Test
  public void testEmptyStringToPrimitive() {
    assertEquals("Empty string converted to default char", (char)0, (char)ukConverter.convertValue("", char.class));
    assertEquals("Empty string converted to default int", 0, (int)ukConverter.convertValue("", int.class));
    assertEquals("Empty string converted to default long", 0L, (long)ukConverter.convertValue("", long.class));
    assertEquals("Empty string converted to default float", 0f, ukConverter.convertValue("", float.class), 0.0001);
    assertEquals("Empty string converted to default double", 0d, ukConverter.convertValue("", double.class), 0.0001);
    assertFalse("Empty string converted to default boolean", ukConverter.convertValue("", boolean.class));
  }


  /**
   * Test that numbers must be parsed in their entirety, and that trailing
   * characters cause a failure, rather than be ignored.
   *
   * @throws ParseException if something goes wrong
   */
  @Test
  public void testNumberParsing() throws ParseException {
    checkFailingNumber("4abc", 1); // Trailing characters
    checkFailingNumber("4e1", 1);  // Scientific notation not supported
    checkFailingNumber("abc1", 0); // Preceding characters
    checkFailingNumber("0-1", 1);  // Sums are a no-no
    checkFailingNumber("9,,9", 2); // Sequential grouping chars
    checkFailingNumber("9,9,9", 3); // Invalid group size
    checkFailingNumber("999.000,999,999", 7); // Groups to right of decimal point
    checkFailingNumber("99,99",  2); // Groups of two characters are not allowed to be separated with a comma within a UK Locale
    assertEquals(9999, (int)ukConverter.convert("9,999", Integer.TYPE));
    assertEquals(9.9, ukConverter.convert("9.9", Double.TYPE), 0.0001);
    assertEquals(9.9, ukConverter.convert("9.9", Double.TYPE), 0.0001);

    try {
      ukConverter.convert("5,00", double.class);
      fail("Exception should have been raised for passing in a number with only 2 digits after a comma.");
    } catch (ParseException e) {
      // Do nothing as this is what was expected
    }

    TypeConverter frenchConverter = new TypeConverter(Locale.FRANCE);
    checkFailingNumber(frenchConverter, "9.9", 1);
    checkFailingNumber(frenchConverter, "9 9", 1);
    assertEquals(9.9, frenchConverter.convert("9,9", Double.TYPE), 0.0001);
  }


  /**
   * Tests that a {@link ParseException} is thrown when trying to parse value
   * that will overflow the type capacity for a positive value
   */
  @Test(expected = ParseException.class)
  public void testPositiveInvalidLongNumberParsingFails() throws ParseException {
    ukConverter.convert("77777777777777777777777777777777777777777777777777777777777777", Long.TYPE);
    fail("Test should throw parse exception");

  }


  /**
   * Tests that a {@link ParseException} is thrown when trying to parse value
   * that will overflow the type capacity for value with decimal places
   */
  @Test(expected = ParseException.class)
  public void testDecimalInvalidLongNumberParsingFails() throws ParseException {
    ukConverter.convert("777777777777777777777777777777777777777777777777777777777777.77", Long.TYPE);
    fail("Test should throw parse exception");
  }


  /**
   * Tests that a {@link ParseException} is thrown when trying to parse value
   * that will overflow the type capacity for a negative value
   */
  @Test(expected = ParseException.class)
  public void testNegativeInvalidLongNumberParsingFails() throws ParseException {
    ukConverter.convert("-777777777777777777777777777777777777777777777777777777777777.77", Long.TYPE);
    fail("Test should throw parse exception");
  }


  /**
   * Test number grouping in Indian locales.
   *
   * @throws ParseException If something goes wrong.
   * @see <a href="http://en.wikipedia.org/wiki/Decimal_mark#Digit_grouping">Examples of use</a>
   */
  @Test
  public void testNumberGroupingInIndianLocale() throws ParseException {
    TypeConverter englishIndianConverter = new TypeConverter(new Locale("en", "IN"));
    TypeConverter hindiIndianConverter = new TypeConverter(new Locale("hi", "IN"));

    assertEquals("Complex grouping", 100000000000L, (long) englishIndianConverter.convert("1,00,00,00,00,000", Long.class));

    // Check that a locale of en_IN converting "5,00,000.00" to a double is allowed
    assertEquals(500000.00, englishIndianConverter.convert("5,00,000.00", double.class), 0);

    // Check that a locale of hi_IN converting "5,00,000" to an integer is allowed
    assertEquals("Complex grouping", 500000, (int) hindiIndianConverter.convert("5,00,000", int.class));

    // Check that a locale of hi_IN converting "5,00,000,000" to an integer is allowed, incase the user is not used
    // to the convention used for Indian locale.
    assertEquals("Complex grouping", 500000000, (int) hindiIndianConverter.convert("5,00,000,000", int.class));

    // Check that a locale of en_IN converting "5,0,000" to an integer generates a
    // ParseException
    try {
      englishIndianConverter.convert("5,0,000", int.class);
      fail("Exception should have been raised for passing in a number with 1 digit between commas.");
    } catch (ParseException e) {
      // Do nothing as this is what was expected
    }

    // Check that a locale of en_IN converting "5,0,000" to an integer generates a
    // ParseException
    try {
      englishIndianConverter.convert("5,00,00", int.class);
      fail("Exception should have been raised for passing in a number with 2 digits as the last grouping of numbers.");
    } catch (ParseException e) {
      // Do nothing as this is what was expected
    }

  }


  /**
   * Test number grouping in Chinese locales.
   *
   * @throws ParseException if something goes wrong.
   * @see <a href="http://en.wikipedia.org/wiki/Decimal_mark#Digit_grouping">Examples of use</a>
   */
  @Test
  public void testNumberGroupingInChineseLocale() throws ParseException {
    assertEquals("Complex grouping", 1000000000000L, (long)new TypeConverter(new Locale("zh", "ZH")).convert("10,0000,000,0000", Long.class));
  }


  /**
   * Test return of value if it already matches target type.
   */
  @Test
  public void testValueTypeSameAsTarget() {
    Currency currency = Currency.getInstance("GBP");
    assertSame("Currency in should be the same as Currency out", currency, ukConverter.convertValue(currency, Currency.class));
    assertEquals("Currency in should be equal to Currency out", currency, ukConverter.convertValue(currency, Currency.class));
  }


  /**
   * Test return of value if target type is assignable from it
   * and no other conversion has taken place. This is to handle the
   * situation where something like JNDI (or another object source)
   * returns an incompatible object.
   */
  @Test
  public void testTargetAssignableFromValueType() {
    Animal animal = new Animal();
    Dog dog = new Dog();
    assertSame("Dog in should be the same as the Dog out", dog, ukConverter.convertValue(dog, Dog.class));
    assertEquals("Dog in should be equal to Dog out", dog, ukConverter.convertValue(dog, Animal.class));
    assertNull("Animal out should be null, cos we're after a Dog and who knows what Animal we were given?!", ukConverter.convertValue(animal, Dog.class));

    try {
      ukConverter.convert(animal, Dog.class);
      fail("Expected ParseException when trying to convert an animal to a dog");
    } catch (ParseException e) {
      assertEquals("Message when trying to convert an animal to a dog", "Cannot convert [" + animal + "] of type " + Animal.class.getName() + " to " + Dog.class, e.getMessage());
    }
  }


  /**
   * Check that the {@link TypeConverter#convert(Object, Class)} method
   * which throws an exception does so.
   */
  @Test
  public void testExceptionThrowingMethod() throws ParseException {
    assertNull("Empty string to null", ukConverter.convert("", Integer.class));
    try {
      ukConverter.convert("foo", Integer.class);
      fail("Expected ParseException");
    } catch (ParseException e) {
      assertEquals("Exception text", "Unparseable number: \"foo\"", e.getMessage());
    }
  }


  /**
   * Test that enums which can't be bound because they're private cause
   * exceptions which aren't swallowed.
   */
  @Test
  public void testIllegalAccessException() throws ParseException {
    try {
      ukConverter.convert("A", PoorVisibility.getEnumClass());
      fail("Expected exception");
    } catch (IllegalStateException e) {
      assertTrue(e.getCause() instanceof IllegalAccessException);
    }
  }


  /**
   * Test that unexpected exceptions in valueOf don't get swallowed.
   */
  @Test
  public void testInvocationTargetException() throws ParseException {
    try {
      ukConverter.convert("whatever", ProblematicInvocation.class);
      fail("Expected exception");
    } catch (IllegalStateException e) {
      assertTrue(e.getCause() instanceof InvocationTargetException);
    }
  }


  /**
   * Test that a recently published Java denial-of-service
   * bug against {@code double} does not impact code.
   */
  @Test
  public void testJavaDosDoubleBug() {
    assertTypeConversionForDos(DOUBLE_PARSING_POISION_VALUE.toLowerCase());
  }


  /**
   * Repeat of {@link #testJavaDosDoubleBug()}, but using an uppercase
   * value of {@value #DOUBLE_PARSING_POISION_VALUE}.
   */
  @Test
  public void testJavaDosDoubleBugUpperCase() {
    assertTypeConversionForDos(DOUBLE_PARSING_POISION_VALUE.toUpperCase());
  }


  /**
   * Do the work of {@link #testJavaDosDoubleBug()}.
   *
   * @param poisonValue Value to test.
   */
  private void assertTypeConversionForDos(String poisonValue) {
    try {
      Double value = ukConverter.convert(poisonValue, Double.TYPE);
      assertNotNull("Value converted", value); // OK if we support scientific notation
      assertEquals("Value converted accurately", Double.MIN_VALUE, value, 0.0);
    } catch (ParseException e) {
      // OK, if we don't support scientific notation.
    }
  }


  /**
   * @param dateType The type of date we can convert to / from.
   * @param fourthDecemberSeventySeven A representation of fourth of December 1977.
   * @param twentyEighthFebTwentyFourteen A representation of 28th of February 2014.
   */
  private <T> void assertDates(Class<T> dateType, T fourthDecemberSeventySeven, T twentyEighthFebTwentyFourteen) {
//    assertEquals("UK short date after 2010", twentyEighthFebTwentyFourteen, ukConverter.convertValue("28/02/14", dateType));
//    assertEquals("UK short date after 2010 without leading zero", twentyEighthFebTwentyFourteen, ukConverter.convertValue("28/2/14", dateType));
//    assertEquals("UK short date after 2010 without leading zero", twentyEighthFebTwentyFourteen, ukConverter.convertValue("28/2/2014", dateType));
    assertEquals("ISO-8601 date independent of UK locale", twentyEighthFebTwentyFourteen, ukConverter.convertValue(twentyEighthFebTwentyFourteen, dateType));

//    assertEquals("UK short date", fourthDecemberSeventySeven, ukConverter.convertValue("04/12/77", dateType));
//    assertEquals("UK short date without leading zero", fourthDecemberSeventySeven, ukConverter.convertValue("4/12/77", dateType));
//    assertEquals("UK short date with 4-digit year", fourthDecemberSeventySeven, ukConverter.convertValue("4/12/1977", dateType));
    assertEquals("ISO-8601 date independent of UK locale", fourthDecemberSeventySeven, ukConverter.convertValue(fourthDecemberSeventySeven, dateType));

//    assertNull("Pushing forward dates", ukConverter.convertValue("33/3/08", dateType));

    TypeConverter usConverter = new TypeConverter(Locale.US);
//    assertEquals("US short date", twentyEighthFebTwentyFourteen, usConverter.convertValue("02/28/14", dateType));
//    assertEquals("US short date", fourthDecemberSeventySeven, usConverter.convertValue("12/04/77", dateType));
    assertEquals("ISO-8601 date independent of US locale", fourthDecemberSeventySeven, usConverter.convertValue(fourthDecemberSeventySeven, dateType));

    assertNull("Invalid date string", usConverter.convertValue("wibble", dateType));

    try {
      usConverter.convert("wibble", dateType);
      fail("Expected Exception");
    } catch (ParseException e) {
      String expectedMessage = "Cannot convert [wibble] to " + dateType.getSimpleName();
      assertEquals("Message", expectedMessage, e.getMessage());
    }

    try {
      new TypeConverter(Locale.UK).convert("12/20/39", dateType);
      fail("Expected Exception");
    } catch (ParseException e) {
      assertEquals("Message", "Cannot convert [12/20/39] to " + dateType.getSimpleName(), e.getMessage());
    }
  }


  /**
   * @param dateType The type of date we can convert to / from.
   * @param fourthDecemberSeventySeven A representation of fourth of December 1977.
   * @param twentyEighthFebTwentyFourteen A representation of 28th of February 2014.
   */
  private <T> void assertDatesWithWhitespace(Class<T> dateType, T fourthDecemberSeventySeven, T twentyEighthFebTwentyFourteen) {
//    assertEquals("UK short date after 2010", twentyEighthFebTwentyFourteen, ukConverter.convertValue("   28/02/14   ", dateType));
//    assertEquals("UK short date after 2010 without leading zero", twentyEighthFebTwentyFourteen, ukConverter.convertValue("   28/2/14   ", dateType));
//    assertEquals("UK short date after 2010 without leading zero", twentyEighthFebTwentyFourteen, ukConverter.convertValue("   28/2/2014   ", dateType));
    assertEquals("ISO-8601 date independent of UK locale", twentyEighthFebTwentyFourteen, ukConverter.convertValue(twentyEighthFebTwentyFourteen, dateType));

//    assertEquals("UK short date", fourthDecemberSeventySeven, ukConverter.convertValue("   04/12/77   ", dateType));
//    assertEquals("UK short date without leading zero", fourthDecemberSeventySeven, ukConverter.convertValue("   4/12/77   ", dateType));
//    assertEquals("UK short date with 4-digit year", fourthDecemberSeventySeven, ukConverter.convertValue("   4/12/1977   ", dateType));
    assertEquals("ISO-8601 date independent of UK locale", fourthDecemberSeventySeven, ukConverter.convertValue(fourthDecemberSeventySeven, dateType));

    TypeConverter usConverter = new TypeConverter(Locale.US);
//    assertEquals("US short date", twentyEighthFebTwentyFourteen, usConverter.convertValue("   02/28/14   ", dateType));
//    assertEquals("US short date", fourthDecemberSeventySeven, usConverter.convertValue("   12/04/77   ", dateType));
    assertEquals("ISO-8601 date independent of US locale", fourthDecemberSeventySeven, usConverter.convertValue(fourthDecemberSeventySeven, dateType));
  }


  /**
   * Convenience method for checking that a number of different alphanumeric
   * sequences cannot be parsed to integers.
   */
  private void checkFailingNumber(String input, int errorIndex) {
    try {
      ukConverter.convert(input, Double.TYPE);
      fail("Expected conversion error character-containing number [" + input + "]");
    } catch (ParseException e) {
      assertEquals("Exception text", "Unparseable number: \"" + input + "\"", e.getMessage());
      assertEquals("Error index", errorIndex, e.getErrorOffset());
    }
  }


  /**
   * Convenience method for checking that a number of different alphanumeric
   * sequences cannot be parsed to integers.
   */
  private void checkFailingNumber(TypeConverter typeConverter, String input, int errorIndex) {
    try {
      typeConverter.convert(input, Double.TYPE);
      fail("Expected conversion error character-containing number [" + input + "]");
    } catch (ParseException e) {
      assertEquals("Exception text", "Unparseable number: \"" + input + "\"", e.getMessage());
      assertEquals("Error index", errorIndex, e.getErrorOffset());
    }
  }


  static class Animal {

  }

  static class Dog extends Animal {

  }

}


