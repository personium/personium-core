/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.model;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpStatus;
import org.apache.wink.webdav.model.Creationdate;
import org.apache.wink.webdav.model.Getcontentlength;
import org.apache.wink.webdav.model.Getcontenttype;
import org.apache.wink.webdav.model.Getlastmodified;
import org.apache.wink.webdav.model.Multistatus;
import org.apache.wink.webdav.model.ObjectFactory;
import org.apache.wink.webdav.model.Propertyupdate;
import org.apache.wink.webdav.model.Propfind;
import org.apache.wink.webdav.model.Resourcetype;
import org.apache.wink.webdav.model.WebDAVModelHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.personium.common.auth.token.Role;
import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreAuthzException;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.model.jaxb.Ace;
import io.personium.core.model.jaxb.Acl;
import io.personium.core.model.jaxb.ObjectIo;
import io.personium.core.rs.box.DavCollectionResource;
import io.personium.core.rs.box.DavFileResource;
import io.personium.core.rs.box.NullResource;
import io.personium.core.rs.box.ODataSvcCollectionResource;
import io.personium.core.rs.box.PersoniumEngineSvcCollectionResource;
import io.personium.core.rs.box.StreamCollectionResource;
import io.personium.core.utils.ResourceUtils;

/**
 * A component class to process WebDAV related request delegated from JaxRS Resource objects.
 * Some process are further delegated to DavCmp classes.
 */
public class DavRsCmp {
    /**
     * Logger.
     */
    private static Logger log = LoggerFactory.getLogger(DavRsCmp.class);

    DavCmp davCmp;
    DavRsCmp parent;
    String pathName;
    ObjectFactory of;

    /**
     * constructor.
     * @param parent Parent Object
     * @param davCmp Component that handle processes dependent on backend implementation
     */
    public DavRsCmp(final DavRsCmp parent, final DavCmp davCmp) {
        this.parent = parent;
        this.davCmp = davCmp;
        this.of = new ObjectFactory();

        if (this.davCmp != null) {
            this.pathName = this.davCmp.getName();
        }
    }

    /**
     * returns Jax-RS resource in charge of child path.
     * @param nextPath child path name
     * @param request HttpServletRequest
     * @return Jax-RS resource in charge of the child path
     */
    public Object nextPath(final String nextPath, final HttpServletRequest request) {

        // return NullResource (Non-Existent) if davCmp does not exist.
        if (this.davCmp == null) {
            return new NullResource(this, null, true);
        }
        DavCmp nextCmp = this.davCmp.getChild(nextPath);
        String type = nextCmp.getType();

        if (DavCmp.TYPE_NULL.equals(type)) {
            if (DavCmp.TYPE_NULL.equals(this.davCmp.getType())) {
                return new NullResource(this, nextCmp, true);
            } else {
                return new NullResource(this, nextCmp, false);
            }
        } else if (DavCmp.TYPE_COL_WEBDAV.equals(type)) {
            return new DavCollectionResource(this, nextCmp);
        } else if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            return new DavFileResource(this, nextCmp);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            return new ODataSvcCollectionResource(this, nextCmp);
        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            return new PersoniumEngineSvcCollectionResource(this, nextCmp);
        } else if (DavCmp.TYPE_COL_STREAM.equals(type)) {
            return new StreamCollectionResource(this, nextCmp);
        }

