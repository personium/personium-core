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
package com.fujitsu.dc.core.rs.odata;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityIds;
import org.odata4j.core.OEntityKey;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.format.FormatWriter;
import org.odata4j.producer.EntitiesResponse;
import org.odata4j.producer.EntityResponse;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.ODataBatchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreAuthzException;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcReadDeleteModeManager;
import com.fujitsu.dc.core.auth.AccessContext;
import com.fujitsu.dc.core.auth.Privilege;
import com.fujitsu.dc.core.exceptions.ODataErrorMessage;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.UserDataODataProducer;
import com.fujitsu.dc.core.odata.DcFormatWriterFactory;
import com.fujitsu.dc.core.odata.OEntityWrapper;
import com.fujitsu.dc.core.rs.DcCoreExceptionMapper;

/**
 * ODataBatchResourceクラス.
 */
public class ODataBatchResource extends AbstractODataResource {

    private static final String X_DC_PRIORITY = "X-Dc-Priority";

    /**
     * Lockを他プロセスに譲るためにスリープするか否か.
     */
    public enum BatchPriority {
        /** Lockを他プロセスに譲らない. */
        HIGH("high"),
        /** Lockを他プロセスに譲る. */
        LOW("low");

        private String priority;

        BatchPriority(String priority) {
            this.priority = priority;
        }

        /**
         * 文字列から列挙値を生成する(デフォルト値: LOW).
         * @param val 文字列
         * @return 列挙値
         */
        public static BatchPriority fromString(String val) {
            for (BatchPriority e : BatchPriority.values()) {
                if (e.priority.equalsIgnoreCase(val)) {
                    return e;
                }
            }
            return LOW;
        }
    }

    Logger logger = LoggerFactory.getLogger(ODataBatchResource.class);

    ODataResource odataResource;
    LinkedHashMap<String, BulkRequest> bulkRequests = new LinkedHashMap<String, BulkRequest>();

    // Batchリクエスト中にToo Many Concurrentが発生後の実行/スキップを制御するクラス
    BatchRequestShutter shutter;

    Map<Privilege, BatchAccess> readAccess = new HashMap<Privilege, BatchAccess>();
    Map<Privilege, BatchAccess> writeAccess = new HashMap<Privilege, BatchAccess>();

    // EntityType名とEntityTypeIDのマッピングデータ
    Map<String, String> entityTypeIds;

    /**
     * コンストラクタ.
     * @param odataResource ODataResource
     */
    public ODataBatchResource(final ODataResource odataResource) {
        this.odataResource = odataResource;
        this.shutter = new BatchRequestShutter();
    }

    /**
     * バッチリクエストを処理する.
     * @param uriInfo uriInfo
     * @param headers headers
     * @param request request
     * @param reader reader
     * @return レスポンス
     */
    @POST
    public Response batchRequest(
            @Context UriInfo uriInfo,
            @Context HttpHeaders headers,
            @Context Request request,
            Reader reader) {

        long startTime = System.currentTimeMillis();
        // タイムアウト時間 (dc_config.properties com.fujitsu.dc.core.odata.batch.timeoutInSecで設定. 単位は秒)
        long batchTimeoutInSec = DcCoreConfig.getOdataBatchRequestTimeoutInMillis();

        // Lockを他プロセスに譲るためにスリープするか否かの拡張ヘッダの値を取得する
        BatchPriority priority = BatchPriority.LOW;
        List<String> priorityHeaders = headers.getRequestHeader(X_DC_PRIORITY);
        if (priorityHeaders != null) {
            priority = BatchPriority.fromString(priorityHeaders.get(0));
        }

        timer = new BatchElapsedTimer(startTime, batchTimeoutInSec, priority);

        checkAccessContext(this.odataResource.getAccessContext());

        // TODO 不正なコンテントタイプが指定された場合エラーを返却する
        String boundary = headers.getMediaType().getParameters().get("boundary");

        // リクエストボディのパース
        BatchBodyParser parser = new BatchBodyParser();
        List<BatchBodyPart> bodyParts = parser.parse(boundary, reader, uriInfo.getRequestUri().toString());
        if (bodyParts == null || bodyParts.size() == 0) {
            // パース処理失敗
            throw DcCoreException.OData.BATCH_BODY_PARSE_ERROR;
        }
        if (bodyParts.size() > Integer.parseInt(DcCoreConfig.getOdataBatchBulkRequestMaxSize())) {
            // $Batchで指定されたリクエスト数が不正
            throw DcCoreException.OData.TOO_MANY_REQUESTS.params(bodyParts.size());
        }

        UserDataODataProducer producer = (UserDataODataProducer) this.odataResource.getODataProducer();
        entityTypeIds = producer.getEntityTypeIds();

        List<NavigationPropertyBulkContext> npBulkContexts = new ArrayList<NavigationPropertyBulkContext>();

        StringBuilder responseBody = new StringBuilder();

        // １件ずつリクエストを実行
        for (BatchBodyPart bodyPart : bodyParts) {
            executePartRequest(responseBody, uriInfo, boundary, npBulkContexts, bodyPart);
        }

        // POSTのbulk実行
        checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);

        // バウンダリ終端文字列
        responseBody.append("--" + boundary + "--");

