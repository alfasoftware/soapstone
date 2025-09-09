package org.alfasoftware.soapstone.testsupport.mappers;

import org.alfasoftware.soapstone.testsupport.PackageAnnotatedAdaptable;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * XmlAdapter for {@link PackageAnnotatedAdaptable}
 */
public class PackageAnnotatedAdaptableAdapter extends XmlAdapter<String, PackageAnnotatedAdaptable> {

  @Override
  public PackageAnnotatedAdaptable unmarshal(String v) {
    return v.equalsIgnoreCase("non-null") ? new PackageAnnotatedAdaptable(new PackageAnnotatedAdaptable(null)) : new PackageAnnotatedAdaptable(null);
  }

  @Override
  public String marshal(PackageAnnotatedAdaptable v) {
    return v.getInnerAdaptableState();
  }
}
