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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * UserDataのNavigationProperty経由登録のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataCreateWithNPTest extends AbstractUserDataTest {

    private final String userDataId = "userdataNp001";
    private final String userDataNpId = "userdataNp002";

    /**
     * コンストラクタ.
     */
    public UserDataCreateWithNPTest() {
        super();
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(1:N).
     */
    @Test
    public final void one対ＮUserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
            entityTypeName = "Sales";
            deleteUserData(userDataId);
            entityTypeName = navPropName;
        }
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(N:0..1).
     */
    @Test
    public final void N対0UserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Supplier";
        navPropName = "Sales";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            deleteUserData(userDataId);
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
        }
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(N:1).
     */
    @Test
    public final void N対1UserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "SalesDetail";
        navPropName = "Sales";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            deleteUserData(userDataId);
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
        }
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(N:N).
     */
    @Test
    public final void Ｎ対ＮUserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Product";
        navPropName = "Sales";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            // link削除
            deleteUserDataLinks(userDataId, userDataNpId);
            // data削除
            deleteUserData(userDataId);
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
        }
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(0..1:1).
     */
    @Test
    public final void zero対１UserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Supplier";
        navPropName = "Product";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            deleteUserData(userDataId);
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
        }
    }

    /**
     * UserDataをNavigationProperty経由で新規作成して正常に登録できること(0..1:N).
     */
    @Test
    public final void zero対ＮUserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Sales";
        navPropName = "Supplier";

        // リクエスト実行
        try {
            CreateUserDataWithNP();
        } finally {
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
            entityTypeName = "Sales";
            deleteUserData(userDataId);
            entityTypeName = navPropName;
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに最大数の要素を指定して正常に登録できること(1:N).
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void one対ＮUserDataをNavigationProperty経由でリクエストボディに最大数の要素を指定して正常に登録できること() {
        entityTypeName = Setup.TEST_ENTITYTYPE_M1;
        navPropName = Setup.TEST_ENTITYTYPE_MDP;

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum; i++) {
            bodyNp.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_CREATED, bodyNp);
        } finally {
            entityTypeName = Setup.TEST_ENTITYTYPE_MDP;
            deleteUserData(userDataNpId);
            entityTypeName = Setup.TEST_ENTITYTYPE_M1;
            deleteUserData(userDataId);
            entityTypeName = navPropName;

        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディにリクエストボディに最大数の要素プラス１を指定して４００になること(1:N).
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void one対ＮUserDataをNavigationProperty経由でリクエストボディに最大数の要素プラス１を指定して４００になること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum + 1; i++) {
            bodyNp.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_BAD_REQUEST, bodyNp);
        } finally {
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに最大数の要素を指定して正常に登録できること(N:N).
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Ｎ対ＮUserDataをNavigationProperty経由でリクエストボディに最大数の要素を指定して正常に登録できること() {
        entityTypeName = Setup.TEST_ENTITYTYPE_MN;
        navPropName = Setup.TEST_ENTITYTYPE_MDP;

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum; i++) {
            bodyNp.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_CREATED, bodyNp);
        } finally {
            // link削除
            deleteUserDataLinks(userDataId, userDataNpId);
            // data削除
            deleteUserData(userDataId);
            entityTypeName = Setup.TEST_ENTITYTYPE_MDP;
            deleteUserData(userDataNpId);
            entityTypeName = "Sales";
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに最大数の要素プラス１を指定して４００になること(N:N).
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Ｎ対ＮUserDataをNavigationProperty経由でリクエストボディに最大数の要素プラス１を指定して４００になること() {
        entityTypeName = "Product";
        navPropName = "Sales";

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);

        int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
        for (int i = 0; i < maxPropNum + 1; i++) {
            bodyNp.put("dynamicProperty" + i, "dynamicPropertyValue" + i);
        }

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_BAD_REQUEST, bodyNp);
        } finally {
            // data削除
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに管理情報__publishedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをNavigationProperty経由でリクエストボディに管理情報__publishedを指定した場合400エラーとなること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);
        bodyNp.put(PUBLISHED, "/Date(0)/");

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_BAD_REQUEST, bodyNp);
        } finally {
            // data削除
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに管理情報__updatedを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをNavigationProperty経由でリクエストボディに管理情報__updatedを指定した場合400エラーとなること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);
        bodyNp.put(UPDATED, "/Date(0)/");

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_BAD_REQUEST, bodyNp);
        } finally {
            // data削除
            deleteUserData(userDataId);
        }
    }

    /**
     * UserDataをNavigationProperty経由でリクエストボディに管理情報__metadataを指定した場合400エラーとなること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void UserDataをNavigationProperty経由でリクエストボディに管理情報__metadataを指定した場合400エラーとなること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);
        bodyNp.put(METADATA, "test");

        // リクエスト実行
        try {
            CreateUserDataWithNP(HttpStatus.SC_BAD_REQUEST, bodyNp);
        } finally {
            // data削除
            deleteUserData(userDataId);
        }
    }

    /**
     * 登録済のダイナミックプロパティにnull指定でユーザODataをNP経由で正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 登録済のダイナミックプロパティにnull指定でユーザODataをNP経由で正常に登録できること() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String cell = Setup.TEST_CELL1;
        final String box = Setup.TEST_BOX1;
        final String col = Setup.TEST_ODATA;
        // リクエストボディを設定
        String userDataFirstId = "first";
        String userDataSecondId = "second";

        try {
            // EntityType作成
            EntityTypeUtils.create(cell, token, box, col, "srcEntity", HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cell, token, box, col, "tgtEntity", HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(token, "0..1", cell, box, col,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(token, "*", cell, box, col,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                    col, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
            // ユーザデータ（srouce）作成 JSONObject bodyFirst = new JSONObject();
            JSONObject body = new JSONObject();
            body.put("__id", "srouce");

            JSONObject targetBodyFirst = new JSONObject();
            targetBodyFirst.put("__id", userDataFirstId);
            targetBodyFirst.put("dynamicProperty", null);
            targetBodyFirst.put("First", "test1");

            JSONObject targetBodySecond = new JSONObject();
            targetBodySecond.put("__id", userDataSecondId);
            targetBodySecond.put("dynamicProperty", null);
            targetBodySecond.put("Second", "test2");


            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME,
                    HttpStatus.SC_CREATED, body, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "srcEntity");
            UserDataUtils.createViaNP(token,
                    targetBodyFirst, cell, box, col, "srcEntity", "srouce", "tgtEntity", HttpStatus.SC_CREATED);
            UserDataUtils.createViaNP(token,
                    targetBodySecond, cell, box, col, "srcEntity", "srouce", "tgtEntity", HttpStatus.SC_CREATED);

            // ユーザデータの取得
            TResponse response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "tgtEntity",
                    userDataFirstId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            JSONObject resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertTrue(resBody.containsKey("First"));
            assertNotNull(resBody.get("First"));

            // ユーザデータの取得
            response = getUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, "tgtEntity",
                    userDataSecondId, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK);

            resBody = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // レスポンスボディーのチェック
            assertTrue(resBody.containsKey("dynamicProperty"));
            assertNull(resBody.get("dynamicProperty"));
            assertFalse(resBody.containsKey("First"));
            assertTrue(resBody.containsKey("Second"));
            assertNotNull(resBody.get("Second"));
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(token, -1, "tgtEntity", userDataFirstId, col);
            UserDataUtils.delete(token, -1, "tgtEntity", userDataSecondId, col);
            UserDataUtils.delete(token, -1, "srcEntity", "srouce", col);
            // AssociationEndの削除
            String url = UrlUtils.associationEndLink(cell, box, col, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cell, box, col, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(col, "srcEntity", cell, box);
            Setup.entityTypeDelete(col, "tgtEntity", cell, box);
        }
    }

    /**
     * リンクがN対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクがN対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "*", "0..1");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CONFLICT);
            // NP経由で登録しようとしたユーザODataが存在しないこと
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクがN対1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクがN対1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "*", "1");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CONFLICT);
            // NP経由で登録しようとしたユーザODataが存在しないこと
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクがN対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクがN対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "*", "*");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            // NP経由で登録しようとしたユーザODataが存在すること
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_OK);
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "testNp", -1);
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "targetNpCreateTest", -1);
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクが1対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクが1対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "1", "0..1");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CONFLICT);
            // NP経由で登録しようとしたユーザODataが存在しないこと
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクが1対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクが1対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "1", "*");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            // NP経由で登録しようとしたユーザODataが存在すること
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_OK);
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "testNp", -1);
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "targetNpCreateTest", -1);
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクが0_1対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクが0_1対0_1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "0..1", "0..1");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CONFLICT);
            // NP経由で登録しようとしたユーザODataが存在しないこと
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクが0_1対1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクが0_1対1のUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して409が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "0..1", "1");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CONFLICT);
            // NP経由で登録しようとしたユーザODataが存在しないこと
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_NOT_FOUND);
        } finally {
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * リンクが0_1対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リンクが0_1対NのUserDataですでにリンクデータが存在する場合にNavigationProperty経由で新規作成して201が返却されること() {
        String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        String entityTypeName = "srcEntity";
        String navPropName = "tgtEntity";
        try {
            // スキーマ作成
            createSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName, "0..1", "*");
            // ソース側のユーザOData作成
            JSONObject body = new JSONObject();
            body.put("__id", "sourceNpCreateTest");
            UserDataUtils.create(masterTokenName,
                    HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityTypeName);
            getUserData(cellName, boxName, colName, entityTypeName,
                    "sourceNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成
            JSONObject bodyNp1 = new JSONObject();
            bodyNp1.put("__id", "targetNpCreateTest");
            UserDataUtils.createViaNP(masterTokenName, bodyNp1, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            getUserData(cellName, boxName, colName, navPropName,
                    "targetNpCreateTest", masterTokenName, HttpStatus.SC_OK);
            // NavigationProperty側のユーザOData作成(既にリンクデータが登録されているので409が返却されること)
            JSONObject bodyNp2 = new JSONObject();
            bodyNp2.put("__id", "testNp");
            UserDataUtils.createViaNP(masterTokenName, bodyNp2, cellName,
                    boxName, colName, entityTypeName, "sourceNpCreateTest", navPropName, HttpStatus.SC_CREATED);
            // NP経由で登録しようとしたユーザODataが存在すること
            getUserData(cellName, boxName, colName, navPropName,
                    "testNp", masterTokenName, HttpStatus.SC_OK);
        } finally {
            // ユーザデータの$links削除
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "testNp", -1);
            UserDataUtils.deleteLinks(cellName, boxName, colName,
                    entityTypeName, "sourceNpCreateTest", navPropName, "targetNpCreateTest", -1);
            // ユーザデータの削除
            UserDataUtils.delete(masterTokenName, -1, entityTypeName, "sourceNpCreateTest", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "testNp", colName);
            UserDataUtils.delete(masterTokenName, -1, navPropName, "targetNpCreateTest", colName);
            deleteSchemaForNpConflictTest(cellName, boxName, colName, entityTypeName, navPropName);
        }
    }

    /**
     * 制御コードを含むUserDataをNavigationProperty経由で新規作成して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 制御コードを含むUserDataをNavigationProperty経由で新規作成して正常に登録できること() {
        entityTypeName = "Sales";
        navPropName = "SalesDetail";

        // リクエスト実行
        try {
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);

            JSONObject npBody = new JSONObject();
            npBody.put("__id", userDataNpId);
            npBody.put("name", "\\u0003");
            TResponse npResponse = UserDataUtils.createViaNP(AbstractCase.MASTER_TOKEN_NAME, npBody,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, userDataId, navPropName, HttpStatus.SC_CREATED);
            String resBody = npResponse.getBody();
            assertTrue(resBody.contains("\\u0003"));
            assertFalse(resBody.contains("\u0003"));
        } finally {
            entityTypeName = navPropName;
            deleteUserData(userDataNpId);
            entityTypeName = "Sales";
            deleteUserData(userDataId);
            entityTypeName = navPropName;
        }
    }

    /**
     * NavigationProperty経由登録時にリンク定義による409エラーとなるテスト用のスキーマ削除.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityTypeName EntityType名
     * @param navPropName NavigationProperty名("_"なし)
     */
    private void deleteSchemaForNpConflictTest(String cellName, String boxName, String colName,
            String entityTypeName, String navPropName) {
        // AssociationEndの削除
        String url = UrlUtils.associationEndLink(cellName, boxName, colName,
                "ae1", entityTypeName, "ae2", navPropName);
        ODataCommon.deleteOdataResource(url);
        ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cellName, boxName, colName, "ae1", entityTypeName));
        ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cellName, boxName, colName, "ae2", navPropName));
        // EntityTypeの削除
        Setup.entityTypeDelete(colName, entityTypeName, cellName, boxName);
        Setup.entityTypeDelete(colName, navPropName, cellName, boxName);
    }

    /**
     * NavigationProperty経由登録時にリンク定義による409エラーとなるテスト用のスキーマ作成.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param colName コレクション名
     * @param entityTypeName EntityType名
     * @param navPropName NavigationProperty名("_"なし)
     */
    private void createSchemaForNpConflictTest(String cellName, String boxName, String colName,
            String entityTypeName, String navPropName, String souceMultiplicity, String targetMultiplicity) {
        final String masterTokenName = AbstractCase.MASTER_TOKEN_NAME;
        // EntityType作成
        EntityTypeUtils.create(cellName, masterTokenName,
                boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(cellName, masterTokenName,
                boxName, colName, navPropName, HttpStatus.SC_CREATED);
        // AssociationEnd作成
        AssociationEndUtils.create(masterTokenName, souceMultiplicity, cellName, boxName, colName,
                HttpStatus.SC_CREATED, "ae1", entityTypeName);
        AssociationEndUtils.create(masterTokenName, targetMultiplicity, cellName, boxName, colName,
                HttpStatus.SC_CREATED, "ae2", navPropName);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                colName, entityTypeName, navPropName, "ae1", "ae2", HttpStatus.SC_NO_CONTENT);
    }

    @SuppressWarnings("unchecked")
    private void CreateUserDataWithNP() {
        JSONObject bodyNp = new JSONObject();
        bodyNp.put("__id", userDataNpId);
        bodyNp.put("dynamicProperty", "dynamicPropertyValueNp");

        CreateUserDataWithNP(HttpStatus.SC_CREATED, bodyNp);
    }

    @SuppressWarnings("unchecked")
    private void CreateUserDataWithNP(int expectedSC, JSONObject bodyNp) {
        // リクエストボディを設定
        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        createUserData(body, HttpStatus.SC_CREATED);
        TResponse response = createUserDataWithNP(userDataId, bodyNp, expectedSC);

        if (expectedSC == HttpStatus.SC_CREATED) {
            // 登録データを一件取得し、レスポンスヘッダからETAGを取得する
            TResponse getres = getUserData("testcell1", "box1", "setodata", navPropName,
                    userDataNpId, DcCoreConfig.getMasterToken(), "", HttpStatus.SC_OK);
            String etag = getres.getHeader(HttpHeaders.ETAG);

            // レスポンスヘッダーのチェック
            String location = UrlUtils.userData(cellName, boxName, colName, navPropName
                    + "('" + userDataNpId + "')");
            ODataCommon.checkCommonResponseHeader(response, location);

            // レスポンスボディーのチェック
            String nameSpace = getNameSpace(navPropName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, nameSpace, bodyNp, null, etag);
        }
    }

    /**
     * ユーザーデータを取得する.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param entityType エンティティタイプ名
     * @param userDataIda ユーザデータID
     * @param token 認証トークン
     * @param query クエリ
     * @param sc 期待するステータスコード
     * @return レスポンス
     */
    protected TResponse getUserData(String cell, String box, String col, String entityType,
            String userDataIda, String token, String query, int sc) {
        TResponse response = Http.request("box/odatacol/get.txt")
                .with("cell", cell)
                .with("box", box)
                .with("collection", col)
                .with("entityType", entityType)
                .with("id", userDataIda)
                .with("token", token)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("query", query)
                .returns()
                .statusCode(sc)
                .debug();

        return response;
    }

}
