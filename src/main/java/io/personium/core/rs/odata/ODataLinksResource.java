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
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.core4j.Enumerable;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmMultiplicity;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatParserFactory;
import org.odata4j.format.FormatType;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.Settings;
import org.odata4j.format.SingleLink;
import org.odata4j.format.SingleLinks;
import org.odata4j.producer.EntityIdResponse;
import org.odata4j.producer.ODataProducer;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.DcCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ReceivedMessage;
import io.personium.core.model.impl.es.odata.EsODataProducer;
import io.personium.core.odata.DcFormatWriterFactory;

/**
 * ODataの$linksを扱う JAX-RS Resource.
 */
public final class ODataLinksResource {
    private final OEntityId sourceEntity;
    private final String targetNavProp;
    private final OEntityKey targetEntityKey;
    private final ODataResource odataResource;
    private final ODataProducer odataProducer;
    private final AccessContext accessContext;

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(ODataLinksResource.class);

    /**
     * コンストラクタ.
     * @param odataResource 親の ODataResource
     * @param sourceEntity リンク元Entity
     * @param targetNavProp リンク先 Navigation Property
     * @param targetEntityKey リンク先 EntityKey
     */
    public ODataLinksResource(
            final ODataResource odataResource,
            final OEntityId sourceEntity,
            final String targetNavProp,
            final OEntityKey targetEntityKey) {
        this.odataResource = odataResource;
        this.accessContext = this.odataResource.getAccessContext();
        this.odataProducer = this.odataResource.getODataProducer();
        this.sourceEntity = sourceEntity;
        this.targetNavProp = targetNavProp;
        this.targetEntityKey = targetEntityKey;
    }

    /**
     * POSTメソッドを受けて linkを作成する.
     * 成功時のレスポンスは204.特に記述が無いためLocationヘッダは返さない。
     * InsertLink Request
     * If an InsertLink Request is successful, the response MUST have a 204 status code,
     * as specified in [RFC2616], and contain an empty response body.
     * @param uriInfo UriInfo
     * @param reqBody リクエストボディ
     * @return JAX-RS Response
     */
    @POST
    public Response createLink(
            @Context UriInfo uriInfo,
            final Reader reqBody) {

        // アクセス制御
        this.checkWriteAccessContext();

        // リンク作成前処理
        this.odataResource.beforeLinkCreate(this.sourceEntity, this.targetNavProp);

        // $links の POSTでNav Propのキー指定があってはいけない。
        if (this.targetEntityKey != null) {
            throw DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
        }
        log.debug("POSTING $LINK");
        OEntityId newTargetEntity =
                parseRequestUri(PersoniumCoreUtils.createUriInfo(uriInfo, NUM_LEVELS_FROM_SVC_ROOT), reqBody);

        // URLで指定したリンク先オブジェクトとBodyに指定したオブジェクトが等しいかをチェックする
        Pattern p = Pattern.compile("(.+)/([^/]+)$");
        Matcher m = p.matcher(newTargetEntity.getEntitySetName());
        String bodyNavProp = m.replaceAll("$2");
        String targetEntitySetName = null;
        // 受信メッセージとアカウントの$linksの場合、リンク先にAccountを設定
        if (ReceivedMessage.EDM_NPNAME_FOR_ACCOUNT.equals(this.targetNavProp)) {
            targetEntitySetName = Account.EDM_TYPE_NAME;
            // アカウントと受信メッセージの$linksの場合、リンク先にReceivedMessageを設定
        } else if (Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE.equals(this.targetNavProp)) {
            targetEntitySetName = ReceivedMessage.EDM_TYPE_NAME;
        } else {
            targetEntitySetName = this.targetNavProp.substring(1);
        }
        if (!targetEntitySetName.equals(bodyNavProp)) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(Common.DC_FORMAT_PATTERN_URI);
        }

