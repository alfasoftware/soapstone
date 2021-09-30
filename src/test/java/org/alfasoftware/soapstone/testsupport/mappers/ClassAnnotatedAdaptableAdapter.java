package org.alfasoftware.soapstone.testsupport.mappers;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.alfasoftware.soapstone.testsupport.PackageAnnotatedAdaptable;
import org.alfasoftware.soapstone.testsupport.ClassAnnotatedAdaptable;

/**
 * XmlAdapter for {@link PackageAnnotatedAdaptable}
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
