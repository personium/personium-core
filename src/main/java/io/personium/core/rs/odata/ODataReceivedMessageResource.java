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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;

import io.personium.core.PersoniumCoreException;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Message;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.impl.es.odata.MessageODataProducer;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.cell.MessageResource;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;

/**
 * OData operation class of received message.
 */
public class ODataReceivedMessageResource extends ODataMessageResource {

    /**
     * Constructor.
     * @param messageResource Message resource
     * @param producer OData producer
     * @param entityTypeName Entity type name
     */
    public ODataReceivedMessageResource(MessageResource messageResource,
            PersoniumODataProducer producer, String entityTypeName) {
        super(messageResource, producer, entityTypeName);
    }

    /**
     * Create received message entity.
     * @param uriInfo URL info
     * @param reader Request body
     * @return Response
     */
    public Response createMessage(UriInfo uriInfo, Reader reader) {
        return createMessage(uriInfo, reader, PersoniumEventType.Operation.RECEIVE);
    }

    /**
     * Change received message status.
     * @param reader Request body
     * @param key message id
     * @return Response
     */
    public Response changeMessageStatus(Reader reader, String key) {

        JSONObject body;
        body = ResourceUtils.parseBodyAsJSON(reader);

        String status = (String) body.get(ReceivedMessage.MESSAGE_COMMAND);
        // ステータスのチェック
        if (!ReceivedMessage.STATUS_UNREAD.equals(status)
                && !ReceivedMessage.STATUS_READ.equals(status)
                && !ReceivedMessage.STATUS_APPROVED.equals(status)
                && !ReceivedMessage.STATUS_REJECTED.equals(status)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.MESSAGE_COMMAND);
        }

        // EdmEntitySetの取得
        EdmEntitySet edmEntitySet = getOdataProducer().getMetadata()
                .getEdmEntitySet(ReceivedMessagePort.EDM_TYPE_NAME);

        // OEntityKeyの作成
        OEntityKey oEntityKey;
        try {
            oEntityKey = OEntityKey.parse("('" + key + "')");
        } catch (IllegalArgumentException e) {
            throw PersoniumCoreException.OData.ENTITY_KEY_PARSE_ERROR.reason(e);
        }

        // ステータス更新、及び関係登録/削除をProducerに依頼
        String etag = ((MessageODataProducer) getOdataProducer()).changeStatusAndUpdateRelation(
                edmEntitySet, oEntityKey, status);

