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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.Message;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.cell.MessageResource;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.UriUtils;

/**
 * OData operation class of sent message.
 */
public class ODataSentMessageResource extends ODataMessageResource {

    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(ODataSentMessageResource.class);

    /** Maximum allowed number of transmissions. */
    private static final int MAX_SENT_NUM = 1000;

    /** X-Personium-Version. */
    private String version;

    /**
     * Constructor.
     * @param messageResource Message resource
     * @param producer OData producer
     * @param entityTypeName Entity type name
     * @param version X-Personium-Version
     */
    public ODataSentMessageResource(MessageResource messageResource, PersoniumODataProducer producer,
            String entityTypeName, String version) {
        super(messageResource, producer, entityTypeName);
        this.version = version;
    }

    /**
     * Create sent message entity.
     * @param uriInfo URL info
     * @param reader Request body
     * @return Response
     */
    public Response createMessage(UriInfo uriInfo, Reader reader) {
        return createMessage(uriInfo, reader, PersoniumEventType.Operation.SEND);
    }

    /**
     * {@inheritDoc}
     * @param id Message id
     */
    @Override
    protected void editProperty(List<OProperty<?>> props, String id) {
        EdmCollectionType collectionType = new EdmCollectionType(SentMessage.P_RESULT.getCollectionKind(),
                SentMessage.P_RESULT.getType());

        // 受信APIの呼び出し、呼出し結果を送信メッセージのResultsにセット
        OCollection.Builder<OObject> builder = requestReceivedMessage(collectionType, id);

        // ComplexTypeの配列要素をプロパティに追加する
        props.add(OProperties.collection(SentMessage.P_RESULT.getName(), collectionType, builder.build()));

        for (int i = 0; i < props.size(); i++) {
            if (SentMessagePort.P_BOX_BOUND.getName().equals(props.get(i).getName())) {
                // メッセージ送信でBoxBoundはデータとして保持しないため、削除する
                props.remove(i);
            } else if (Common.P_BOX_NAME.getName().equals(props.get(i).getName())) {
                String schema = getMessageResource().getAccessContext().getSchema();
                Box box = getMessageResource().getAccessContext().getCell().getBoxForSchema(schema);
                String boxName = box != null ? box.getName() : null; // CHECKSTYLE IGNORE - To eliminate useless code
                // Replace with BoxName obtained from schema
                props.set(i, OProperties.string(Common.P_BOX_NAME.getName(), boxName));
            } else if (SentMessagePort.P_RESULT.getName().equals(props.get(i).getName())) {
                // メッセージ受信でResultはデータとして保持しているため置き換える
                props.set(i, OProperties.collection(SentMessage.P_RESULT.getName(),
                        collectionType, builder.build()));
            }
        }
    }

    /**
     * Individual validation processing.
     * @param props OProperty list
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        validateSentMessage();
        // InReplyTo: no check
        // Type: message
        // Title: no check
        // Body
        validateBody(getPropMap().get(Message.P_BODY.getName()), Message.MAX_MESSAGE_BODY_LENGTH);
        // Prority: no check
    }

    /**
     * Call message received API.
     * @param collectionType EdmCollectionType
     * @param idKey Sent message id
     * @return Message received API response
     */
    private OCollection.Builder<OObject> requestReceivedMessage(
            final EdmCollectionType collectionType,
            String idKey) {

        OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType.getItemType());
        // ComplexTypeの型情報を取得する
        EdmComplexType ct = SentMessage.COMPLEX_TYPE_RESULT.build();

        String fromCellUrl = getMessageResource().getAccessContext().getCell().getUrl();

        // 宛先リスト作成
        List<String> toList = createRequestUrl();

        // リクエストの件数分繰り返し
        for (String toCellUrl : toList) {
            List<OProperty<?>> result = null;
            toCellUrl = formatCellUrl(toCellUrl);

            // 受信API呼出しのトークン作成
            TransCellAccessToken token = new TransCellAccessToken(
                    fromCellUrl, fromCellUrl, toCellUrl, new ArrayList<Role>(), "");

            // ('ID')からIDを抜き出す
            Pattern formatPattern = Pattern.compile("\\('(.+)'\\)");
            Matcher formatMatcher = formatPattern.matcher(idKey);
            formatMatcher.matches();
            String id = formatMatcher.group(1);
            // 受信API呼出しのリクエストボディ作成
            JSONObject requestBody = createRequestJsonBody(fromCellUrl, toCellUrl, toList, id);

            // 受信API呼出し
            result = requestHttpReceivedMessage(token, toCellUrl, requestBody);

            // 呼出し結果を配列に追加する
            builder.add(OComplexObjects.create(ct, result));
        }

