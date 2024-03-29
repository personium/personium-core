/**
 * Personium
 * Copyright 2019-2022 Personium Project Authors
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * D: JAXB object corresponding to inherited tag.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(namespace = "DAV:", name = "inherited")
public final class Inherited {
    /**
     * D: href tag.
     */
    @XmlElement(namespace = "DAV:", name = "href")
    String href;

    /**
     * set href.
     * @param href href
     */
    public void setHref(String href) {
        this.href = href;
    }
}
