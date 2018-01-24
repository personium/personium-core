/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.OCollection;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.odata4j.producer.EntityResponse;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Message;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.cell.MessageResource;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.UriUtils;

/**
 * OData operation class of message.
 */
public class ODataMessageResource extends AbstractODataResource {

    /** MessageResource. */
    private MessageResource messageResource;
    /** X-Personium-RequestKey. */
    private String requestKey;
    /** Property map. */
    private Map<String, String> propMap = new HashMap<String, String>();
    /** Property map.(RequestObjects). */
    private List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();

    /**
     * Constructor.
     * @param messageResource Message resource
     * @param requestKey X-Personium-RequestKey header
     * @param producer OData producer
     * @param entityTypeName Entity type name
     */
    protected ODataMessageResource(MessageResource messageResource, String requestKey,
            PersoniumODataProducer producer, String entityTypeName) {
        this.messageResource = messageResource;
        this.requestKey = requestKey;
        setOdataProducer(producer);
        setEntitySetName(entityTypeName);
    }

    /**
     * Create receive / send message entity.
     * @param uriInfo URL information
     * @param reader Request body
     * @param operation Event operation
     * @return Jax-RS response
     */
    protected Response createMessage(UriInfo uriInfo, Reader reader, String operation) {

        // response用URLに__ctlを追加する
        UriInfo resUriInfo = PersoniumCoreUtils.createUriInfo(uriInfo, 2, "__ctl");

        EntityResponse res = createEntity(reader, messageResource);

        // レスポンスボディを生成する
        OEntity ent = res.getEntity();
        MediaType outputFormat = MediaType.APPLICATION_JSON_TYPE;
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(MediaType.APPLICATION_JSON_TYPE);
        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());
        String responseStr = renderEntityResponse(resUriInfo, res, "json", contentTypes);

        // 制御コードのエスケープ処理
        responseStr = escapeResponsebody(responseStr);

        ResponseBuilder rb = getPostResponseBuilder(ent, outputFormat, responseStr, resUriInfo, key);
        Response response = rb.build();

        // personium-localcell:/__ctl/SentMessage('key')
        String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, getEntitySetName(), key);
        String info = Integer.toString(response.getStatus());
        this.messageResource.postEvent(getEntitySetName(), object, info, requestKey, operation);

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void collectProperties(List<OProperty<?>> props) {
        for (OProperty<?> property : props) {
            if (property.getValue() == null) {
                propMap.put(property.getName(), null);
            } else if (property.getType().isSimple()) {
                propMap.put(property.getName(), property.getValue().toString());
            } else {
                // Should it be general-purpose processing according to property.getType()?
                // Since did not feel the necessity in particular, made it exclusive to RequestObjects.
                OCollection<OComplexObject> oCollection = (OCollection<OComplexObject>) property.getValue();
                for (OComplexObject object : oCollection) {
                    requestObjectPropMapList.add(getComplexTypeProperty(object));
                }
            }
        }
    }

    /**
     * Retrieve the property from ComplexTypeProperty and return it to map.
     * @param property complex type object
     * @return property map
     */
    private Map<String, String> getComplexTypeProperty(OComplexObject property) {
        Map<String, String> complexMap = new HashMap<String, String>();
        List<OProperty<?>> propertyList = (List<OProperty<?>>) property.getProperties();
        for (OProperty<?> p : propertyList) {
            if (p.getValue() != null) {
                complexMap.put(p.getName(), p.getValue().toString());
            }
        }
        return complexMap;
    }

    /**
     * Validate list of Cell URL in csv format.
     * @param propKey property key
     * @param propValue property value
     */
    protected void validateUriCsv(String propKey, String propValue) {
        if (propValue == null) {
            return;
        }
        if (propValue.contains(",")) {
            String[] uriList = propValue.split(",");
            for (String uri : uriList) {
                if (uri.length() != 0) {
                    if (!ODataUtils.isValidCellUrl(uri)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propKey);
                    }
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propKey);
                }
            }
        } else {
            if (!ODataUtils.isValidCellUrl(propValue)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propValue);
            }
        }
    }

    /**
     * Validate bocy.
     * @param value body
     * @param maxLength max length
     */
    protected void validateBody(String value, int maxLength) {
        if (value.getBytes().length > maxLength) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.P_BODY.getName());
        }
    }

    /**
     * Validate RequestRelation.
     * @param name Relation name
     * @param classUrl Relation class URL
     * @param targetUrl Target URL
     */
    protected void validateRequestRelation(String name, String classUrl, String targetUrl) {
        // Conditional required check
        if (name == null && classUrl == null
                || name != null && classUrl != null) {
            String detail = concatRequestObjectPropertyName(RequestObject.P_NAME.getName())
                    + "," + concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName());
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
        if (targetUrl == null) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
        }

        // Correlation format check
        if (name != null && !ODataUtils.validateRegEx(name, Common.PATTERN_RELATION_NAME)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
        }
        if (classUrl != null && !ODataUtils.validateClassUrl(classUrl, Common.PATTERN_RELATION_CLASS_URL)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
        }
    }

    /**
     * Validate RequestRole.
     * @param name Role name
     * @param classUrl Role class URL
     * @param targetUrl Target URL
     */
    protected void validateRequestRole(String name, String classUrl, String targetUrl) {
        // Conditional required check
        if (name == null && classUrl == null
                || name != null && classUrl != null) {
            String detail = concatRequestObjectPropertyName(RequestObject.P_NAME.getName())
                    + "," + concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName());
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
        if (targetUrl == null) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
        }

        // Correlation format check
        if (name != null && !ODataUtils.validateRegEx(name, Common.PATTERN_NAME)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
        }
        if (classUrl != null && !ODataUtils.validateClassUrl(classUrl, Common.PATTERN_ROLE_CLASS_URL)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
        }
    }

    /**
     * Concat specified property name and RequestObject property name and returns it.
     * @param child Property name
     * @return Concatenated string
     */
    protected String concatRequestObjectPropertyName(String child) {
        return Message.P_REQUEST_OBJECTS.getName() + "." + child;
    }

    /**
     * Get messageResource.
     * @return messageResource
     */
    protected MessageResource getMessageResource() {
        return messageResource;
    }

    /**
     * Get requestKey.
     * @return requestKey
     */
    protected String getRequestKey() {
        return requestKey;
    }

    /**
     * Get propMap.
     * @return propMap
     */
    protected Map<String, String> getPropMap() {
        return propMap;
    }

    /**
     * Get requestObjectPropMapList.
     * @return requestObjectPropMapList
     */
    protected List<Map<String, String>> getRequestObjectPropMapList() {
        return requestObjectPropMapList;
    }
}
