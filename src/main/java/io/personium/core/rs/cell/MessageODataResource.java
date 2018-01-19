/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.auth.token.Role;
import io.personium.common.auth.token.TransCellAccessToken;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ExtCell;
import io.personium.core.model.ctl.Message;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.ReceivedMessagePort;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Rule;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.model.ctl.SentMessagePort;
import io.personium.core.model.impl.es.odata.MessageODataProducer;
import io.personium.core.odata.PersoniumODataProducer;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.rs.odata.ODataResource;
import io.personium.core.utils.HttpClientFactory;
import io.personium.core.utils.ODataUtils;
import io.personium.core.utils.ResourceUtils;
import io.personium.core.utils.UriUtils;
import io.personium.core.event.PersoniumEventType;

/**
 * __messageのOData操作クラス.
 */
public final class MessageODataResource extends AbstractODataResource {

    static Logger log = LoggerFactory.getLogger(MessageODataResource.class);

    private MessageResource odataResource;
    private Map<String, String> propMap = new HashMap<String, String>();
    private String version;

    private String requestKey;

    /** 最大送信許可数. */
    private static final int MAX_SENT_NUM = 1000;

    /**
     * constructor.
     * @param odataResource ODataリソース
     * @param producer ODataプロデューサ
     * @param entityTypeName エンティティタイプ名
     */
    public MessageODataResource(MessageResource odataResource, PersoniumODataProducer producer, String entityTypeName) {
        this.odataResource = odataResource;
        setOdataProducer(producer);
        setEntitySetName(entityTypeName);
    }

