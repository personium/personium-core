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
package com.fujitsu.dc.test.jersey.box.odatacol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserDataのNavigationProperty経由一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataListWithNPTest extends AbstractUserDataWithNP {

    int topMaxNum = DcCoreConfig.getTopQueryMaxSize();
    int skipMaxNum = DcCoreConfig.getSkipQueryMaxSize();

    /**
     * コンストラクタ.
     */
    public UserDataListWithNPTest() {
        super();
    }

    /**
     * UserDataのNavigationProperty経由でID指定を行い取得を実行して400が取得できること.
     */
    @Test
    public final void UserDataのNavigationProperty経由でID指定を行い取得を実行して400が取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_A);

            // ユーザデータの一覧取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + ENTITY_TYPE_A + "('parent')")
                    .with("entityType", "_" + ENTITY_TYPE_B + "('xxx')")
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * 存在しないNavigationPropertyを指定してUserDataのNavigationProperty経由で取得を行い404が取得できること.
     */
    @Test
    public final void 存在しないNavigationPropertyを指定してUserDataのNavigationProperty経由で取得を行い404が取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_A);

            // ユーザデータの一覧取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + ENTITY_TYPE_A + "('parent')")
                    .with("entityType", "_test")
                    .with("query", "")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_FOUND)
                    .debug();
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * 親データが存在しない状態でUserDataをNavigationProperty経由で取得して404が取得できること.
     */
    @Test
    public final void 親データが存在しない状態でUserDataをNavigationProperty経由で取得して404が取得できること() {
        // ユーザデータの一覧取得
        Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + ENTITY_TYPE_A + "('test')")
                .with("entityType", "_" + ENTITY_TYPE_B)
                .with("query", "")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_NOT_FOUND)
                .debug();
    }

    /**
     * データが登録されていない状態でZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_A);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_B);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent", DcCoreConfig.getMasterToken(), -1);

        }
    }

    /**
     * データが登録されていない状態でZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_A);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_C);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent", DcCoreConfig.getMasterToken(), -1);

        }
    }

    /**
     * データが登録されていない状態でZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_A);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_A, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * データが登録されていない状態でONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_C);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_C, "parent", ENTITY_TYPE_A);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_C, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * データが登録されていない状態でONE対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でONE対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_B);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_B, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * データが登録されていない状態でAST対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でAST対ZERO_ONEのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_D);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_D, "parent", ENTITY_TYPE_A);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_D, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * データが登録されていない状態でAST対ONEのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でAST対ONEのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_D);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_D, "parent", ENTITY_TYPE_B);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_D, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * データが登録されていない状態でAST対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること.
     */
    @Test
    public final void データが登録されていない状態でAST対ASTのUserDataをNavigationProperty経由で取得して空データが取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_C);
            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_C, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_C, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * ZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();

            // 事前にデータを登録する
            createUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_B, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_B);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, String> navigationProp = new HashMap<String, String>();
            String baseUrl = UrlUtils.userData(cellName, boxName, colName, ENTITY_TYPE_B + "('userdataNP')/_");
            navigationProp.put("_" + ENTITY_TYPE_A, baseUrl + ENTITY_TYPE_A);
            navigationProp.put("_" + ENTITY_TYPE_D, baseUrl + ENTITY_TYPE_D);

            checkResponseForONE(response, ENTITY_TYPE_A, "parent", ENTITY_TYPE_B, etags, navigationProp);
        } finally {
            deleteUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_B);
        }
    }

    /**
     * ZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        Map<String, String> etags = new HashMap<String, String>();

        try {
            // 事前にデータを登録する
            createUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_C, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_C);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            Map<String, String> navigationProp = new HashMap<String, String>();
            String baseUrl = UrlUtils.userData(cellName, boxName, colName, ENTITY_TYPE_C + "('userdataNP')/_");
            navigationProp.put("_" + ENTITY_TYPE_A, baseUrl + ENTITY_TYPE_A);
            navigationProp.put("_" + ENTITY_TYPE_D, baseUrl + ENTITY_TYPE_D);

            checkResponseForONE(response, ENTITY_TYPE_A, "parent", ENTITY_TYPE_C, etags, navigationProp);
        } finally {
            deleteUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_C);
        }
    }

    /**
     * ZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForAST(response, ENTITY_TYPE_A, "parent", ENTITY_TYPE_D, ODataCommon.COUNT_NONE, etags);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ONE対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForONE(ENTITY_TYPE_C, ENTITY_TYPE_A);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_C, "parent", ENTITY_TYPE_A);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForONE(response, ENTITY_TYPE_C, "parent", ENTITY_TYPE_A);
        } finally {
            deleteUserDataForONE(ENTITY_TYPE_C, ENTITY_TYPE_A);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForAST(response, ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, ODataCommon.COUNT_NONE, etags);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void AST対ZERO_ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_D, "userdataNP", ENTITY_TYPE_A);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                    colName, ENTITY_TYPE_A + "('parent')"));

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("parent", additionalprop);
            additionalprop.put("dynamicProperty", "dynamicPropertyPatent");

            String nameSpace = getNameSpace(ENTITY_TYPE_A);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, etags);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_A, ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void AST対ONEのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_D, "userdataNP", ENTITY_TYPE_B);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                    colName, ENTITY_TYPE_B + "('parent')"));

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("parent", additionalprop);
            additionalprop.put("dynamicProperty", "dynamicPropertyPatent");

            String nameSpace = getNameSpace(ENTITY_TYPE_B);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void AST対ASTのUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_C, ENTITY_TYPE_D, etags);

            // ユーザデータの一覧取得
            TResponse response = execNpList(ENTITY_TYPE_C, "parent", ENTITY_TYPE_D);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForAST(response, ENTITY_TYPE_C, "parent", ENTITY_TYPE_D, ODataCommon.COUNT_NONE, etags);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_C, ENTITY_TYPE_D);
        }
    }

    /**
     * データが存在しない場合にUserDataをNavigationProperty経由で取得時にinlinecountを指定して件数が取得できること.
     */
    @Test
    public final void データが存在しない場合にUserDataをNavigationProperty経由で取得時にinlinecountを指定して件数が取得できること() {
        try {
            createUserDataParent(ENTITY_TYPE_C);
            // ユーザデータの一覧取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + ENTITY_TYPE_C + "('parent')")
                    .with("entityType", "_" + ENTITY_TYPE_D)
                    .with("query", "?\\$inlinecount=allpages")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(entityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), null, nameSpace, null, 0);
        } finally {
            deleteUserData(cellName, boxName, colName, ENTITY_TYPE_C, "parent", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でfilterを指定して対象データが取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でfilterを指定して対象データが取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$filter=dynamicProperty%20eq%20%27dynamicPropertyValueNp%27";
            TResponse response = execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForONE(response, ENTITY_TYPE_B, "parent", ENTITY_TYPE_D);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でorderbyを指定して対象データがソートされて取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でorderbyを指定して対象データがソートされて取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$orderby=dynamicProperty%20desc";
            TResponse response = execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName,
                    colName, ENTITY_TYPE_D + "('userdataNP2')"));
            uri.add(UrlUtils.userData(cellName, boxName,
                    colName, ENTITY_TYPE_D + "('userdataNP')"));
            ODataCommon.checkCommonResponseUri(response.bodyAsJson(), uri);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でinlinecountを指定して取得件数が取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でinlinecountを指定して取得件数が取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$inlinecount=allpages";
            TResponse response = execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForAST(response, ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, 2, null);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でtopを指定して指定した件数分データが取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でtopを指定して指定した件数分データが取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$top=1&\\$orderby=dynamicProperty";
            TResponse response = execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            checkResponseForONE(response, ENTITY_TYPE_B, "parent", ENTITY_TYPE_D);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でtopに最大値を指定した場合正常終了すること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でtopに最大値を指定した場合正常終了すること() {
        String top = Integer.toString(topMaxNum);
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$top=" + top;
            execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * UserDataをNavigationProperty経由でtopに最大値プラス1を指定した場合400エラーが発生すること.
     */
    @Test
    public final void UserDataをNavigationProperty経由でtopに最大値プラス1を指定した場合400エラーが発生すること() {
        String top = Integer.toString(topMaxNum + 1);
        // ユーザデータの一覧取得
        String query = "?\\$top=" + top;
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + ENTITY_TYPE_B + "('parent')")
                .with("entityType", "_" + ENTITY_TYPE_D)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", top).getMessage());
    }

    /**
     * UserDataをNavigationProperty経由でtopにマイナス1を指定した場合400エラーが発生すること.
     */
    @Test
    public final void UserDataをNavigationProperty経由でtopにマイナス1を指定した場合400エラーが発生すること() {
        // ユーザデータの一覧取得
        String query = "?\\$top=-1";
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + ENTITY_TYPE_B + "('parent')")
                .with("entityType", "_" + ENTITY_TYPE_D)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", "-1").getMessage());
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でskipを指定して指定した件数分飛ばしてデータが取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でskipを指定して指定した件数分飛ばしてデータが取得できること() {
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$skip=1&\\$orderby=dynamicProperty";
            TResponse response = execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdataNP", UrlUtils.userData(cellName, boxName,
                    colName, ENTITY_TYPE_D + "('userdataNP2')"));

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("userdataNP2", additionalprop);
            additionalprop.put("dynamicProperty", "dynamicPropertyValueNp2");

            String nameSpace = getNameSpace(ENTITY_TYPE_D);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でskipに最大値を指定した場合正常取得できること.
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でskipに最大値を指定した場合正常取得できること() {
        String skip = Integer.toString(skipMaxNum);
        try {
            // 事前にデータを登録する
            createUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            String query = "?\\$skip=" + skip;
            execNpListWithQuery(ENTITY_TYPE_B, "parent", ENTITY_TYPE_D, query);
        } finally {
            deleteUserDataForAST(ENTITY_TYPE_B, ENTITY_TYPE_D);
        }
    }

    /**
     * UserDataをNavigationProperty経由でskipにマイナス1を指定した場合400エラーが発生すること.
     */
    @Test
    public final void UserDataをNavigationProperty経由でskipにマイナス1を指定した場合400エラーが発生すること() {
        // ユーザデータの一覧取得
        String query = "?\\$skip=-1";
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + ENTITY_TYPE_B + "('parent')")
                .with("entityType", "_" + ENTITY_TYPE_D)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$skip", "-1").getMessage());
    }

    /**
     * UserDataをNavigationProperty経由でskipに最大値プラス1を指定した場合400エラーが発生すること.
     */
    @Test
    public final void UserDataをNavigationProperty経由でskipに最大値プラス1を指定した場合400エラーが発生すること() {
        String skip = Integer.toString(skipMaxNum + 1);

        // ユーザデータの一覧取得
        String query = "?\\$skip=" + skip;
        TResponse res = Http.request("box/odatacol/list.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName + "/" + ENTITY_TYPE_B + "('parent')")
                .with("entityType", "_" + ENTITY_TYPE_D)
                .with("query", query)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .debug();
        ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.QUERY_INVALID_ERROR.getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$skip", skip).getMessage());
    }

    /**
     * 制御コードを含むUserDataのNP経由一覧取得時に制御コードがエスケープされて取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 制御コードを含むUserDataのNP経由一覧取得時に制御コードがエスケープされて取得できること() {
        try {
            Map<String, String> etags = new HashMap<String, String>();

            // 事前にデータを登録する
            JSONObject body;

            createUserDataParent(ENTITY_TYPE_A, etags);

            entityTypeName = ENTITY_TYPE_A;
            navPropName = ENTITY_TYPE_B;
            body = new JSONObject();
            body.put("__id", "userdataNP");
            body.put("testField", "value_\\u0001_value");
            createUserDataWithNP("parent", body, HttpStatus.SC_CREATED);

            // ユーザデータのNP経由一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_B);

            // レスポンスボディーのチェック
            String resBody = response.getBody();
            assertTrue(resBody.contains("\\u0001"));
            assertFalse(resBody.contains("\u0001"));

        } finally {
            deleteUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_B);
        }
    }

    /**
     * Double型のプロパティを含むUserDataのNP経由一覧取得時にDouble型のプロパティの値が丸められないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Double型のプロパティを含むUserDataのNP経由一覧取得時にDouble型のプロパティの値が丸められないこと() {
        try {
            Map<String, String> etags = new HashMap<String, String>();

            // 事前にデータを登録する
            JSONObject body;

            createUserDataParent(ENTITY_TYPE_A, etags);

            entityTypeName = ENTITY_TYPE_A;
            navPropName = ENTITY_TYPE_B;
            body = new JSONObject();
            body.put("__id", "userdataNP");
            body.put("testFieldDouble", 1234567890.12345d);
            createUserDataWithNP("parent", body, HttpStatus.SC_CREATED);

            // ユーザデータのNP経由一覧取得
            TResponse response = execNpList(ENTITY_TYPE_A, "parent", ENTITY_TYPE_B);

            // レスポンスボディーのチェック
            String resBody = response.getBody();
            assertTrue(resBody.contains("1234567890.12345"));
            assertFalse(resBody.contains("1.23456789012345E9"));
        } finally {
            deleteUserDataForONE(ENTITY_TYPE_A, ENTITY_TYPE_B);
        }
    }

}
