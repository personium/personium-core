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
package com.fujitsu.dc.test.jersey.box;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Collectionを扱うオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://www.w3.org/2007/app")
public class Collection {

    Collection() {
    }

    Collection(String title, String href) {
        this.title = title;
        this.href = href;
    }

    @XmlElement(namespace = "http://www.w3.org/2005/Atom", name = "title", type = String.class)
    String title;
    @XmlAttribute
    String href;

    /**
     * サービスドキュメントの比較.
     * @param obj オブジェクト
     * @return equal trueまたはfalseを返却する
     */
    @Override
    public boolean equals(Object obj) {
        Collection col = (Collection) obj;

        if (!this.title.equals(col.title)) {
            return false;
        }
        if (!this.href.equals(col.href)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Collection => Title:").append(title).append(", Href:").append(title).append("\n");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }

}
