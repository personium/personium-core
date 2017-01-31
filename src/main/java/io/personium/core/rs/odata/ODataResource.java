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

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.odata4j.core.ODataConstants;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.FormatWriterFactory;
import org.odata4j.format.xml.EdmxFormatWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.DcCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.auth.BoxPrivilege;
import io.personium.core.auth.OAuth2Helper.AcceptableAuthScheme;
import io.personium.core.auth.Privilege;
import io.personium.core.odata.DcODataProducer;
import io.personium.core.odata.OEntityWrapper;

/**
 * OData のサービスを提供する JAX-RS Resource リソースのルート. Unit制御 ・ Cell制御 ・ User OData Schema・ User ODataの４種の用途で使う.
 * サブクラスを作って、コンストラクタでrootUrl, odataProducerを与える。 このクラスでスキーマチェックなど裏側の実装に依存しない処理はすべて済ませる。
 */
public abstract class ODataResource extends ODataCtlResource {

    DcODataProducer odataProducer;
    String rootUrl;
    EdmDataServices metadata;
    AccessContext accessContext;

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(ODataResource.class);

    /**
     * コンストラクタ.
     * @param accessContext AccessContext
     * @param rootUrl ルートURL
     * @param producer ODataProducer
     */
    public ODataResource(final AccessContext accessContext,
            final String rootUrl, final DcODataProducer producer) {
        this.accessContext = accessContext;
        this.odataProducer = producer;
        this.rootUrl = rootUrl;
        this.metadata = this.odataProducer.getMetadata();
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * 認証ヘッダのチェック処理.
     * @param ac accessContext
     * @param privilege Privilege
     */
    public abstract void checkAccessContext(AccessContext ac, Privilege privilege);

    /**
     * 認証に使用できるAuth Schemeを取得する.
     * @return 認証に使用できるAuth Scheme
     */
    public abstract AcceptableAuthScheme getAcceptableAuthScheme();

    /**
     * リソースに対するアクセス権限チェック処理.
     * @param ac accessContext
     * @param privilege privilege
     * @return アクセス可否
     */
    public abstract boolean hasPrivilege(AccessContext ac, Privilege privilege);

    /**
     * スキーマ認証のチェック処理.
     * @param ac accessContext
     */
    public abstract void checkSchemaAuth(AccessContext ac);

    /**
     * Basic認証できるかのチェック処理（Batchリクエスト専用）.
     * @param ac accessContext
     */
    public abstract void setBasicAuthenticateEnableInBatchRequest(AccessContext ac);

    /**
     * エンティティ毎のアクセス可否判断.
     * @param ac accessContext
     * @param oew OEntityWrapper
     */
    public void checkAccessContextPerEntity(AccessContext ac, OEntityWrapper oew) {
        // Unitレベルの場合だけチェックを実施する。
    }

    /**
     * AccessContextによる追加検索条件の定義. サブクラスで必要に応じて定義する。
     * @param ac accessContext
     * @return $filterの文法？
     */
    public String defineAccessContextSearchContext(AccessContext ac) {
        return null;
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    @OPTIONS
//    @Path("")
    public Response optionsRoot() {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET
                ).build();
    }

    /**
     * OPTIONSメソッド.
     * @return JAX-RS Response
     */
    protected Response doGetOptionsMetadata() {
        return PersoniumCoreUtils.responseBuilderForOptions(
                HttpMethod.GET
                ).build();
    }

    /**
     * $batchの処理を行う.
     * @return レスポンス
     */
    @Path("{first: \\$}batch")
    public ODataBatchResource processBatch() {
        return new ODataBatchResource(this);
    }

    /**
     * サービスドキュメントを返す.
     * @param uriInfo UriInfo
     * @param format String
     * @param httpHeaders HttpHeaders
     * @return JAX-RS Response Object
     */
    @GET
//    @Path("")
    public Response getRoot(
            @Context final UriInfo uriInfo,
            @QueryParam("$format") final String format,
            @Context HttpHeaders httpHeaders) {
        // アクセス制御
        this.checkAccessContext(this.getAccessContext(), BoxPrivilege.READ);

        StringWriter w = new StringWriter();

        log.debug(format);
        List<MediaType> acceptableMediaTypes = null; // Enumerable.create(MediaType.APPLICATION_XML_TYPE).toList();

        FormatWriter<EdmDataServices> fw = FormatWriterFactory.getFormatWriter(EdmDataServices.class,
                acceptableMediaTypes, format, "");

        fw.write(PersoniumCoreUtils.createUriInfo(uriInfo, 0), w, this.metadata);

        return Response.ok(w.toString(), fw.getContentType())
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .build();
    }

    /**
     * metadataオブジェクトを取得する.
     * @return EdmDataServices型のメタデータ
     */
    public EdmDataServices getMetadataSource() {
        return this.metadata;
    }

    /**
     * サービスメタデータリクエストに対応する.
     * @return JAX-RS 応答オブジェクト
     */
    protected Response doGetMetadata() {

        StringWriter w = new StringWriter();
        EdmxFormatWriter.write(this.metadata, w);
        return Response.ok(w.toString(), ODataConstants.APPLICATION_XML_CHARSET_UTF8)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataConstants.DATA_SERVICE_VERSION_HEADER)
                .build();
    }