        return builder;
    }

    /**
     * Create destination list.
     * @return List of destination cell URLs
     */
    private List<String> createRequestUrl() {
        List<String> toList = new ArrayList<String>();
        String requestToStr = null;
        String requestToRelationStr = null;

        requestToStr = getPropMap().get(SentMessage.P_TO.getName());
        requestToRelationStr = getPropMap().get(SentMessage.P_TO_RELATION.getName());

        // リクエストボディのTo項目からCellURLを取得
        if (requestToStr != null) {
            String[] uriList = requestToStr.split(",");
            for (String uri : uriList) {
                toList.add(uri);
            }
        }

        // リクエストボディのToRelation項目からCellURLを取得する
        if (requestToRelationStr != null) {
            List<String> extCellUrlList = getExtCellListFromToRelation(requestToRelationStr);
            toList.addAll(extCellUrlList);
        }

        List<String> formatToList = new ArrayList<String>();
        // 宛先リストの整形
        for (String to : toList) {
            // 末尾に"/"を追加
            if (!to.endsWith("/")) {
                to += "/";
            }
            // ToとToRelationで重複している送信先を省く
            if (!formatToList.contains(to)) {
                formatToList.add(to);
            }
        }
        checkMaxDestinationsSize(formatToList.size());
        return formatToList;
    }

    /**
     * Format CellURL.
     * @param cellUrl cellUrl
     * @return Formulation result
     */
    private String formatCellUrl(String cellUrl) {
        String formatCellUrl = cellUrl;
        if (!cellUrl.endsWith("/")) {
            formatCellUrl += "/";
        }
        return formatCellUrl;
    }

    /**
     * Get URL list of ExtCell linked with Relation specified by ToRelation.
     * @param toRelation ToRelation
     * @return URL list of ExtCell
     */
    private List<String> getExtCellListFromToRelation(String toRelation) {
        List<String> extCellUrlList = new ArrayList<String>();
        String keyString = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(
                "'" + toRelation + "'");
        OEntityKey oEntityKey = OEntityKey.parse(keyString);
        EntitiesResponse response = null;
        try {
            QueryInfo query = new QueryInfo(InlineCount.ALLPAGES, MAX_SENT_NUM,
                    null, null, null, null, null, null, null);
            response = (EntitiesResponse) getOdataProducer().getNavProperty(
                    Relation.EDM_TYPE_NAME,
                    oEntityKey,
                    "_" + ExtCell.EDM_TYPE_NAME,
                    query);
        } catch (PersoniumCoreException e) {
            if (PersoniumCoreException.OData.NO_SUCH_ENTITY.getCode().equals(e.getCode())) {
                // ToRelationで指定されたRelationが存在しない場合は400エラーを返却
                throw PersoniumCoreException.SentMessage.TO_RELATION_NOT_FOUND_ERROR.params(toRelation);
            } else {
                throw e;
            }
        }
        // ToRelationから取得したExtCellのURLを追加する
        List<OEntity> extCellEntities = response.getEntities();
        checkMaxDestinationsSize(response.getInlineCount());
        for (OEntity extCell : extCellEntities) {
            extCellUrlList.add(extCell.getProperty(ExtCell.P_URL.getName()).getValue().toString());
        }
        if (extCellUrlList.isEmpty()) {
            // ToRelationで指定されたRelationに紐付くExtCellが存在しない場合は400エラーを返却
            throw PersoniumCoreException.SentMessage.RELATED_EXTCELL_NOT_FOUND_ERROR.params(toRelation);
        }
        return extCellUrlList;
    }

    /**
     * Check if the destination exceeds the maximum transmission permitted number.
     * @param destinationsSize Number of destinations
     */
    private void checkMaxDestinationsSize(int destinationsSize) {
        if (destinationsSize > MAX_SENT_NUM) {
            throw PersoniumCoreException.SentMessage.OVER_MAX_SENT_NUM;
        }
    }

    /**
     * Create request body of message reception API call.
     * @param fromCellUrl From cell URL
     * @param targetCellUrl To cell URL
     * @param toList To list
     * @param id Received message id
     * @return Request body
     */
    @SuppressWarnings("unchecked")
    private JSONObject createRequestJsonBody(String fromCellUrl, String targetCellUrl, List<String> toList, String id) {
        JSONObject requestBody = new JSONObject();
        Map<String, String> propMap = getPropMap();
        String type = propMap.get(SentMessage.P_TYPE.getName());
        boolean boxBound = Boolean.parseBoolean(propMap.get(SentMessagePort.P_BOX_BOUND.getName()));

        // Statusの設定
        String status = null;
        if (ReceivedMessage.TYPE_MESSAGE.equals(type)) {
            status = ReceivedMessage.STATUS_UNREAD;
        } else {
            status = ReceivedMessage.STATUS_NONE;
        }

        // MulticastToの設定
        String multicastTo = null;
        StringBuilder sbMulticastTo = null;
        for (String to : toList) {
            String formatTo = formatCellUrl(to);
            if (!formatTo.equals(targetCellUrl)) {
                if (sbMulticastTo == null) {
                    sbMulticastTo = new StringBuilder();
                } else {
                    sbMulticastTo.append(",");
                }
                sbMulticastTo.append(formatTo);
            }
        }
        if (sbMulticastTo != null) {
            multicastTo = sbMulticastTo.toString();
        }

        requestBody.put(ReceivedMessage.P_ID.getName(), id);
        if (boxBound) {
            requestBody.put(ReceivedMessagePort.P_SCHEMA.getName(),
                    getMessageResource().getAccessContext().getSchema());
        }
        requestBody.put(ReceivedMessage.P_IN_REPLY_TO.getName(), propMap.get(SentMessage.P_IN_REPLY_TO.getName()));
        requestBody.put(ReceivedMessage.P_FROM.getName(), fromCellUrl);
        requestBody.put(ReceivedMessage.P_MULTICAST_TO.getName(), multicastTo);
        requestBody.put(ReceivedMessage.P_TYPE.getName(), type);
        requestBody.put(ReceivedMessage.P_TITLE.getName(), propMap.get(SentMessage.P_TITLE.getName()));
        requestBody.put(ReceivedMessage.P_BODY.getName(), propMap.get(SentMessage.P_BODY.getName()));
        requestBody.put(ReceivedMessage.P_PRIORITY.getName(),
                Integer.valueOf(propMap.get(SentMessage.P_PRIORITY.getName())));
        requestBody.put(ReceivedMessage.P_STATUS.getName(), status);
        if (ReceivedMessage.TYPE_REQUEST.equals(type)) {
            JSONArray requestObjects = new JSONArray();
            for (Map<String, String> requestObjectMap : getRequestObjectPropMapList()) {
                JSONObject requestObject = new JSONObject();
                String requestType = requestObjectMap.get(RequestObject.P_REQUEST_TYPE.getName());
                requestObject.put(RequestObject.P_REQUEST_TYPE.getName(), requestType);
                if (RequestObject.REQUEST_TYPE_RELATION_ADD.equals(requestType)
                        || RequestObject.REQUEST_TYPE_RELATION_REMOVE.equals(requestType)
                        || RequestObject.REQUEST_TYPE_ROLE_ADD.equals(requestType)
                        || RequestObject.REQUEST_TYPE_ROLE_REMOVE.equals(requestType)) {
                    requestObject.put(RequestObject.P_NAME.getName(),
                            requestObjectMap.get(RequestObject.P_NAME.getName()));
                    requestObject.put(RequestObject.P_CLASS_URL.getName(),
                            requestObjectMap.get(RequestObject.P_CLASS_URL.getName()));
                    requestObject.put(RequestObject.P_TARGET_URL.getName(),
                            requestObjectMap.get(RequestObject.P_TARGET_URL.getName()));
                } else if (RequestObject.REQUEST_TYPE_RULE_ADD.equals(requestType)
                        || RequestObject.REQUEST_TYPE_RULE_REMOVE.equals(requestType)) {
                    if (requestObjectMap.get(RequestObject.P_NAME.getName()) != null) {
                        requestObject.put(RequestObject.P_NAME.getName(),
                                requestObjectMap.get(RequestObject.P_NAME.getName()));
                    } else {
                        requestObject.put(RequestObject.P_NAME.getName(), id);
                    }
                    requestObject.put(Rule.P_SUBJECT.getName(), requestObjectMap.get(Rule.P_SUBJECT.getName()));
                    requestObject.put(Rule.P_TYPE.getName(), requestObjectMap.get(Rule.P_TYPE.getName()));
                    requestObject.put(Rule.P_OBJECT.getName(), requestObjectMap.get(Rule.P_OBJECT.getName()));
                    requestObject.put(Rule.P_INFO.getName(), requestObjectMap.get(Rule.P_INFO.getName()));
                    requestObject.put(Rule.P_ACTION.getName(), requestObjectMap.get(Rule.P_ACTION.getName()));
                    requestObject.put(RequestObject.P_TARGET_URL.getName(),
                            requestObjectMap.get(RequestObject.P_TARGET_URL.getName()));
                }
                requestObjects.add(requestObject);
            }
            requestBody.put(ReceivedMessage.P_REQUEST_OBJECTS.getName(), requestObjects);
        }

        return requestBody;
    }

    /**
     * Call message received API.
     * @param token Token
     * @param requestCellUrl Request CellURL
     * @param jsonBody Request body
     * @return Request results
     */
    private List<OProperty<?>> requestHttpReceivedMessage(
            TransCellAccessToken token,
            String requestCellUrl,
            JSONObject jsonBody) {
        String requestUrl = requestCellUrl + "__message/port";

        // リクエストヘッダを取得し、以下内容を追加
        HttpClient client = HttpClientFactory.create(HttpClientFactory.TYPE_INSECURE);
        HttpPost req = new HttpPost(requestUrl);

        // リクエストボディ
        StringEntity body = null;
        try {
            body = new StringEntity(jsonBody.toJSONString(), CharEncoding.UTF_8);
        } catch (UnsupportedCharsetException e) {
            throw PersoniumCoreException.SentMessage.SM_BODY_PARSE_ERROR.reason(e);
        }
        req.setEntity(body);

        req.addHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION, version);
        req.addHeader(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER_CREDENTIALS_PREFIX + token.toTokenString());
        req.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        req.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        // リクエストを投げる
        HttpResponse objResponse = null;
        try {
            objResponse = client.execute(req);
        } catch (Exception ioe) {
            throw PersoniumCoreException.SentMessage.SM_CONNECTION_ERROR.reason(ioe);
        }

        // リクエスト結果の作成
        String statusCode = Integer.toString(objResponse.getStatusLine().getStatusCode());
        List<OProperty<?>> properties = new ArrayList<OProperty<?>>();
        properties.add(OProperties.string(SentMessage.P_RESULT_TO.getName(), requestCellUrl));
        properties.add(OProperties.string(SentMessage.P_RESULT_CODE.getName(), statusCode));
        if (Integer.toString(HttpStatus.SC_CREATED).equals(statusCode)) {
            properties.add(OProperties.string(SentMessage.P_RESULT_REASON.getName(), "Created."));
        } else {
            properties.add(OProperties.string(SentMessage.P_RESULT_REASON.getName(), getErrorMessage(objResponse)));
        }

        return properties;

    }

    /**
     * Get messages from HttpResponse.
     * @param objResponse HttpResponse
     * @return Message
     */
    private String getErrorMessage(HttpResponse objResponse) {
        JSONObject resBody = bodyAsJson(objResponse);
        String message = "";
        try {
            if (resBody != null) {
                message = (String) ((JSONObject) resBody.get("message")).get("value");
            }
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        return message;
    }

    /**
     * Get body of JSON type from HttpResponse.
     * @param objResponse HttpResponse
     * @return Body
     */
    private JSONObject bodyAsJson(HttpResponse objResponse) {
        JSONObject body = null;

        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        String bodyString = null;
        try {
            objResponse.getHeaders("Content-Encoding");
            is = objResponse.getEntity().getContent();
            isr = new InputStreamReader(is, "UTF-8");
            reader = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            int chr;
            while ((chr = is.read()) != -1) {
                sb.append((char) chr);
            }
            bodyString = sb.toString();
        } catch (IllegalStateException e) {
            log.info(e.getMessage());
        } catch (IOException e) {
            log.info(e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (isr != null) {
                    isr.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.info(e.getMessage());
            }
        }

        try {
            if (bodyString != null) {
                body = (JSONObject) new JSONParser().parse(bodyString);
            }
        } catch (ParseException e) {
            log.info(e.getMessage());
        }

        return body;
    }

    /**
     * Validate sent message.
     */
    private void validateSentMessage() {
        Map<String, String> propMap = getPropMap();
        String type = propMap.get(Message.P_TYPE.getName());

        // BoxBound
        Boolean boxBound = Boolean.valueOf(propMap.get(SentMessagePort.P_BOX_BOUND.getName()));
        validateSentBoxBoundSchema(getMessageResource(),
                Boolean.valueOf(propMap.get(SentMessagePort.P_BOX_BOUND.getName())));
        // To
        // ToRelation
        validateUriCsv(SentMessage.P_TO.getName(), propMap.get(SentMessage.P_TO.getName()));
        validateToAndToRelation(
                propMap.get(SentMessage.P_TO.getName()),
                propMap.get(SentMessage.P_TO_RELATION.getName()));
        validateToValue(
                propMap.get(SentMessage.P_TO.getName()),
                getMessageResource().getAccessContext().getBaseUri());

        // validate properties per Type
        //   type 'message' is nothing to do
        if (SentMessage.TYPE_REQUEST.equals(type)) {
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
                    //   Name pattern id
                    //   Action required
                    //   Action: relay or relay.event or exec -> TargetUrl required
                    //   BoxBound: true  -> EventObject: personium-localbox:/xxx
                    //                   -> Action: exec -> TargetUrl: personium-localbox:/xxx
                    //   BoxBound: false -> EventObject: personium-localcell:/xxx
                    //                   -> Action: exec -> TargetUrl: personium-localcell:/xxx
                    //   Action: relay       -> TargetUrl: personium-localunit: or http: or https:
                    //   Action: relay.event -> TargetUrl: cell url
                    String name = requestObjectMap.get(RequestObject.P_NAME.getName());
                    if (name != null && !ODataUtils.validateRegEx(name, Common.PATTERN_ID)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
                    }
                    String action = requestObjectMap.get(Rule.P_ACTION.getName());
                    if (action == null) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(Rule.P_ACTION.getName()));
                    }
                    if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_RELAY_EVENT.equals(action)
                            || Rule.ACTION_EXEC.equals(action))
                            && requestObjectMap.get(RequestObject.P_TARGET_URL.getName()) == null) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                    }
                    String object = requestObjectMap.get(Rule.P_OBJECT.getName());
                    String targetUrl = requestObjectMap.get(RequestObject.P_TARGET_URL.getName());
                    if (boxBound.booleanValue()) {
                        log.debug("validate: boxBound is true");
                        if (object != null && !ODataUtils.isValidLocalBoxUrl(object)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(Rule.P_OBJECT.getName()));
                        }
                        if (Rule.ACTION_EXEC.equals(action)
                                && !targetUrl.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                    concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                        }
                    } else {
                        log.debug("validate: boxBound is false");
                        if (object != null && !ODataUtils.isValidLocalCellUrl(object)) {
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
                    if (Rule.ACTION_RELAY_EVENT.equals(action)
                            && !ODataUtils.isValidCellUrl(targetUrl)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
                    }
                } else if (RequestObject.REQUEST_TYPE_RULE_REMOVE.equals(requestType)) {
                    // rule.remove
                    //   Name required
                    String name = requestObjectMap.get(RequestObject.P_NAME.getName());
                    if (name == null || !ODataUtils.validateRegEx(name, Common.PATTERN_ID)) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                                concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
                    }
                }
            }
        } else if (!SentMessage.TYPE_MESSAGE.equals(type)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Message.P_TYPE.getName());
        }
    }

    /**
     * Schema check at Box Bound.
     * @param messageResource ODataResource
     * @param boxboundFlag BoxBoundFlag
     */
    private void validateSentBoxBoundSchema(MessageResource messageResource, boolean boxboundFlag) {
        if (boxboundFlag) {
            String schema = messageResource.getAccessContext().getSchema();
            Box box = messageResource.getAccessContext().getCell().getBoxForSchema(schema);
            if (box == null) {
                throw PersoniumCoreException.SentMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            }
        }
    }

    /**
     * Toで指定されたリクエスト先がbaseUrlのホストと一致するかのチェック.
     * @param toValue toで指定された値
     * @param baseUrl baseUrl
     */
    private void validateToValue(String toValue, String baseUrl) {
        if (toValue == null) {
            return;
        }
        // リクエストURLのドメインチェック
        String checkBaseUrl = null;
        String[] uriList = toValue.split(",");
        for (String uriStr : uriList) {
            try {
                URI uri = new URI(uriStr);
                checkBaseUrl = uri.getScheme() + "://" + uri.getHost();
                int port = uri.getPort();
                if (port != -1) {
                    checkBaseUrl += ":" + Integer.toString(port);
                }
                checkBaseUrl += "/";
            } catch (URISyntaxException e) {
                log.info(e.getMessage());
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(SentMessage.P_TO.getName());
            }

            if (checkBaseUrl == null || !checkBaseUrl.equals(baseUrl)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(SentMessage.P_TO.getName());
            }
        }

    }

    /**
     * To-ToRelationのバリデート.
     * @param to 送信先セルURL
     * @param toRelation 送信対象の関係名
     */
    private void validateToAndToRelation(String to, String toRelation) {
        if (to == null && toRelation == null) {
            String detail = SentMessage.P_TO.getName() + "," + SentMessage.P_TO_RELATION.getName();
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
    }
}
