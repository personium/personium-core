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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.BaseResponse;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;

import io.personium.common.es.util.PersoniumUUID;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.odata.PersoniumFormatWriterFactory;

/**
 * Navigationプロパティを扱うリソース.
 * Jax-RS Resource.
 */
public class ODataPropertyResource extends AbstractODataResource {

    private final ODataResource sourceOData;
    private final OEntityId sourceEntityId;
    private final String targetNavProp;
    private final EdmEntitySet targetEntitySet;
    private final AccessContext accessContext;
    private final ODataResource odataResource;

    /**
     * コンストラクタ.
     * @param entityResource 親リソース
     * @param targetNavProp Navigation Property
     */
    public ODataPropertyResource(
            final ODataEntityResource entityResource,
            final String targetNavProp) {
        this.targetNavProp = targetNavProp;
        this.sourceOData = entityResource.getOdataResource();
        this.sourceEntityId = entityResource.getOEntityId();
        setOdataProducer(entityResource.getOdataProducer());
        this.accessContext = entityResource.getAccessContext();
        this.odataResource = entityResource.getOdataResource();
        // Navigationプロパティのスキーマ上の存在確認
        EdmEntitySet eSet = getOdataProducer().getMetadata().findEdmEntitySet(this.sourceEntityId.getEntitySetName());
        EdmNavigationProperty enp = eSet.getType().findNavigationProperty(this.targetNavProp);
        if (enp == null) {
            throw PersoniumCoreException.OData.NOT_SUCH_NAVPROP;
        }
        // TargetのEntityKey, EdmEntitySetを準備
        EdmEntityType tgtType = enp.getToRole().getType();
        this.targetEntitySet = getOdataProducer().getMetadata().findEdmEntitySet(tgtType.getName());
    }

    /**
     * POST メソッドへの処理.
     * @param uriInfo UriInfo
     * @param accept アクセプトヘッダ
     * @param requestKey X-Personium-RequestKey Header
     * @param format $formatクエリ
     * @param reader リクエストボディ
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public final Response postEntity(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @DefaultValue(FORMAT_JSON) @QueryParam("$format") final String format,
            final Reader reader) {
        // アクセス制御
        this.checkWriteAccessContext();

        OEntityWrapper oew = createEntityFromInputStream(reader);
        EntityResponse res = getOdataProducer().createNp(this.sourceEntityId, this.targetNavProp,
                oew, getEntitySetName());
        if (res == null || res.getEntity() == null) {
            return Response.status(HttpStatus.SC_PRECONDITION_FAILED).entity("conflict").build();
        }

        OEntity ent = res.getEntity();
        String etag = oew.getEtag();

        // バージョンが更新された場合、etagを更新する
        if (etag != null && ent instanceof OEntityWrapper) {
            ((OEntityWrapper) ent).setEtag(etag);
        }

        // 現状は、ContentTypeはJSON固定
        MediaType outputFormat = this.decideOutputFormat(accept, format);
        // Entity Responseをレンダー
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);
        UriInfo resUriInfo = PersoniumCoreUtils.createUriInfo(uriInfo, 2);
        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());

        String responseStr = renderEntityResponse(resUriInfo, res, format, contentTypes);

        // 制御コードのエスケープ処理
        responseStr = escapeResponsebody(responseStr);
        ResponseBuilder rb = getPostResponseBuilder(ent, outputFormat, responseStr, resUriInfo, key);
        Response ret = rb.build();

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntityId.getEntityKey().toKeyString());
        String object = String.format("%s%s%s/%s%s",
                this.odataResource.getRootUrl(),
                this.sourceEntityId.getEntitySetName(),
                srcKey,
                this.targetNavProp,
                key);
        String info = String.format("%s,%s",
                Integer.toString(ret.getStatus()), uriInfo.getRequestUri());
        String op = PersoniumEventType.Operation.NAVPROP
                + PersoniumEventType.SEPALATOR + this.targetNavProp.substring(1)
                + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.CREATE;
        this.odataResource.postEvent(this.sourceEntityId.getEntitySetName(), object, info, requestKey, op);

        return ret;
    }

    /**
     * NavigationProperty経由で登録するEntityデータを入力ストリームから生成する.
     * @param reader 入力ストリーム
     * @return 入力ストリームから生成したOEntityWrapperオブジェクト
     */
    OEntityWrapper createEntityFromInputStream(final Reader reader) {
        EdmEntityType edmEntityType = getOdataProducer().getMetadata()
                .findEdmEntitySet(this.sourceEntityId.getEntitySetName()).getType();
        return createEntityFromInputStream(
                reader,
                edmEntityType,
                this.sourceEntityId.getEntityKey(),
                this.targetEntitySet.getName());
    }

