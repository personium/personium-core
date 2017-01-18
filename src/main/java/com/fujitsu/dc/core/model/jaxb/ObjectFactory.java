/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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
package com.fujitsu.dc.core.model.jaxb;

import javax.xml.bind.annotation.XmlRegistry;

/**
 * このパッケージを管轄するJAXBのObjectFactory.
 * org.apache.wink.webdav.model.ObjectFactoryを継承しているため、
 * このクラスに定義されたファクトリメソッドのほかにWebDav系の様々なファクトリメソッドも提供される。
 */
@XmlRegistry
public final class ObjectFactory extends org.apache.wink.webdav.model.ObjectFactory {

    /**
     * Mkcol オブジェクトを生成.
     * @return JAXB用オブジェクト
     */
    public Mkcol createMkcol() {
        return new Mkcol();
    }

    /**
     * MkcolResponse オブジェクトを生成.
     * @return JAXB用オブジェクト
     */
    public MkcolResponse createMkcolResponse() {
        return new MkcolResponse();
    }

    /**
     * ValidResourcetype オブジェクトを生成.
     * @return JAXB用オブジェクト
     */
    public ValidResourcetype createValidResourceType() {
        return new ValidResourcetype();
    }
    /**
     * Acl オブジェクトを生成.
     * @return JAXB用オブジェクト
     */
    public Acl createAcl() {
        return new Acl();
    }
}
