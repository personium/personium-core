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
package io.personium.core.model.jaxb;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Propstat;

/**
 * A JAXB object corresponding to the mkcol-response tag.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "stats" })
@XmlRootElement(namespace = "DAV:", name = "mkcol-response")
public final class MkcolResponse {
    /**
     * propstat tag.
     */
    @XmlElements({ @XmlElement(namespace = "DAV:", name = "propstat", type = Propstat.class) })
    List<Propstat> stats;

    /**
     * @return Prop List
     */
    public List<Prop> getPropList() {
        List<Prop> ret = new ArrayList<Prop>();
        for (Propstat stat : this.stats) {
            ret.add(stat.getProp());
        }
        return ret;
    }

    /**
     * @param stat Propstat Object
     */
    public void addPropstat(final Propstat stat) {
        if (this.stats == null) {
            this.stats = new ArrayList<Propstat>();
        }
        this.stats.add(stat);
    }
}
