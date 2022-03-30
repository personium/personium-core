/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OEntityKey.KeyType;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;

import io.personium.core.PersoniumCoreException;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.ctl.Rule;
import io.personium.core.odata.OEntityWrapper;

/**
 * JAX-RS resource handling MERGE method of Entity resource of OData.
 */
public class ODataMergeResource extends ODataEntityResource {

    /**
     * constructor.
     * @param odataResource ODataResource which is the parent resource
     * @param entitySetName EntitySet Name
     * @param keyString key string
     * @param oEntityKey OEntityKey object
     */
    public ODataMergeResource(ODataResource odataResource,
            String entitySetName, String keyString, OEntityKey oEntityKey) {
        super(odataResource, entitySetName, keyString, oEntityKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkNotAllowedMethod() {
        // Rule can not be merged.
        if (Rule.EDM_TYPE_NAME.equals(getEntitySetName())) {
            throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        }
        super.checkNotAllowedMethod();
    }

    /**
     * Processing of MERGE method.
     * @param reader request body
     * @param accept Accept header
     * @param ifMatch If-Match header
     * @return JAX-RSResponse
     */
    public Response merge(Reader reader,
            final String accept,
            final String ifMatch) {
        //Method execution feasibility check
        checkNotAllowedMethod();

        //Access control
        getOdataResource().checkAccessContext(
                getOdataResource().getNecessaryWritePrivilege(getEntitySetName()));

        //Create an OEntityWrapper from the request.
        OEntity oe = this.createRequestEntity(reader, getOEntityKey());
        OEntityWrapper oew = new OEntityWrapper(null, oe, null);

        //Process of attaching meta information if necessary
        getOdataResource().beforeMerge(oew, getOEntityKey());

        //Set the ETag entered in the If-Match header to OEntityWrapper for collision detection for MVCC.
        String etag = ODataResource.parseEtagHeader(ifMatch);
        oew.setEtag(etag);

        //Ask MERGE processing to ODataProducer.
        //We will also check the existence of resources here.
        getOdataProducer().mergeEntity(getEntitySetName(), getOEntityKey(), oew);

        //If there are no exceptions, return a response.
        //Return ETag newly registered in oew
        etag = oew.getEtag();
        Response res = Response.noContent()
                .header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag))
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

        // post event to EventBus
        String key = AbstractODataResource.replaceDummyKeyToNull(getOEntityKey().toKeyString());
        String object = getOdataResource().getRootUrl() + getEntitySetName() + key;
        // set new entitykey's string to Info
        String newKey = AbstractODataResource.replaceDummyKeyToNull(oew.getEntityKey().toKeyString());
        String info = Integer.toString(res.getStatus()) + "," + newKey;
        getOdataResource().postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.MERGE);

        return res;
    }

    /**
     * Set default value to OProperty based on schema definition <br>
     * For MERGE, do not set default values ​​for items other than key, updated, and published.
     * @param ep EdmProperty
     * @param propName property name
     * @param op OProperty
     * @param metadata EdmDataServices schema definition
     * @return Oproperty
     */
    @Override
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op, EdmDataServices metadata) {

        if (metadata != null) {
            //Retrieving Schema Information
            EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(getEntitySetName());
            EdmEntityType edmEntityType = edmEntitySet.getType();
            //Retrieve the key list defined in the schema
            List<String> keysDefined = edmEntityType.getKeys();
            String epName = ep.getName();

            //Do not set default values ​​for items other than key, updated, and published
            if (!keysDefined.contains(epName) && !Common.P_PUBLISHED.getName().equals(epName)
                    && !Common.P_UPDATED.getName().equals(epName)) {
                return null;
            }

            //Get key value from OEntityKey.
            if (keysDefined.contains(epName)) {
                String value = getParameterFromEntityKey(propName);
                if (value != null) {
                    return OProperties.string(propName, value);
                }
            }
        }

        return super.setDefaultValue(ep, propName, op, metadata);
    }

    /**
     * Get parameter from oEntityKey.
     * @param propName prop name
     * @return value
     */
    private String getParameterFromEntityKey(String propName) {
        // It excludes items other than CellCtl Object.
        if (!Account.EDM_TYPE_NAME.equals(getEntitySetName())
                && !Box.EDM_TYPE_NAME.equals(getEntitySetName())
                && !Role.EDM_TYPE_NAME.equals(getEntitySetName())
                && !ExtCell.EDM_TYPE_NAME.equals(getEntitySetName())
                && !Relation.EDM_TYPE_NAME.equals(getEntitySetName())
                && !ExtRole.EDM_TYPE_NAME.equals(getEntitySetName())) {
            return null;
        }

        if (KeyType.COMPLEX.equals(getOEntityKey().getKeyType())) {
            Set<OProperty<?>> keys = getOEntityKey().asComplexProperties();
            for (OProperty<?> key : keys) {
                if (propName.equals(key.getName()) && key.getValue() != null) {
                    String value = key.getValue().toString();
                    if (DUMMY_KEY.equals(value)) {
                        return null;
                    }
                    return key.getValue().toString();
                }
            }
        } else {
            Object key = getOEntityKey().asSingleValue();
            if (key != null) {
                return key.toString();
            }
        }
        return null;
    }

    /**
     * Refer to the ComplexType schema and set mandatory checks and default values.
     * @param metadata schema information
     * @param edmComplexType Schema information of ComplexType
     * @param complexProperties List of ComplexTypeProperty
     * @return List of ComplexType properties with default values
     */
    @Override
    protected List<OProperty<?>> createNewComplexProperties(EdmDataServices metadata,
            EdmComplexType edmComplexType,
            Map<String, OProperty<?>> complexProperties) {
        //Refer to the ComplexType schema to set mandatory checks and default values
        List<OProperty<?>> newComplexProperties = new ArrayList<OProperty<?>>();
        for (EdmProperty ctp : edmComplexType.getProperties()) {
            //Acquire property information
            String compPropName = ctp.getName();
            OProperty<?> complexProperty = complexProperties.get(compPropName);
            if (ctp.getType().isSimple()) {
                //In case of simple type
                //For MERGE, do not set default value
                if (complexProperty == null) {
                    continue;
                } else if (complexProperty.getValue() == null) {
                    //Nullable check
                    complexProperty = setDefaultValue(ctp, compPropName, complexProperty);
                }
            } else {
                //In case of Complex type
                complexProperty = getComplexProperty(ctp, compPropName, complexProperty, metadata);
            }
            if (complexProperty != null) {
                //In the MERGE request, ignore the Property of ComplexType if it is not specified
                newComplexProperties.add(complexProperty);
            }
        }
        return newComplexProperties;
    }

}
