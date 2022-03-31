/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.test.jersey.box.odatacol;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.setup.Setup;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * UserData登録のバリデートテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataValidateTest extends ODataCommon {

    private static final int MAX_LEN_STRING_VALUE = 1024 * 50;

    /**
     * コンストラクタ.
     */
    public UserDataValidateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * UserDataの新規作成時IDに空文字を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDに空文字を指定した場合400になること() {
        String userDataId = "";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDにアンダーバー始まりの文字列を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDにアンダーバー始まりの文字列を指定した場合201になること() {
        String userDataId = "_userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDにハイフン始まりの文字列を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDにハイフン始まりの文字列を指定した場合201になること() {
        String userDataId = "-userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDにスラッシュを含むの文字列を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDにスラッシュを含む文字列を指定した場合201になること() {
        String userDataId = "user/data001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDに有効桁長の最小値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDに有効桁長の最小値を指定した場合201になること() {
        String userDataId = "1";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDに有効桁長の最大値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDに有効桁長の最大値を指定した場合201になること() {
        String userDataId = "123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567890";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDに有効桁長の最大値をオーバーした文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDに有効桁長の最大値をオーバーした文字列を指定した場合400になること() {
        String userDataId = "123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567890x";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時IDに日本語を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時IDに日本語を指定した場合201になること() {
        String userDataId = "日本語";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyに空文字を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyに空文字を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyにアンダーバー始まりの文字列を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("_dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyにハイフン始まりの文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyにハイフン始まりの文字列を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("-dynamicProperty", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyにスラッシュを含む文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyにスラッシュを含む文字列を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamic/Property", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyに有効桁長の最小値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyに有効桁長の最小値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("1", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyに有効桁長の最大値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyに有効桁長の最大値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyに有効桁長の最大値をオーバーした文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyに有効桁長の最大値をオーバーした文字列を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時keyに日本語を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時keyに日本語を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("日本語", "dynamicPropertyValue");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時valueに空文字を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに空文字を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時valueに制御コードを含む文字列を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに制御コードを含む文字列を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", "\\u0003");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            locationHeader = res.getLocationHeader();
            String resBody = res.getBody();
            assertTrue(resBody.contains("\\u0003"));
            assertFalse(resBody.contains("\u0003"));

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時valueに有効桁長の最大値の文字列を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに有効桁長の最大値の文字列を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String value = UserDataUtils.createString(MAX_LEN_STRING_VALUE);
        body.put("dynamicProperty", value);

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            res.getLocationHeader();
        } finally {
            deleteUserData(userDataId, -1);
        }

        body = new JSONObject();
        body.put("__id", userDataId);
        value = UserDataUtils.createString(MAX_LEN_STRING_VALUE - 3);
        body.put("dynamicProperty", value + "ｘ");

        try {
            TResponse res = createUserData(body, HttpStatus.SC_CREATED);
            res.getLocationHeader();
        } finally {
            deleteUserData(userDataId, -1);
        }
    }

    /**
     * UserDataの新規作成時valueに有効桁長の最大値をオーバーした文字列を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに有効桁長の最大値をオーバーした文字列を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        String maxLengthValue = UserDataUtils.createString(MAX_LEN_STRING_VALUE);
        body.put("dynamicProperty", maxLengthValue + "x");

        String locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }

        body = new JSONObject();
        body.put("__id", userDataId);
        body.put("dynamicProperty", maxLengthValue + "ｘ");

        locationHeader = null;

        try {
            TResponse res = createUserData(body, HttpStatus.SC_BAD_REQUEST);
            locationHeader = res.getLocationHeader();
        } finally {
            if (locationHeader != null) {
                deleteUserData(userDataId, -1);
            }
        }
    }

    /**
     * UserDataの新規作成時valueに整数型の有効値より小さい値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数型の有効値より小さい値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("intProperty", Integer.MIN_VALUE - 1L);

        String locationHeader = null;
        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            TResponse res = UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);
            locationHeader = res.getLocationHeader();

        } finally {
            if (locationHeader != null) {
                UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                        cellName, boxName, colName, entityTypeName, userDataId);
            }
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数型の有効値の最小値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数型の有効値の最小値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("intProperty", Integer.MIN_VALUE);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数型の0を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数型の0を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("intProperty", 0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }

    }

    /**
     * UserDataの新規作成時valueに整数型の有効値の最大値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数型の有効値の最大値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("intProperty", Integer.MAX_VALUE);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数型の有効値より大きい値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数型の有効値より大きい値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("intProperty", Integer.MAX_VALUE + 1L);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "intProperty", entityTypeName, "Edm.Int32",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "intProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が有効桁数より大きい正の小数値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が有効桁数より大きい正の小数値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 100000.0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が最大桁数の正の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が最大桁数の正の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 99999.9);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が最小桁数の正の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が最小桁数の正の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 1.0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数型で0を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数型で0を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 0.0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が有効桁数より大きい負の小数値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が有効桁数より大きい負の小数値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -100000.0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が最大桁数の負の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が最大桁数の負の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -99999.9);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに整数部分が最小桁数の負の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに整数部分が最小桁数の負の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -1.0);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が有効桁数より大きい正の小数値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が有効桁数より大きい正の小数値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 0.999991);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が最大桁数の正の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が最大桁数の正の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 0.99999);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が最小桁数の正の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が最小桁数の正の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", 0.1);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が有効桁数より大きい負の小数値を指定した場合400になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が有効桁数より大きい負の小数値を指定した場合400になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -0.999991);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が最大桁数の負の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が最大桁数の負の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -0.99999);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * UserDataの新規作成時valueに小数部分が最小桁数の負の小数値を指定した場合201になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void UserDataの新規作成時valueに小数部分が最小桁数の負の小数値を指定した場合201になること() {
        String userDataId = "userdata001";

        JSONObject body = new JSONObject();
        body.put("__id", userDataId);
        body.put("singleProperty", -0.1);

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String entityTypeName = "scmTest";

        try {
            EntityTypeUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME,
                    boxName, colName, entityTypeName, HttpStatus.SC_CREATED);
            UserDataUtils.createProperty(cellName, boxName, colName, "singleProperty", entityTypeName,
                    "Edm.Single",
                    true, null, "None", false, null);

            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                    body, cellName, boxName, colName, entityTypeName);

        } finally {
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1,
                    cellName, boxName, colName, entityTypeName, userDataId);
            ODataCommon.deleteOdataResource(UrlUtils
                    .property(cellName, boxName, colName, "singleProperty", entityTypeName));
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME,
                    "application/xml", entityTypeName, cellName, -1);

        }
    }

    /**
     * Create user data.
     * @param body request body
     * @param code expected response code
     * @return TResponse
     */
    private TResponse createUserData(JSONObject body, int code) {
        return UserDataUtils.create(PersoniumUnitConfig.getMasterToken(), code, body, Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, "Category");
    }

    /**
     * Delete user data.
     * @param id user data id
     * @param code expected response code
     * @return TResponse
     */
    private TResponse deleteUserData(String id, int code) {
        return UserDataUtils.delete(PersoniumUnitConfig.getMasterToken(), code, Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, "Category", id);
    }

}
