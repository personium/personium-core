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
package com.fujitsu.dc.test.utils;

import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.auth.OAuth2Helper;
import com.fujitsu.dc.core.model.ctl.Account;
import com.fujitsu.dc.core.model.ctl.ExtCell;
import com.fujitsu.dc.core.model.ctl.ExtRole;
import com.fujitsu.dc.core.model.ctl.Relation;
import com.fujitsu.dc.core.model.ctl.Role;
import com.fujitsu.dc.test.jersey.bar.BarInstallTestUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;

/**
 * Httpリクエストドキュメントを利用するユーティリティ.
 */
public class ResourceUtils {
    private static final long EXPIRE = (long) 24 * 60 * 60;

    private ResourceUtils() {
    }

    /**
     * 自分セルトークンの取得.
     * @param cell セル名
     * @param account アカウント名
     * @param pass パスワード
     * @return トークン
     */
    public static String getMyCellLocalToken(String cell, String account, String pass) {
        JSONObject json = ResourceUtils.getLocalTokenByPassAuth(cell, account, pass, -1);
        assertEquals(EXPIRE, json.get(OAuth2Helper.Key.REFRESH_TOKEN_EXPIRES_IN));
        return (String) json.get("access_token");
    }

    /**
     * パスワード認証-自分セルローカルトークン取得のユーティリティ.
     * @param cellName セル名
     * @param account アカウント名
     * @param pass パスワード
     * @param code 期待するレスポンスコード
     * @return トークン
     */
    public static JSONObject getLocalTokenByPassAuth(String cellName, String account, String pass, int code) {
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", account)
                .with("password", pass)
                .returns()
                .statusCode(code);
        JSONObject json = res.bodyAsJson();
        return json;
    }