        Response response = Response.noContent()
                .header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag))
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

        // sned event to EventBus
        String op = null;
        if (ReceivedMessage.STATUS_UNREAD.equals(status)) {
            op = PersoniumEventType.Operation.UNREAD;
        } else if (ReceivedMessage.STATUS_READ.equals(status)) {
            op = PersoniumEventType.Operation.READ;
        } else if (ReceivedMessage.STATUS_APPROVED.equals(status)) {
            op = PersoniumEventType.Operation.APPROVE;
        } else if (ReceivedMessage.STATUS_REJECTED.equals(status)) {
            op = PersoniumEventType.Operation.REJECT;
        }
        String object = String.format("%s:/__ctl/%s%s",
                UriUtils.SCHEME_LOCALCELL, getEntitySetName(), oEntityKey.toKeyString());
        String info = Integer.toString(response.getStatus());
        getMessageResource().postEvent(getEntitySetName(), object, info, op);

        return response;
    }

    /**
     * {@inheritDoc}
     * @param id Message id
     */
    @Override
    protected void editProperty(List<OProperty<?>> props, String id) {
        for (int i = 0; i < props.size(); i++) {
            if (ReceivedMessagePort.P_SCHEMA.getName().equals(props.get(i).getName())) {
                String schema = (String) props.get(i).getValue();
                Box box = getMessageResource().getAccessContext().getCell().getBoxForSchema(schema);
                String boxName = box != null ? box.getName() : null; // CHECKSTYLE IGNORE - To eliminate useless code
                // メッセージ受信でSchemaはデータとして保持しないため、削除する
                props.remove(i);
                for (int j = 0; j < props.size(); j++) {
                    if (Common.P_BOX_NAME.getName().equals(props.get(j).getName())) {
                        // Replace with BoxName obtained from schema
                        props.set(j, OProperties.string(Common.P_BOX_NAME.getName(), boxName));
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Individual validation processing.
     * @param props OProperty list
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        validateReceivedMessage();
        // InReplyTo: no check
        // Type: message
        // Title: no check
        // Body
        validateBody(getPropMap().get(Message.P_BODY.getName()), Message.MAX_MESSAGE_BODY_LENGTH);
        // Prority: no check
    }

    /**
     * Validate received message.
     */
    private void validateReceivedMessage() {
        Map<String, String> propMap = getPropMap();
        String type = propMap.get(Message.P_TYPE.getName());

        // From
        // Schema
        String schema = propMap.get(ReceivedMessagePort.P_SCHEMA.getName());
        validateReceivedBoxBoundSchema(getMessageResource(), schema);
        // MulticastTo
        validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(),
                propMap.get(ReceivedMessage.P_MULTICAST_TO.getName()));

        if (ReceivedMessage.TYPE_MESSAGE.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_UNREAD.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
        } else if (ReceivedMessage.TYPE_REQUEST.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_NONE.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
            // --------------------
            // RequestObjects
            // --------------------
            if (getRequestObjectPropMapList().isEmpty()) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        Message.P_REQUEST_OBJECTS.getName());
            }
            for (Map<String, String> requestObjectMap : getRequestObjectPropMapList()) {
                // RequestType
                String requestType = requestObjectMap.get(RequestObject.P_REQUEST_TYPE.getName());
                if (RequestObject.REQUEST_TYPE_RELATION_ADD.equals(requestType)
                        || RequestObject.REQUEST_TYPE_RELATION_REMOVE.equals(requestType)) {
                    // Name
                    // ClassUrl
                    // TargetUrl
                    validateRequestRelation(requestObjectMap.get(RequestObject.P_NAME.getName()),
                            requestObjectMap.get(RequestObject.P_CLASS_URL.getName()),
                            requestObjectMap.get(RequestObject.P_TARGET_URL.getName()));
                } else if (RequestObject.REQUEST_TYPE_ROLE_ADD.equals(requestType)
                        || RequestObject.REQUEST_TYPE_ROLE_REMOVE.equals(requestType)) {
                    // Name
                    // ClassUrl
                    // TargetUrl
                    validateRequestRole(requestObjectMap.get(RequestObject.P_NAME.getName()),
                            requestObjectMap.get(RequestObject.P_CLASS_URL.getName()),
                            requestObjectMap.get(RequestObject.P_TARGET_URL.getName()));
                } else if (RequestObject.REQUEST_TYPE_RULE_ADD.equals(requestType)) {
                    // rule.add
                    //   Name, Action required
                    //   Action: relay or exec -> TargetUrl required
                    //   Schema: exists -> EventObject: personium-localbox:/xxx
                    //                  -> Action: exec -> TargetUrl: personium-localbox:/xxx
                    //   Schema: null   -> EventObject: personium-localcell:/xxx
                    //                  -> Action: exec -> TargetUrl: personium-localcell:/xxx
                    //   Action: relay -> TargetUrl: personium-localunit: or http: or https:
                    String name = requestObjectMap.get(RequestObject.P_NAME.getName());
                    if (name == null || !ODataUtils.validateRegEx(name, Common.PATTERN_ID)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
                    }
                    String action = requestObjectMap.get(Rule.P_ACTION.getName());
                    if (action == null) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(Rule.P_ACTION.getName()));
                    }
                    if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_EXEC.equals(action))
                            && requestObjectMap.get(RequestObject.P_TARGET_URL.getName()) == null) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                    }
                    String object = requestObjectMap.get(Rule.P_OBJECT.getName());
                    String targetUrl = requestObjectMap.get(RequestObject.P_TARGET_URL.getName());
                    if (schema != null) {
                        if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(Rule.P_OBJECT.getName()));
                        }
                        if (Rule.ACTION_EXEC.equals(action)
                                && !targetUrl.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                        }
                    } else {
                        if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(Rule.P_OBJECT.getName()));
                        }
                        if (Rule.ACTION_EXEC.equals(action)
                                && !targetUrl.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                        }
                    }
                    if (Rule.ACTION_RELAY.equals(action)
                            && !targetUrl.startsWith(UriUtils.SCHEME_LOCALUNIT)
                            && !targetUrl.startsWith(UriUtils.SCHEME_HTTP)
                            && !targetUrl.startsWith(UriUtils.SCHEME_HTTPS)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                    }
                } else if (RequestObject.REQUEST_TYPE_RULE_REMOVE.equals(requestType)) {
                    // rule.remove
                    // Name required
                    String name = requestObjectMap.get(RequestObject.P_NAME.getName());
                    if (name == null || !ODataUtils.validateRegEx(name, Common.PATTERN_ID)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
                    }
                } else {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concatRequestObjectPropertyName(RequestObject.P_REQUEST_TYPE.getName()));
                }
            }
        }
    }

    /**
     * Schema check at Box Bound.
     * @param messageResource ODataResource
     * @param schema Schema
     */
    private void validateReceivedBoxBoundSchema(MessageResource messageResource, String schema) {
        if (StringUtils.isNotEmpty(schema)) {
            Box box = messageResource.getAccessContext().getCell().getBoxForSchema(schema);
            if (box == null) {
                throw PersoniumCoreException.ReceivedMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            }
        }
    }
}
