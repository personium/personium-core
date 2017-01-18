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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.wink.webdav.model.Prop;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.Set;
import org.w3c.dom.Element;

import com.fujitsu.dc.core.model.DavCmp;

/**
 * mkcol タグに対応するJAXBオブジェクト.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"sets" })
@XmlRootElement(namespace = "DAV:", name = "mkcol")
public final class Mkcol {
    /**
     * setタグ.
     */
    @XmlElements({@XmlElement(namespace = "DAV:", name = "set", type = Set.class) })
    List<Set> sets = null;

    /**
     * Propsタグ.
     * @return JAXB Object
     */
    public List<Prop> getPropList() {
        List<Prop> ret = new ArrayList<Prop>();
        if (sets != null) {
            for (Set set : sets) {
                ret.add(set.getProp());
            }
        }
        return ret;
    }

    /**
     * @return JAXB Object
     */
    public Resourcetype getResourcetype() {
        Resourcetype ret = null;
        if (sets != null) {
            for (Set set : sets) {
                Prop p = set.getProp();
                if (p != null) {
                    ret = p.getResourcetype();
                }
            }
        }
        return ret;
    }

    /**
     * コレクションタイプ文字列を返します.
     * @return ColType文字列
     * @throws RequestException 例外
     */
    public String getDcColType() throws RequestException {
        Resourcetype rt = this.getResourcetype();
        if (rt == null) {
            throw new RequestException("resourcetype should be defined in mkcol request, See RFC5689 : extended MKCOL");
        }
        if (rt.getCollection() == null) {
            throw new RequestException("collection should be included in request. See RFC5689 : extended MKCOL");
        }
        List<Element> el = rt.getAny();
        if (el.size() == 0) {
            return DavCmp.TYPE_COL_WEBDAV;
        }
        if (el.size() != 1) {
            throw new RequestException(
                    "only 1 extension to collection specified in RFC5689 is allowed on this server. ");
        }
        Element e = el.get(0);
        String localName = e.getLocalName();
        String nsUri = e.getNamespaceURI();

        if ("urn:x-dc1:xmlns".equals(nsUri)) {
            if ("odata".equals(localName)) {
                return DavCmp.TYPE_COL_ODATA;
            } else if ("service".equals(localName)) {
                return DavCmp.TYPE_COL_SVC;
            }
            throw new RequestException("such DC1 extension not supported on this server");
        } else if ("urn:ietf:params:xml:ns:caldav".equals(e.getNamespaceURI())) {
            throw new RequestException("such Caldav extension not supported on this server");
        }
        throw new RequestException("such RFC5689 extension not supported on this server");
    }

    /**
     * 本クラスで発生する例外オブジェクト.
     */
    @SuppressWarnings("serial")
    public static class RequestException extends Exception {
        /**
         * @param msg
         */
        RequestException(final String msg) {
            super(msg);
        }
    }
}
