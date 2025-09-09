package org.alfasoftware.soapstone;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

public class CustomConverter<IN,OUT>  implements Converter<IN,OUT> {

  protected final JavaType _inputType, _targetType;

  protected final XmlAdapter<Object,Object> _adapter;

  protected final boolean _forSerialization;


  @SuppressWarnings("unchecked")
  public CustomConverter(XmlAdapter<?,?> adapter,
                          JavaType inType, JavaType outType, boolean ser)
  {
    _adapter = (XmlAdapter<Object,Object>) adapter;
    _inputType = inType;
    _targetType = outType;
    _forSerialization = ser;
  }

  @Override
  public Object convert(Object value)
  {
    try {
      if (_forSerialization) {
        return _adapter.marshal(value);
      }
      return _adapter.unmarshal(value);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public JavaType getInputType(TypeFactory typeFactory) {
    return _inputType;
  }

  @Override
  public JavaType getOutputType(TypeFactory typeFactory) {
    return _targetType;
  }


  protected JavaType _findConverterType(TypeFactory tf) {
    JavaType thisType = tf.constructType(getClass());
    JavaType convType = thisType.findSuperType(Converter.class);
    if (convType == null || convType.containedTypeCount() < 2) {
      throw new IllegalStateException("Can not find OUT type parameter for Converter of type "+getClass().getName());
    }
    return convType;
  }
}