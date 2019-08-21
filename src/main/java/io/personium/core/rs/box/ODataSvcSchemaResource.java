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
package io.personium.core.rs.box;

import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.NotImplementedException;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.format.xml.AtomServiceDocumentFormatWriter;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.DavRsCmp;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.CtlSchema;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.Property;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumOptionsQueryParser;
import io.personium.core.rs.odata.ODataResource;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.UriUtils;

/**
 * JAX-RS resource in charge of ODataSvcSchemaResource.
 */
public final class ODataSvcSchemaResource extends ODataResource {
    private static final MediaType APPLICATION_ATOMSVC_XML_MEDIATYPE =
            MediaType.valueOf(ODataConstants.APPLICATION_ATOMSVC_XML);
    ODataSvcCollectionResource odataSvcCollectionResource;
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param davRsCmp Resource of user data in charge of this schema
     * @param odataSvcCollectionResource ODataSvcCollectionResource object
     */
    ODataSvcSchemaResource(
            final DavRsCmp davRsCmp, final ODataSvcCollectionResource odataSvcCollectionResource) {
        super(davRsCmp.getAccessContext(),
                UriUtils.convertSchemeFromHttpToLocalCell(davRsCmp.getCell().getUrl(),
                        davRsCmp.getUrl() + "/$metadata/"),
                davRsCmp.getDavCmp().getSchemaODataProducer(davRsCmp.getCell()));
        this.odataSvcCollectionResource = odataSvcCollectionResource;
        this.davRsCmp = davRsCmp;
    }

    @Override
    public void checkAccessContext(AccessContext ac, Privilege privilege) {
        this.davRsCmp.checkAccessContext(privilege);
    }

    /**
     * Obtain Auth Scheme that can be used for authentication.
     * Autret Scheme that can be used for @return authentication
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.davRsCmp.getAcceptableAuthScheme();
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.davRsCmp.hasSubjectPrivilege(privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
    }

    /**
     * Corresponds to the service metadata request.
     * @param uriInfo UriInfo
     * @param format String
     * @param httpHeaders HttpHeaders
     * @return JAX-RS response object
     */
    @Override
    @GET
//    @Path("")
    public Response getRoot(@Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @Context HttpHeaders httpHeaders) {
        //Access control
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        //From the contents of $ format and Accept header,
        //Should Schema's Atom ServiceDocument be returned?
        //It is judged whether EDMX of data should be returned or not.
        if ("atomsvc".equals(format) || isAtomSvcRequest(httpHeaders)) {
            //Return Schema's Atom ServiceDocument
            EdmDataServices edmDataServices = CtlSchema.getEdmDataServicesForODataSvcSchema().build();

            StringWriter w = new StringWriter();
            AtomServiceDocumentFormatWriter fw = new AtomServiceDocumentFormatWriter();
            fw.write(UriUtils.createUriInfo(uriInfo, 0), w, edmDataServices);

            return Response.ok(w.toString(), fw.getContentType())
                    .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                    .build();
        }

        //Return EDMX of data
        ODataProducer userDataODataProducer = this.odataSvcCollectionResource.getODataProducer();
        EdmDataServices dataEdmDataSearvices = userDataODataProducer.getMetadata();
        StringWriter w = new StringWriter();
        EdmxFormatWriter.write(dataEdmDataSearvices, w);
        return Response.ok(w.toString(), ODataConstants.APPLICATION_XML_CHARSET_UTF8)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .build();
    }

    private boolean isAtomSvcRequest(HttpHeaders h) {
        return h.getAcceptableMediaTypes().contains(APPLICATION_ATOMSVC_XML_MEDIATYPE);
    }

    /**
     * Corresponds to the service metadata request.
     * @return JAX-RS response object
     */
    @GET
    @Path("{first: \\$}metadata")
    public Response getMetadata() {
        //Access control
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        //Return EDMX of the schema
        //Auth header check
        return super.doGetMetadata();
    }

    /**
     * OPTIONS Method.
     * @return JAX-RS Response
     */
    @Override
    @OPTIONS
//    @Path("")
    public Response optionsRoot() {
        //Access control
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        return super.doGetOptionsMetadata();
    }

    @Override
    public Privilege getNecessaryReadPrivilege(String entitySetNameStr) {
        return BoxPrivilege.READ;
    }

    @Override
    public Privilege getNecessaryWritePrivilege(String entitySetNameStr) {
        return BoxPrivilege.ALTER_SCHEMA;
    }

    @Override
    public Privilege getNecessaryOptionsPrivilege() {
        return BoxPrivilege.READ;
    }

    /**
     * Partial update preprocessing.
     * @param oEntityWrapper OEntityWrapper object
     * @param oEntityKey The entityKey to delete
     */
    @Override
    public void beforeMerge(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        //Check if EntityType is not supported
        String entityTypeName = oEntityWrapper.getEntitySetName();
        //Updates of Property, ComplexType and ComplexTypeProperty are not supported and therefore return 501
        if (entityTypeName.equals(Property.EDM_TYPE_NAME)
                || entityTypeName.equals(ComplexType.EDM_TYPE_NAME)
                || entityTypeName.equals(ComplexTypeProperty.EDM_TYPE_NAME)) {
            throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
        }
    }

