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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import io.personium.common.auth.token.Role;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.CellPrivilege;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.Privilege;

/**
 * A model object representing an ACL.
 * It behaves as a JAXB object corresponding to the D: acl tag of the WebDAV ACL,
 * Object creation is possible by directly unmarshalling the XML received by the ACL method.
 * On the other hand, it also supports serialization to JSON and deserialization from JSON,
 * It can be used in JSON - based persistence mechanism including ElasticSearch.
 * By giving this object to the AccessContext object,
 * Generate Privilege list to be given.
 */
@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = { "aces" })
@XmlRootElement(namespace = "DAV:", name = "acl")
public final class Acl {

    static final String KEY_REQUIRE_SCHEMA_AUTHZ = "@requireSchemaAuthz";

    /** xml:base. */
    @XmlAttribute(namespace = "http://www.w3.org/XML/1998/namespace")
    String base;

    /** p:requireSchemaAuthz. */
    @XmlAttribute(namespace = PersoniumCoreUtils.XmlConst.NS_PERSONIUM)
    String requireSchemaAuthz;

    /** Ace tag.*/
    @XmlElements({ @XmlElement(namespace = "DAV:", name = "ace", type = Ace.class) })
    List<Ace> aces = new ArrayList<Ace>();

    /**
     * p:requireSchemaAuthz setter.
     * @param requireSchemaAuthz requireSchemaAuthz
     */
    public void setRequireSchemaAuthz(String requireSchemaAuthz) {
        this.requireSchemaAuthz = requireSchemaAuthz;
    }

    /**
     * p:requireSchemaAuthz getter.
     * @return requireSchemaAuthz
     */
    public String getRequireSchemaAuthz() {
        return requireSchemaAuthz;
    }

    /**
     * xml:base setter.
     * @param base baseUrl
     */
    public void setBase(String base) {
        this.base = base;
    }

    /**
     * xml:base getter.
     * @return base
     */
    public String getBase() {
        return base;
    }

    /**
     * Ace.
     * @return Ace Object
     */
    public List<Ace> getAceList() {
        return aces;
    }

    /**
     * Make it JSON.
     * @return Map object
     */
    public String toJSON() {
        StringWriter sw = new StringWriter();
        try {
            ObjectIo.toJson(this, sw);
            return sw.toString();
        } catch (IOException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        } catch (JAXBException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        }
    }

    /**
     * @param jsonString acl json
     * @return Acl obj
     */
    public static Acl fromJson(final String jsonString) {
        StringReader sr = new StringReader(jsonString);
        try {
            Acl ret = ObjectIo.fromJson(sr, Acl.class);
            if (ret == null) {
                ret = new Acl();
            }
            //  attr somehow not unmarshalled so manually fix the object
            JSONParser parser = new JSONParser();
            JSONObject j = (JSONObject) parser.parse(jsonString);
            ret.setRequireSchemaAuthz((String) j.get(KEY_REQUIRE_SCHEMA_AUTHZ));
            return ret;
        } catch (IOException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        } catch (JAXBException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        } catch (ParseException e) {
            throw PersoniumCoreException.Server.DATA_STORE_UNKNOWN_ERROR.reason(e);
        }
    }

    /**
     * For AccessContext, what kind of privilege this ACL gives is returned.
     * @ param ac AccessContext object
     * @return Privilege List
     */
    public List<String> allows(final AccessContext ac) {
        List<Role> roles = ac.getRoleList();
        List<String> ret = new ArrayList<String>();
        for (Role role : roles) {
            for (Ace ace : this.aces) {
                if (ace.getPrincipalHref().equals(role.createUrl())) {
                    List<String> privList = ace.getGrantedPrivilegeList();
                    for (String priv : privList) {
                        ret.add(priv);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * For AccessContext, it returns whether this ACL gives a specific Privilege.
     * @ param priv Privilege you want to check
     * @ param ac AccessContext object
     * @ param privilegeMap Privilege management
     * @return True if given
     */
    public boolean allows(final Privilege priv, final AccessContext ac, Map<String, Privilege> privilegeMap) {
        List<String> privs = this.allows(ac);
        boolean ret = false;
        for (String p : privs) {
            Privilege pObj = privilegeMap.get(p);
            if (pObj.includes(priv)) {
                ret = true;
            }
        }
        return ret;
    }

    /**
     * Validate ACL.
     * @param isCellLevel true:CellLevel
     */
    public void validateAcl(boolean isCellLevel) {
        // Check whether requireSchemaAuthz matches permitted value.
        if (!OAuth2Helper.SchemaLevel.isMatchPermittedValue(requireSchemaAuthz)) {
            String cause = String.format("Value [%s] for requireSchemaAuthz is invalid", requireSchemaAuthz);
            throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(cause);
        }
        // <!ELEMENT acl (ace*) >
        if (aces == null) {
            return;
        }
        for (Ace ace : aces) {
            // <!ELEMENT ace ((principal or invert), (grant or deny), protected?,inherited?)>
            if (ace.grant == null) {
                throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("Can not read grant");
            }
            if (ace.principal == null) {
                throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("Can not read principal");
            }
            // <!ELEMENT grant (privilege+)>
            if (ace.grant.privileges == null) {
                throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("Can not read privilege");
            }
            Map<String, CellPrivilege> cellPrivilegeMap = CellPrivilege.getPrivilegeMap();
            Map<String, BoxPrivilege> boxPrivilegeMap = BoxPrivilege.getPrivilegeMap();

            for (io.personium.core.model.jaxb.Privilege privilege : ace.grant.privileges) {
                // Privilege is not empty.
                if (privilege.body == null) {
                    throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("Privilege is empty");
                }
                // This tag that can be set to privilege?
                String localName = privilege.body.getLocalName();
                if (isCellLevel) {
                    if (!cellPrivilegeMap.containsKey(localName)
                     && !boxPrivilegeMap.containsKey(localName)) {
                        String cause = String.format("[%s] that can not be set in privilege", localName);
                        throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(cause);
                    }
                } else {
                    if (!boxPrivilegeMap.containsKey(localName)) {
                        String cause = String.format("[%s] that can not be set in privilege", localName);
                        throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params(cause);
                    }
                }
            }
            // <!ELEMENT principal (href or all)>
            // <!ELEMENT href ANY>
            if (ace.principal.all == null) {
                if (ace.principal.href == null) {
                    throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("Principal is neither href nor all");
                } else if (ace.principal.href.equals("")) {
                    throw PersoniumCoreException.Dav.XML_VALIDATE_ERROR.params("href in principal is empty");
                }
            }
        }
    }
}
