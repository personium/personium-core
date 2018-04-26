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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OProperty;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.annotations.WriteAPI;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.PersoniumEventType;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.odata.PersoniumFormatWriterFactory;

/**
 * ODataのEntitiesリソース( id 指定がなくentitySetが指定されたURL）を扱うJAX-RSリソース.
 */
public final class ODataEntitiesResource extends AbstractODataResource {

    private static final int Q_MAX_LENGTH = Common.MAX_Q_VALUE_LENGTH;
    ODataResource odataResource;
    AccessContext accessContext;

    /**
     * コンストラクタ.
     * @param odataResource 親Resource
     * @param entitySetName エンティティセット名
     */
    public ODataEntitiesResource(final ODataResource odataResource, final String entitySetName) {
        this.odataResource = odataResource;
        this.accessContext = this.odataResource.getAccessContext();
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);
    }

    /**
     * @param uriInfo UriInfo
     * @param accept Acceptヘッダ
     * @param format $format パラメタ
     * @param callback コールバック
     * @param skipToken スキップトークン
     * @param q 全文検索パラメタ
     * @return JAX-RS Response
     */
    @GET
    public Response listEntities(
            @Context UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @QueryParam("$format") String format,
            @QueryParam("$callback") final String callback,
            @QueryParam("$skiptoken") final String skipToken,
            @QueryParam("q") final String q) {

        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryReadPrivilege(getEntitySetName()));

        // リクエストの取得をProducerに依頼
        EntitiesResponse resp = getEntities(uriInfo, q);
        StringWriter sw = new StringWriter();

        // $formatとAcceptヘッダの値から出力形式を決定
        List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
        MediaType contentType = decideOutputFormat(accept, format);
        acceptableMediaTypes.add(contentType);

        FormatWriter<EntitiesResponse> fw = PersoniumFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                acceptableMediaTypes, null, callback);
        UriInfo uriInfo2 = PersoniumCoreUtils.createUriInfo(uriInfo, 1);

        fw.write(uriInfo2, sw, resp);
        String entity = null;
        entity = sw.toString();

        // 制御コードのエスケープ処理
        entity = escapeResponsebody(entity);

        // TODO remove this hack, check whether we are Version 2.0 compatible anyway
        ODataVersion version = null;
        version = ODataVersion.V2;

        Response response = Response.ok(entity, fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, version.asString).build();

        // post event to EventBus
        String object = String.format("%s%s",
                this.odataResource.getRootUrl(),
                getEntitySetName());
        String info = String.format("%s,%s",
                Integer.toString(response.getStatus()),
                uriInfo.getRequestUri());
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.LIST);

        return response;
    }

    /**
     * リクエストの取得をProducerに依頼.
     * @param queryInfo QueryInfo
     * @return レスポンス
     */
    EntitiesResponse getEntities(QueryInfo queryInfo) {
        EntitiesResponse resp = getOdataProducer().getEntities(getEntitySetName(), queryInfo);
        return resp;
    }

    /**
     * リクエストの取得をProducerに依頼.
     * @param uriInfo UriInfo
     * @param fullTextSearchKeyword String 全文検索を行うキーワード
     * @return レスポンス
     */
    EntitiesResponse getEntities(UriInfo uriInfo, String fullTextSearchKeyword) {
        QueryInfo queryInfo = null;
        if (uriInfo != null) {
            queryInfo = queryInfo(uriInfo, fullTextSearchKeyword);
        }
        EntitiesResponse resp = getOdataProducer().getEntities(getEntitySetName(), queryInfo);
        return resp;
    }

    /**
     * @param uriInfo UriInfo
     * @param accept Acceptヘッダ
     * @param format $format パラメタ
     * @param reader リクエストボディ
     * @return JAX-RS Response
     */
    @WriteAPI
    @POST
    public Response post(
            @Context final UriInfo uriInfo,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @DefaultValue(FORMAT_JSON) @QueryParam("$format") final String format,
            final Reader reader) {

        // メソッド実行可否チェック
        checkNotAllowedMethod(uriInfo);

        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        UriInfo resUriInfo = PersoniumCoreUtils.createUriInfo(uriInfo, 1);

        // Entityの作成を Producerに依頼
        EntityResponse res = this.createEntity(reader, this.odataResource);

        // 作成結果の評価
        OEntity ent = res.getEntity();

        // 現状は、ContentTypeはJSON固定
        MediaType outputFormat = this.decideOutputFormat(accept, format);
        // Entity Responseをレンダー
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);

        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());
        String responseStr = renderEntityResponse(resUriInfo, res, format, contentTypes);

        // 制御コードのエスケープ処理
        responseStr = escapeResponsebody(responseStr);

        ResponseBuilder rb = getPostResponseBuilder(ent, outputFormat, responseStr, resUriInfo, key);
        Response response = rb.build();

        // post event to EventBus
        String object = String.format("%s%s%s",
                this.odataResource.getRootUrl(),
                getEntitySetName(),
                key);
        String info = String.format("%s,%s",
                Integer.toString(response.getStatus()),
                uriInfo.getRequestUri());
        this.odataResource.postEvent(getEntitySetName(), object, info, PersoniumEventType.Operation.CREATE);

        return response;
    }

    static QueryInfo queryInfo(UriInfo uriInfo) {
        return queryInfo(uriInfo, null);
    }

    static QueryInfo queryInfo(UriInfo uriInfo, String fullTextSearchKeyword) {
        MultivaluedMap<String, String> mm = uriInfo.getQueryParameters(true);

        Integer top = QueryParser.parseTopQuery(mm.getFirst("$top"));
        Integer skip = QueryParser.parseSkipQuery(mm.getFirst("$skip"));
        BoolCommonExpression filter = QueryParser.parseFilterQuery(mm.getFirst("$filter"));
        List<EntitySimpleProperty> select = QueryParser.parseSelectQuery(mm.getFirst("$select"));
        List<EntitySimpleProperty> expand = QueryParser.parseExpandQuery(mm.getFirst("$expand"));
        InlineCount inlineCount = QueryParser.parseInlinecountQuery(mm.getFirst("$inlinecount"));
        String skipToken = QueryParser.parseSkipTokenQuery(mm.getFirst("$skiptoken"));
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(mm.getFirst("$orderby"));

        // 全文検索クエリqのバリデート
        if (fullTextSearchKeyword != null && (fullTextSearchKeyword.getBytes().length < 1
                || fullTextSearchKeyword.getBytes().length > Q_MAX_LENGTH)) {
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("q", fullTextSearchKeyword);
        }

        // $expand指定時は$topの最大値が変わるためチェックする
        if (expand != null && top != null && top > PersoniumUnitConfig.getTopQueryMaxSizeWithExpand()) {
            // Integerでそのまま値を返却すると、カンマが付くため、文字列でエラーメッセージを返却する
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", top.toString());
        }

        Map<String, String> options = new HashMap<String, String>();
        options.put("q", fullTextSearchKeyword);
        return new QueryInfo(
                inlineCount,
                top,
                skip,
                filter,
                orderBy,
                skipToken,
                options,
                expand,
                select);
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
                HttpMethod.POST
                ).build();
    }

    /**
     * p:Format以外のチェック処理.
     * @param props プロパティ一覧
     */
    @Override
    public void validate(List<OProperty<?>> props) {
        this.odataResource.validate(getEntitySetName(), props);
    }

    /**
     * メソッド実行可否チェック.
     * @param uriInfo リクエストされたリソースパス
     */
    private void checkNotAllowedMethod(UriInfo uriInfo) {
        // メソッド許可チェック
        String[] uriPath = uriInfo.getPath().split("/");
        if (ReceivedMessage.EDM_TYPE_NAME.equals(uriPath[uriPath.length - 1])
                || SentMessage.EDM_TYPE_NAME.equals(uriPath[uriPath.length - 1])) {
            throw PersoniumCoreException.Misc.METHOD_NOT_ALLOWED;
        }
    }
}
