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
package io.personium.test.utils;

import java.util.List;

import org.json.simple.JSONObject;

/**
 * $batch用のUtilクラス.
 */
public class BatchUtils {
    private BatchUtils() {
    }

    /** バウンダリ文字列. */
    public static final String BOUNDARY = "batch_XAmu9BiJJLBa20sRWIq74jp2UlNAVueztqu";
    /** 開始バウンダリ文字列. */
    public static final String START_BOUNDARY = "--" + BOUNDARY + "\n";
    /** 終了バウンダリ文字列. */
    public static final String END_BOUNDARY = "--" + BOUNDARY + "--";

    /**
     * バッチリクエストを実行する.
     * @param token トークン(認証スキーマを含む)
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param boundary バッチのバウンダリー
     * @param body バッチのボディ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse batchRequestAnyAuthScheme(String token,
            String cellName,
            String boxName,
            String colName,
            String boundary,
            String body,
            int code) {
        return Http.request("box/odatacol/batch-anyAuthSchema.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", boundary)
                .with("token", token)
                .with("body", body)
                .returns()
                .statusCode(code);
    }

    /**
     * GETリクエストのBody取得.
     * @param path パス
     * @return GETリクエストのBody
     */
    public static String retrieveGetBody(String path) {
        return "Content-Type: application/http\n"
                + "Content-Transfer-Encoding:binary\n\n"
                + "GET " + path + "\n"
                + "Host: host\n\n";
    }