    /**
     * NavigationProperty経由で登録するEntityデータを入力ストリームから生成する.
     * @param reader 入力ストリーム
     * @return 入力ストリームから生成したOEntityWrapperオブジェクト
     */
    OEntityWrapper createEntityFromInputStream(
            final Reader reader,
            EdmEntityType sourceEdmEntityType,
            OEntityKey sourceEntityKey,
            String targetEntitySetName) {
        // 主キーのバリデート
        validatePrimaryKey(sourceEntityKey, sourceEdmEntityType);

        // 登録すべきOEntityを作成
        setEntitySetName(targetEntitySetName);
        OEntity newEnt = createRequestEntity(reader, null);

        // ラッパにくるむ. POSTでIf-Match等 ETagを受け取ることはないのでetagはnull。
        String uuid = PersoniumUUID.randomUUID();
        OEntityWrapper oew = new OEntityWrapper(uuid, newEnt, null);
        return oew;
    }

    /**
     * NavigationProperty経由でEntityを登録する.
     * @param oew 登録用OEntityWrapperオブジェクト
     * @return 登録した内容から生成したEntityレスポンス
     */
    EntityResponse createEntity(OEntityWrapper oew) {
        // 必要ならばメタ情報をつける処理
        this.sourceOData.beforeCreate(oew);

        // Entityの作成を Producerに依頼.この中であわせて、存在確認もしてもらう。
        EntityResponse res = getOdataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * NavPropに対するGETメソッドによる検索処理.
     * @param uriInfo UriInfo
     * @param accept Acceptヘッダ
     * @param requestKey X-Personium-RequestKey Header
     * @param callback ?? なんだこれは？JSONP?
     * @param skipToken ?? なんだこれは？
     * @param q 全文検索パラメタ
     * @return JAX-RS Response
     */
    @GET
    @Produces({ODataConstants.APPLICATION_ATOM_XML_CHARSET_UTF8, ODataConstants.TEXT_JAVASCRIPT_CHARSET_UTF8,
            ODataConstants.APPLICATION_JAVASCRIPT_CHARSET_UTF8 })
    public final Response getNavProperty(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_REQUESTKEY) String requestKey,
            @QueryParam("$callback") final String callback,
            @QueryParam("$skiptoken") final String skipToken,
            @QueryParam("q") final String q) {
        // アクセス制御
        this.checkReadAccessContext();

        // queryのパース
        UriInfo uriInfo2 = PersoniumCoreUtils.createUriInfo(uriInfo, 2);
        QueryInfo queryInfo = ODataEntitiesResource.queryInfo(uriInfo);

        // NavigationProperty経由の一覧取得を実行する
        BaseResponse response = getOdataProducer().getNavProperty(
                this.sourceEntityId.getEntitySetName(),
                this.sourceEntityId.getEntityKey(),
                this.targetNavProp,
                queryInfo);

        StringWriter sw = new StringWriter();
        // TODO 制限事項でAcceptは無視してJSONで返却するため固定でJSONを指定する.
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        acceptableMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
        // TODO 制限事項でQueryは無視するため固定でnullを指定する.
        FormatWriter<EntitiesResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                acceptableMediaTypes, null, callback);

        fw.write(uriInfo2, sw, (EntitiesResponse) response);

        String entity = sw.toString();
        // 制御コードのエスケープ処理
        entity = escapeResponsebody(entity);

        ODataVersion version = ODataVersion.V2;

        Response ret = Response.ok(entity, fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString).build();

        // post event to EventBus
        String srcKey = AbstractODataResource.replaceDummyKeyToNull(this.sourceEntityId.getEntityKey().toKeyString());
        String object = String.format("%s%s%s/%s",
                this.odataResource.getRootUrl(),
                this.sourceEntityId.getEntitySetName(),
                srcKey,
                this.targetNavProp);
        String info = String.format("%s,%s",
                Integer.toString(ret.getStatus()), uriInfo.getRequestUri());
        String op = PersoniumEventType.Operation.NAVPROP
                + PersoniumEventType.SEPALATOR + this.targetNavProp.substring(1)
                + PersoniumEventType.SEPALATOR + PersoniumEventType.Operation.LIST;
        this.odataResource.postEvent(this.sourceEntityId.getEntitySetName(), object, info, requestKey, op);

        return ret;
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
    public Response options() {
        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryOptionsPrivilege());
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET,
                HttpMethod.POST
                ).build();
    }

    private void checkWriteAccessContext() {
        // アクセス制御
        // TODO BOXレベルの場合に同じ処理が2回走る。無駄なのでcheckAccessContextにPrivilegeを配列で渡す等の工夫が必要
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(this.sourceEntityId.getEntitySetName()));
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(targetNavProp.substring(1)));
    }

    private void checkReadAccessContext() {
        // アクセス制御
        // TODO BOXレベルの場合に同じ処理が2回走る。無駄なのでcheckAccessContextにPrivilegeを配列で渡す等の工夫が必要
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(this.sourceEntityId.getEntitySetName()));
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(targetNavProp.substring(1)));
    }
}