        // レスポンス作成
        String contentType = ODataBatchProvider.MULTIPART_MIXED + "; boundary=" + boundary;
        return Response.status(HttpStatus.SC_ACCEPTED)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .entity(responseBody.toString())
                .build();
    }

    /**
     * $batch内のタイムアウトレスポンスを設定する(Changeset).
     */
    private void setChangesetTimeoutResponse(StringBuilder builder, String boundary, BatchBodyPart bodyPart) {
        BatchResponse res = getTimeoutResponse();
        builder.append(getChangesetResponseBody(boundary, bodyPart, res));
    }

    /**
     * $batch内のタイムアウトレスポンスを設定する.
     */
    private void setTimeoutResponse(StringBuilder builder, String boundary) {
        BatchResponse res = getTimeoutResponse();
        builder.append(getRetrieveResponseBody(boundary, res));
    }

    /**
     * タイムアウトレスポンスを作成.
     */
    private BatchResponse getTimeoutResponse() {
        BatchResponse res = new BatchResponse();
        res.setErrorResponse(DcCoreException.Misc.SERVER_REQUEST_TIMEOUT);
        return res;
    }

    private BatchElapsedTimer timer = null;
    private volatile boolean timedOut = false;

    /**
     * timeout時間が経過しているか否かを判定する.<br />
     * timerオブジェクトは、API呼び出し時にインスタンス化されていることが呼び出し条件.<br />
     * modeがYIELDの場合、timeout時間が経過しているか判定前に、Lockを他プロセスに譲るためにスリープする.
     * @param mode Lockを他プロセスに譲るためにスリープするか否か
     * @return timeout時間が経過しているか否か
     */
    private boolean isTimedOut(BatchElapsedTimer.Lock mode) {
        if (!timedOut) {
            timedOut = timer.shouldBreak(mode);
            if (timedOut) {
                logger.info("Batch request timed out after " + timer.getElapseTimeToBreak() + " msec.");
            }
        }
        return timedOut;
    }

    private void executePartRequest(StringBuilder responseBody, UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> npBulkContexts,
            BatchBodyPart bodyPart) {
        // ReadDeleteOnlyMode中はGETとDELETEメソッド以外は許可しないため、エラーレスポンスを設定する
        if (!DcReadDeleteModeManager.isAllowedMethod(bodyPart.getHttpMethod())) {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(DcCoreException.Server.READ_DELETE_ONLY);
            responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            return;
        }
        // Batchリクエスト中にToo Many Concurrentが発生した以降の更新系リクエストレスポンスを設定する.
        if (!shutter.accept(bodyPart.getHttpMethod())) {
            setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
            return;
        }
        if (!isValidNavigationProperty(bodyPart)) {
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED);
            if (HttpMethod.GET.equals(bodyPart.getHttpMethod())) {
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            }
            return;
        }
        if (bodyPart.isLinksRequest()) {
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = new BatchResponse();
            if (bodyPart.getHttpMethod().equals(HttpMethod.POST)) {
                if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                    if (!shutter.isShuttered()) {
                        res = createLinks(uriInfo, bodyPart);
                        responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                    } else {
                        setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                    }
                } else {
                    setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
                }
            } else if (bodyPart.getHttpMethod().equals(HttpMethod.GET)) {
                res.setErrorResponse(DcCoreException.Misc.METHOD_NOT_IMPLEMENTED);
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                res.setErrorResponse(DcCoreException.Misc.METHOD_NOT_IMPLEMENTED);
                responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.POST)) {
            if (bodyPart.hasNavigationProperty()) {
                // NP経由ユーザデータ登録
                if (bulkRequests.size() != 0) {
                    if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                        execBulk(responseBody, uriInfo, boundary);
                    } else {
                        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
                            setChangesetTimeoutResponse(responseBody, boundary, request.getValue().getBodyPart());
                            BatchResponse res = new BatchResponse();
                            Exception exception = DcCoreException.Misc.SERVER_REQUEST_TIMEOUT;
                            res.setErrorResponse(exception);
                            responseBody.append(
                                    getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
                        }
                        bulkRequests.clear();
                    }
                }
                NavigationPropertyBulkContext navigationPropertyBulkContext =
                        new NavigationPropertyBulkContext(bodyPart);
                if (shutter.isShuttered()) {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                } else {
                    try {
                        navigationPropertyBulkContext = createNavigationPropertyBulkContext(bodyPart);
                        npBulkContexts.add(navigationPropertyBulkContext);
                    } catch (Exception e) {
                        navigationPropertyBulkContext.setException(e);
                        npBulkContexts.add(navigationPropertyBulkContext);
                    }
                }
            } else {
                // ユーザデータ登録
                if (!npBulkContexts.isEmpty()) {
                    if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                        // NP経由ユーザデータ登録のbulk実行
                        execBulkRequestForNavigationProperty(npBulkContexts);
                        createNavigationPropertyBulkResponse(
                                responseBody,
                                uriInfo,
                                boundary,
                                npBulkContexts);
                    } else {
                        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
                            npBulkContext.setException(DcCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                            npBulkContext.getBatchResponse().setErrorResponse(
                                    DcCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                            responseBody.append(getChangesetResponseBody(boundary, npBulkContext.getBodyPart(),
                                    npBulkContext.getBatchResponse()));
                        }
                    }
                    npBulkContexts.clear();
                }
                if (!shutter.isShuttered()) {
                    setBulkRequestsForEntity(bodyPart);
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.GET)) {
            // POSTのbulk実行
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            BatchResponse res = null;
            if (!isTimedOut(BatchElapsedTimer.Lock.HOLD)) {
                if (isListRequst(bodyPart)) {
                    res = list(uriInfo, bodyPart);
                } else {
                    res = retrieve(uriInfo, bodyPart);
                }
                responseBody.append(getRetrieveResponseBody(boundary, res));
            } else {
                setTimeoutResponse(responseBody, boundary);
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.PUT)) {
            // POSTのbulk実行
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    BatchResponse res = update(bodyPart);
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            } else {
                setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
            }
        } else if (bodyPart.getHttpMethod().equals(HttpMethod.DELETE)) {
            // POSTのbulk実行
            checkAndExecBulk(responseBody, uriInfo, boundary, npBulkContexts);
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    BatchResponse res = delete(bodyPart);
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
                } else {
                    setChangesetTooManyConcurrentResponse(responseBody, boundary, bodyPart);
                }
            } else {
                setChangesetTimeoutResponse(responseBody, boundary, bodyPart);
            }
        } else {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(DcCoreException.Misc.METHOD_NOT_ALLOWED);
            responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
        }
    }

    private void setChangesetTooManyConcurrentResponse(StringBuilder responseBody,
            String boundary,
            BatchBodyPart bodyPart) {
        // 直前のPOSTリクエストがTooManyConcurrentだったため、エラーレスポンスを作成する
        BatchResponse res = new BatchResponse();
        res.setErrorResponse(DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
        responseBody.append(getChangesetResponseBody(boundary, bodyPart, res));
    }

    /**
     * NP経由ユーザデータをバルク登録する.
     * @param npBulkContexts NavigationPropertyコンテキストのリスト
     */
    private void execBulkRequestForNavigationProperty(List<NavigationPropertyBulkContext> npBulkContexts) {
        // バルク登録用にコンテキストからBulkRequestを作成
        // NP側のEntityTypeの存在チェック、バルクデータ内でのID競合チェックもここで行う
        LinkedHashMap<String, BulkRequest> npBulkRequests = new LinkedHashMap<String, BulkRequest>();
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            BatchBodyPart bodyPart = npBulkContext.getBodyPart();
            BulkRequest bulkRequest = new BulkRequest(bodyPart);
            String key = DcUUID.randomUUID();

            if (npBulkContext.isError()) {
                bulkRequest.setError(npBulkContext.getException());
                npBulkRequests.put(key, bulkRequest);
                continue;
            }

            String targetEntitySetName = bodyPart.getTargetEntitySetName();
            bulkRequest = createBulkRequest(bodyPart, targetEntitySetName);
            // データ内でのID競合チェック
            // TODO 複合主キー対応、ユニークキーのチェック、NTKP対応
            if (bulkRequest.getError() == null) {
                EntitySetDocHandler docHandler = bulkRequest.getDocHandler();
                key = docHandler.getEntityTypeId() + ":" + (String) docHandler.getStaticFields().get("__id");
                if (npBulkRequests.containsKey(key)) {
                    key = DcUUID.randomUUID();
                    bulkRequest.setError(DcCoreException.OData.ENTITY_ALREADY_EXISTS);
                }
            }

            npBulkRequests.put(key, bulkRequest);
        }

        try {
            this.odataResource.getODataProducer().bulkCreateEntityViaNavigationProperty(npBulkContexts, npBulkRequests);
        } catch (DcCoreException e) {
            // 503が発生した後の処理を継続させるため、shutterにステータスを設定。
            shutter.updateStatus(e);
            if (!DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS.equals(e)) {
                throw e;
            } else {
                createTooManyConcurrentResponse(npBulkContexts);
            }
        }
        npBulkRequests.clear();
    }

    private void createNavigationPropertyBulkResponse(
            StringBuilder responseBody,
            UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> npBulkContexts) {
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            BatchBodyPart bodyPart = npBulkContext.getBodyPart();
            if (!npBulkContext.isError()) {
                try {
                    setNavigationPropertyResponse(uriInfo, bodyPart, npBulkContext);
                } catch (Exception e) {
                    npBulkContext.getBatchResponse().setErrorResponse(e);
                    npBulkContext.setException(e);
                }
            } else {
                npBulkContext.getBatchResponse().setErrorResponse(npBulkContext.getException());
            }
            responseBody.append(getChangesetResponseBody(boundary, bodyPart,
                    npBulkContext.getBatchResponse()));
        }
    }

    /**
     * 各リクエストのChangesetのレスポンスボディを作成する.
     * @param boundaryStr バウンダリ文字列
     * @param bodyPart BatchBodyPart
     * @param res レスポンス
     * @return リクエストのレスポンスボディ
     */
    private String getChangesetResponseBody(String boundaryStr,
            BatchBodyPart bodyPart,
            BatchResponse res) {
        StringBuilder resBody = new StringBuilder();

        // レスポンスボディ作成
        // バウンダリ文字列
        if (bodyPart.isChangesetStart()) {
            // レスポンスにバウンダリ文字列を追加
            resBody.append("--" + boundaryStr + "\n");
            resBody.append(HttpHeaders.CONTENT_TYPE + ": ");
            resBody.append(ODataBatchProvider.MULTIPART_MIXED + "; boundary="
                    + bodyPart.getChangesetStr() + "\n\n");
        }

        // changeset文字列
        resBody.append("--" + bodyPart.getChangesetStr() + "\n");
        // ContentType
        resBody.append(HttpHeaders.CONTENT_TYPE + ": ");
        resBody.append("application/http\n");
        // Content-Transfer-Encoding
        resBody.append("Content-Transfer-Encoding: binary\n\n");

        // HTTP/1.1 {レスポンスコード} {レスポンスコードの説明}
        resBody.append("HTTP/1.1 ");
        resBody.append(res.getResponseCode() + " ");
        resBody.append(res.getResponseMessage() + "\n");
        // レスポンスヘッダ
        for (String key : res.getHeaders().keySet()) {
            resBody.append(key + ": " + res.getHeaders().get(key) + "\n");
        }
        resBody.append("\n");

        // レスポンスボディ
        if (res.getBody() != null) {
            resBody.append(res.getBody() + "\n\n");
        }

        // changeset終端文字列
        if (bodyPart.isChangesetEnd()) {
            resBody.append("--" + bodyPart.getChangesetStr() + "--\n\n");
        }

        return resBody.toString();
    }

    /**
     * 各リクエストのGET, LISTのレスポンスボディを作成する.
     * @param boundaryStr バウンダリ文字列
     * @param res レスポンス
     * @return リクエストのレスポンスボディ
     */
    private String getRetrieveResponseBody(String boundaryStr,
            BatchResponse res) {
        StringBuilder resBody = new StringBuilder();

        // レスポンスボディ作成
        // バウンダリ文字列
        // レスポンスにバウンダリ文字列を追加
        resBody.append("--" + boundaryStr + "\n");
        resBody.append(HttpHeaders.CONTENT_TYPE + ": application/http\n\n");

        // HTTP/1.1 {レスポンスコード} {レスポンスコードの説明}
        resBody.append("HTTP/1.1 ");
        resBody.append(res.getResponseCode() + " ");
        resBody.append(res.getResponseMessage() + "\n");
        // レスポンスヘッダ
        for (String key : res.getHeaders().keySet()) {
            resBody.append(key + ": " + res.getHeaders().get(key) + "\n");
        }
        resBody.append("\n");

        // レスポンスボディ
        if (res.getBody() != null) {
            resBody.append(res.getBody() + "\n\n");
        }

        return resBody.toString();
    }

    /**
     * 一覧取得か１件取得かを判定する.
     * @param uri リクエストURI
     * @return true: 一覧取得, false:１件取得
     */
    private boolean isListRequst(BatchBodyPart bodyPart) {
        if (bodyPart.getEntityKeyWithParences() == null
                || bodyPart.hasNavigationProperty()) {
            return true;
        }
        return false;
    }

    /**
     * バッチリクエストの登録データをバルクリクエストに設定する.
     * @param bodyPart BatchBodyPart
     */
    private void setBulkRequestsForEntity(BatchBodyPart bodyPart) {
        String key = DcUUID.randomUUID();

        BulkRequest bulkRequest = createBulkRequest(bodyPart, bodyPart.getEntitySetName());
        if (bulkRequest.getError() == null) {
            // データ内でのID競合チェック
            // TODO 複合主キー対応、ユニークキーのチェック、NTKP対応
            EntitySetDocHandler docHandler = bulkRequest.getDocHandler();
            key = docHandler.getEntityTypeId() + ":" + (String) docHandler.getStaticFields().get("__id");
            if (bulkRequests.containsKey(key)) {
                key = DcUUID.randomUUID();
                bulkRequest.setError(DcCoreException.OData.ENTITY_ALREADY_EXISTS);
            }
        }

        bulkRequests.put(key, bulkRequest);
    }

    private BulkRequest createBulkRequest(BatchBodyPart bodyPart, String entitySetName) {
        BulkRequest bulkRequest = new BulkRequest(bodyPart);
        try {
            // アクセス制御
            checkWriteAccessContext(bodyPart);

            // 存在しないエンティティセットを指定されたときは404例外を発生する
            EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(entitySetName);
            if (eSet == null) {
                throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            // リクエストボディを生成する
            ODataEntitiesResource resource = new ODataEntitiesResource(this.odataResource, entitySetName);
            OEntity oEntity = resource.getOEntityWrapper(new StringReader(bodyPart.getEntity()),
                    this.odataResource, this.odataResource.metadata);
            EntitySetDocHandler docHandler = resource.getOdataProducer().getEntitySetDocHandler(entitySetName, oEntity);
            String docType = UserDataODataProducer.USER_ODATA_NAMESPACE;
            docHandler.setType(docType);
            docHandler.setEntityTypeId(entityTypeIds.get(entitySetName));

            this.odataResource.metadata = resource.getOdataProducer().getMetadata();

            // ID指定がない場合はUUIDを払い出す
            if (docHandler.getId() == null) {
                docHandler.setId(DcUUID.randomUUID());
            }
            bulkRequest.setEntitySetName(entitySetName);
            bulkRequest.setDocHandler(docHandler);

        } catch (Exception e) {
            bulkRequest.setError(e);
        }
        return bulkRequest;
    }

    private void setNavigationPropertyResponse(UriInfo uriInfo,
            BatchBodyPart bodyPart,
            NavigationPropertyBulkContext npBulkContext) {
        OEntity ent = npBulkContext.getEntityResponse().getEntity();
        // 現状は、ContentTypeはJSON固定
        String accept = bodyPart.getHttpHeaders().get(org.apache.http.HttpHeaders.ACCEPT);
        MediaType outputFormat = this.decideOutputFormat(accept, "json");
        // Entity Responseをレンダー
        List<MediaType> contentTypes = new ArrayList<MediaType>();
        contentTypes.add(outputFormat);
        UriInfo resUriInfo = DcCoreUtils.createUriInfo(uriInfo, 1);
        String key = AbstractODataResource.replaceDummyKeyToNull(ent.getEntityKey().toKeyString());
        String responseStr = renderEntityResponse(resUriInfo, npBulkContext.getEntityResponse(), "json",
                contentTypes);

        // ヘッダーの設定
        npBulkContext.setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        String entityName = bodyPart.getTargetNavigationProperty().substring(1) + key;
        npBulkContext.setResponseHeader(HttpHeaders.LOCATION,
                resUriInfo.getBaseUri().toASCIIString() + entityName);
        npBulkContext.setResponseHeader(ODataConstants.Headers.DATA_SERVICE_VERSION,
                ODataVersion.V2.asString);
        // 応答にETAGを付与
        if (ent instanceof OEntityWrapper) {
            OEntityWrapper oew2 = (OEntityWrapper) ent;
            String etag = oew2.getEtag();
            if (etag != null) {
                npBulkContext.setResponseHeader(HttpHeaders.ETAG, "W/\"" + etag + "\"");
            }
        }
        npBulkContext.setResponseBody(responseStr);
        npBulkContext.setResponseCode(HttpStatus.SC_CREATED);
    }

    private NavigationPropertyBulkContext createNavigationPropertyBulkContext(BatchBodyPart bodyPart) {
        // アクセス制御
        checkWriteAccessContext(bodyPart);

        // 存在しないエンティティセットを指定されたときは404例外を発生する
        EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(bodyPart.getEntitySetName());
        if (eSet == null) {
            throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
        }

        // Navigationプロパティのスキーマ上の存在確認
        ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                bodyPart.getEntitySetName(), bodyPart.getEntityKey());
        OEntityId oEntityId = entityResource.getOEntityId();
        EdmNavigationProperty enp = eSet.getType().findNavigationProperty(bodyPart.getTargetNavigationProperty());
        if (enp == null) {
            throw DcCoreException.OData.NOT_SUCH_NAVPROP;
        }

        // リクエスト情報作成
        StringReader requestReader = new StringReader(bodyPart.getEntity());
        OEntityWrapper oew = createEntityFromInputStream(
                requestReader,
                eSet.getType(),
                oEntityId.getEntityKey(),
                bodyPart.getTargetEntitySetName());

        String navigationPropertyName = bodyPart.getTargetNavigationProperty();
        return new NavigationPropertyBulkContext(bodyPart, oew, oEntityId, navigationPropertyName);
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
        EdmDataServices metadata = this.odataResource.getODataProducer().getMetadata();
        OEntity newEnt = createRequestEntity(reader, null, metadata, targetEntitySetName);

        // ラッパにくるむ. POSTでIf-Match等 ETagを受け取ることはないのでetagはnull。
        String uuid = DcUUID.randomUUID();
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
        this.odataResource.beforeCreate(oew);

        // Entityの作成を Producerに依頼.この中であわせて、存在確認もしてもらう。
        EntityResponse res = this.odataResource.getODataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * NP経由登録用のリンクの関連タイプ.
     */
    public enum NavigationPropertyLinkType {
        /** 1:1 / 0..1:1 / 1:0..1 / 0..1:0..1 . */
        oneToOne,
        /** n:1 / n:0..1 . */
        manyToOne,
        /** 1:n / 0..1:n . */
        oneToMany,
        /** n:n. */
        manyToMany
    }

    /**
     * NP経由登録用クラス.
     */
    public static class NavigationPropertyBulkContext {
        private OEntityWrapper oew;
        private EntityResponse entityResponse;
        private OEntityId srcEntityId;
        private String tgtNavProp;
        private BatchBodyPart bodyPart;

        private NavigationPropertyLinkType linkType;
        private EntitySetDocHandler sourceDocHandler;
        private EntitySetDocHandler targetDocHandler;
        private LinkDocHandler linkDocHandler;

        private BatchResponse res;
        private boolean isError;
        private Exception exception;

        /**
         * コンストラクタ.
         * @param bodyPart BatchBodyPart
         */
        public NavigationPropertyBulkContext(BatchBodyPart bodyPart) {
            this(bodyPart, null, null, null);
        }

        /**
         * コンストラクタ.
         * @param bodyPart 登録するBatchBodyPart
         * @param oew 登録するOEntity
         * @param srcEntityId リンク元OEntityId
         * @param tgtNavProp NavigationProperty名
         */
        public NavigationPropertyBulkContext(
                BatchBodyPart bodyPart,
                OEntityWrapper oew,
                OEntityId srcEntityId,
                String tgtNavProp) {
            this.oew = oew;
            this.entityResponse = null;
            this.srcEntityId = srcEntityId;
            this.tgtNavProp = tgtNavProp;

            this.bodyPart = bodyPart;
            this.res = new BatchResponse();
        }

        /**
         * 登録するOEntityを設定する.
         * @param entity 設定するOEntity
         */
        public void setOEntityWrapper(OEntityWrapper entity) {
            this.oew = entity;
        }

        /**
         * 登録するOEntityを取得する.
         * @return 登録するOEntity
         */
        public OEntityWrapper getOEntityWrapper() {
            return this.oew;
        }

        /**
         * 登録した結果のEntityResponseを取得する.
         * @return EntityResponse
         */
        public EntityResponse getEntityResponse() {
            return this.entityResponse;
        }

        /**
         * 登録した結果のEntityResponseを設定する.
         * @param entityResponse 登録した結果のEntityResponse
         */
        public void setEntityResponse(EntityResponse entityResponse) {
            this.entityResponse = entityResponse;
        }

        /**
         * リンク元OEntityIdを取得する.
         * @return リンク元OEntityId
         */
        public OEntityId getSrcEntityId() {
            return this.srcEntityId;
        }

        /**
         * NavigationProperty名を取得する.
         * @return NavigationProperty名
         */
        public String getTgtNavProp() {
            return this.tgtNavProp;
        }

        /**
         * 登録するBatchBodyPartを取得する.
         * @return 登録するBatchBodyPart
         */
        public BatchBodyPart getBodyPart() {
            return this.bodyPart;
        }

        /**
         * Batch用のレスポンスボディを取得する.
         * @return Batch用のレスポンスボディ
         */
        public BatchResponse getBatchResponse() {
            return this.res;
        }

        /**
         * Batch用のレスポンスヘッダを設定する.
         * @param key BatchResponseのヘッダのキー
         * @param value BatchResponseのヘッダの値
         */
        public void setResponseHeader(String key, String value) {
            this.res.setHeader(key, value);
        }

        /**
         * Batch用のレスポンスボディを設定する.
         * @param body BatchResponseのbody
         */
        public void setResponseBody(String body) {
            this.res.setBody(body);
        }

        /**
         * Batch用のレスポンスコードを設定する.
         * @param code BatchResponseのレスポンスコード
         */
        public void setResponseCode(int code) {
            this.res.setResponseCode(code);
        }

        /**
         * リンクの関連タイプを取得する.
         * @return リンクの関連タイプ
         */
        public NavigationPropertyLinkType getLinkType() {
            return linkType;
        }

        /**
         * リンクの関連タイプを設定する.
         * @param linkType リンクの関連タイプ
         */
        public void setLinkType(NavigationPropertyLinkType linkType) {
            this.linkType = linkType;
        }

        /**
         * リンクエンティティのコンテキスト情報を設定する.
         * /EntityType('ID')/_NavigationProperty のEntityType側のコンテキスト
         * @return リンクエンティティのコンテキスト情報
         */
        public EntitySetDocHandler getSourceDocHandler() {
            return sourceDocHandler;
        }

        /**
         * リンクエンティティのコンテキスト情報を設定する.
         * /EntityType('ID')/_NavigationProperty のEntityType側のコンテキスト
         * @param sourceDocHandler リンクエンティティのコンテキスト情報
         */
        public void setSourceDocHandler(EntitySetDocHandler sourceDocHandler) {
            this.sourceDocHandler = sourceDocHandler;
        }

        /**
         * 登録コンテキスト情報の取得する.
         * /EntityType('ID')/_NavigationProperty の_NavigationProperty側のコンテキスト
         * @return 登録コンテキスト情報
         */
        public EntitySetDocHandler getTargetDocHandler() {
            return targetDocHandler;
        }

        /**
         * 登録コンテキスト情報を設定する.
         * /EntityType('ID')/_NavigationProperty の_NavigationProperty側のコンテキスト
         * @param targetDocHandler 登録コンテキスト情報
         */
        public void setTargetDocHandler(EntitySetDocHandler targetDocHandler) {
            this.targetDocHandler = targetDocHandler;
        }

        /**
         * 登録するリンク情報を取得する.
         * @return 登録するリンク情報
         */
        public LinkDocHandler getLinkDocHandler() {
            return linkDocHandler;
        }

        /**
         * 登録するリンク情報を設定する.
         * @param linkDocHandler 登録するリンク情報
         */
        public void setLinkDocHandler(LinkDocHandler linkDocHandler) {
            this.linkDocHandler = linkDocHandler;
        }

        /**
         * 例外情報を設定する.
         * @param ex 例外情報
         */
        public void setException(Exception ex) {
            this.exception = ex;
            this.isError = true;
        }

        /**
         * 処理中にエラーが発生したか.
         * @return true: エラー発生, false: エラーなし
         */
        public boolean isError() {
            return this.isError;
        }

        /**
         * 処理中に発生したエラーの例外情報を取得する.
         * @return 例外情報
         */
        public Exception getException() {
            return this.exception;
        }
    }

    /**
     * bulkリクエストのチェックと実行処理.
     * @param uriInfo uriInfo
     * @param boundary boundary
     * @param navigationPropertyBulkContexts ナビゲーションプロパティ経由登録リクエスト情報のリスト
     */
    private void checkAndExecBulk(
            StringBuilder responseBody,
            UriInfo uriInfo,
            String boundary,
            List<NavigationPropertyBulkContext> navigationPropertyBulkContexts) {
        if (!navigationPropertyBulkContexts.isEmpty()) {
            if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                if (!shutter.isShuttered()) {
                    execBulkRequestForNavigationProperty(navigationPropertyBulkContexts);

                    createNavigationPropertyBulkResponse(
                            responseBody,
                            uriInfo,
                            boundary,
                            navigationPropertyBulkContexts);
                } else {
                    // 前のブロックで503エラーが発生している場合
                    createTooManyConcurrentResponse(navigationPropertyBulkContexts);
                }
            } else {
                for (NavigationPropertyBulkContext npBulkContext : navigationPropertyBulkContexts) {
                    npBulkContext.setException(DcCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                    npBulkContext.getBatchResponse().setErrorResponse(DcCoreException.Misc.SERVER_REQUEST_TIMEOUT);
                    BatchBodyPart bodyPart = npBulkContext.getBodyPart();
                    responseBody.append(getChangesetResponseBody(boundary, bodyPart,
                            npBulkContext.getBatchResponse()));
                }
            }
            navigationPropertyBulkContexts.clear();
        } else {
            if (bulkRequests.size() != 0) {
                if (!isTimedOut(BatchElapsedTimer.Lock.YIELD)) {
                    if (!shutter.isShuttered()) {
                        execBulk(responseBody, uriInfo, boundary);
                    } else {
                        // 前のブロックで503エラーが発生している場合
                        createTooManyConcurrentResponse(responseBody, boundary);
                        bulkRequests.clear();
                    }
                } else {
                    for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
                        BatchResponse res = new BatchResponse();
                        Exception exception = DcCoreException.Misc.SERVER_REQUEST_TIMEOUT;
                        res.setErrorResponse(exception);
                        // レスポンスボディ作成
                        responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
                    }
                    bulkRequests.clear();
                }
            }
        }
    }

    /**
     * bulkリクエストの実行処理.
     * @param responseBody 結果格納用
     * @param uriInfo uriInfo
     * @param boundary boundary
     */
    private void execBulk(StringBuilder responseBody, UriInfo uriInfo, String boundary) {
        EntityResponse entityRes = null;

        // データ登録を実行する
        String cellId = this.odataResource.accessContext.getCell().getId();
        List<EntityResponse> resultList = null;
        try {
            resultList = this.odataResource.getODataProducer().bulkCreateEntity(
                    this.odataResource.metadata, bulkRequests, cellId);
        } catch (DcCoreException e) {
            // 503が発生した後の処理を継続させるため、shutterにステータスを設定。
            shutter.updateStatus(e);
            if (shutter.isShuttered()) {
                createTooManyConcurrentResponse(responseBody, boundary);
                bulkRequests.clear();
                return;
            } else {
                throw e;
            }
        }

        // レスポンスを生成する
        int index = 0;
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            BatchResponse res = new BatchResponse();
            Exception exception = request.getValue().getError();
            if (exception != null) {
                res.setErrorResponse(exception);
            } else {
                // レスポンス作成
                entityRes = resultList.get(index);
                OEntity oEntity = entityRes.getEntity();

                // ステータスコード
                res.setResponseCode(HttpStatus.SC_CREATED);

                // ヘッダ情報
                String key = oEntity.getEntityKey().toKeyString();
                res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                res.setHeader(HttpHeaders.LOCATION, request.getValue().getBodyPart().getUri() + key);
                res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
                String etag = ((OEntityWrapper) oEntity).getEtag();
                if (etag != null) {
                    res.setHeader(HttpHeaders.ETAG, "W/\"" + etag + "\"");
                }

                // ボディ情報
                UriInfo resUriInfo = DcCoreUtils.createUriInfo(uriInfo, 1);
                String format = AbstractODataResource.FORMAT_JSON;
                List<MediaType> contentTypes = new ArrayList<MediaType>();
                contentTypes.add(MediaType.APPLICATION_JSON_TYPE);
                String responseStr = renderEntityResponse(resUriInfo, entityRes, format, contentTypes);
                res.setBody(responseStr);
                index++;
            }
            // レスポンスボディ作成
            responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
        }
        bulkRequests.clear();
    }

    /**
     * POSTリクエスト用のToo Many Concurrentエラー レスポンスを作成する.
     * @param responseBody
     * @param boundary
     */
    private void createTooManyConcurrentResponse(StringBuilder responseBody, String boundary) {
        for (Entry<String, BulkRequest> request : bulkRequests.entrySet()) {
            BatchResponse res = new BatchResponse();
            res.setErrorResponse(DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
            // レスポンスボディ作成
            responseBody.append(getChangesetResponseBody(boundary, request.getValue().getBodyPart(), res));
        }
    }

    /**
     * NP経由のPOSTリクエスト用のToo Many Concurrentエラー レスポンスを作成する.
     * @param responseBody
     * @param boundary
     */
    private void createTooManyConcurrentResponse(List<NavigationPropertyBulkContext> npBulkContexts) {
        for (NavigationPropertyBulkContext npBulkContext : npBulkContexts) {
            npBulkContext.setException(DcCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS);
        }
    }

    /**
     * バッチリクエストの一覧取得処理.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return レスポンス
     */
    private BatchResponse list(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        EntitiesResponse entitiesResp = null;
        try {
            // アクセス制御
            checkReadAccessContext(bodyPart);
            // NavigationProperty経由の一覧取得は 501
            if (bodyPart.hasNavigationProperty()) {
                throw DcCoreException.Misc.METHOD_NOT_IMPLEMENTED;
            }
            ODataEntitiesResource entitiesResource = new ODataEntitiesResource(this.odataResource,
                    bodyPart.getEntitySetName());

            // Entityの一覧取得
            String query = bodyPart.getRequestQuery();
            QueryInfo queryInfo = QueryParser.createQueryInfo(query);
            entitiesResp = entitiesResource.getEntities(queryInfo);

            // レスポンス作成
            res.setResponseCode(HttpStatus.SC_OK);
            // TODO 現状は、ContentTypeはJSON固定
            res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            // レスポンスボディ
            UriInfo resUriInfo = DcCoreUtils.createUriInfo(uriInfo, 1);
            StringWriter sw = new StringWriter();
            // TODO 制限事項でAcceptは無視してJSONで返却するため固定でJSONを指定する.
            List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
            acceptableMediaTypes.add(MediaType.APPLICATION_JSON_TYPE);
            // TODO 制限事項でQueryは無視するため固定でnullを指定する.
            FormatWriter<EntitiesResponse> fw = DcFormatWriterFactory.getFormatWriter(EntitiesResponse.class,
                    acceptableMediaTypes, null, null);
            UriInfo uriInfo2 = DcCoreUtils.createUriInfo(resUriInfo, 1);

            fw.write(uriInfo2, sw, entitiesResp);
            String entity = sw.toString();

            res.setBody(entity);

        } catch (Exception e) {
            res.setErrorResponse(e);
        }

        return res;
    }

    /**
     * バッチリクエストの一件取得処理.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return レスポンス
     */
    private BatchResponse retrieve(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        EntityResponse entityResp = null;
        try {
            // アクセス制御
            checkReadAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            // Entityの一件取得
            // TODO クエリ対応
            entityResp = entityResource.getEntity(null, null, null);

            // レスポンス作成
            res.setResponseCode(HttpStatus.SC_OK);
            // TODO 現状は、ContentTypeはJSON固定
            res.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            // レスポンスボディ
            UriInfo resUriInfo = DcCoreUtils.createUriInfo(uriInfo, 1);
            // TODO 現状は、ContentTypeはJSON固定
            String format = AbstractODataResource.FORMAT_JSON;
            List<MediaType> contentTypes = new ArrayList<MediaType>();
            contentTypes.add(MediaType.APPLICATION_JSON_TYPE);
            String responseStr = entityResource.renderEntityResponse(resUriInfo, entityResp, format, null);
            res.setBody(responseStr);

        } catch (Exception e) {
            res.setErrorResponse(e);
        }

        return res;
    }

    /**
     * バッチリクエストの更新処理.
     * @param bodyPart BatchBodyPart
     */
    private BatchResponse update(BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            // アクセス制御
            checkWriteAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            // Entityの更新
            Reader reader = new StringReader(bodyPart.getEntity());
            String ifMatch = bodyPart.getHttpHeaders().get(HttpHeaders.IF_MATCH);
            OEntityWrapper oew = entityResource.updateEntity(reader, ifMatch);

            // レスポンス作成
            // 特に例外があがらなければ、レスポンスを返す。
            // oewに新たに登録されたETagを返す
            String etag = oew.getEtag();
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
            res.setHeader(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag));
        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * バッチリクエストの削除処理.
     * @param bodyPart BatchBodyPart
     * @return BatchResponse
     */
    private BatchResponse delete(BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            // アクセス制御
            checkWriteAccessContext(bodyPart);

            ODataEntityResource entityResource = new ODataEntityResource(this.odataResource,
                    bodyPart.getEntitySetName(), bodyPart.getEntityKey());

            // Entityの削除
            String ifMatch = bodyPart.getHttpHeaders().get(HttpHeaders.IF_MATCH);
            entityResource.deleteEntity(ifMatch);

            // レスポンス作成
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, "2.0");
        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * バッチリクエストの$links登録処理.
     * @param uriInfo uriInfo
     * @param bodyPart BatchBodyPart
     * @return BatchResponse
     */
    private BatchResponse createLinks(UriInfo uriInfo, BatchBodyPart bodyPart) {
        BatchResponse res = new BatchResponse();
        try {
            // 存在しないエンティティセットを指定されたときは即刻エラー
            EdmEntitySet eSet = this.odataResource.metadata.findEdmEntitySet(bodyPart.getEntitySetName());
            if (eSet == null) {
                throw DcCoreException.OData.NO_SUCH_ENTITY_SET;
            }

            // アクセス制御
            checkWriteAccessContext(bodyPart);

            // $links の POSTでNav Propのキー指定があってはいけない。
            if (bodyPart.getTargetEntityKey().length() > 0) {
                throw DcCoreException.OData.KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED;
            }

            OEntityKey oeKey = OEntityKey.parse(bodyPart.getEntityKeyWithParences());
            OEntityId sourceEntityId = OEntityIds.create(bodyPart.getEntitySetName(), oeKey);

            StringReader requestReader = new StringReader(bodyPart.getEntity());
            OEntityId targetEntityId = ODataLinksResource.parseRequestUri(DcCoreUtils.createUriInfo(uriInfo, 1),
                    requestReader, bodyPart.getEntitySetName(), this.odataResource.metadata);

            this.odataResource.getODataProducer().createLink(sourceEntityId, bodyPart.getTargetEntitySetName(),
                    targetEntityId);
            // レスポンス作成
            res.setResponseCode(HttpStatus.SC_NO_CONTENT);
            res.setHeader(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);

        } catch (Exception e) {
            res.setErrorResponse(e);
            shutter.updateStatus(e);
        }

        return res;
    }

    /**
     * $batchリクエストに対して行うアクセス制御.
     * @param ac アクセスコンテキスト
     */
    private void checkAccessContext(AccessContext ac) {
        // スキーマ認証
        this.odataResource.checkSchemaAuth(this.odataResource.getAccessContext());

        // ユニットユーザトークンチェック
        if (ac.isUnitUserToken()) {
            return;
        }

        // Basic認証できるかチェック
        this.odataResource.setBasicAuthenticateEnableInBatchRequest(ac);

        // principalがALL以外の場合は、認証処理を行う
        // なお、アクセス制御は$batchリクエスト内の各MIMEパートにて行っている
        if (!this.odataResource.hasPrivilegeForBatch(ac)) {
            // トークンの有効性チェック
            // トークンがINVALIDでもACL設定でPrivilegeがallに設定されているとアクセスを許可する必要があるのでこのタイミングでチェック
            if (AccessContext.TYPE_INVALID.equals(ac.getType())) {
                ac.throwInvalidTokenException(this.odataResource.getAcceptableAuthScheme());
            } else if (AccessContext.TYPE_ANONYMOUS.equals(ac.getType())) {
                throw DcCoreAuthzException.AUTHORIZATION_REQUIRED.realm(ac.getRealm(),
                        this.odataResource.getAcceptableAuthScheme());
            }
            // $batchとして許可しないprivilegeが指定された場合はここに到達するため403エラーとする
            throw DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }
    }

    /**
     * $batchリクエスト内の各MIMEパートに対して行うアクセス制御.
     * @param ac アクセスコンテキスト
     * @param privilege 許可する権限
     */
    private void checkAccessContextForMimePart(AccessContext ac, Privilege privilege) {
        // ユニットユーザトークンチェック
        if (ac.isUnitUserToken()) {
            return;
        }

        if (!this.odataResource.hasPrivilege(ac, privilege)) {
            // $batchのリクエストに対し、すでに認証処理は実施済みのため、ここでは認可の判定のみ行う
            throw DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        }

    }

    /**
     * $batch用writeアクセス制御.
     * @param bodyPart bodyPart
     */
    private void checkWriteAccessContext(BatchBodyPart bodyPart) {

        // TODO EntitySet毎にPrivilegeの管理が必要

        Privilege priv = this.odataResource.getNecessaryWritePrivilege(bodyPart.getEntitySetName());

        BatchAccess batchAccess = writeAccess.get(priv);
        if (batchAccess == null) {
            batchAccess = new BatchAccess();
            writeAccess.put(priv, batchAccess);
            try {
                this.checkAccessContextForMimePart(this.odataResource.getAccessContext(), priv);
            } catch (DcCoreException ex) {
                batchAccess.setAccessContext(ex);
            }
        }

        batchAccess.checkAccessContext();
    }

    /**
     * $batch用readアクセス制御.
     * @param bodyPart bodyPart
     */
    private void checkReadAccessContext(BatchBodyPart bodyPart) {

        // TODO EntitySet毎にPrivilegeの管理が必要

        Privilege priv = this.odataResource.getNecessaryReadPrivilege(bodyPart.getEntitySetName());

        BatchAccess batchAccess = readAccess.get(priv);
        if (batchAccess == null) {
            batchAccess = new BatchAccess();
            readAccess.put(priv, batchAccess);
            try {
                this.checkAccessContextForMimePart(this.odataResource.getAccessContext(), priv);
            } catch (DcCoreException ex) {
                batchAccess.setAccessContext(ex);
            }
        }

        batchAccess.checkAccessContext();
    }

    /**
     * リクエストパスのNavigationProperty指定値が正しい形式かどうかをチェックする.
     * @param bodyPart bodyPart
     * @return 指定が正しい場合はtrueを、それ以外はfalseを返す
     */
    private boolean isValidNavigationProperty(BatchBodyPart bodyPart) {
        if (bodyPart.hasNavigationProperty()
                && bodyPart.getTargetNavigationProperty().indexOf("(") >= 0) { // キーありはNG
            return false;
        }
        return true;
    }

    /**
     * Batchアクセス情報を管理するクラス.
     */
    static class BatchAccess {
        private DcCoreException exception = null;

        void checkAccessContext() {
            if (this.exception != null) {
                throw this.exception;
            }
        }

        void setAccessContext(DcCoreException ex) {
            this.exception = ex;
        }

    }

    /**
     * Batchリクエストのレスポンス情報クラス.
     */
    static class BatchResponse {

        private int responseCode;
        private Map<String, String> headers = new HashMap<String, String>();
        private String body = null;

        /**
         * BatchResponseのレスポンスコードを返却する.
         * @return BatchResponseのレスポンスコード
         */
        public int getResponseCode() {
            return responseCode;
        }

        /**
         * BatchResponseのレスポンスコードを設定する.
         * @param responseCode BatchResponseのレスポンスコード
         */
        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        /**
         * BatchResponseのヘッダを返却する.
         * @return BatchResponseのヘッダ
         */
        public Map<String, String> getHeaders() {
            return headers;
        }

        /**
         * レスポンスコードの説明を取得する.
         * @return レスポンスコードの説明（例：OK, No Content）
         */
        public String getResponseMessage() {
            String message = null;

            switch (this.responseCode) {
            case HttpStatus.SC_NO_CONTENT:
                message = "No Content";
                break;

            case HttpStatus.SC_CREATED:
                message = "Created";
                break;

            case HttpStatus.SC_OK:
                message = "OK";
                break;

            default:
                message = "";
                break;
            }

            return message;
        }

        /**
         * BatchResponseのヘッダを設定する.
         * @param key BatchResponseのヘッダのキー
         * @param value BatchResponseのヘッダの値
         */
        public void setHeader(String key, String value) {
            this.headers.put(key, value);
        }

        /**
         * BatchResponseのボディを返却する.
         * @return BatchResponseのボディ
         */
        public String getBody() {
            return body;
        }

        /**
         * BatchResponseのボディを設定する.
         * @param body BatchResponseのボディ
         */
        public void setBody(String body) {
            this.body = body;
        }

        /**
         * エラー情報を設定.
         * @param res BatchResponse
         * @param e DcCoreException
         */
        void setErrorResponse(Exception e) {
            // ログ出力
            DcCoreExceptionMapper mapper = new DcCoreExceptionMapper();
            mapper.toResponse(e);

            if (e instanceof DcCoreException) {
                // レスポンス作成
                setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                setResponseCode(((DcCoreException) e).getStatus());
                setBody(createJsonBody((DcCoreException) e));
            } else {
                // レスポンス作成
                setHeader(org.apache.http.HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                setResponseCode(DcCoreException.Server.UNKNOWN_ERROR.getStatus());
                setBody(createJsonBody(DcCoreException.Server.UNKNOWN_ERROR));
            }
        }

        /**
         * Json形式のエラーメッセージを作成する.
         * @param exception DcCoreException
         * @return レスポンスボディ用Json形式エラーメッセージ
         */
        private String createJsonBody(DcCoreException exception) {
            String code = exception.getCode();
            String message = exception.getMessage();
            LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
            LinkedHashMap<String, Object> jsonMessage = new LinkedHashMap<String, Object>();
            json.put("code", code);
            jsonMessage.put("lang", ODataErrorMessage.DEFAULT_LANG_TAG);
            jsonMessage.put("value", message);
            json.put("message", jsonMessage);
            return JSONObject.toJSONString(json);
        }

    }
}