    /**
     * personium Version情報のセット.
     * @param version セットする version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Set the requestKey.
     * @param requestKey X-Personium-RequestKey header
     */
    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }

    /**
     * 受信／送信メッセージEntityを作成する.
     * @param uriInfo URL情報
     * @param reader リクエストボディ
     * @return response情報
     */
    protected Response createMessage(UriInfo uriInfo,
            Reader reader) {

        // response用URLに__ctlを追加する
        UriInfo resUriInfo = PersoniumCoreUtils.createUriInfo(uriInfo, 2, "__ctl");

        EntityResponse res = createEntity(reader, odataResource);

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

        String op;
        if (SentMessagePort.EDM_TYPE_NAME.equals(getEntitySetName())) {
            op = PersoniumEventType.Operation.SEND;
        } else if (ReceivedMessagePort.EDM_TYPE_NAME.equals(getEntitySetName())) {
            op = PersoniumEventType.Operation.RECEIVE;
        } else {
            return response;
        }
        // personium-localcell:/__ctl/SentMessage('key')
        String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, getEntitySetName(), key);
        String info = Integer.toString(response.getStatus());
        this.odataResource.postEvent(getEntitySetName(), object, info, requestKey, op);

        return response;
    }

    /**
     * 受信メッセージステータスを変更する.
     * @param reader リクエストボディ
     * @param key メッセージId
     * @return response情報
     */
    protected Response changeMessageStatus(Reader reader, String key) {

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
        this.odataResource.postEvent(getEntitySetName(), object, info, requestKey, op);

        return response;
    }

    /**
     * プロパティ操作.
     * @param props プロパティ一覧
     * @param id メッセージのID
     */
    @Override
    protected void editProperty(List<OProperty<?>> props, String id) {

        if (ReceivedMessage.EDM_TYPE_NAME.equals(this.getEntitySetName())) {
            for (int i = 0; i < props.size(); i++) {
                if (ReceivedMessagePort.P_SCHEMA.getName().equals(props.get(i).getName())) {
                    String schema = (String) props.get(i).getValue();
                    Box box = this.odataResource.getAccessContext().getCell().getBoxForSchema(schema);
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
        } else if (SentMessage.EDM_TYPE_NAME.equals(this.getEntitySetName())) {

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
                    String schema = this.odataResource.getAccessContext().getSchema();
                    Box box = this.odataResource.getAccessContext().getCell().getBoxForSchema(schema);
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

    }

    /**
     * メッセージ受信APIを呼出す.
     * @param collectionType EdmCollectionType
     * @param idKey 送信メッセージのIDキー
     * @return メッセージ受信の結果
     */
    private OCollection.Builder<OObject> requestReceivedMessage(
            final EdmCollectionType collectionType,
            String idKey) {

        OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType.getItemType());
        // ComplexTypeの型情報を取得する
        EdmComplexType ct = SentMessage.COMPLEXTYPE_BUILDER.build();

        String fromCellUrl = this.odataResource.getAccessContext().getCell().getUrl();

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
     * 宛先リスト作成.
     * @return 送信先のCellURLのリスト
     */
    private List<String> createRequestUrl() {
        List<String> toList = new ArrayList<String>();
        String requestToStr = null;
        String requestToRelationStr = null;

        requestToStr = this.propMap.get(SentMessage.P_TO.getName());
        requestToRelationStr = this.propMap.get(SentMessage.P_TO_RELATION.getName());

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
     * ToRelationで指定されたRelationにも付くExtCellのURL一覧を取得する.
     * @param toRelation ToRelationのプロパティ値
     * @return extCellUrlList ExtCellのURL一覧
     */
    protected List<String> getExtCellListFromToRelation(String toRelation) {
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
     * 送信先URLが最大送信許可数を超えているかチェック.
     * @param destinationsSize 送信先URLの件数
     */
    public void checkMaxDestinationsSize(int destinationsSize) {
        if (destinationsSize > MAX_SENT_NUM) {
            throw PersoniumCoreException.SentMessage.OVER_MAX_SENT_NUM;
        }
    }

    /**
     * メッセージ受信API呼出しのリクエストボディ作成.
     * @param fromCellUrl 送信元CellURL
     * @param targetCellUrl 送信先CellURL
     * @param toList 宛先リスト
     * @param id 受信メッセージのID
     * @return リクエストボディ
     */
    @SuppressWarnings("unchecked")
    private JSONObject createRequestJsonBody(String fromCellUrl, String targetCellUrl, List<String> toList, String id) {
        JSONObject requestBody = new JSONObject();
        String type = this.propMap.get(SentMessage.P_TYPE.getName());
        boolean boxBound = Boolean.parseBoolean(this.propMap.get(SentMessagePort.P_BOX_BOUND.getName()));

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
            requestBody.put(ReceivedMessagePort.P_SCHEMA.getName(), this.odataResource.getAccessContext().getSchema());
        }
        requestBody.put(ReceivedMessage.P_IN_REPLY_TO.getName(), this.propMap.get(SentMessage.P_IN_REPLY_TO.getName()));
        requestBody.put(ReceivedMessage.P_FROM.getName(), fromCellUrl);
        requestBody.put(ReceivedMessage.P_MULTICAST_TO.getName(), multicastTo);
        requestBody.put(ReceivedMessage.P_TYPE.getName(), type);
        requestBody.put(ReceivedMessage.P_TITLE.getName(), this.propMap.get(SentMessage.P_TITLE.getName()));
        requestBody.put(ReceivedMessage.P_BODY.getName(), this.propMap.get(SentMessage.P_BODY.getName()));
        requestBody.put(ReceivedMessage.P_PRIORITY.getName(),
                Integer.valueOf(this.propMap.get(SentMessage.P_PRIORITY.getName())));
        requestBody.put(ReceivedMessage.P_STATUS.getName(), status);
        requestBody.put(ReceivedMessage.P_REQUEST_RELATION.getName(),
                this.propMap.get(SentMessage.P_REQUEST_RELATION.getName()));
        requestBody.put(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(),
                this.propMap.get(SentMessage.P_REQUEST_RELATION_TARGET.getName()));

        if (ReceivedMessage.TYPE_REQ_RULE_REGISTER.equals(type)
                || ReceivedMessage.TYPE_REQ_RULE_UNREGISTER.equals(type)) {
            JSONObject ruleObj = new JSONObject();
            if (this.propMap.get(
                    concat(SentMessage.P_REQUEST_RULE.getName(), SentMessage.P_REQUESTRULE_NAME.getName())) != null) {
                ruleObj.put(ReceivedMessage.P_REQUESTRULE_NAME.getName(), this.propMap.get(
                        concat(SentMessage.P_REQUEST_RULE.getName(), SentMessage.P_REQUESTRULE_NAME.getName())));
            } else {
                ruleObj.put(ReceivedMessage.P_REQUESTRULE_NAME.getName(), id);
            }
            ruleObj.put(Rule.P_SUBJECT.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SUBJECT.getName())));
            ruleObj.put(Rule.P_TYPE.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_TYPE.getName())));
            ruleObj.put(Rule.P_OBJECT.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName())));
            ruleObj.put(Rule.P_INFO.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_INFO.getName())));
            ruleObj.put(Rule.P_ACTION.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_ACTION.getName())));
            ruleObj.put(Rule.P_SERVICE.getName(),
                    this.propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName())));

            requestBody.put(ReceivedMessage.P_REQUEST_RULE.getName(), ruleObj);
        }

        return requestBody;
    }

    /**
     * CellURLの書式を整形する.
     * @param cellUrl cellのUrl
     * @return 整形後の値
     */
    private String formatCellUrl(String cellUrl) {
        String formatCellUrl = cellUrl;
        if (!cellUrl.endsWith("/")) {
            formatCellUrl += "/";
        }
        return formatCellUrl;
    }

    /**
     * メッセージ受信API呼出し.
     * @param token トークン
     * @param requestCellUrl リクエスト先CellURL
     * @param jsonBody リクエストボディ
     * @return リクエスト結果
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

        req.addHeader(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_VERSION, this.version);
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
     * HttpResponseからメッセージ取得.
     * @param objResponse HttpResponse
     * @return メッセージ
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
     * HttpResponseからJSON型のボディ取得.
     * @param objResponse HttpResponse
     * @return ボディ
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
     * 個別バリデート処理.
     * @param props OProperty一覧
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        if (ReceivedMessage.EDM_TYPE_NAME.equals(this.getEntitySetName())) {
            validateReceivedMessage();
        } else if (SentMessage.EDM_TYPE_NAME.equals(this.getEntitySetName())) {
            validateSentMessage();
        }
        // InReplyTo: no check
        // Type: message
        // Title: no check
        // Body
        validateBody(propMap.get(Message.P_BODY.getName()), Message.MAX_MESSAGE_BODY_LENGTH);
        // Prority: no check
    }

    private void validateReceivedMessage() {
        String type = propMap.get(Message.P_TYPE.getName());

        // From
        // Schema
        String schema = propMap.get(ReceivedMessagePort.P_SCHEMA.getName());
        validateReceivedBoxBoundSchema(this.odataResource, schema);
        // MulticastTo
        validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(),
                propMap.get(ReceivedMessage.P_MULTICAST_TO.getName()));

        if (ReceivedMessage.TYPE_MESSAGE.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_UNREAD.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
        } else if (ReceivedMessage.TYPE_REQ_RELATION_BUILD.equals(type)
                || ReceivedMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_NONE.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
            // RequestRelation
            // RequestRelationTarget
            validateReqRelationOnRelation(
                    propMap.get(ReceivedMessage.P_REQUEST_RELATION.getName()),
                    propMap.get(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName()));
        } else if (ReceivedMessage.TYPE_REQ_ROLE_GRANT.equals(type)
                || ReceivedMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_NONE.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
            // RequestRelation
            // RequestRelationTarget
            validateReqRelationOnRole(
                    propMap.get(ReceivedMessage.P_REQUEST_RELATION.getName()),
                    propMap.get(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName()));
        } else if (ReceivedMessage.TYPE_REQ_RULE_REGISTER.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_NONE.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
            // RequestRule
            //   Name, Action required
            //   Action: relay or exec -> Service required
            //   Schema: exists -> Object: personium-localbox:/xxx
            //                  -> Action: exec -> Service: personium-localbox:/xxx
            //   Schema: null   -> Object: personium-localcell:/xxx
            //                  -> Action: exec -> Service: personium-localcell:/xxx
            //   Action: relay -> Service: personium-localunit: or http: or https:
            String action = propMap.get(concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_ACTION.getName()));
            if (action == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_ACTION.getName()));
            }
            if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_EXEC.equals(action))
                    && propMap.get(
                            concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName())) == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            }
            String object = propMap.get(concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
            String service = propMap.get(concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            if (schema != null) {
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
                }
                if (Rule.ACTION_EXEC.equals(action)
                        && !service.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
                }
            } else {
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
                }
                if (Rule.ACTION_EXEC.equals(action)
                        && !service.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
                }
            }
            if (Rule.ACTION_RELAY.equals(action)
                    && !service.startsWith(UriUtils.SCHEME_LOCALUNIT)
                    && !service.startsWith(UriUtils.SCHEME_HTTP)
                    && !service.startsWith(UriUtils.SCHEME_HTTPS)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(ReceivedMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            }
        } else if (ReceivedMessage.TYPE_REQ_RULE_UNREGISTER.equals(type)) {
            // Status
            if (!ReceivedMessage.STATUS_NONE.equals(propMap.get(ReceivedMessage.P_STATUS.getName()))) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        ReceivedMessage.P_STATUS.getName());
            }
            // RequestRule
            // Name required
        } else {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Message.P_TYPE.getName());
        }
    }

    private void validateSentMessage() {
        String type = propMap.get(Message.P_TYPE.getName());

        // BoxBound
        Boolean boxBound = Boolean.valueOf(propMap.get(SentMessagePort.P_BOX_BOUND.getName()));
        validateSentBoxBoundSchema(this.odataResource,
                Boolean.valueOf(propMap.get(SentMessagePort.P_BOX_BOUND.getName())));
        // To
        // ToRelation
        validateUriCsv(SentMessage.P_TO.getName(), propMap.get(SentMessage.P_TO.getName()));
        validateToAndToRelation(
                propMap.get(SentMessage.P_TO.getName()),
                propMap.get(SentMessage.P_TO_RELATION.getName()));
        validateToValue(
                propMap.get(SentMessage.P_TO.getName()),
                this.odataResource.getAccessContext().getBaseUri());

        // validate properties per Type
        //   type 'message' is nothing to do
        if (SentMessage.TYPE_REQ_RELATION_BUILD.equals(type) || SentMessage.TYPE_REQ_RELATION_BREAK.equals(type)) {
            // RequestRelation
            // RequestRelationTarget
            validateReqRelationOnRelation(
                    propMap.get(SentMessage.P_REQUEST_RELATION.getName()),
                    propMap.get(SentMessage.P_REQUEST_RELATION_TARGET.getName()));
        } else if (SentMessage.TYPE_REQ_ROLE_GRANT.equals(type) || SentMessage.TYPE_REQ_ROLE_REVOKE.equals(type)) {
            // RequestRelation
            // RequestRelationTarget
            validateReqRelationOnRole(
                    propMap.get(SentMessage.P_REQUEST_RELATION.getName()),
                    propMap.get(SentMessage.P_REQUEST_RELATION_TARGET.getName()));
        } else if (SentMessage.TYPE_REQ_RULE_REGISTER.equals(type)) {
            // RequestRule
            //   Action required
            //   Action: relay or exec -> Service required
            //   BoxBound: true  -> Object: personium-localbox:/xxx
            //                   -> Action: exec -> Service: personium-localbox:/xxx
            //   BoxBound: false -> Object: personium-localcell:/xxx
            //                   -> Action: exec -> Service: personium-localcell:/xxx
            //   Action: relay -> Service: personium-localunit: or http: or https:
            String action = propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_ACTION.getName()));
            if (action == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_ACTION.getName()));
            }
            if ((Rule.ACTION_RELAY.equals(action) || Rule.ACTION_EXEC.equals(action))
                    && propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName())) == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            }
            String object = propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
            String service = propMap.get(concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            if (boxBound.booleanValue()) {
                log.debug("validate: boxBound is true");
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
                }
                if (Rule.ACTION_EXEC.equals(action)
                        && !service.startsWith(UriUtils.SCHEME_LOCALBOX)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
                }
            } else {
                log.debug("validate: boxBound is false");
                if (object != null && !object.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_OBJECT.getName()));
                }
                if (Rule.ACTION_EXEC.equals(action)
                        && !service.startsWith(UriUtils.SCHEME_LOCALCELL)) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                            concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
                }
            }
            if (Rule.ACTION_RELAY.equals(action)
                    && !service.startsWith(UriUtils.SCHEME_LOCALUNIT)
                    && !service.startsWith(UriUtils.SCHEME_HTTP)
                    && !service.startsWith(UriUtils.SCHEME_HTTPS)) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(SentMessage.P_REQUEST_RULE.getName(), Rule.P_SERVICE.getName()));
            }
        } else if (SentMessage.TYPE_REQ_RULE_UNREGISTER.equals(type)) {
            // RequestRule
            //   Name required
            if (propMap.get(
                    concat(SentMessage.P_REQUEST_RULE.getName(), SentMessage.P_REQUESTRULE_NAME.getName())) == null) {
                throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                        concat(SentMessage.P_REQUEST_RULE.getName(), SentMessage.P_REQUESTRULE_NAME.getName()));
            }
        } else if (!SentMessage.TYPE_MESSAGE.equals(type)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Message.P_TYPE.getName());
        }
    }

    private String concat(String parent, String child) {
        return parent + "." + child;
    }

    /**
     * プロパティの一覧取得.
     * @param props プロパティ一覧
     * @param
     */
    @Override
    public void collectProperties(List<OProperty<?>> props) {
        for (OProperty<?> property : props) {
            if (property.getValue() == null) {
                propMap.put(property.getName(), null);
            } else if (property.getType().isSimple()) {
                propMap.put(property.getName(), property.getValue().toString());
            } else {
                List<OProperty<?>> list = (List<OProperty<?>>) property.getValue();
                for (OProperty<?> p : list) {
                    if (p.getValue() != null) {
                        propMap.put(concat(property.getName(), p.getName()), p.getValue().toString());
                    }
                }
            }
        }
    }

    /**
     * Schema check at Box Bound.
     * @param messageResource ODataResource
     * @param schema Schema
     */
    public void validateReceivedBoxBoundSchema(MessageResource messageResource, String schema) {
        if (StringUtils.isNotEmpty(schema)) {
            Box box = messageResource.getAccessContext().getCell().getBoxForSchema(schema);
            if (box == null) {
                throw PersoniumCoreException.ReceivedMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            }
        }
    }

    /**
     * Schema check at Box Bound.
     * @param messageResource ODataResource
     * @param boxboundFlag BoxBoundFlag
     */
    public void validateSentBoxBoundSchema(MessageResource messageResource, boolean boxboundFlag) {
        if (boxboundFlag) {
            String schema = messageResource.getAccessContext().getSchema();
            Box box = messageResource.getAccessContext().getCell().getBoxForSchema(schema);
            if (box == null) {
                throw PersoniumCoreException.SentMessage.BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS.params(schema);
            }
        }
    }

    /**
     * Validate list of Cell URL in csv format.
     * @param propKey property key
     * @param propValue property value
     */
    public static void validateUriCsv(String propKey, String propValue) {
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
     * Toで指定されたリクエスト先がbaseUrlのホストと一致するかのチェック.
     * @param toValue toで指定された値
     * @param baseUrl baseUrl
     */
    public static void validateToValue(String toValue, String baseUrl) {
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
     * Bodyのバリデート.
     * @param value チェック対象値
     * @param maxLength 最大長
     */
    public static void validateBody(String value, int maxLength) {
        if (value.getBytes().length > maxLength) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.P_BODY.getName());
        }
    }

    /**
     * RequestRelationOnRelationのバリデート.
     * @param requestRelation 関係登録依頼リレーションクラスURL
     * @param requestRelationTarget 関係登録依頼CellURL
     */
    public static void validateReqRelationOnRelation(String requestRelation, String requestRelationTarget) {
        // Conditional required check
        if (requestRelation == null || requestRelationTarget == null) {
            String detail = Message.P_REQUEST_RELATION.getName()
                    + "," + Message.P_REQUEST_RELATION_TARGET.getName();
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
        // Correlation format check
        if (!ODataUtils.validateClassUrl(requestRelation, Common.PATTERN_RELATION_CLASS_URL)
                && !ODataUtils.validateRegEx(requestRelation, Common.PATTERN_RELATION_NAME)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    Message.P_REQUEST_RELATION.getName());
        }
    }

    /**
     * RequestRelationOnRoleのバリデート.
     * @param requestRelation 関係登録依頼リレーションクラスURL
     * @param requestRelationTarget 関係登録依頼CellURL
     */
    public static void validateReqRelationOnRole(String requestRelation, String requestRelationTarget) {
        // Conditional required check
        if (requestRelation == null || requestRelationTarget == null) {
            String detail = Message.P_REQUEST_RELATION.getName()
                    + "," + Message.P_REQUEST_RELATION_TARGET.getName();
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
        // Correlation format check
        if (!ODataUtils.validateClassUrl(requestRelation, Common.PATTERN_ROLE_CLASS_URL)
                && !ODataUtils.validateRegEx(requestRelation, Common.PATTERN_NAME)) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    ReceivedMessage.P_REQUEST_RELATION.getName());
        }
    }

    /**
     * To-ToRelationのバリデート.
     * @param to 送信先セルURL
     * @param toRelation 送信対象の関係名
     */
    public static void validateToAndToRelation(String to, String toRelation) {
        if (to == null && toRelation == null) {
            String detail = SentMessage.P_TO.getName() + "," + SentMessage.P_TO_RELATION.getName();
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(detail);
        }
    }
}
