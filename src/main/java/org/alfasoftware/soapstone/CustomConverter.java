package org.alfasoftware.soapstone;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

/**
 * Custom implementation of {@link Converter}, uses provided {@link XmlAdapter} for conversion.
 *
 * @param <IN>
 * @param <OUT>
 */
class CustomConverter<IN,OUT> implements Converter<IN,OUT> {

  private final JavaType inputType;

  private final JavaType targetType;

  private final XmlAdapter<OUT,IN> adapter;


  CustomConverter(XmlAdapter<OUT,IN> adapter, JavaType inType, JavaType outType) {
    this.adapter = adapter;
    this.inputType = inType;
    this.targetType = outType;
  }

  @Override
  public OUT convert(IN value) {
    try {
        return this.adapter.marshal(value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }


  @Override
  public JavaType getInputType(TypeFactory typeFactory) {
    return this.inputType;
  }


  @Override
  public JavaType getOutputType(TypeFactory typeFactory) {
    return this.targetType;
  }
}