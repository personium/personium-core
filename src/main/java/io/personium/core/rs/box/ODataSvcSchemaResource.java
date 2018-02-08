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
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.format.xml.AtomServiceDocumentFormatWriter;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.odata4j.producer.CountResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;

import io.personium.common.utils.PersoniumCoreUtils;
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
 * ODataSvcSchemaResourceを担当するJAX-RSリソース.
 */
public final class ODataSvcSchemaResource extends ODataResource {
    private static final MediaType APPLICATION_ATOMSVC_XML_MEDIATYPE =
            MediaType.valueOf(ODataConstants.APPLICATION_ATOMSVC_XML);
    ODataSvcCollectionResource odataSvcCollectionResource;
    DavRsCmp davRsCmp;

    /**
     * constructor.
     * @param davRsCmp このスキーマが担当するユーザデータのResource
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
        this.davRsCmp.checkAccessContext(ac, privilege);
    }

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    @Override
    public AcceptableAuthScheme getAcceptableAuthScheme() {
        return this.davRsCmp.getAcceptableAuthScheme();
    }

    @Override
    public boolean hasPrivilege(AccessContext ac, Privilege privilege) {
        return this.davRsCmp.hasPrivilege(ac, privilege);
    }

    @Override
    public void checkSchemaAuth(AccessContext ac) {
    }

    /**
     * サービスメタデータリクエストに対応する.
     * @param uriInfo UriInfo
     * @param format String
     * @param httpHeaders HttpHeaders
     * @return JAX-RS 応答オブジェクト
     */
    @Override
    @GET
//    @Path("")
    public Response getRoot(@Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @Context HttpHeaders httpHeaders) {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        // $format と Acceptヘッダの内容から、
        // SchemaのAtom ServiceDocumentを返すべきか
        // データのEDMXを返すべきかをを判定する。
        if ("atomsvc".equals(format) || isAtomSvcRequest(httpHeaders)) {
            // SchemaのAtom ServiceDocumentを返す
            EdmDataServices edmDataServices = CtlSchema.getEdmDataServicesForODataSvcSchema().build();

            StringWriter w = new StringWriter();
            AtomServiceDocumentFormatWriter fw = new AtomServiceDocumentFormatWriter();
            fw.write(PersoniumCoreUtils.createUriInfo(uriInfo, 0), w, edmDataServices);

            return Response.ok(w.toString(), fw.getContentType())
                    .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                    .build();
        }

        // データのEDMXを返す
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
     * サービスメタデータリクエストに対応する.
     * @return JAX-RS 応答オブジェクト
     */
    @GET
    @Path("{first: \\$}metadata")
    public Response getMetadata() {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        // スキーマのEDMXを返す
        // Authヘッダチェック
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
        // アクセス制御
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
     * 部分更新前処理.
     * @param oEntityWrapper OEntityWrapperオブジェクト
     * @param oEntityKey 削除対象のentityKey
     */
    @Override
    public void beforeMerge(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        // 未対応のEntityTypeかチェックする
        String entityTypeName = oEntityWrapper.getEntitySetName();
        // PropertyとComplexTypeとComplexTypePropertyの更新は未対応のため501を返却する
        if (entityTypeName.equals(Property.EDM_TYPE_NAME)
                || entityTypeName.equals(ComplexType.EDM_TYPE_NAME)
                || entityTypeName.equals(ComplexTypeProperty.EDM_TYPE_NAME)) {
            throw PersoniumCoreException.Misc.METHOD_NOT_IMPLEMENTED;
        }
    }

    /**
     * リンク登録前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    @Override
    public void beforeLinkCreate(OEntityId sourceEntity, String targetNavProp) {
        // 未対応のEntityTypeかチェックする
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * リンク取得前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    @Override
    public void beforeLinkGet(OEntityId sourceEntity, String targetNavProp) {
    }

    /**
     * リンク削除前処理.
     * @param sourceEntity リンク対象のエンティティ
     * @param targetNavProp リンク対象のナビゲーションプロパティ
     */
    @Override
    public void beforeLinkDelete(OEntityId sourceEntity, String targetNavProp) {
        // 未対応のEntityTypeかチェックする
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * p:Format以外のチェック処理.
     * @param entitySetName entityset name
     * @param props プロパティ一覧
     */
    @Override
    public void validate(String entitySetName, List<OProperty<?>> props) {
        String type = null;
        for (OProperty<?> property : props) {
            if (property.getValue() == null) {
                continue;
            }
            // プロパティ名と値を取得
            String propValue = property.getValue().toString();
            String propName = property.getName();

            if (propName.equals(Property.P_TYPE.getName())) {
                // Typeのバリデート
                // Edm.Boolean / Edm.String / Edm.Single / Edm.Int32 / Edm.Double / Edm.DateTime
                type = propValue;
                if (!propValue.equals(EdmSimpleType.STRING.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.SINGLE.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.INT32.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.DOUBLE.getFullyQualifiedTypeName())
                        && !propValue.equals(EdmSimpleType.DATETIME.getFullyQualifiedTypeName())) {
                    // 登録済みのComplexTypeのチェック
                    BoolCommonExpression filter = PersoniumOptionsQueryParser.parseFilter(
                            "Name eq '" + propValue + "'");
                    QueryInfo query = new QueryInfo(null, null, null, filter, null, null, null, null, null);
                    CountResponse reponse = this.getODataProducer().getEntitiesCount(ComplexType.EDM_TYPE_NAME, query);
                    if (reponse.getCount() == 0) {
                        throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                    }
                }
            } else if (propName.equals(Property.P_COLLECTION_KIND.getName())) {
                // CollectionKindのバリデート
                // None / List
                if (!propValue.equals(Property.COLLECTION_KIND_NONE)
                        && !propValue.equals(CollectionKind.List.toString())) {
                    throw PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                }
            } else if (propName.equals(Property.P_DEFAULT_VALUE.getName())) {
                // DefaultValueのバリデート
                // Typeの値によってチェック内容を切り替える
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
        // EntityTypeとAssociationEndの$links指定は不可（EntityType:AssociationEndは1:Nの関係だから）
        // EntityTypeとPropertyの$links指定は不可（EntityType:Propertyは1:Nの関係だから）
        // ComplexTypeとComplexTypePropertyの$links指定は不可（ComplexType:ComplexTypePropertyは1:Nの関係だから）
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
        // スキーマレベルAPIはバッチリクエストに対応していないため、ここでは何もしない
    }

    /**
     * Not Implemented. <br />
     * 現状、$batchのアクセス制御でのみ必要なメソッドのため未実装. <br />
     * アクセスコンテキストが$batchしてよい権限を持っているかを返す.
     * @param ac アクセスコンテキスト
     * @return true: アクセスコンテキストが$batchしてよい権限を持っている
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
        String type = PersoniumEventType.Category.ODATA + PersoniumEventType.SEPALATOR
                + entitySetName + PersoniumEventType.SEPALATOR + op;
        PersoniumEvent ev = new PersoniumEvent(PersoniumEvent.INTERNAL_EVENT, type, object, info, this.davRsCmp);
        EventBus eventBus = this.getAccessContext().getCell().getEventBus();
        eventBus.post(ev);
    }
}
