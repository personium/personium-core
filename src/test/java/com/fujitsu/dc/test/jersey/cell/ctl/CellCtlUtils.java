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
package com.fujitsu.dc.test.jersey.cell.ctl;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * Cell管理のユーティリティークラス.
 */
public class CellCtlUtils {

    /**
     * コンストラクタ.
     */
    private CellCtlUtils() {
    };

    /**
     * 指定されたurlに従いリソースを削除する.
     * @param url URL
     */
    static void deleteOdataResource(String url) {
        DcRequest req = DcRequest.delete(url)
                .header(HttpHeaders.AUTHORIZATION, AbstractCase.BEARER_MASTER_TOKEN)
                .header(HttpHeaders.IF_MATCH, "*");
        AbstractCase.request(req);
    }

    /**
     * 指定されたボックス名にリンクされたRelationを作成する.
     * @param cellName セル名
     * @param testRelationName リレーション名
     * @param boxname ボックス名
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createRelation(String cellName, String testRelationName, String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        body.put("_Box.Name", boxname);

        return Http.request("relation-create.txt")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * ボックス名にリンクされていないRelationを作成する.
     * @param cellName セル名
     * @param testRelationName リレーション名
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createRelation(String cellName, String testRelationName) {
        JSONObject body = new JSONObject();
        body.put("Name", testRelationName);
        body.put("_Box.Name", null);

        return Http.request("relation-create.txt")
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * 指定されたボックス名にリンクされたRelationを削除する.
     * @param cellName セル名
     * @param testRelationName リレーション名
     * @param boxname ボックス名
     */
    public static void deleteRelation(String cellName, String testRelationName, String boxname) {
        Http.request("relation-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("relationname", testRelationName)
                .with("boxname", "'" + boxname + "'")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ボックス名にリンクされていないRelationを削除する.
     * @param cellName セル名
     * @param testRelationName リレーション名
     */
    public static void deleteRelation(String cellName, String testRelationName) {
        Http.request("relation-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("relationname", testRelationName)
                .with("boxname", "null")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する.
     * @param cellName セル名
     * @param testRoleName ロール名
     * @param boxname ボックス名
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createRole(String cellName, String testRoleName, String boxname) {
        // Role作成
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", boxname);
        return Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * ボックス名にリンクされていないロール情報を作成する.
     * @param cellName セル名
     * @param testRoleName ロール名
     * @return レスポンス
     */
    @SuppressWarnings("unchecked")
    public static TResponse createRole(String cellName, String testRoleName) {

        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", null);

        return Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を削除する.
     * @param cellName セル名
     * @param testRoleName ロール名
     * @param boxname ボックス名
     */
    public static void deleteRole(String cellName, String testRoleName, String boxname) {
        Http.request("role-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rolename", testRoleName)
                .with("boxname", "'" + boxname + "'")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ボックス名にリンクされていないロール情報を削除する.
     * @param cellName セル名
     * @param testRoleName ロール名
     */
    public static void deleteRole(String cellName, String testRoleName) {
        Http.request("role-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("rolename", testRoleName)
                .with("boxname", "null")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたリレーション名にリンクされたExtRole情報を作成する.
     * @param cellName セル名
     * @param testExtRoleName 外部ロール名
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     * @param relationNameEmpty _Relation.Nameを指定しない
     * @param relationBoxNameEmpty _Relation._Box.Nameを指定しない
     */
    @SuppressWarnings({"unchecked", "unused" })
    public static void createExtRole(
            String cellName,
            String testExtRoleName,
            String relationName,
            String relationBoxName,
            boolean relationNameEmpty,
            boolean relationBoxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("ExtRole", testExtRoleName);
        if (!relationNameEmpty) {
            body.put("_Relation.Name", relationName);
        }
        if (!relationBoxNameEmpty) {
            body.put("_Relation._Box.Name", relationBoxName);
        }
        TResponse response = Http.request("cell/extRole/extRole-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);
    }

    /**
     * 指定されたリレーション名にリンクされたExtRole情報を作成する.
     * @param cellName セル名
     * @param testExtRoleName 外部ロール名
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    public static void createExtRole(
            String cellName,
            String testExtRoleName,
            String relationName,
            String relationBoxName) {
        createExtRole(cellName, testExtRoleName, relationName, relationBoxName, false, false);
    }

    /**
     * 指定されたボックス名にリンクされたExtRole情報を削除する.
     * @param cellName セル名
     * @param testExtRoleName 外部ロール名
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    public static void deleteExtRole(String cellName,
            String testExtRoleName,
            String relationName,
            String relationBoxName) {

        String relName;
        if (relationName == null) {
            relName = "null";
        } else {
            relName = ("'" + relationName + "'");
        }
        String relBoxName;
        if (relationBoxName == null) {
            relBoxName = "null";
        } else {
            relBoxName = ("'" + relationBoxName + "'");
        }
        Http.request("cell/extRole/extRole-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(testExtRoleName))
                .with("relationName", relName)
                .with("relationBoxName", relBoxName)
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたボックス名にリンクされたExtRole情報を削除する.
     * @param cellName セル名
     * @param testExtRoleName 外部ロール名
     * @param relationName リレーション名
     */
    public static void deleteExtRole(String cellName,
            String testExtRoleName,
            String relationName) {
        Http.request("cell/extRole/extRole-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(testExtRoleName))
                .with("relationName", "'" + relationName + "'")
                .with("relationBoxName", "null")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * ボックス名にリンクされていないExtRole情報を削除する.
     * @param cellName セル名
     * @param testExtRoleName 外部ロール名
     */
    public static void deleteExtRole(String cellName, String testExtRoleName) {
        Http.request("cell/extRole/extRole-delete.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("extRoleName", DcCoreUtils.encodeUrlComp(testExtRoleName))
                .with("relationName", "null")
                .with("relationBoxName", "null")
                .returns()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 文字列が指定された場合はシングルクオートで囲んだ文字列を返却する.
     * @param value 文字列
     * @return シングルクオートで囲んだ文字列
     */
    public static String addSingleQuarto(String value) {
        if (value == null) {
            return "null";
        } else {
            return "'" + value + "'";
        }
    }
}
