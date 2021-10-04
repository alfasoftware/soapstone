package org.alfasoftware.soapstone.testsupport;

/**
 * A slightly interesting complex class with an XmlAdapter
 */
@WebService.Documentation("Class: Adaptable")
public class PackageAnnotatedAdaptable {

  private final PackageAnnotatedAdaptable innerAdaptable;

  public PackageAnnotatedAdaptable(PackageAnnotatedAdaptable innerAdaptable) {
    this.innerAdaptable = innerAdaptable;
  }

  public String getInnerAdaptableState() {
    return innerAdaptable == null ? "null" : "non-null";
  }
}
