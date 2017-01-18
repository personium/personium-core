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

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ComplexTypeUserDataのNavigationProperty経由一覧のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataComplexTypeListWithNPTest extends AbstractUserDataWithNP {

    /**
     * コンストラクタ.
     */
    public UserDataComplexTypeListWithNPTest() {
        super();
    }

    /**
     * ZERO_ONE対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForONE(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_B);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_B);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> expectedNaviProp = new HashMap<String, String>();
            String baseUrl = UrlUtils.userData(cellName, boxName, colName, CT_ENTITY_TYPE_B + "('userdataNP')/_");
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_A, baseUrl + CT_ENTITY_TYPE_A);
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_D, baseUrl + CT_ENTITY_TYPE_D);

            checkComplexTypeResponseForONE(response, CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_B, expectedNaviProp);
        } finally {
            deleteUserDataForONE(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_B);
        }
    }

    /**
     * ZERO_ONE対ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForONE(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_C);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_C);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> expectedNaviProp = new HashMap<String, String>();
            String baseUrl = UrlUtils.userData(cellName, boxName, colName, CT_ENTITY_TYPE_C + "('userdataNP')/_");
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_A, baseUrl + CT_ENTITY_TYPE_A);
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_D, baseUrl + CT_ENTITY_TYPE_D);

            checkComplexTypeResponseForONE(response, CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_C, expectedNaviProp);
        } finally {
            deleteUserDataForONE(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_C);
        }
    }

    /**
     * ZERO_ONE対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ZERO_ONE対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForAST(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_D);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            checkComplexTypeResponseForAST(response, CT_ENTITY_TYPE_A, "parent", CT_ENTITY_TYPE_D,
                    ODataCommon.COUNT_NONE);
        } finally {
            deleteUserDataForAST(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_D);
        }
    }

    /**
     * ONE対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ONE対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForONE(CT_ENTITY_TYPE_C, CT_ENTITY_TYPE_A);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_C, "parent", CT_ENTITY_TYPE_A);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> expectedNaviProp = new HashMap<String, String>();
            String baseUrl = UrlUtils.userData(cellName, boxName, colName, CT_ENTITY_TYPE_A + "('userdataNP')/_");
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_B, baseUrl + CT_ENTITY_TYPE_B);
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_C, baseUrl + CT_ENTITY_TYPE_C);
            expectedNaviProp.put("_" + CT_ENTITY_TYPE_D, baseUrl + CT_ENTITY_TYPE_D);

            checkComplexTypeResponseForONE(response, CT_ENTITY_TYPE_C, "parent", CT_ENTITY_TYPE_A, expectedNaviProp);
        } finally {
            deleteUserDataForONE(CT_ENTITY_TYPE_C, CT_ENTITY_TYPE_A);
        }
    }

    /**
     * ONE対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void ONE対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForAST(CT_ENTITY_TYPE_B, CT_ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_B, "parent", CT_ENTITY_TYPE_D);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            checkComplexTypeResponseForAST(response, CT_ENTITY_TYPE_B, "parent", CT_ENTITY_TYPE_D,
                    ODataCommon.COUNT_NONE);
        } finally {
            deleteUserDataForAST(CT_ENTITY_TYPE_B, CT_ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void AST対ZERO_ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForAST(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_D, "userdataNP", CT_ENTITY_TYPE_A);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdataNP", UrlUtils.userData(cellName, boxName, colName, CT_ENTITY_TYPE_A + "('parent')"));

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put("parent", additionalprop1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            String nameSpace = getNameSpace(CT_ENTITY_TYPE_A);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataForAST(CT_ENTITY_TYPE_A, CT_ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void AST対ONEのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {
        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForAST(CT_ENTITY_TYPE_B, CT_ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_D, "userdataNP", CT_ENTITY_TYPE_B);

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("userdataNP", UrlUtils.userData(cellName, boxName, colName, CT_ENTITY_TYPE_B + "('parent')"));

            JSONObject ct1stProp = new JSONObject();
            ct1stProp.put("ct1stStrProp", "ct1stStrPropValue1");
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put("parent", additionalprop1);
            additionalprop1.put(UserDataComplexTypeUtils.ET_STRING_PROP, "etStrPropValue1");
            additionalprop1.put(UserDataComplexTypeUtils.ET_CT1ST_PROP, ct1stProp);

            String nameSpace = getNameSpace(CT_ENTITY_TYPE_B);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id");
        } finally {
            deleteUserDataForAST(CT_ENTITY_TYPE_B, CT_ENTITY_TYPE_D);
        }
    }

    /**
     * AST対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること.
     */
    @Test
    public final void AST対ASTのComplexTypeUserDataをNavigationProperty経由で取得して対象データが取得できること() {

        try {
            // 事前にデータを登録する
            createComplexTypeUserDataForAST(CT_ENTITY_TYPE_C, CT_ENTITY_TYPE_D);

            // ユーザデータの一覧取得
            TResponse response = execNpList(CT_ENTITY_TYPE_C, "parent", CT_ENTITY_TYPE_D);

            // レスポンスヘッダのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディのチェック
            checkComplexTypeResponseForAST(response, CT_ENTITY_TYPE_C, "parent", CT_ENTITY_TYPE_D,
                    ODataCommon.COUNT_NONE);
        } finally {
            deleteUserDataForAST(CT_ENTITY_TYPE_C, CT_ENTITY_TYPE_D);
        }
    }
}