        return null;
    }

    /**
     * returns the URL string of this resource.
     * @return URL String
     */
    public String getUrl() {
        // recursively goes to the BoxResource. BoxResource overrides this method and provide root url.
        return this.parent.getUrl() + "/" + this.pathName;
    }

    /**
     * returns the Cell which this resource belongs to.
     * @return Cell Object
     */
    public Cell getCell() {
        // recursively goes to the BoxResource. BoxResource overrides this method and provide Cell object.
        return this.parent.getCell();
    }

    /**
     * returns the Box which this resource belongs to.
     * @return Box Object
     */
    public Box getBox() {
        // recursively goes to the BoxResource. BoxResource overrides this method and provide Box object.
        return this.parent.getBox();
    }

    /**
     * Returns davCmp Object.
     * @return davCmp
     */
    public DavCmp getDavCmp() {
        return this.davCmp;
    }

    /**
     * returns parent DavRsCmp object.
     * @return DavRsCmp
     */
    public DavRsCmp getParent() {
        return this.parent;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.parent.getAccessContext();
    }

    /**
     * @param etag string
     * @return true if given string matches  the stored Etag
     */
    public boolean matchesETag(String etag) {
        if (etag == null) {
            return false;
            }
        String storedEtag = this.davCmp.getEtag();
        String weakEtag = "W/" +  storedEtag;
        return etag.equals(storedEtag) || etag.equals(weakEtag);
    }

    /**
     * Process a GET request.
     * @param ifNoneMatch ifNoneMatch header
     * @param rangeHeaderField range header
     * @return ResponseBuilder object
     */
    public final ResponseBuilder get(final String ifNoneMatch, final String rangeHeaderField) {
        // return "Not-Modified" if "If-None-Match" header matches.
        if (matchesETag(ifNoneMatch)) {
            return Response.notModified().header(HttpHeaders.ETAG, this.davCmp.getEtag());
        }
        return this.davCmp.get(rangeHeaderField);
    }

    /**
     * Process PROPFIND method. Common behavior independent from backend implementation.
     * @param requestBodyXml requestBody
     * @param depth Depth Header
     * @param contentLength Content-Length Header
     * @param transferEncoding Transfer-Encoding Header
     * @param requiredForReadAcl Privilege required for ACL reading
     * @return Jax-RS Response object
     */
    public final Response doPropfind(final Reader requestBodyXml, final String depth,
            final Long contentLength, final String transferEncoding, final Privilege requiredForReadAcl) {

        // ACL config output is allowed by Unit User or when ACL Privilege is configured.
        boolean canAclRead = false;
        if (this.getAccessContext().isUnitUserToken(requiredForReadAcl)
                || this.hasPrivilege(this.getAccessContext(), requiredForReadAcl)) {
            canAclRead = true;
        }

        // Parse the request and create propfind object
        Propfind propfind = null;
        if (ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncoding)) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(requestBodyXml);
                propfind = Propfind.unmarshal(br);
            } catch (Exception e1) {
                throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
            }
        } else {
            log.debug("Content-Length 0");
        }

        // Valid values for Depth Header are either 0 or 1
        // We do not support infinity so return 403
        if ("infinity".equals(depth)) {
            throw PersoniumCoreException.Dav.PROPFIND_FINITE_DEPTH;
        } else if (depth == null) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params("null");
        } else if (!("0".equals(depth) || "1".equals(depth))) {
            throw PersoniumCoreException.Dav.INVALID_DEPTH_HEADER.params(depth);
        }

        String reqUri = this.getUrl();
        // take away trailing slash
        if (reqUri.endsWith("/")) {
            reqUri = reqUri.substring(0, reqUri.length() - 1);
        }

        // The actural processing
        final Multistatus ms = this.of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> resList = ms.getResponse();
        resList.add(createDavResponse(pathName, reqUri, this.davCmp, propfind, canAclRead));

        // if Depth is not 0, then process children.
        if (!"0".equals(depth)) {
            resList.addAll(createChildrenDavResponseList(reqUri, propfind, canAclRead));
        }

        // output the result
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException {
                Multistatus.marshal(ms, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .header(HttpHeaders.ETAG, this.davCmp.getEtag())
                .header("Content-Type", "application/xml")
                .entity(str).build();
    }

    /**
     * create children DavResponse list.
     * @param reqUri request url
     * @param propfind propfind
     * @param canAclRead can acl read
     * @return DavResponse list
     */
    protected List<org.apache.wink.webdav.model.Response> createChildrenDavResponseList(String reqUri,
            Propfind propfind, boolean canAclRead) {
        List<org.apache.wink.webdav.model.Response> resList = new ArrayList<>();
        Map<String, DavCmp> childrenMap = this.davCmp.getChildren();
        for (String childName : childrenMap.keySet()) {
            DavCmp child = childrenMap.get(childName);
            resList.add(createDavResponse(childName, reqUri + "/" + child.getName(), child, propfind, canAclRead));
        }
        return resList;
    }

    /**
     * process PROPPATCH request.
     * @param reqBodyXml requestBody
     * @return Jax-RS Response object
     */
    public final Response doProppatch(final Reader reqBodyXml) {

        // parse the requet and create Propertyupdate object
        BufferedReader br = null;
        Propertyupdate pu = null;
        try {
            br = new BufferedReader(reqBodyXml);
            pu = Propertyupdate.unmarshal(br);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.XML_ERROR.reason(e1);
        }

        // Actual Logic
        final Multistatus ms = this.davCmp.proppatch(pu, this.getUrl());

        // Output the results
        StreamingOutput str = new StreamingOutput() {
            @Override
            public void write(final OutputStream os) throws IOException {
                Multistatus.marshal(ms, os);
            }
        };
        return Response.status(HttpStatus.SC_MULTI_STATUS)
                .header(HttpHeaders.ETAG, this.davCmp.getEtag())
                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                .entity(str).build();
    }

    /**
     * ACL Method. configuring ACL.
     * @param reader Configuration XML
     * @return JAX-RS Response
     */
    public final Response doAcl(final Reader reader) {

        return this.davCmp.acl(reader).build();
    }

    /**
     * @return Schema Authentication level
     */
    public String getConfidentialLevel() {
        String confidentialStringTmp = null;
        if (this.davCmp == null) {
            confidentialStringTmp = this.parent.getConfidentialLevel();
        } else {
            confidentialStringTmp = this.davCmp.getConfidentialLevel();
        }

        if (confidentialStringTmp == null || "".equals(confidentialStringTmp)) {
            if (this.parent == null) {
                // App Authn regarded not necessary
                // if there is no configuration up to box
                return OAuth2Helper.SchemaLevel.NONE;
            }
            confidentialStringTmp = this.parent.getConfidentialLevel();
        }
        return confidentialStringTmp;
    }

    /**
     * merging with ancestorial ACL, and check if it is accessible.
     * @param ac AccessContext
     * @param privilege ACL Privilege (read/write/bind/unbind)
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return hasPrivilege(ac, privilege, privilege);
    }

    /**
     * merging with ancestorial ACL, and check if it is accessible.
     * @param ac AccessContext
     * @param privilege ACL Privilege (read/write/bind/unbind) If it is null, it does not refer to the current's authority.
     * @param parentPrivilege parent ACL Privilege (read/write/bind/unbind) If it is null, it does not refer to the parent's authority.
     * @return boolean
     */
    public boolean hasPrivilege(AccessContext ac, Privilege privilege, Privilege parentPrivilege) {
        // skip ACL check if davCmp does not exist.
        // (nonexistent resource is specified)
        if (privilege != null && this.davCmp != null
                && this.getAccessContext().requirePrivilege(this.davCmp.getAcl(), privilege)) {
            return true;
        }

        // check parent (recursively)
        if (parentPrivilege != null && this.parent != null && this.parent.hasPrivilege(ac, parentPrivilege)) {
            return true;
        }

        return false;
    }

    /**
     * OPTIONS Method.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // AccessControl
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);

        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                HttpMethod.DELETE,
                io.personium.common.utils.CommonUtils.HttpMethod.MKCOL,
                io.personium.common.utils.CommonUtils.HttpMethod.PROPFIND,
                io.personium.common.utils.CommonUtils.HttpMethod.PROPPATCH,
                io.personium.common.utils.CommonUtils.HttpMethod.ACL
                ).build();
    }

    /**
     * Check Access Control.
     * Exceptions are thrown if it does not have the privilege
     * @param ac AccessContext
     * @param privilege Privilege to check if it is given
     */
    public void checkAccessContext(final AccessContext ac, Privilege privilege) {
        checkAccessContext(ac, privilege, privilege);
    }

    /**
     * Check Access Control.
     * Exceptions are thrown if it does not have the privilege
     * @param ac AccessContext
     * @param privilege Privilege to check if it is given
     * @param parentPrivilege parent ACL Privilege
     */
    public void checkAccessContext(final AccessContext ac, Privilege privilege, Privilege parentPrivilege) {
        // if accessed with valid UnitUserToken then fine.
        if (ac.isUnitUserToken(privilege)) {
            return;
        }

        // if accessed with PasswordChangeAccessToken then invalid.
        if (AccessContext.TYPE_PASSWORD_CHANGE.equals(ac.getType())) {
            throw PersoniumCoreAuthzException.ACCESS_WITH_PASSWORD_CHANGE_ACCESS_TOKEN.realm(
                    ac.getRealm(), getAcceptableAuthScheme());
        }

        AcceptableAuthScheme allowedAuthScheme = getAcceptableAuthScheme();

        // check Schema Authn
        ac.checkSchemaAccess(this.getConfidentialLevel(), this.getBox(), allowedAuthScheme);

        // check if Basic Authn is possible
        ac.updateBasicAuthenticationStateForResource(this.getBox());

        // check Access Privilege
        if (!this.hasPrivilege(ac, privilege, parentPrivilege)) {
            // check token validity
            // check here because access should be allowed when Privilege "all" is configured
            // even if the token is invalid
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(allowedAuthScheme);
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw PersoniumCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(), allowedAuthScheme);
            }
            throw PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * get Acceptable Auth Scheme.
     * @return AcceptableAuthScheme
     */
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        AcceptableAuthScheme allowedAuthScheme = AcceptableAuthScheme.ALL;
        // check if this resource if under a box with Schema URL
        String boxSchema = this.getBox().getSchema();
        // only Bearer scheme is allowed if Box Schema URL is defined
        if (boxSchema != null && boxSchema.length() > 0 && !Role.DEFAULT_BOX_NAME.equals(this.getBox().getName())) {
            allowedAuthScheme = AcceptableAuthScheme.BEARER;
        }
        return allowedAuthScheme;
    }

    /**
     * check if the specified account can be the representative of the Cell Owner Unit User.
     * @param account Account name to check
     * @return true if it has privilege
     * @deprecated
     */
    public boolean checkOwnerRepresentativeAccounts(final String account) {
        List<String> ownerRepresentativeAccountsSetting = this.davCmp.getOwnerRepresentativeAccounts();
        if (ownerRepresentativeAccountsSetting == null || account == null) {
            return false;
        }

        for (String ownerRepresentativeAccount : ownerRepresentativeAccountsSetting) {
            if (account.equals(ownerRepresentativeAccount)) {
                return true;
            }
        }
        return false;
    }

    /**
     * URL esacaping if the resource name is multibyte.
     * @return Escaping URL
     */
    protected String getEsacapingUrl() {
        String reqUri = this.getUrl();
        // take away trailing slash
        if (reqUri.endsWith("/")) {
            reqUri = reqUri.substring(0, reqUri.length() - 1);
        }

        // URL esacaping if the resource name is multibyte
        int resourcePos = reqUri.lastIndexOf("/");
        if (resourcePos != -1) {
            String resourceName = reqUri.substring(resourcePos + 1);
            try {
                resourceName = URLEncoder.encode(resourceName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.debug("UnsupportedEncodingException");
            }
            String collectionUrl = reqUri.substring(0, resourcePos);
            reqUri = collectionUrl + "/" + resourceName;
        }
        return reqUri;
    }

    static final org.apache.wink.webdav.model.Response createDavResponse(final String pathName,
            final String href,
            final DavCmp dCmp,
            final Propfind propfind,
            final boolean isAclRead) {
        ObjectFactory of = new ObjectFactory();
        org.apache.wink.webdav.model.Response ret = of.createResponse();
        ret.getHref().add(href);

        // TODO change what to return depending on PROPFIND request content
        if (propfind != null) {

            log.debug("isAllProp:" + propfind.isAllprop());
            log.debug("isPropName:" + propfind.isPropname());
        } else {
            log.debug("propfind is null");
        }

        /*
         * Displayname dn = of.createDisplayname(); dn.setValue(name); ret.setPropertyOk(dn);
         */

        Long updated = dCmp.getUpdated();
        if (updated != null) {
            Getlastmodified lm = of.createGetlastmodified();
            lm.setValue(new Date(updated));
            ret.setPropertyOk(lm);
        }
        Long published = dCmp.getPublished();
        if (published != null) {
            Creationdate cd = of.createCreationdate();
            cd.setValue(new Date(published));
            ret.setPropertyOk(cd);
        }
        String type = dCmp.getType();
        if (DavCmp.TYPE_DAV_FILE.equals(type)) {
            // Dav File
            Resourcetype rt1 = of.createResourcetype();
            ret.setPropertyOk(rt1);
            Getcontentlength gcl = new Getcontentlength();
            gcl.setValue(String.valueOf(dCmp.getContentLength()));
            ret.setPropertyOk(gcl);
            String contentType = dCmp.getContentType();
            Getcontenttype gct = new Getcontenttype();
            gct.setValue(contentType);
            ret.setPropertyOk(gct);
        } else if (DavCmp.TYPE_COL_ODATA.equals(type)) {
            // OData Service Resource
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            List<Element> listElement = colRt.getAny();
            QName qname = new QName(CommonUtils.XmlConst.NS_PERSONIUM, CommonUtils.XmlConst.ODATA,
                    CommonUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            listElement.add(element);
            ret.setPropertyOk(colRt);

        } else if (DavCmp.TYPE_COL_SVC.equals(type)) {
            // Engine Service Resource
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            List<Element> listElement = colRt.getAny();
            QName qname = new QName(CommonUtils.XmlConst.NS_PERSONIUM, CommonUtils.XmlConst.SERVICE,
                    CommonUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            listElement.add(element);
            ret.setPropertyOk(colRt);

        } else if (DavCmp.TYPE_COL_STREAM.equals(type)) {
            // Stream Resource
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            List<Element> listElement = colRt.getAny();
            QName qname = new QName(CommonUtils.XmlConst.NS_PERSONIUM, CommonUtils.XmlConst.STREAM,
                    CommonUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            listElement.add(element);
            ret.setPropertyOk(colRt);

        } else if (DavCmp.TYPE_CELL.equals(type)) {
            // Cell
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            ret.setPropertyOk(colRt);

            // Add cellstatus.
            QName qname = new QName(CommonUtils.XmlConst.NS_PERSONIUM, CommonUtils.XmlConst.CELL_STATUS,
                    CommonUtils.XmlConst.NS_PREFIX_PERSONIUM);
            Element element = WebDAVModelHelper.createElement(qname);
            element.setTextContent(dCmp.getCellStatus());
            ret.setPropertyOk(element);
        } else if (DavCmp.TYPE_NULL.equals(type)) {
            // If TYPE_NULL, not display anything.
            ret.getHref().remove(href);
        } else {
            // Collection Resource
            Resourcetype colRt = of.createResourcetype();
            colRt.setCollection(of.createCollection());
            ret.setPropertyOk(colRt);
        }

        // Processing ACL
        Acl acl = dCmp.getAcl();
        if (isAclRead && acl != null) {

            Acl outputAcl = new Acl();
            outputAcl.setBase(acl.getBase());
            outputAcl.setRequireSchemaAuthz(acl.getRequireSchemaAuthz());

            // Get ace list including parents.
            List<Ace> aces = getAces(dCmp, true);
            outputAcl.getAceList().addAll(aces);

            // Convert to Element.
            Document aclDoc = null;

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            try {
                aclDoc = dbf.newDocumentBuilder().newDocument();
                ObjectIo.marshal(outputAcl, aclDoc);
            } catch (Exception e) {
                throw new WebApplicationException(e);
            }
            if (aclDoc != null) {
                Element e = aclDoc.getDocumentElement();
                ret.setPropertyOk(e);
            }
        }

        // Processing Other Props
        Map<String, String> props = dCmp.getProperties();
        if (props != null) {
            List<String> nsList = new ArrayList<String>();
            for (Map.Entry<String, String> entry : props.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                int idx = key.indexOf("@");
                String ns = key.substring(idx + 1, key.length());

                int nsIdx = nsList.indexOf(ns);
                if (nsIdx == -1) {
                    nsList.add(ns);
                }

                Element e = parseProp(val);

                ret.setPropertyOk(e);
            }

        }
        return ret;
    }
    private static Element parseProp(String value) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            ByteArrayInputStream is = new ByteArrayInputStream(value.getBytes(CharEncoding.UTF_8));
            doc = builder.parse(is);
        } catch (Exception e1) {
            throw PersoniumCoreException.Dav.DAV_INCONSISTENCY_FOUND.reason(e1);
        }
        Element e = doc.getDocumentElement();
        return e;
    }

    /**
     * Get ACEs.
     * @param dCmp JaxRS Resources handling DAV
     * @param isCurrent is current
     * @return Ace list
     */
    private static List<Ace> getAces(final DavCmp dCmp, boolean isCurrent) {
        List<Ace> aces = new ArrayList<>();
        Acl acl = dCmp.getAcl();
        if (acl != null) {
            if (isCurrent) {
                aces.addAll(acl.getAceList());
            } else {
                for (Ace parentAce : acl.getAceList()) {
                    parentAce.setInheritedHref(dCmp.getUrl());
                    aces.add(parentAce);
                }
            }
        }
        if (dCmp.getParent() != null) {
            aces.addAll(getAces(dCmp.getParent(), false));
        }
        return aces;
    }

    /**
     * returns the RequestKey string.
     * @return RequestKey String
     */
    public String getRequestKey() {
        return this.parent.getRequestKey();
    }

    /**
     * returns the EventId string.
     * @return EventId String
     */
    public String getEventId() {
        return this.parent.getEventId();
    }

    /**
     * returns the RuleChain string.
     * @return RuleChain String
     */
    public String getRuleChain() {
        return this.parent.getRuleChain();
    }

    /**
     * returns the Via string.
     * @return Via String
     */
    public String getVia() {
        return this.parent.getVia();
    }

}
