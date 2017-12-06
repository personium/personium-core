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
package io.personium.test.jersey.box.odatacol.schema.complextype;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.json.simple.JSONObject;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.ComplexType;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;

/**
 * ComplexTypeテスト用のユーティリティクラス.
 */
public class ComplexTypeUtils {

    /** 名前空間. */
    public static final String NAMESPACE = Common.EDM_NS_ODATA_SVC_SCHEMA + "." + ComplexType.EDM_TYPE_NAME;

    private ComplexTypeUtils() {
    }

    /**
     * ComplexTypeを登録する.
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static PersoniumResponse create(
            String cell, String box, String col,
            String complexTypeName, int code) {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.complexType(cell, box, col, null));
        req.header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN);
        req.addJsonBody(ComplexType.P_COMPLEXTYPE_NAME.getName(), complexTypeName);

        // リクエスト実行
        PersoniumResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * ComplexTypeを登録する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static PersoniumResponse createWithToken(
            String token,
            String cell,
            String box,
            String col,
            String complexTypeName,
            int code) {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.post(UrlUtils.complexType(cell, box, col, null));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);
        req.addJsonBody(ComplexType.P_COMPLEXTYPE_NAME.getName(), complexTypeName);

        // リクエスト実行
        PersoniumResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

    /**
     * ComplexTypeを一件取得する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse get(
            String token,
            String cell,
            String box,
            String col,
            String complexTypeName,
            int code) {
        return Http.request("box/odatacol/get.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexType")
                .with("id", complexTypeName)
                .with("query", "")
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypeを一覧取得する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            String token,
            String cell,
            String box,
            String col,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexType")
                .with("query", "")
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypeを一覧取得する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param query 検索クエリ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse list(
            String token,
            String cell,
            String box,
            String col,
            String query,
            int code) {
        return Http.request("box/odatacol/list.txt")
                .with("token", token)
                .with("accept", "application/json")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexType")
                .with("query", query)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypeを更新する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param newComplexTypeName 更新用ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse update(
            String token,
            String cell,
            String box,
            String col,
            String complexTypeName,
            String newComplexTypeName,
            int code) {
        String body = String.format("{\"Name\":\"%s\"}", newComplexTypeName);
        return Http.request("box/odatacol/update.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexType")
                .with("ifMatch", "*")
                .with("id", complexTypeName)
                .with("body", body)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypeを更新する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param body リクエストボディ
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse update(
            String token,
            String cell,
            String box,
            String col,
            String complexTypeName,
            JSONObject body,
            int code) {
        return Http.request("box/odatacol/update.txt")
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("contentType", MediaType.APPLICATION_JSON)
                .with("cell", cell)
                .with("box", box)
                .with("collection", col + "/\\$metadata")
                .with("entityType", "ComplexType")
                .with("ifMatch", "*")
                .with("id", complexTypeName)
                .with("body", body.toJSONString())
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * ComplexTypeを削除する.
     * @param token トークン
     * @param cell Cell名
     * @param box Box名
     * @param col Collection名
     * @param complexTypeName ComplexType名
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static PersoniumResponse delete(
            String token, String cell, String box, String col, String complexTypeName, int code) {
        // リクエストパラメータ設定
        PersoniumRequest req = PersoniumRequest.delete(UrlUtils.complexType(
                cell, box, col, complexTypeName));
        req.header(HttpHeaders.AUTHORIZATION, OAuth2Helper.Scheme.BEARER + " " + token);

        // リクエスト実行
        PersoniumResponse response = AbstractCase.request(req);
        if (code != -1) {
            assertEquals(code, response.getStatusCode());
        }
        return response;
    }

}