    /**
     * $metadataを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path パス
     * @return レスポンス
     */
    public static TResponse getMetadata(String token, int code, String path) {
        TResponse res = Http.request("box/$metadata-$metadata-get.txt")
                .with("path", path)
                .with("col", "setodata")
                .with("accept", "application/xml")
                .with("token", token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * $metadataのOPTIONS実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse optionsMetadata(String token, int code) {
        TResponse res = Http.request("box/$metadata-$metadata-options.txt")
                .with("path", "\\$metadata")
                .with("accept", "application/xml")
                .with("token", token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * ユーザーデータのリンク情報を削除する.
     * @param userDataId 削除対象ID
     * @param navPropId 削除対象のNavigationPropertyのID
     * @param navProp 削除対象のNavigationPropertyのEntityType
     * @param cell 削除対象Cell
     * @param box 削除対象Box
     * @param col 削除対象Collection
     * @param entity 削除対象EntityTYpe
     * @param code コード
     * @return レスポンス
     */
    public static TResponse deleteUserDataLinks(String userDataId,
            String navPropId,
            String navProp,
            String cell,
            String box,
            String col,
            String entity,
            int code) {
        // リクエスト実行
        TResponse res = Http.request("box/odatacol/delete-link.txt").with("cell", cell).with("box", box)
                .with("collection", col).with("entityType", entity).with("id", userDataId)
                .with("navProp", "_" + navProp).with("navKey", navPropId)
                .with("contentType", MediaType.APPLICATION_JSON).with("token", DcCoreConfig.getMasterToken())
                .with("ifMatch", "*").returns().statusCode(code);
        return res;
    }

    /**
     * barファイルインストール.
     * @param token トークン
     * @param cellName セル名
     * @param boxName ボックス名
     * @param code レスポンスコード
     * @return response レスポンス情報
     */
    public static TResponse barInstall(final String token,
            final String cellName, final String boxName, final int code) {
        File barFile = new File("requestData/barInstall/V1_1_2_bar_acltest.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);

        Http client = Http.request("bar-install.txt")
                .with("cellPath", cellName)
                .with("path", boxName)
                .with("token", "Bearer " + token)
                .with("contType", "application/zip")
                .with("contLength", String.valueOf(body.length));
        client.setBodyBinary(body);
        TResponse response = client.returns()
                .statusCode(code)
                .debug();
        return response;
    }

    /**
     * BODY付き$linkのユーティリティ.
     * @param cellName cellName
     * @param entitySet entitySet
     * @param key key
     * @param boxName boxName
     * @param navProp navProp
     * @param uri uri
     * @param token token
     * @param code レスポンスコード
     */
    public static void linksWithBody(final String cellName,
            final String entitySet, final String key, final String boxName, final String navProp, final String uri,
            final String token, int code) {
        String keyPredicate = "Name='" + key + "'";
        if ("null".equals(boxName)) {
            keyPredicate += ",_Box.Name=" + boxName + "";
        } else if (boxName != null) {
            keyPredicate += ",_Box.Name='" + boxName + "'";
        }

        // アカウント・ロールの紐付け
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("cellPath", cellName)
                .with("entitySet", entitySet)
                .with("key", keyPredicate)
                .with("navProp", "_" + navProp)
                .with("token", token)
                .with("uri", uri)
                .returns()
                .statusCode(code);
    }

    /**
     * ユーザデータの$linksを登録する.
     * @param srcEntity リンク元エンティティタイプ名
     * @param srcUserId リンク元ユーザデータID
     * @param targetEntity リンク先エンティティタイプ名
     * @param targetUserId リンク先ユーザデータID
     * @param code 期待するステータスコード
     */
    public static void linksUserData(String srcEntity, String srcUserId,
             String targetEntity, String targetUserId, int code) {

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + targetEntity + "('" + targetUserId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", srcEntity + "('" + srcUserId + "')")
                .with("trgPath", targetEntity)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(code);
    }

    /**
     * $linkの削除ユーティリティ.
     * @param cellName cellName
     * @param entitySet entitySet
     * @param key key
     * @param boxName boxName
     * @param navProp navProp
     * @param navKey navKey
     * @param token token
     */
    public static void linksDelete(final String cellName,
            final String entitySet, final String key, final String boxName,
            final String navProp, final String navKey, final String token) {
        String keyPredicate = "Name='" + key + "'";
        if ("null".equals(boxName)) {
            keyPredicate += ",_Box.Name=" + boxName + "";
        } else if (boxName != null) {
            keyPredicate += ",_Box.Name='" + boxName + "'";
        }

        TResponse res = Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("cellPath", cellName)
                .with("entitySet", entitySet)
                .with("key", keyPredicate)
                .with("navProp", "_" + navProp)
                .with("navKey", navKey)
                .with("token", token)
                .returns();
        if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return;
        } else {
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        }
    }

    /**
     * ExtRoleとロールの結びつけユーティリティ.
     * @param cellName cellName
     * @param extRole ExtRole
     * @param relationName relationName
     * @param relationBoxName relationBoxName
     * @param roleUrl roleUrl
     * @param token token
     */
    public static void linksExtRoleToRole(final String cellName, final String extRole,
            final String relationName, final String relationBoxName, final String roleUrl, final String token) {
        String keyPredicate = String.format("ExtRole='%s',_Relation.Name=%s,_Relation._Box.Name=%s",
                extRole, relationName, relationBoxName);

        // アカウント・ロールの紐付けC
        Http.request("links-request-with-body.txt")
                .with("method", "POST")
                .with("cellPath", cellName)
                .with("entitySet", ExtRole.EDM_TYPE_NAME)
                .with("key", keyPredicate)
                .with("navProp", "_" + Role.EDM_TYPE_NAME)
                .with("token", token)
                .with("uri", roleUrl)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ExtRoleとロールの結びつけ削除.
     * @param cellName cellName
     * @param extRole ExtRole
     * @param relationName relationName
     * @param relationBoxName relationBoxName
     * @param roleName roleName
     * @param token token
     */
    public static void linksDeleteExtRoleToRole(final String cellName, final String extRole,
            final String relationName, final String relationBoxName, final String roleName, final String token) {
        String keyPredicate = null;
        if ("null".equals(relationName)) {
            keyPredicate = String.format("ExtRole='%s',_Relation.Name=%s,_Relation._Box.Name=%s",
                    extRole, relationName, relationBoxName);
        } else {
            keyPredicate = String.format("ExtRole='%s',_Relation.Name='%s',_Relation._Box.Name=%s",
                    extRole, relationName, relationBoxName);
        }
        String navKey = String.format("_Box.Name=%s,Name='%s'", relationBoxName, roleName);

        TResponse res = Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("cellPath", cellName)
                .with("entitySet", ExtRole.EDM_TYPE_NAME)
                .with("key", keyPredicate)
                .with("navProp", "_" + Role.EDM_TYPE_NAME)
                .with("navKey", navKey)
                .with("token", token)
                .returns();
        if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return;
        } else {
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        }
    }

    /**
     * アカウントとロールの結びつけ.
     * @param cellName cellName
     * @param token token
     * @param testAccountName testAccountName
     * @param boxName boxName
     * @param roleName roleName
     * @param code レスポンスコード
     */
    public static void linkAccountRole(final String cellName, final String token,
            final String testAccountName, final String boxName, final String roleName, int code) {
        String roleKeyPredicate = "Name='" + roleName + "'";
        if (boxName != null) {
            roleKeyPredicate += ",_Box.Name='" + boxName + "'";
        }
        String roleUrl = UrlUtils.cellCtl(cellName, "Role");
        roleUrl += "(" + roleKeyPredicate + ")";

        // アカウント・ロールの紐付けC
        Http.request("link-account-role.txt")
                .with("token", token)
                .with("cellPath", cellName)
                .with("username", testAccountName)
                .with("roleUrl", roleUrl)
                .returns()
                .statusCode(code);
    }

    /**
     * アカウントとロールの結びつけを削除.
     * @param cellName cellName
     * @param token token
     * @param testAccountName testAccountName
     * @param boxName boxName
     * @param roleName roleName
     */
    public static void linkAccountRollDelete(final String cellName, final String token,
            final String testAccountName, final String boxName, final String roleName) {
        String roleKey = null;
        if (boxName == null) {
            roleKey = "_Box.Name=null,Name='" + roleName + "'";
        } else {
            roleKey = "_Box.Name='" + boxName + "',Name='" + roleName + "'";
        }

        TResponse res = Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", token)
                .with("cellPath", cellName)
                .with("entitySet", Account.EDM_TYPE_NAME)
                .with("key", "'" + testAccountName + "'")
                .with("navProp", "_" + Role.EDM_TYPE_NAME)
                .with("navKey", roleKey)
                .returns();
        if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return;
        } else {
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        }
    }

    /**
     * extCellとロールの結びつけを削除.
     * @param cellName cellName
     * @param token token
     * @param testExtCellName testExtCellName
     * @param boxName boxName
     * @param roleName roleName
     */
    public static void linkExtCellRoleDelete(final String cellName, final String token,
            final String testExtCellName, final String boxName, final String roleName) {
        String roleKey = null;
        if (boxName == null) {
            roleKey = "_Box.Name=null,Name='" + roleName + "'";
        } else {
            roleKey = "_Box.Name='" + boxName + "',Name='" + roleName + "'";
        }
        TResponse res = Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", token)
                .with("cellPath", cellName)
                .with("entitySet", ExtCell.EDM_TYPE_NAME)
                .with("key", "'" + testExtCellName + "'")
                .with("navProp", "_" + Role.EDM_TYPE_NAME)
                .with("navKey", roleKey)
                .returns();
        if (res.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return;
        } else {
            assertEquals(HttpStatus.SC_NO_CONTENT, res.getStatusCode());
        }
    }

    /**
     * extCellとRelationの結びつけを削除.
     * @param token token
     * @param cellName cellName
     * @param testExtCellName testExtCellName
     * @param relationName relationName
     * @param expectedStatusCode expectedStatusCode
     */
    public static void linkExtCellRelationDelete(final String token, final String cellName,
            final String testExtCellName, final String relationName, final int expectedStatusCode) {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", token)
                .with("cellPath", cellName)
                .with("entitySet", ExtCell.EDM_TYPE_NAME)
                .with("key", "'" + DcCoreUtils.encodeUrlComp(testExtCellName) + "'")
                .with("navProp", "_" + Relation.EDM_TYPE_NAME)
                .with("navKey", "'" + relationName + "'")
                .returns()
                .debug()
                .statusCode(expectedStatusCode);
    }

    /**
     * ExtCellとロールの結びつけ.
     * @param cellName 結びつけるセル名
     * @param cellPath リクエストを投げるセル
     * @param roleUrl 結びつけるロールURL
     * @param token トークン
     * @param code レスポンスコード
     * @return レスポンス
     */
    public static TResponse linkExtCelltoRole(final String cellName, final String cellPath, final String roleUrl,
            final String token, final int code) {
        TResponse tresponseWebDav = null;
        // DAVコレクションの作成
        tresponseWebDav = Http.request("cell/link-extCell-role.txt")
                .with("cellPath", cellPath)
                .with("cellName", cellName)
                .with("token", token)
                .with("roleUrl", roleUrl)
                .returns();
        tresponseWebDav.statusCode(code);
        return tresponseWebDav;
    }

    /**
     * リソースに対しGETを行うユーティリティー.
     * @param path パス
     * @param token トークン
     * @param status ステータス
     * @param boxName box名
     * @param cellPath セル名
     * @return レスポンス
     */
    public static TResponse accessResource(String path, String token, int status, String boxName, String cellPath) {
        // リソースに対してGETアクセス
        TResponse tresponse = Http.request("box/dav-get.txt")
                .with("cellPath", cellPath)
                .with("path", path)
                .with("token", token)
                .with("box", boxName)
                .returns()
                .statusCode(status);
        return tresponse;
    }

    /**
     * リソースに対し、トークンなしでGETを行うユーティリティー.
     * @param path パス
     * @param status ステータス
     * @param cellPath セル名
     * @return レスポンス
     */
    public static TResponse accessResourceNoAuth(String path, int status, String cellPath) {
        // リソースに対してGETアクセス
        TResponse tresponse = Http.request("box/dav-get-noAuthHeader.txt")
                .with("cellPath", cellPath)
                .with("path", path)
                .returns()
                .statusCode(status);
        return tresponse;
    }

    /**
     * OPTIONSを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse options(String token, String path, int code) {
        TResponse res = Http.request("options.txt")
                .with("path", path)
                .with("token", token)
                .returns()
                .debug()
                .statusCode(code);
        return res;
    }

    /**
     * OPTIONSを実行し、レスポンスコードをチェックする.
     * @param cellName セル名
     * @param token トークン
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse optionsUnderBox1(String cellName, String token, String path, int code) {
        TResponse res = Http.request("box/dav-options.txt")
                .with("cellPath", cellName)
                .with("path", path)
                .with("token", "Bearer " + token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * OPTIONSを実行し、レスポンスコードをチェックする.
     * @param cellName セル名
     * @param token トークン(認証スキーマを含めて指定する)
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse optionsWithAnyAuthSchema(String cellName, String token, String path, int code) {
        TResponse res = Http.request("box/dav-options.txt")
                .with("cellPath", cellName)
                .with("path", path)
                .with("token", token)
                .returns()
                .debug()
                .statusCode(code);
        return res;
    }

    /**
     * GETを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @param cellPath アクセスするセル名
     * @param boxName box名
     */
    public static void retrieve(String token, String path, int code, String cellPath, String boxName) {
        Http.request("box/dav-get.txt")
                .with("cellPath", cellPath)
                .with("path", path)
                .with("box", boxName)
                .with("token", token)
                .returns()
                .statusCode(code);
    }

    /**
     * PUTを実行し、レスポンスコードをチェックする.
     * @param token トークン
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @param cellPath アクセスするセル名
     * @param boxName box名
     */
    public static void put(String token, String path, int code, String cellPath, String boxName) {
        Http.request("box/dav-put.txt")
                .with("cellPath", cellPath)
                .with("path", path)
                .with("token", token)
                .with("box", boxName)
                .with("contentType", MediaType.TEXT_PLAIN)
                .with("source", "this is resource.")
                .returns()
                .statusCode(code);
    }

    /**
     * DELETEを実行し、レスポンスコードをチェックする.
     * @param fileName リソースファイル名
     * @param cell セル名
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @param path 対象のコレクションのパス
     */
    public static void delete(String fileName, String cell, String token, int code, String path) {
        Http.request(fileName)
                .with("cellPath", cell)
                .with("path", path)
                .with("token", token)
                .returns()
                .statusCode(code);
    }

    /**
     * REPORTを実行し、レスポンスコードをチェックする.
     * @param cellName セル名
     * @param token トークン
     * @param path アクセスするパス
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse report(String cellName, String token, String path, int code) {
        TResponse res = Http.request("box/report.txt")
                .with("cellPath", cellName)
                .with("path", path)
                .with("token", token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * 任意リクエストを実行し、レスポンスコードをチェックする.
     * @param method リクエストメソッド
     * @param url リクエストURL
     * @param token トークン
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse requestUtil(String method, String token, String url, int code) {
        TResponse res = Http.request("request-util.txt")
                .with("method", method)
                .with("url", url)
                .with("token", "Bearer " + token)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * 任意リクエストを実行し、レスポンスコードをチェックする.
     * @param method リクエストメソッド
     * @param url リクエストURL
     * @param authorization Authorizationヘッダの値(auth-schemaを含む文字列)
     * @param code 期待するレスポンスコード
     * @return レスポンス
     */
    public static TResponse requestUtilWithAuthSchema(String method, String authorization, String url, int code) {
        TResponse res = Http.request("request-util.txt")
                .with("method", method)
                .with("url", url)
                .with("token", authorization)
                .returns()
                .statusCode(code);
        return res;
    }

    /**
     * リフレッシュトークン認証を実行するユーティリティー.
     * @param cellName セル名
     * @param refreshToken リフレッシュトークン
     * @return レスポンス
     */
    public static TResponse refreshTokenAuthCl(String cellName, String refreshToken) {
        TResponse res = Http.request("authn/refresh-cl.txt")
                .with("remoteCell", cellName)
                .with("refresh_token", refreshToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res;
    }

    /**
     * ログ情報取得(PROPFIND).
     * @param cellName セル名
     * @param collection 取得対象のコレクション(current or archive)
     * @param depth Depthヘッダーの値
     * @param accessToken アクセストークン
     * @param code チェックするレスポンスコード
     * @return レスポンス情報
     */
    public static TResponse logCollectionPropfind(String cellName,
            String collection,
            String depth,
            String accessToken,
            int code) {
        return Http.request("cell/log-propfind-with-nobody.txt")
                .with("METHOD", com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND)
                .with("token", accessToken)
                .with("cellPath", cellName)
                .with("collection", collection)
                .with("depth", depth)
                .returns()
                .statusCode(code);
    }
}
