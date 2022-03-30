/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.personium.core.model.jaxb;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * JAXB's ObjectFactory jurisdiction over this package.
 * Because it inherits org.apache.wink.webdav.model.ObjectFactory,
 * In addition to the factory method defined for this class, various factory methods of the WebDav system are also provided.
 */
@XmlRegistry
public final class ObjectFactory extends org.apache.wink.webdav.model.ObjectFactory {

    /**
     * Generate Mkcol object.
     * @return Object for JAXB
     */
    public Mkcol createMkcol() {
        return new Mkcol();
    }

    /**
     * Generate MkcolResponse object.
     * @return Object for JAXB
     */
    public MkcolResponse createMkcolResponse() {
        return new MkcolResponse();
    }

    /**
     * Generate a ValidResourcetype object.
     * @return Object for JAXB
     */
    public ValidResourcetype createValidResourceType() {
        return new ValidResourcetype();
    }
    /**
     * Generate Acl object.
     * @return Object for JAXB
     */
    public Acl createAcl() {
        return new Acl();
    }
}