        this.odataProducer.createLink(sourceEntity, targetNavProp, newTargetEntity);
        return noContent();
    }

    /**
     * PUTメソッドを受けて linkを更新する.
     * @param uriInfo UriInfo
     * @param reqBody リクエストボディ
     * @return JAX-RS Response
     */
    @PUT
    public Response updateLink(
            @Context UriInfo uriInfo,
            final Reader reqBody) {

        // アクセス制御
        this.checkWriteAccessContext();

        if (this.targetEntityKey == null) {
            throw DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED;
        } else {
            throw DcCoreException.Misc.METHOD_NOT_IMPLEMENTED;
        }
    }

    /**
     * リクエストボディで指定された値が正しい形式かチェックし、$links先のOEntityIdを返却する.
     * @param uriInfo リクエストURL
     * @param reqBody リクエストボディ
     * @param srcEntitySetName $links元EntitySet名
     * @param metadata メタデータ
     * @return $links先のOEntityId
     */
    static OEntityId parseRequestUri(final UriInfo uriInfo,
            final Reader reqBody,
            String srcEntitySetName,
            EdmDataServices metadata) {
        Settings settings = new Settings(ODataVersion.V1, metadata, srcEntitySetName, null, null);
        FormatParser<SingleLink> parser = FormatParserFactory.getParser(SingleLink.class, FormatType.JSON, settings);
        SingleLink link = null;
        try {
            link = parser.parse(reqBody);
        } catch (Exception e) {
            throw DcCoreException.OData.JSON_PARSE_ERROR.reason(e);
        }
        if (link.getUri() == null) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        log.debug(uriInfo.getBaseUri().toASCIIString());

        // 複合キー対応
        // nullが指定されているとパースに失敗するため、null値が設定されている場合はダミーキーに置き換える
        String linkUrl = AbstractODataResource.replaceNullToDummyKey(link.getUri());
        log.debug(linkUrl);
        OEntityId oid = null;
        String serviceRootUri = uriInfo.getBaseUri().toASCIIString();
        try {
            oid = OEntityIds.parse(serviceRootUri, linkUrl);
        } catch (IllegalArgumentException e) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        // parse処理では後ろ括弧のチェックの対応を行っていないため、括弧の対応チェックを行う
        String entityId = linkUrl;
        if (entityId.toLowerCase().startsWith(serviceRootUri.toLowerCase())) {
            entityId = linkUrl.substring(serviceRootUri.length());
        }
        int indexOfParen = entityId.indexOf('(');
        String entitySetName = entityId.substring(indexOfParen);
        Pattern p = Pattern.compile("^\\(.+\\)$");
        Matcher m = p.matcher(entitySetName);
        if (!m.find()) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("uri");
        }

        return oid;
    }

    /**
     * リクエストボディで指定された値が正しい形式かチェックし、$links先のOEntityIdを返却する.
     * @param uriInfo リクエストURL
     * @param reqBody リクエストボディ
     * @return $links先のOEntityId
     */
    private OEntityId parseRequestUri(final UriInfo uriInfo, final Reader reqBody) {
        return parseRequestUri(uriInfo, reqBody, this.sourceEntity.getEntitySetName(),
                this.odataProducer.getMetadata());
    }

    private Response noContent() {
        return Response.noContent().header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
    }

    /**
     * DELETEメソッドを受けて linkを削除する.
     * @return JAX-RS Response
     */
    @DELETE
    public Response deleteLink() {

        // アクセス制御
        this.checkWriteAccessContext();

        // リンク削除前処理
        this.odataResource.beforeLinkDelete(this.sourceEntity, this.targetNavProp);

        // TODO $links の 削除は以下の2つのリクエストで実行可能であるが、NavPropのKey未指定は未実装
        // 1. http://host/service.svc/Customers('ALFKI')/$links/Orders(1)
        // 2. http://host/service.svc/Orders(1)/$links/Customer.
        if (this.targetEntityKey == null) {
            throw DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED;
        }
        this.odataProducer.deleteLink(sourceEntity, targetNavProp, targetEntityKey);
        return noContent();
    }

    static final int NUM_LEVELS_FROM_SVC_ROOT = 3;

    /**
     * GETメソッドを受けて link一覧を返す.
     * @param uriInfo UriInfo
     * @param format $format
     * @param callback ??
     * @return JAX-RS Response
     */
    @GET
    public Response getLinks(
            @Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @QueryParam("$callback") final String callback) {

        // アクセス制御
        this.checkReadAccessContext();
        if (this.targetEntityKey != null) {
            return Response
                    .status(HttpStatus.SC_BAD_REQUEST)
                    .entity("targetId should not be specified in $links GET. your value = "
                            + this.targetEntityKey.toKeyString()).build();
        }
        log.debug("GETTING $LINK");

        // リンク取得前処理
        this.odataResource.beforeLinkGet(this.sourceEntity, this.targetNavProp);

        EntityIdResponse response = getLinks(uriInfo);

        StringWriter sw = new StringWriter();
        // context.getRequest().getAcceptableMediaTypes()
        UriInfo uriInfo2 = PersoniumCoreUtils.createUriInfo(uriInfo, NUM_LEVELS_FROM_SVC_ROOT);
        String serviceRootUri = uriInfo2.getBaseUri().toASCIIString();
        String contentType;

        if (response.getMultiplicity() == EdmMultiplicity.MANY) {
            SingleLinks links = SingleLinks.create(serviceRootUri, response.getEntities());
            // TODO レスポンスはJSON固定とする.
            FormatWriter<SingleLinks> fw = DcFormatWriterFactory.getFormatWriter(SingleLinks.class, null, "json",
                    callback);
            fw.write(uriInfo2, sw, links);
            contentType = fw.getContentType();
        } else {
            OEntityId entityId = Enumerable.create(response.getEntities()).firstOrNull();
            if (entityId == null) {
                throw new NotFoundException();
            }
            SingleLink link = SingleLinks.create(serviceRootUri, entityId);
            FormatWriter<SingleLink> fw = DcFormatWriterFactory.getFormatWriter(SingleLink.class, null, "json",
                    callback);
            fw.write(uriInfo, sw, link);
            contentType = fw.getContentType();
        }

        String entity = sw.toString();

        return Response.ok(entity, contentType)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();
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
                HttpMethod.DELETE,
                HttpMethod.PUT,
                HttpMethod.POST
                ).build();
    }

    private EntityIdResponse getLinks(UriInfo uriInfo) {
        QueryInfo queryInfo = null;
        if (uriInfo != null) {
            queryInfo = queryInfo(uriInfo);
        }
        EntityIdResponse response = null;
        // getLinksメソッドにパラメータを追加したが、インターフェースを修正すると影響範囲が大きいため、
        // プロデューサーをキャストする
        if (this.odataProducer instanceof EsODataProducer) {
            EsODataProducer producer = (EsODataProducer) this.odataProducer;
            response = producer.getLinks(sourceEntity, targetNavProp, queryInfo);
        }
        return response;
    }

    QueryInfo queryInfo(UriInfo uriInfo) {
        MultivaluedMap<String, String> mm = uriInfo.getQueryParameters(true);
        Integer top = QueryParser.parseTopQuery(mm.getFirst("$top"));
        Integer skip = QueryParser.parseSkipQuery(mm.getFirst("$skip"));

        return new QueryInfo(
                null,
                top,
                skip,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private void checkWriteAccessContext() {
        // アクセス制御
        // TODO BOXレベルの場合に同じ処理が2回走る。無駄なのでcheckAccessContextにPrivilegeを配列で渡す等の工夫が必要
        String entitySetNameFrom = sourceEntity.getEntitySetName();
        String entitySetNameTo = targetNavProp;
        if (entitySetNameFrom.equals(ReceivedMessage.EDM_TYPE_NAME)
                || entitySetNameTo.equals(Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE)) {
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryWritePrivilege(ReceivedMessage.EDM_TYPE_NAME));
        } else {
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryWritePrivilege(entitySetNameFrom));
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryWritePrivilege(entitySetNameTo.substring(1)));
        }
    }

    private void checkReadAccessContext() {
        // アクセス制御
        // TODO BOXレベルの場合に同じ処理が2回走る。無駄なのでcheckAccessContextにPrivilegeを配列で渡す等の工夫が必要
        String entitySetNameFrom = sourceEntity.getEntitySetName();
        String entitySetNameTo = targetNavProp;
        if (entitySetNameFrom.equals(ReceivedMessage.EDM_TYPE_NAME)
                || entitySetNameTo.equals(Account.EDM_NPNAME_FOR_RECEIVED_MESSAGE)) {
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryReadPrivilege(ReceivedMessage.EDM_TYPE_NAME));
        } else {
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryReadPrivilege(entitySetNameFrom));
            this.odataResource.checkAccessContext(this.accessContext,
                    this.odataResource.getNecessaryReadPrivilege(entitySetNameTo.substring(1)));
        }
    }
}