    /**
     * /{entitySet}というパスを処理する.
     * @param entitySetName entitySet名を表すパス
     * @param request Request
     * @return ODataEntitiesResource
     */
    @Path("{entitySet}")
    public ODataEntitiesResource entities(
            @PathParam("entitySet") final String entitySetName,
            @Context Request request) {
        // 存在しないエンティティセットを指定されたときは即刻エラー
        EdmEntitySet eSet = this.metadata.findEdmEntitySet(entitySetName);
        if (eSet == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
        }
        String method = request.getMethod();
        if (isChangeMethod(method.toUpperCase())) {
            this.odataProducer.onChange(entitySetName);
        }
        return new ODataEntitiesResource(this, entitySetName);
    }

    /**
     * /{entitySet}({key})というパスを処理する.
     * @param entitySetName entitySet名
     * @param key キー文字列
     * @param request Request
     * @return ODataEntityResourceクラスのオブジェクト
     */
    @Path("{entitySet}({key})")
    public ODataEntityResource entity(
            @PathParam("entitySet") final String entitySetName,
            @PathParam("key") final String key,
            @Context Request request) {
        // 存在しないエンティティセットを指定されたときは即刻エラー
        EdmEntitySet eSet = this.getMetadataSource().findEdmEntitySet(entitySetName);
        if (eSet == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
        }
        String method = request.getMethod();
        if (isChangeMethod(method.toUpperCase())) {
            this.odataProducer.onChange(entitySetName);
        }
        return new ODataEntityResource(this, entitySetName, key);
    }

    private boolean isChangeMethod(String method) {
        List<String> methods = Arrays.asList("GET", "OPTIONS", "HEAD", "PROPFIND");
        return !methods.contains(method);
    }

    /**
     * このODataサービスのRootURLを返します.
     * @return Root Url of this OData Service
     */
    public String getRootUrl() {
        return this.rootUrl;
    }

    /**
     * このODataサービスのODataProducerを返します.
     * @return ODataProducer of this OData Service
     */
    public DcODataProducer getODataProducer() {
        return this.odataProducer;
    }

    /**
     * Etagヘッダ値からETagの値を取り出します。
     * @param etagHeaderValue etagHeaderValue
     * @return etag
     */
    public static String parseEtagHeader(final String etagHeaderValue) {
        if (etagHeaderValue == null) {
            return null;
        } else if ("*".equals(etagHeaderValue)) {
            return "*";
        }
        // Weak形式 W/"()"
        Pattern pattern = Pattern.compile("^W/\"(.+)\"$");
        Matcher m = pattern.matcher(etagHeaderValue);

        if (!m.matches()) {
            throw DcCoreException.OData.ETAG_NOT_MATCH;
        }

        return m.replaceAll("$1");
    }

    /**
     * Etagヘッダ値を生成します。
     * @param etag etag
     * @return etagHeaderValue
     */
    public static String renderEtagHeader(final String etag) {
        return "W/\"" + etag + "\"";
    }

    /**
     * 処理するクラスのレベルとエンティティセット名から読み込みに必要な権限を返す.
     * @param entitySetNameStr 対象のエンティティセット
     * @return 処理に必要な権限
     */
    public abstract Privilege getNecessaryReadPrivilege(String entitySetNameStr);

    /**
     * 処理するクラスのレベルとエンティティセット名から書き込みに必要な権限を返す.
     * @param entitySetNameStr 対象のエンティティセット
     * @return 処理に必要な権限
     */
    public abstract Privilege getNecessaryWritePrivilege(String entitySetNameStr);

    /**
     * 処理するクラスのレベルとエンティティセット名からOPTIONSに必要な権限を返す.
     * @return 処理に必要な権限
     */
    public abstract Privilege getNecessaryOptionsPrivilege();

    /**
     * アクセスコンテキストが$batchしてよい権限を持っているかを返す.
     * @param ac アクセスコンテキスト
     * @return true: アクセスコンテキストが$batchしてよい権限を持っている
     */
    public abstract boolean hasPrivilegeForBatch(AccessContext ac);
}
