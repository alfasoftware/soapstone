package org.alfasoftware.soapstone.testsupport.mappers;

import org.alfasoftware.soapstone.testsupport.ClassAnnotatedAdaptable;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XmlAdapter for {@link ClassAnnotatedAdaptable}
 */
public class ClassAnnotatedAdaptableAdapter extends XmlAdapter<String, ClassAnnotatedAdaptable> {

  @Override
  public ClassAnnotatedAdaptable unmarshal(String v) {
    return v.equalsIgnoreCase("non-null") ? new ClassAnnotatedAdaptable(new ClassAnnotatedAdaptable(null)) : new ClassAnnotatedAdaptable(null);
  }

  @Override
  public String marshal(ClassAnnotatedAdaptable v) {
    return v.getInnerAdaptableState();
  }
}
