/**
 * personium.io
 * Copyright 2014 FUJITSU LIMITED
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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.producer.EntityQueryInfo;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.resources.OptionsQueryParser;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.annotations.MERGE;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.odata.DcODataProducer;
import io.personium.core.odata.DcOptionsQueryParser;
import io.personium.core.odata.OEntityWrapper;

/**
 * ODataのEntityリソース(id指定されたURL)を扱うJAX-RS リソース.
 */
public class ODataEntityResource extends AbstractODataResource {

    // public static final String PATH_CELL_ID = "cellid";

    private final String keyString;
    private final ODataResource odataResource;
    private final AccessContext accessContext;

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return accessContext;
    }

    /**
     * @return the odataResource
     */
    public ODataResource getOdataResource() {
        return odataResource;
    }

    private OEntityKey oEntityKey;

    /**
     * @return the odataResource
     */
    public OEntityKey getOEntityKey() {
        return this.oEntityKey;
    }

    /**
     * このリソースが担当する ODataリソースの OEntityIdオブジェクト.
     * @return OEntityIdオブジェクト
     */
    public OEntityId getOEntityId() {
        return OEntityIds.create(getEntitySetName(), this.oEntityKey);
    }

    /**
     * コンストラクタ.
     */
    public ODataEntityResource() {
        this.odataResource = null;
        this.accessContext = null;
        this.keyString = null;
        this.oEntityKey = null;
    }

    /**
     * コンストラクタ.
     * @param odataResource 親リソースであるODataResource
     * @param entitySetName EntitySet Name
     * @param key キー文字列
     */
    public ODataEntityResource(final ODataResource odataResource, final String entitySetName, final String key) {
        this.odataResource = odataResource;
        this.accessContext = this.odataResource.accessContext;
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);

        // 複合キー対応
        // nullが指定されているとパースに失敗するため、null値が設定されている場合はダミーキーに置き換える
        this.keyString = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(key);

        try {
            this.oEntityKey = OEntityKey.parse(this.keyString);
        } catch (IllegalArgumentException e) {
            throw DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.reason(e);
        }

        EdmDataServices metadata = getOdataProducer().getMetadata();
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(entitySetName);
        if (edmEntitySet == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
        }
        EdmEntityType edmEntityType = edmEntitySet.getType();
        validatePrimaryKey(oEntityKey, edmEntityType);
    }

    /**
     * GETメソッドの処理.
     * @param uriInfo UriInfo
     * @param accept Accept ヘッダ
     * @param ifNoneMatch If-None-Match ヘッダ
     * @param format $format パラメタ
     * @param expand $expand パラメタ
     * @param select $select パラメタ
     * @return JAX-RSResponse
     */
    @GET
    public Response get(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) String accept,
            @HeaderParam(HttpHeaders.IF_NONE_MATCH) String ifNoneMatch,
            @QueryParam("$format") String format,
            @QueryParam("$expand") String expand,
            @QueryParam("$select") String select) {
        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        UriInfo resUriInfo = PersoniumCoreUtils.createUriInfo(uriInfo, 1);

        // $formatとAcceptヘッダの値から出力形式を決定
        MediaType contentType = decideOutputFormat(accept, format);
        String outputFormat = FORMAT_JSON;
        if (MediaType.APPLICATION_ATOM_XML_TYPE.equals(contentType)) {
            outputFormat = FORMAT_ATOM;
        }

        // Entityの取得をProducerに依頼
        EntityResponse entityResp = getEntity(expand, select, resUriInfo);
        String respStr = renderEntityResponse(resUriInfo, entityResp, outputFormat, null);

        // 制御コードのエスケープ処理
        respStr = escapeResponsebody(respStr);

        ResponseBuilder rb = Response.ok().type(contentType);
        rb.header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);
        // ETagを正式実装するときに、返却する必要がある
        OEntity entity = entityResp.getEntity();
        String etag = null;
        // 基本的にこのIF文に入る。
        if (entity instanceof OEntityWrapper) {
            OEntityWrapper oew = (OEntityWrapper) entity;

            // エンティティごとのアクセス可否判断
            this.odataResource.checkAccessContextPerEntity(this.accessContext, oew);

            etag = oew.getEtag();
            // 基本的にこのIF文に入る。
            if (etag != null) {
                // If-None-Matchヘッダの指定があるとき
                if (ifNoneMatch != null && ifNoneMatch.equals(ODataResource.renderEtagHeader(etag))) {
                    return Response.notModified().build();
                }
                // ETagヘッダの付与
                rb.header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag));
            }
        }
        return rb.entity(respStr).build();
    }

    /**
     * Entityの取得をProducerに依頼.
     * @param expand expand
     * @param select select
     * @param resUriInfo UriInfo
     * @return EntityResponse
     */
    EntityResponse getEntity(String expand, String select, UriInfo resUriInfo) {
        EntityQueryInfo queryInfo = null;

        if (resUriInfo != null) {
            queryInfo = queryInfo(expand, select, resUriInfo);
        }
        EntityResponse entityResp = getOdataProducer().getEntity(getEntitySetName(), this.oEntityKey, queryInfo);
        return entityResp;
    }

    EntityQueryInfo queryInfo(String expand, String select, UriInfo resUriInfo) {
        List<EntitySimpleProperty> selects = null;
        List<EntitySimpleProperty> expands = null;

        // $select
        if ("".equals(select)) {
            throw DcCoreException.OData.SELECT_PARSE_ERROR;
        }
        if ("*".equals(select)) {
            select = null;
        }
        try {
            selects = DcOptionsQueryParser.parseSelect(select);
        } catch (Exception e) {
            throw DcCoreException.OData.SELECT_PARSE_ERROR.reason(e);
        }

        try {
            expands = DcOptionsQueryParser.parseExpand(expand);
        } catch (Exception e) {
            throw DcCoreException.OData.EXPAND_PARSE_ERROR.reason(e);
        }
        // $expandに指定されたプロパティ数の上限チェック
        if (expands != null && expands.size() > DcCoreConfig.getExpandPropertyMaxSizeForRetrieve()) {
            throw DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED;
        }

        EntityQueryInfo queryInfo = new EntityQueryInfo(null,
                OptionsQueryParser.parseCustomOptions(resUriInfo),
                expands,
                selects);
        return queryInfo;
    }

    /**
     * PUT メソッドの処理.
     * @param reader リクエストボディ
     * @param accept Accept ヘッダ
     * @param ifMatch If-Match ヘッダ
     * @return JAX-RSResponse
     */
    @PUT
    public Response put(Reader reader,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {

        // メソッド実行可否チェック
        checkNotAllowedMethod();

        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        String etag;

        // リクエストの更新をProducerに依頼
        OEntityWrapper oew = updateEntity(reader, ifMatch);

        // 特に例外があがらなければ、レスポンスを返す。
        // oewに新たに登録されたETagを返す
        etag = oew.getEtag();
        return Response.noContent()
                .header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag))
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
    }

    /**
     * リクエストの更新をProducerに依頼.
     * @param reader リクエストボディ
     * @param ifMatch ifMatch
     * @return OEntityWrapper
     */
    OEntityWrapper updateEntity(Reader reader, final String ifMatch) {
        // リクエストからOEntityWrapperを作成する.
        OEntity oe = this.createRequestEntity(reader, this.oEntityKey);
        OEntityWrapper oew = new OEntityWrapper(null, oe, null);

        // 必要ならばメタ情報をつける処理
        this.odataResource.beforeUpdate(oew, this.oEntityKey);

        // If-Matchヘッダで入力されたETagをMVCC用での衝突検知用にOEntityWrapperに設定する。
        String etag = ODataResource.parseEtagHeader(ifMatch);
        oew.setEtag(etag);

        // UPDATE処理をODataProducerに依頼。
        // こちらでリソースの存在確認もしてもらう。
        getOdataProducer().updateEntity(getEntitySetName(), this.oEntityKey, oew);
        return oew;
    }

    /**
     * MERGE メソッドの処理.
     * @param reader リクエストボディ
     * @param accept Accept ヘッダ
     * @param ifMatch If-Match ヘッダ
     * @return JAX-RSResponse
     */
    @MERGE
    public Response merge(Reader reader,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        ODataMergeResource oDataMergeResource = new ODataMergeResource(this.odataResource, this.getEntitySetName(),
                this.keyString);
        return oDataMergeResource.merge(reader, accept, ifMatch);
    }

    /**
     * DELETEメソッドの処理.
     * @param accept Accept ヘッダ
     * @param ifMatch If-Match ヘッダ
     * @return JAX-RS Response
     */
    @DELETE
    public Response delete(
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        deleteEntity(ifMatch);
        return Response.noContent().header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
    }

    /**
     * Entityの削除をProducerに依頼.
     * @param ifMatch
     */
    void deleteEntity(final String ifMatch) {
        // 削除前処理
        this.odataResource.beforeDelete(getEntitySetName(), this.oEntityKey);
        String etag = ODataResource.parseEtagHeader(ifMatch);

        // 削除処理
        DcODataProducer op = this.getOdataProducer();
        op.deleteEntity(getEntitySetName(), this.oEntityKey, etag);

        // 削除後処理
        this.odataResource.afterDelete(getEntitySetName(), this.oEntityKey);
    }

    /**
     * $links/{navProp} というパスの処理.
     * ODataLinksResourceに処理を委譲.
     * @param targetNavProp Navigation Property
     * @return ODataLinksResource オブジェクト
     */
    @Path("{first: \\$}links/{targetNavProp:.+?}")
    public ODataLinksResource links(@PathParam("targetNavProp") final String targetNavProp) {
        OEntityKey oeKey = OEntityKey.parse(this.keyString);
        OEntityId oeId = OEntityIds.create(getEntitySetName(), oeKey);
        return new ODataLinksResource(this.odataResource, oeId, targetNavProp, null);
    }

    /**
     * $links/{navProp}({targetKey})というパスの処理.
     * ODataLinksResourceに処理を委譲.
     * @param targetNavProp ターゲット NavigationPropert
     * @param targetId ターゲットのID
     * @return ODataLinksResourceオブジェクト
     */
    @Path("{first: \\$}links/{targetNavProp:.+?}({targetId})")
    public ODataLinksResource link(@PathParam("targetNavProp") final String targetNavProp,
            @PathParam("targetId") final String targetId) {
        OEntityKey targetEntityKey = null;
        try {
            if (targetId != null && !targetId.isEmpty()) {
                // 複合キー対応
                // nullが指定されているとパースに失敗するため、null値が設定されている場合はダミーキーに置き換える
                String targetKey = AbstractODataResource.replaceNullToDummyKeyWithParenthesis(targetId);
                targetEntityKey = OEntityKey.parse(targetKey);
            }
        } catch (IllegalArgumentException e) {
            throw DcCoreException.OData.ENTITY_KEY_LINKS_PARSE_ERROR.reason(e);
        }
        OEntityId oeId = OEntityIds.create(getEntitySetName(), this.oEntityKey);
        return new ODataLinksResource(this.odataResource, oeId, targetNavProp, targetEntityKey);
    }

    /**
     * {navProp:.+}というパスに対する処理を ODataPropertyResourceに飛ばす.
     * @param navProp Navigation Property
     * @return ODataPropertyResource Object
     */
    @Path("{navProp: _.+}")
    public ODataPropertyResource getNavProperty(@PathParam("navProp") final String navProp) {
        return new ODataPropertyResource(this, navProp);
    }

    /**
     * NavigationProperty経由はID指定は不可のため404とする.
     * @param navProp Navigation Property
     * @param targetId ターゲットのID
     * @return ODataPropertyResource Object
     */
    @Path("{navProp: _.+}({targetId})")
    public ODataPropertyResource getNavProperty(@PathParam("navProp") final String navProp,
            @PathParam("targetId") final String targetId) {
        throw DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.PUT,
                PersoniumCoreUtils.HttpMethod.MERGE,
                HttpMethod.DELETE
                ).build();
    }

    /**
     * メソッド実行可否チェック.
     */
    protected void checkNotAllowedMethod() {
        if (ReceivedMessage.EDM_TYPE_NAME.equals(getEntitySetName())
                || SentMessage.EDM_TYPE_NAME.equals(getEntitySetName())) {
            throw DcCoreException.Misc.METHOD_NOT_ALLOWED;
        }
    }
}
