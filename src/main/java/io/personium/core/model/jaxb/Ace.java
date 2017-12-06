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
import javax.xml.bind.annotation.XmlType;

/**
 * D:ace タグに対応するJAXBオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(namespace = "DAV:", name = "ace", propOrder = {"principal", "grant" })
public final class Ace {
    /**
     * Principal.
     */
    @XmlElement(namespace = "DAV:", name = "principal")
    Principal principal;
    /**
     * Grant.
     */
    @XmlElement(namespace = "DAV:", name = "grant")
    Grant grant;

    /**
     * @return Principal/href
     */
    public String getPrincipalHref() {
        if (this.principal == null) {
            return null;
        }
        return this.principal.href;
    }
    /**
     * @param href href value to set
     */
    public void setPrincipalHref(String href) {
        if (this.principal == null || this.principal.href == null) {
        throw new IllegalStateException("This principal does not have href");
        }
        this.principal.href = href;
    }

    /**
     * @return Principal/all
     */
    public String getPrincipalAll() {
        if (this.principal == null) {
            return null;
        }
        return this.principal.all;
    }
    /**
     * @return String representation of privileges
     */
    public List<String> getGrantedPrivilegeList() {
        List<String> ret = new ArrayList<String>();
        if (grant == null) {
            return ret;
        }
        List<Privilege> privList = grant.privileges;
        if (privList != null) {
            for (Privilege priv : privList) {
                ret.add(priv.toString());
            }
        }
        return ret;
    }

}
