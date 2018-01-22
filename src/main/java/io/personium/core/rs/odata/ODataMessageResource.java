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
 * __messageのOData操作クラス.
 */
public class ODataMessageResource extends AbstractODataResource {

    protected MessageResource odataResource;
    protected String requestKey;
    protected Map<String, String> propMap = new HashMap<String, String>();
    protected List<Map<String, String>> requestObjectPropMapList = new ArrayList<Map<String, String>>();

    /**
     * constructor.
     * @param odataResource ODataリソース
     * @param requestKey X-Personium-RequestKey header
     * @param producer ODataプロデューサ
     * @param entityTypeName エンティティタイプ名
     */
    protected ODataMessageResource(MessageResource odataResource, String requestKey,
            PersoniumODataProducer producer, String entityTypeName) {
        this.odataResource = odataResource;
        this.requestKey = requestKey;
        setOdataProducer(producer);
        setEntitySetName(entityTypeName);
    }

    /**
     * 受信／送信メッセージEntityを作成する.
     * @param uriInfo URL情報
     * @param reader リクエストボディ
     * @param operation Event operation
     * @return response情報
     */
    protected Response createMessage(UriInfo uriInfo, Reader reader, String operation) {

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

        // personium-localcell:/__ctl/SentMessage('key')
        String object = String.format("%s:/__ctl/%s%s", UriUtils.SCHEME_LOCALCELL, getEntitySetName(), key);
        String info = Integer.toString(response.getStatus());
        this.odataResource.postEvent(getEntitySetName(), object, info, requestKey, operation);

        return response;
    }

    /**
     * プロパティの一覧取得.
     * @param props プロパティ一覧
     * @param
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
                List<OProperty<?>> propertyList = (List<OProperty<?>>) property.getValue();
                for (OProperty<?> p : propertyList) {
                    requestObjectPropMapList.add(getComplexTypeProperty(p));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getComplexTypeProperty(OProperty<?> property) {
        Map<String, String> complexMap = new HashMap<String, String>();
        List<OProperty<?>> propertyList = (List<OProperty<?>>) property.getValue();
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
     * Bodyのバリデート.
     * @param value チェック対象値
     * @param maxLength 最大長
     */
    protected void validateBody(String value, int maxLength) {
        if (value.getBytes().length > maxLength) {
            throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(ReceivedMessage.P_BODY.getName());
        }
    }

    /**
     * RequestRelationOnRelationのバリデート.
     * @param name
     * @param classUrl 関係登録依頼リレーションクラスURL
     * @param targetUrl 関係登録依頼CellURL
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
     * RequestRelationOnRoleのバリデート.
     * @param name
     * @param classUrl 関係登録依頼リレーションクラスURL
     * @param targetUrl 関係登録依頼CellURL
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
}
