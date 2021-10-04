package org.alfasoftware.soapstone.testsupport;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.alfasoftware.soapstone.testsupport.mappers.ClassAnnotatedAdaptableAdapter;

/**
 * A slightly interesting complex class with an XmlAdapter
 */
@WebService.Documentation("Class: ClassAnnotatedAdaptable")
@XmlJavaTypeAdapter(ClassAnnotatedAdaptableAdapter.class)
public class ClassAnnotatedAdaptable {

  private final ClassAnnotatedAdaptable innerAdaptable;

  public ClassAnnotatedAdaptable(ClassAnnotatedAdaptable innerAdaptable) {
    this.innerAdaptable = innerAdaptable;
  }

  public String getInnerAdaptableState() {
    return innerAdaptable == null ? "null" : "non-null";
  }
}
