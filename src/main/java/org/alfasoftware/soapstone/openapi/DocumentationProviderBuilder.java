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
package org.alfasoftware.soapstone.openapi;

import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.ClassDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MemberDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MethodDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.MethodReturnDocumentationProvider;
import org.alfasoftware.soapstone.openapi.ElementDocumentationProvider.ParameterDocumentationProvider;

/**
 * @author Copyright (c) Alfa Financial Software 2019
 */
public class DocumentationProviderBuilder {

  private ClassDocumentationProvider forClass;
  private ParameterDocumentationProvider forParameter;
  private MethodDocumentationProvider forMethod;
  private MethodReturnDocumentationProvider forMethodReturn;
  private MemberDocumentationProvider forMember;


  public DocumentationProviderBuilder withClassDocumentationProvider(ClassDocumentationProvider forClass) {
    this.forClass = forClass;
    return this;
  }

  public DocumentationProviderBuilder withParameterDocumentationProvider(ParameterDocumentationProvider forParameter) {
    this.forParameter = forParameter;
    return this;
  }

  public DocumentationProviderBuilder withMethodDocumentationProvider(MethodDocumentationProvider forMethod) {
    this.forMethod = forMethod;
    return this;
  }

  public DocumentationProviderBuilder withMethodReturnDocumentationProvider(MethodReturnDocumentationProvider forMethodReturn) {
    this.forMethodReturn = forMethodReturn;
    return this;
  }

  public DocumentationProviderBuilder withMemberDocumentationProvider(MemberDocumentationProvider forMember) {
    this.forMember = forMember;
    return this;
  }

  public DocumentationProvider build() {
    return new DocumentationProvider(
      forClass,
      forParameter,
      forMethod,
      forMethodReturn,
      forMember);
  }
}
