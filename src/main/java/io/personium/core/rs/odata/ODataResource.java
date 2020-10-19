/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core.rs.odata;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * Route of JAX-RS Resource resource providing OData service
 * 4 kinds of usages.
 *  1. Unit control objects
 *  2. Cell control objects
 *  3. User OData Schema
 *  4. User OData
 * Create a subclass and give rootUrl and odataProducer in the constructor.
 * This class finishes all processing that does not depend on back side implementation,
 *  such as schema checking.
 */
public abstract class ODataResource extends ODataCtlResource {

    PersoniumODataProducer odataProducer;
    /** rootUrl is personium-localunit:/... or personium-localcell:/... */
    String rootUrl;
    EdmDataServices metadata;
    AccessContext accessContext;

    /**
     * log.
     */
    static Logger log = LoggerFactory.getLogger(ODataResource.class);

    /**
     * constructor.
     * @param accessContext AccessContext
     * @param rootUrl root URL
     * @param producer ODataProducer
     */
    public ODataResource(final AccessContext accessContext,
            final String rootUrl, final PersoniumODataProducer producer) {
        this.accessContext = accessContext;
        this.odataProducer = producer;
        this.rootUrl = rootUrl;
        this.metadata = this.odataProducer.getMetadata();
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * Check processing of authentication header.
     * @param ac accessContext
     * @param privilege Privilege
     */
    public abstract void checkAccessContext(Privilege privilege);

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * @return Autret Scheme that can be used for authentication
     */
    public abstract AcceptableAuthScheme getAcceptableAuthScheme();

    /**
     * Access authority check processing for resources.
     * @param ac accessContext
     * @param privilege privilege
     * @return Accessibility
     */
    public abstract boolean hasPrivilege(Privilege privilege);

    /**
     * Schema authentication check processing.
     * @param ac accessContext
     */
    public abstract void checkSchemaAuth(AccessContext ac);

    /**
     * Check processing for basic authentication (Batch request only).
     * @param ac accessContext
     */
    public abstract void setBasicAuthenticateEnableInBatchRequest(AccessContext ac);

    /**
     * Judgment on accessibility for each entity.
     * @param ac accessContext
     * @param oew OEntityWrapper
     */
    public void checkAccessContextPerEntity(AccessContext ac, OEntityWrapper oew) {
        //Check only at Unit level.
    }

    /**
     * Definition of additional search conditions by AccessContext, defined as necessary in subclass.
     * @param ac accessContext
     * @return $ filter grammar?
     */
    public String defineAccessContextSearchContext(AccessContext ac) {
        return null;
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    @OPTIONS
//    @Path("")
    public Response optionsRoot() {
        //Access control
        this.checkAccessContext(BoxPrivilege.READ);
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET
                ).build();
    }

    /**
     * OPTIONS method.
     * @return JAX-RS Response
     */
    protected Response doGetOptionsMetadata() {
        return ResourceUtils.responseBuilderForOptions(
                HttpMethod.GET
                ).build();
    }

    /**
     * Perform $ batch processing.
     * @return response
     */
    @Path("{first: \\$}batch")
    public ODataBatchResource processBatch() {
        return new ODataBatchResource(this);
    }

    /**
     * Return the service document.
     * @param uriInfo UriInfo
     * @param format String
     * @param httpHeaders HttpHeaders
     * @return JAX-RS Response Object
     */
    @GET
//    @Path("")
    public Response getRoot(
            @Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @Context HttpHeaders httpHeaders) {
        //Access control
        this.checkAccessContext(BoxPrivilege.READ);

        StringWriter w = new StringWriter();

        log.debug(format);
        List<MediaType> acceptableMediaTypes = null; // Enumerable.create(MediaType.APPLICATION_XML_TYPE).toList();

        FormatWriter<EdmDataServices> fw = FormatWriterFactory.getFormatWriter(EdmDataServices.class,
                acceptableMediaTypes, format, "");

        fw.write(UriUtils.createUriInfo(uriInfo, 0), w, this.metadata);

        return Response.ok(w.toString(), fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .build();
    }

    /**
     * Get the metadata object.
     * @return Metadata of type EdmDataServices
     */
    public EdmDataServices getMetadataSource() {
        return this.metadata;
    }

    /**
     * Corresponds to the service metadata request.
     * @return JAX-RS response object
     */
    protected Response doGetMetadata() {

        StringWriter w = new StringWriter();
        EdmxFormatWriter.write(this.metadata, w);
        return Response.ok(w.toString(), ODataConstants.APPLICATION_XML_CHARSET_UTF8)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .build();
    }

    /**
     * @param entitySetName Path representing entitySet name
     * @param request Request
     * @return ODataEntitiesResource
     */
    @Path("{entitySet}")
    public ODataEntitiesResource entities(
            @PathParam("entitySet") final String entitySetName,
            @Context Request request) {
        //If an entity set that does not exist is specified, an immediate error
        EdmEntitySet eSet = this.metadata.findEdmEntitySet(entitySetName);
        if (eSet == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
        }
        String method = request.getMethod();
        if (isChangeMethod(method.toUpperCase())) {
            this.odataProducer.onChange(entitySetName);
        }
        return new ODataEntitiesResource(this, entitySetName);
    }

    /**
     * @param entitySetName entitySet name
     * @param key key string
     * @param request Request
     * @return ODataEntityResource class object
     */
    @Path("{entitySet}({key})")
    public ODataEntityResource entity(
            @PathParam("entitySet") final String entitySetName,
            @PathParam("key") final String key,
            @Context Request request) {
        //If an entity set that does not exist is specified, an immediate error
        EdmEntitySet eSet = this.getMetadataSource().findEdmEntitySet(entitySetName);
        if (eSet == null) {
            throw PersoniumCoreException.OData.NO_SUCH_ENTITY_SET;
        }
        String method = request.getMethod();
        if (isChangeMethod(method.toUpperCase())) {
            this.odataProducer.onChange(entitySetName);
        }
        return new ODataEntityResource(this, entitySetName, key);
    }

    private boolean isChangeMethod(String method) {
        List<String> methods = Arrays.asList("GET", "OPTIONS", "HEAD", "PROPFIND");
        return !methods.contains(method);
    }

    /**
     * Returns the RootURL of this OData service.
     * @return Root Url of this OData Service
     */
    public String getRootUrl() {
        return this.rootUrl;
    }

    /**
     * Returns the ODataProducer of this OData service.
     * @return ODataProducer of this OData Service
     */
    public PersoniumODataProducer getODataProducer() {
        return this.odataProducer;
    }

    /**
     * Retrieve the ETag value from the Etag header value.
     * @param etagHeaderValue etagHeaderValue
     * @return etag
     */
    public static String parseEtagHeader(final String etagHeaderValue) {
        if (etagHeaderValue == null) {
            return null;
        } else if ("*".equals(etagHeaderValue)) {
            return "*";
        }
        //Weak format W / "()"
        Pattern pattern = Pattern.compile("^W/\"(.+)\"$");
        Matcher m = pattern.matcher(etagHeaderValue);

        if (!m.matches()) {
            throw PersoniumCoreException.OData.ETAG_NOT_MATCH;
        }

        return m.replaceAll("$1");
    }

    /**
     * Generate Etag header value.
     * @param etag etag
     * @return etagHeaderValue
     */
    public static String renderEtagHeader(final String etag) {
        return "W/\"" + etag + "\"";
    }

    /**
     * Returns the privileges required for reading from the level of the class to be processed and the entity set name.
     * @param entitySetNameStr target entity set
     * @return Privileges required for processing
     */
    public abstract Privilege getNecessaryReadPrivilege(String entitySetNameStr);

    /**
     * Returns the authority required for writing from the level of the class to be processed and the entity set name.
     * @param entitySetNameStr target entity set
     * @return Privileges required for processing
     */
    public abstract Privilege getNecessaryWritePrivilege(String entitySetNameStr);

    /**
     * Returns the necessary authority for OPTIONS from the level of the class to be processed and the entity set name.
     * @return Privileges required for processing
     */
    public abstract Privilege getNecessaryOptionsPrivilege();

    /**
     * Returns whether the access context has permission to $ batch.
     * @param ac access context
     * @return true: The access context has permission to $ batch
     */
    public abstract boolean hasPrivilegeForBatch(AccessContext ac);

    /**
     * Post links event to EventBus.
     * @param src the name of the entitySet of processing object
     * @param object string of processing object
     * @param info string of the information about processing
     * @param target the name of the target entitySet
     * @param op kind of operation
     */
    public void postLinkEvent(String src, String object, String info, String target, String op) {
    }

    /**
     * Post navprop event to EventBus.
     * @param src the name of the entitySet of processing object
     * @param object string of processing object
     * @param info string of the information about processing
     * @param target the name of the target entitySet
     * @param op kind of operation
     */
    public void postNavPropEvent(String src, String object, String info, String target, String op) {
    }

}