    /**
     * INTデータを含むPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @return POSTリクエストのBody
     */
    public static String retrievePostBodyIntData(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"__id\":\"" + id + "\",\"Name\":\"testName\",\"intProperty\":123}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * POSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @return POSTリクエストのBody
     */
    public static String retrievePostBody(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * Body指定のPOSTリクエストのBody取得.
     * @param path パス
     * @param body bodyのJSONオブジェクト
     * @return POSTリクエストのBody
     */
    public static String retrievePostWithBody(String path, JSONObject body) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + body.toJSONString() + "\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * Body指定なしのPOSTリクエストのBody取得.
     * @param path パス
     * @return POSTリクエストのBody
     */
    public static String retrievePostNoneBody(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * $linkのPOSTリクエストのBody取得.
     * @param method メソッド
     * @param path パス
     * @param body 文字列型のbody
     * @return リクエストのBody
     */
    public static String retrieveLinksPostBody(String method, String path, String body) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + method + " " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + body + "\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * 指定された数の動的プロパティを登録するPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @param propertyNum プロパティ数
     * @return POSTリクエストのBody
     */
    @SuppressWarnings("unchecked")
    public static String retrievePostBodyOfProperty(String path, String id, int propertyNum) {
        JSONObject body = new JSONObject();
        body.put("__id", id);

        for (int i = 0; i < propertyNum; i++) {
            body.put(String.format("dynamicProperty%d", i), "dynamicPropertyValue" + i);
        }
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + body.toJSONString() + "\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * ID指定なしのリクエストのBody取得.
     * @param path パス
     * @param method メソッド
     * @return リクエストのBody
     */
    public static String retrievePostBodyNoId(String path, String method) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + method + " " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * JSONフォーマットエラーとなるPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @return リクエストのBody
     */
    public static String retrievePostBodyJsonFormatError(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * 指定されたバウンダリを使用するPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @param boundaryHeader バウンダリヘッダ
     * @return リクエストのBody
     */
    public static String retrievePostBodyBoundaryHeaderError(String path, String id, String boundaryHeader) {
        return boundaryHeader
                + "\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * 指定されたチェンジセットを使用するPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @param changesetHeader チェンジセット
     * @return リクエストのBody
     */
    public static String retrievePostBodyChangesetHeaderError(String path, String id, String changesetHeader) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + changesetHeader
                + "\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * チェンジセットがネストしたPOSTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @return リクエストのBody
     */
    public static String retrieveNestChangesetBody(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: multipart/mixed;"
                + " boundary=changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz\n"
                + "Content-Length: 995\n\n"
                + "--changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_ADUsdsfNmrFSDsd3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBz--\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * DELETEリクエストのBody取得.
     * @param path パス
     * @return リクエストのBody
     */
    public static String retrieveDeleteBody(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "DELETE " + path + " HTTP/1.1\n"
                + "Host: \n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * LISTリクエストのBody取得.
     * @param path パス
     * @return リクエストのBody
     */
    public static String retrieveListBody(String path) {
        return "Content-Type: application/http\n"
                + "Content-Transfer-Encoding:binary\n\n"
                + "GET " + path + "\n"
                + "Host: host\n\n";
    }

    /**
     * PUTリクエストのBody取得.
     * @param path パス
     * @return リクエストのBody
     */
    public static String retrievePutBody(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testNameUpdated\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * PUTリクエストのBody取得.
     * @param path パス
     * @param body JSON型のbody
     * @return リクエストのBody
     */
    public static String retrievePutBody(String path, JSONObject body) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + body.toJSONString() + "\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * 数値型を含むPUTリクエストのBody取得.
     * @param path パス
     * @return リクエストのBody
     */
    public static String retrievePutBodyIntData(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testNameUpdated\",\"intProperty\":\"123abc\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * フィールドエラーとなるPUTリクエストのBody取得.
     * @param path パス
     * @param unitBody フィールドエラーを発生されるフィールド名
     * @return リクエストのBody
     */
    public static String retrievePutBodyFieledInvalidError(String path, String unitBody) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testName\", \"" + unitBody + "\":\"\\\\/Date(0)\\\\/\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * メタデータを指定することによってフィールドエラーとなるPUTリクエストのBody取得.
     * @param path パス
     * @return リクエストのBody
     */
    public static String retrievePutBodyMetadataFieledInvalidError(String path) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testName\", \"__metadata\":\"test\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * POSTとPUTリクエストのBody取得.
     * @param path パス
     * @param id ID
     * @return リクエストのBody
     */
    public static String retrieveMultiRequestBody(String path, String id) {
        return "Content-Type: multipart/mixed;"
                + " boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Length: 995\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "POST " + path + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "Content-Length: 38\n\n"
                + "{\"__id\":\"" + id + "\",\"Name\":\"testName\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "PUT " + path + "('" + id + "')" + " HTTP/1.1\n"
                + "Host:\n"
                + "Connection: close\n"
                + "Accept: application/json\n"
                + "Content-Type: application/json\n"
                + "If-Match: *\n"
                + "Content-Length: 38\n\n"
                + "{\"Name\":\"testNameUpdated\"}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";

    }

    /**
     * DELETEレスポンスのBody取得.
     * @return レスポンスBody
     */
    public static String retrieveDeleteResBody() {
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1\\.1 204 No Content\n"
                + "DataServiceVersion: 2\\.0\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * PUTレスポンスのBody取得.
     * @return レスポンスBody
     */
    public static String retrievePutResBody() {
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 204 No Content\n"
                + "ETag: .*\n"
                + "DataServiceVersion: 2.0\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * 400エラー時のPUTレスポンスのBody取得.
     * @return レスポンスBody
     */
    public static String retrievePutResBody400() {
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 400 \n"
                + "Content-Type: application/json\n\n"
                + "\\{\"code\":\"PR400-OD-0006\",\"message\":\\{\"lang\":\"en\","
                + "\"value\":\"\\[intProperty\\] field format error.*\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * POSTレスポンスのBody取得.
     * @param cell セル
     * @param box ボックス
     * @param col コレクション
     * @param entitySetName エンティティ
     * @param id ID
     * @return レスポンスBody
     */
    public static String retrievePostResBody(String cell, String box, String col, String entitySetName, String id) {
        return retrievePostResBody(cell, box, col, entitySetName, id, true);
    }

    /**
     * POSTレスポンスのBody取得.
     * @param cell セル
     * @param box ボックス
     * @param col コレクション
     * @param entitySetName エンティティ
     * @param id ID
     * @param isTerminal isTerminal
     * @return レスポンスBody
     */
    public static String retrievePostResBody(String cell,
            String box,
            String col,
            String entitySetName,
            String id,
            boolean isTerminal) {
        String uri = UrlUtils.userData(cell, box, col, entitySetName + "\\('" + id + "'\\)");
        // TODO ボディのURIのチェック
        String terminal = "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
        if (!isTerminal) {
            terminal = "";
        }
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 201 Created\n"
                + "ETag: .*\n"
                + "DataServiceVersion: 2.0\n"
                + "Content-Type: application/json\n"
                + "Location: " + uri + "\n\n"
                + "\\{\"d\":\\{\"results\":\\{.*\"__metadata\":"
                + "\\{.*,\"etag\":\".*\",\"type\":\".+" + entitySetName + "\"}.*\"__id\":\"" + id
                + "\",.*\\}\\}\\}\n\n"
                + terminal;
    }

    /**
     * $link POSTレスポンスのBody取得.
     * @return レスポンスBody
     */
    public static String retrieveLinksPostResBody() {
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 204 No Content\n"
                + "DataServiceVersion: 2.0\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * cahgesetのエラーレスポンスのBody取得.
     * @param code レスポンスコード
     * @return レスポンスBody
     */
    public static String retrieveChangeSetResErrorBody(int code) {
        // TODO ボディメッセージのチェック
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 " + Integer.toString(code) + " \n"
                + "Content-Type: application/json\n\n"
                + ".*\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

    /**
     * GETレスポンスのBody取得.
     * @param type タイプ
     * @param id ID
     * @return レスポンスBody
     */
    public static String retrieveGetResBody(String type, String id) {
        // TODO ボディのURIのチェック
        return "Content-Type: application/http\n\n"
                + "HTTP/1\\.1 200 OK\n"
                + "DataServiceVersion: 2\\.0\n"
                + "Content-Type: application/json\n\n"
                + "\\{\"d\":\\{\"results\":\\{.*\"__metadata\":"
                + "\\{.*\"etag\":\".*\",\"type\":\".+" + type + "\".*\"\\}.*\\}\\}\\}\n\n";
    }

    /**
     * クエリエラー時のGETレスポンスのBody取得.
     * @param code レスポンスコード
     * @return レスポンスBody
     */
    public static String retrieveQueryOperationResErrorBody(int code) {
        // TODO ボディのURIのチェック
        return "Content-Type: application/http\n\n"
                + "HTTP/1.1 " + Integer.toString(code) + " \n"
                + "Content-Type: application/json\n\n"
                + ".*\n\n";
    }

    /**
     * LISTレスポンスのBody取得.
     * @return レスポンスBody
     */
    public static String retrieveListResBody() {
        StringBuilder res = new StringBuilder();
        res.append("Content-Type: application/http\n\n");
        res.append("HTTP/1.1 200 OK\n");
        res.append("DataServiceVersion: 2.0\n");
        res.append("Content-Type: application/json\n\n");
        res.append("\\{\"d\":\\{\"results\":\\[.*");
        res.append("\\]\\}\\}\n\n");
        return res.toString();
    }

    /**
     * LISTレスポンスのBody取得.
     * @param idPrefix IDのプレフィックス
     * @return レスポンスBody
     */
    public static String retrieveListResBody(List<String> idPrefix) {
        StringBuilder res = new StringBuilder();
        // TODO ボディのURIのチェック
        res.append("Content-Type: application/http\n\n");
        res.append("HTTP/1.1 200 OK\n");
        res.append("DataServiceVersion: 2.0\n");
        res.append("Content-Type: application/json\n\n");
        res.append("\\{\"d\":\\{\"results\":\\[");
        if (idPrefix != null) {
            for (int i = 0; i < idPrefix.size(); i++) {
                res.append("\\{.*\"__id\":\"" + idPrefix.get(i) + "[0-9]+\".*");
                res.append("\\}");
                if (i != idPrefix.size() - 1) {
                    res.append(",");
                }
            }
        }
        res.append("");
        res.append("\\]\\}\\}\n\n");
        return res.toString();
    }

    /**
     * クエリエラー時のGETレスポンスのBody取得.
     * @param idPrefix IDのプレフィックス
     * @param navPropName NavigationProperty名
     * @param navIdPrefix NavigationProperty側のIDのプレフィックス
     * @return レスポンスBody
     */
    public static String retrieveListResBodyWithExpand(List<String> idPrefix,
            List<String> navPropName,
            List<String> navIdPrefix) {
        StringBuilder res = new StringBuilder();
        // TODO ボディのURIのチェック
        res.append("Content-Type: application/http\n\n");
        res.append("HTTP/1.1 200 OK\n");
        res.append("DataServiceVersion: 2.0\n");
        res.append("Content-Type: application/json\n\n");
        res.append("\\{\"d\":\\{\"results\":\\[");
        if (idPrefix != null) {
            for (int i = 0; i < idPrefix.size(); i++) {
                res.append("\\{.*\"__id\":\"" + idPrefix.get(i) + "[0-9]+\".*");
                if (navPropName != null) {
                    for (int j = 0; j < navPropName.size(); j++) {
                        res.append("\"_" + navPropName.get(j) + "\":\\[");
                        res.append("\\{.*\"__id\":\"" + navIdPrefix.get(j) + "[0-9]+\".*");
                        res.append("\\}");
                        if (i != idPrefix.size() - 1) {
                            res.append(",");
                        }
                    }
                    res.append("\\]");
                }
                res.append("\\}");
                if (i != idPrefix.size() - 1) {
                    res.append(",");
                }
            }
        }
        res.append("");
        res.append("\\]\\}\\}\n\n");
        return res.toString();
    }

    /**
     * LISTレスポンスのBody取得.
     * @param idPrefix IDのプレフィックス
     * @param size サイズ
     * @return レスポンスBody
     */
    public static String retrieveListResBodyWithCount(List<String> idPrefix, int size) {
        StringBuilder res = new StringBuilder();
        // TODO ボディのURIのチェック
        res.append("Content-Type: application/http\n\n");
        res.append("HTTP/1.1 200 OK\n");
        res.append("DataServiceVersion: 2.0\n");
        res.append("Content-Type: application/json\n\n");
        res.append("\\{\"d\":\\{\"results\":\\[");
        if (idPrefix != null) {
            for (int i = 0; i < idPrefix.size(); i++) {
                res.append("\\{.*\"__id\":\"" + idPrefix.get(i) + "[0-9]+\".*");
                res.append("\\}");
                if (i != idPrefix.size() - 1) {
                    res.append(",");
                }
            }
        }
        res.append("");
        res.append("\\],\"__count\":\"" + size + "\"}\\}\n\n");
        return res.toString();
    }

    /**
     * SupplierのLISTレスポンスのBody取得.
     * @param idPrefix IDのプレフィックス
     * @return レスポンスBody
     */
    public static String retrieveListSupplierResBody(List<String> idPrefix) {
        StringBuilder res = new StringBuilder();
        // TODO ボディのURIのチェック
        res.append("Content-Type: application/http\n\n");
        res.append("HTTP/1.1 200 OK\n");
        res.append("DataServiceVersion: 2.0\n");
        res.append("Content-Type: application/json\n\n");
        res.append("\\{\"d\":\\{\"results\":\\[");
        for (int i = 0; i < idPrefix.size(); i++) {
            res.append("\\{.*\"__id\":\"" + idPrefix.get(i) + "[0-9]+\".*");
            res.append("\"_Product\":\\{");

            res.append("\"__deferred\":\\{");
            res.append("\"uri\":\".*\"");
            res.append("\\}");
            res.append("\\},\"_Sales\":\\{");
            res.append("\"__deferred\":\\{");
            res.append("\"uri\":\".*\"");
            res.append("\\}");
            res.append("\\}");

            res.append("\\}");
            if (i != idPrefix.size() - 1) {
                res.append(",");
            }
        }
        res.append("");
        res.append("\\]\\}\\}\n\n");
        return res.toString();
    }

    /**
     * 複数リクエストのレスポンスのBody取得.
     * @param id id
     * @param uri uri
     * @return レスポンスBody
     */
    public static String retrieveMultiRequestResBody(String id, String uri) {
        // TODO ボディのURIのチェック
        return "Content-Type: multipart/mixed; "
                + "boundary=changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 201 Created\n"
                + "ETag: .*\n"
                + "DataServiceVersion: 2.0\n"
                + "Location: " + uri + "\n"
                + "Content-Type: application/json\n\n"
                + "\\{\"d\":\\{\"results\":\\{.*\"__metadata\":"
                + "\\{.*\"etag\":\".*\",\"type\":\".+Supplier\".*\\}.*\"__id\":\"" + id + "\".*\\}\\}\\}\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb\n"
                + "Content-Type: application/http\n"
                + "Content-Transfer-Encoding: binary\n\n"
                + "HTTP/1.1 204 No Content\n"
                + "ETag: .*\n"
                + "DataServiceVersion: 2.0\n\n"
                + "--changeset_cLzcDEEVPwvvoxS3yJTFTpRauSK_FAQ6mQtyo0aby93-SDP3lAs2A19a2uBb--\n\n";
    }

}
