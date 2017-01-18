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

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

/**
 * Workspaceを扱うオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "http://www.w3.org/2007/app")
public class Workspace {
    Workspace() {
    }

    Workspace(String title, Set<Collection> collections) {
        this.title = title;
        this.collections = collections;
    }

    @XmlElement(namespace = "http://www.w3.org/2005/Atom", name = "title", type = String.class)
    String title;
    @XmlElements({@XmlElement(name = "collection", type = Collection.class) })
    Set<Collection> collections;

    /**
     * サービスドキュメントの比較.
     * @param obj オブジェクト
     * @return equal trueまたはfalseを返却する
     */
    public boolean isEqualTo(Object obj) {
        Workspace ws = (Workspace) obj;
        if (collections.size() != ws.collections.size()) {
            Edmx.printResult(this, "Collection", ws.collections.toString(), collections.toString());
        }
        assertEquals(collections, ws.collections);
        if (!title.equals(ws.title)) {
            Edmx.printResult(this, "Title", ws.title, title);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Workspace => title:").append(title).append("\n");
        for (Collection collection : collections) {
            builder.append(collection);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
