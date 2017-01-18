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

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Propertyを定義したユーザデータの$link取得のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataLinkPropertyTest extends AbstractUserDataTest {

    // セル再帰的削除を使用するために、あらかじめFilterクラスの設定が必要なため、staticメソッドで初期化しておく。
    // また、親クラスとしてODataCommonクラスを指定する。
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    private static final String CELL = "UserDataLinkPropertyTest" + "_cell";
    private static final String BOX = "box";
    private static final String COL = "COL";
    private static final String PARENT_ENTITY = "parentEntity";
    private static final String CHILD_ENTITY = "childEntity";
    private static final String CHILD_ENTITY1 = CHILD_ENTITY + 1;
    private static final String CHILD_ENTITY2 = CHILD_ENTITY + 2;
    private static final String CHILD_ENTITY3 = CHILD_ENTITY + 3;
    private static final String CHILD_ENTITY4 = CHILD_ENTITY + 4;
    private static final String CHILD_ENTITY5 = CHILD_ENTITY + 5;
    private static final String CHILD_ENTITY6 = CHILD_ENTITY + 6;
    private static final String PROPERTY = "property";
    private static final String ASSOCIATION_END_PARENT = "associcationEndParent";
    private static final String ASSOCIATION_END_CHILD = "associcationEndChild";

    /**
     * コンストラクタ.
     */
    public UserDataLinkPropertyTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * テスト用データの作成.
     */
    @Before
    public void createTestData() {
        CellUtils.create(CELL, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL, BOX, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, CELL, BOX, COL);
        // 親EntityType作成
        EntityTypeUtils.create(CELL, MASTER_TOKEN_NAME, BOX, COL, PARENT_ENTITY, HttpStatus.SC_CREATED);
        for (int j = 1; j <= 6; j++) { // テスト対象のEdm型の数
            // 子EntityType作成
            EntityTypeUtils.create(CELL, MASTER_TOKEN_NAME, BOX, COL, CHILD_ENTITY + j, HttpStatus.SC_CREATED);
        }
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY1, "Edm.String", true, null, null,
                false, null);
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY2, "Edm.Int32", true, null, null,
                false, null);
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY3, "Edm.Boolean", true, null, null,
                false, null);
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY4, "Edm.Single", true, null, null,
                false, null);
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY5, "Edm.Double", true, null, null,
                false, null);
        UserDataUtils.createProperty(CELL, BOX, COL, PROPERTY, CHILD_ENTITY6, "Edm.DateTime", true, null, null,
                false, null);
    }

    /**
     * テスト用データの削除.
     * @throws InterruptedException 例外
     */
    @After
    public void deleteTestData() throws InterruptedException {
        CellUtils.bulkDeletion(BEARER_MASTER_TOKEN, CELL);
    }

    /**
     * ユーザデータを作成する.
     */
    @SuppressWarnings("unchecked")
    private void createUserData() {
        // ユーザデータ作成
        // 親Entity
        JSONObject body = new JSONObject();
        body.put("__id", "id");
        UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, CELL, BOX, COL, PARENT_ENTITY);
        // 子Entity
        for (int i = 1; i <= 6; i++) {
            switch (i) {
            case 1: // Edm.String
                body.put(PROPERTY, "123");
                break;
            case 2: // Edm.Int32
                body.put(PROPERTY, 123);
                break;
            case 3: // Edm.Boolean
                body.put(PROPERTY, true);
                break;
            case 4: // Edm.Single
                body.put(PROPERTY, 1.23);
                break;
            case 5: // Edm.Double
                body.put(PROPERTY, 1.23);
                break;
            case 6: // Edm.DateTime
                body.put(PROPERTY, "/Date(1234567890)/");
                break;
            default:
                break;
            }
            UserDataUtils.createViaNP(MASTER_TOKEN_NAME,
                    body, CELL, BOX, COL, PARENT_ENTITY, "id", CHILD_ENTITY + i, HttpStatus.SC_CREATED);
        }
    }

    /**
     * レスポンスをチェックする.
     */
    private void checkListResponse() {
        for (int i = 1; i <= 6; i++) {
            TResponse response = UserDataUtils.listLink(CELL, BOX, COL, PARENT_ENTITY, "id", CHILD_ENTITY + i);
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(CELL, BOX, COL, CHILD_ENTITY + i, "id"));
            ODataCommon.checkLinResponseBody(response.bodyAsJson(), expectedUriList);
        }
    }

    /**
     * 多重度0_1対1の$linkを取得してターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0_1対1の$linkを取得してターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度0_1対0_1の$linkを取得してターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0_1対0_1の$linkを取得してターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度0or1対Nの$linkを取得してターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0or1対Nの$linkを取得してターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度N対Nの$linkを取得してターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度N対Nの$linkを取得してターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度1対0_1の$linkを取得してターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度1対0_1の$linkを取得してターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * NP経由でのユーザOData一覧取得結果をチェックする.
     */
    private void checkNpListResponse() {
        for (int i = 1; i <= 6; i++) {
            TResponse response = UserDataUtils.listViaNP(CELL, BOX, COL,
                    PARENT_ENTITY, "id", CHILD_ENTITY + i, "", HttpStatus.SC_OK);
            Map<String, String> uri = new HashMap<String, String>();
            uri.put("id", UrlUtils.userData(CELL, BOX, COL, CHILD_ENTITY + i + "('id')"));
            String nameSpace = getNameSpace(CHILD_ENTITY + i);

            // プロパティ
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put("id", additionalprop);
            additionalprop.put("__id", "id");

            ODataCommon
                    .checkResponseBodyList(response.bodyAsJson(), uri, nameSpace, additional, "__id", null, null);
        }
    }

    /**
     * 多重度0_1対1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0_1対1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkNpListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度0_1対0_1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0_1対0_1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkNpListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度0_1対NのNP経由一覧取得でターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度0_1対NのNP経由一覧取得でターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkNpListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度N対NのNP経由一覧取得でターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度N対NのNP経由一覧取得でターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkNpListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * 多重度1対0_1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること.
     */
    @Test
    public final void 多重度1対0_1のNP経由一覧取得でターゲット側のユーザODataのみが取得できること() {
        try {
            // AssociationEnd作成
            for (int i = 1; i <= 6; i++) {
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_PARENT + i, PARENT_ENTITY);
                AssociationEndUtils.create(MASTER_TOKEN_NAME, "0..1", CELL, BOX, COL,
                        HttpStatus.SC_CREATED, ASSOCIATION_END_CHILD + i, CHILD_ENTITY + i);
                AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, CELL, BOX,
                        COL, PARENT_ENTITY, CHILD_ENTITY + i, ASSOCIATION_END_PARENT + i,
                        ASSOCIATION_END_CHILD + i, HttpStatus.SC_NO_CONTENT);
            }
            createUserData();
            checkNpListResponse();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