    /**
     * Link registration preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    @Override
    public void beforeLinkCreate(OEntityId sourceEntity, String targetNavProp) {
        //Check if EntityType is not supported
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * Link acquisition preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    @Override
    public void beforeLinkGet(OEntityId sourceEntity, String targetNavProp) {
    }

    /**
     * Link deletion preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    @Override
    public void beforeLinkDelete(OEntityId sourceEntity, String targetNavProp) {
        //Check if EntityType is not supported
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * Check processing other than p: Format.
     * @param entitySetName entityset name
     * @param props property list
     */
    @Override
    public void validate(String entitySetName, List<OProperty<?>> props) {
        String type = null;
        for (OProperty<?> property : props) {
            if (property.getValue() == null) {
                continue;
            }
            //Get property name and value
            String propValue = property.getValue().toString();
            String propName = property.getName();

            if (propName.equals(Property.P_TYPE.getName())) {
                //Type validation
                // Edm.Boolean / Edm.String / Edm.Single / Edm.Int32 / Edm.Double / Edm.DateTime
                type = propValue;
                if (!propValue.equals(EdmSimpleType.STRING.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.SINGLE.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.INT32.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.DATETIME.getFullyQualifiedTypeName())) {
                    //Check registered ComplexType
                    BoolCommonExpression filter = PersoniumOptionsQueryParser.parseFilter(
                            "Name eq '" + propValue + "'");
                    QueryInfo query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
                    CountResponse reponse = this.getODataProducer().getEntitiesCount(ComplexType.EDM_TYPE_NAME, query);
                    if (reponse.getCount() == 0) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                    }
                }
            } else if (propName.equals(Property.P_COLLECTION_KIND.getName())) {
                //CollectionKind's Validate
                // None / List
                if (!propValue.equals(Property.COLLECTION_KIND_NONE)
                        && !propValue.equals(Property.COLLECTION_KIND_LIST)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                }
            } else if (propName.equals(Property.P_DEFAULT_VALUE.getName())) {
                //Validate of DefaultValue
                //Switch check contents according to Type value
                boolean result = false;
                if (type.equals(EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateBoolean(propValue);
                } else if (type.equals(EdmSimpleType.INT32.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateInt32(propValue);
                } else if (type.equals(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateDouble(propValue);
                } else if (type.equals(EdmSimpleType.SINGLE.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateSingle(propValue);
                } else if (type.equals(EdmSimpleType.STRING.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateString(propValue);
                } else if (type.equals(EdmSimpleType.DATETIME.getFullyQualifiedTypeName())) {
                    result = ODataUtils.validateDateTime(propValue);
                }
                if (!result) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                }
            }
        }
    }

    private void checkNonSupportLinks(String sourceEntity, String targetNavProp) {
        if (targetNavProp.startsWith("_")) {
            targetNavProp = targetNavProp.substring(1);
        }
        //$ Link specification of EntityType and AssociationEnd is not allowed (EntityType: AssociationEnd is 1: N relation)
        //$ Links specification of EntityType and Property is not possible (EntityType: Property is 1: N relation)
        //$ Links specification of ComplexType and ComplexTypeProperty is not allowed (ComplexType: ComplexTypeProperty is 1: N relation)
        if ((sourceEntity.equals(EntityType.EDM_TYPE_NAME) //NOPMD -To maintain readability
                        && targetNavProp.equals(AssociationEnd.EDM_TYPE_NAME))
                || (sourceEntity.equals(AssociationEnd.EDM_TYPE_NAME) //NOPMD
                        && targetNavProp.equals(EntityType.EDM_TYPE_NAME))
                || (sourceEntity.equals(EntityType.EDM_TYPE_NAME) //NOPMD
                        && targetNavProp.equals(Property.EDM_TYPE_NAME))
                || (sourceEntity.equals(Property.EDM_TYPE_NAME) //NOPMD
                        && targetNavProp.equals(EntityType.EDM_TYPE_NAME))
                || (sourceEntity.equals(ComplexType.EDM_TYPE_NAME) //NOPMD
                && targetNavProp.equals(ComplexTypeProperty.EDM_TYPE_NAME))
                || (sourceEntity.equals(ComplexTypeProperty.EDM_TYPE_NAME) //NOPMD
                && targetNavProp.equals(ComplexType.EDM_TYPE_NAME))) { //NOPMD
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
    }

    @Override
    public void setBasicAuthenticateEnableInBatchRequest(AccessContext ac) {
        //Because the schema level API does not support batch requests, we do not do anything here
    }

    /**
     * Not Implemented. <br />
     * Currently unimplemented because it is only necessary for $ batch access control <br />
     * Returns whether the access context has permission to $ batch.
     * @param ac access context
     * @return true: The access context has permission to $ batch
     */
    @Override
    public boolean hasPrivilegeForBatch(AccessContext ac) {
        throw new NotImplementedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(String entitySetName, String object, String info, String op) {
        String type = PersoniumEventType.odataSchema(entitySetName, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postLinkEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.odataSchemaLink(src, target, op);
        postEventInternal(type, object, info);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postNavPropEvent(String src, String object, String info, String target, String op) {
        String type = PersoniumEventType.odataSchemaNavProp(src, target, op);
        postEventInternal(type, object, info);
    }

    private void postEventInternal(String type, String object, String info) {
        PersoniumEvent ev = new PersoniumEvent.Builder()
                .type(type)
                .object(object)
                .info(info)
                .davRsCmp(this.davRsCmp)
                .build();
        EventBus eventBus = this.getAccessContext().getCell().getEventBus();
        eventBus.post(ev);
    }
}
