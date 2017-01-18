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
package com.fujitsu.dc.test.jersey.box.odatacol.schema.entitytype;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * EntityTypeCRUDのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class EntityTypeCrudTest extends ODataCommon {

    static final String ACCEPT = "application/xml";

    /**
     * コンストラクタ.
     */
    public EntityTypeCrudTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * EntityTypeの新規作成し201になること.
     */
    @Test
    public final void EntityTypeの新規作成し201になること() {
        String entityTypeName = "testEntity";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameに空文字を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameに空文字を指定した場合400になること() {
        String entityTypeName = "";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameにアンダーバー始まりの文字列を指定した場合400になること() {
        String entityTypeName = "_testEntity";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameにハイフン始まりの文字列を指定した場合400になること() {
        String entityTypeName = "-testEntity";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameにスラッシュを含む文字列を指定した場合400になること() {
        String entityTypeName = "test/Entity";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameに指定可能な文字数の最小値を指定した場合201になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameに指定可能な文字数の最小値を指定した場合201になること() {
        String entityTypeName = "1";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameに指定可能な文字数の最大値を指定した場合201になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameに指定可能な文字数の最大値を指定した場合201になること() {
        String entityTypeName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameに指定可能な文字数の最大値をオーバー指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameに指定可能な文字数の最大値をオーバー指定した場合400になること() {
        String entityTypeName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの新規作成時Nameに日本語を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの新規作成時Nameに日本語を指定した場合400になること() {
        String entityTypeName = "日本語";
        String locationHeader = null;

        try {
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * EntityTypeの削除時にユーザデータがあると409になること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void EntityTypeの削除時にユーザデータがあると409になること() {
        String entityTypeName = "testDataEntity";

        String useDataId = "entityTypeDel";
        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // userDataの作成
            JSONObject body = new JSONObject();
            body.put("__id", useDataId);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);

            // EntityTypeの削除(409になること)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_CONFLICT);

            // userDataの削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, entityTypeName,
                    useDataId, Setup.TEST_ODATA);

            // EntityTypeの削除(削除可能なこと)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);

        } finally {
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, entityTypeName, useDataId, Setup.TEST_ODATA);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeにユーザデータがある場合に更新可能であること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void EntityTypeにユーザデータがある場合に更新可能であること() {
        String entityTypeName = "testDataEntity";
        String entityTypeReName = "testDataEntityReName";

        String userDataId = "entityTypeDel";
        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // userDataの作成
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, entityTypeName);

            // EntityTypeの更新
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // 変名後のEntityTypeを取得できること
            res = EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", entityTypeReName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, EntityTypeUtils.NAMESPACE, additional);

            // 変名前のEntityTypeを取得できないこと
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_NOT_FOUND);

            // 変名後のEntityTypeでuserDataが取得できること
            res = UserDataUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, userDataId, HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);
            String namespace = "UserData." + entityTypeReName;
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, namespace, additional);

            // userDataの削除
            UserDataUtils.delete(MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, entityTypeReName,
                    userDataId, Setup.TEST_ODATA);

            // EntityTypeの更新
            entityTypeBody = "{\"Name\": \"" + entityTypeName + "\"}";
            EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // 変名後のEntityTypeを取得できること
            res = EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", entityTypeName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, EntityTypeUtils.NAMESPACE, additional);

        } finally {
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, entityTypeReName, userDataId, Setup.TEST_ODATA);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeReName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 同一のEntityTypeを複数回更新可能であること.
     */
    @Test
    public final void 同一のEntityTypeを複数回更新可能であること() {
        String entityTypeName = "testDataEntity";
        String entityTypeReName = "testDataEntityReName";
        String entityTypeReName2 = "testDataEntityReName2";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(１回目)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // EntityTypeの更新(2回目)
            entityTypeBody = "{\"Name\": \"" + entityTypeReName2 + "\"}";
            EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // 変名後のEntityTypeを取得できること
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName2, HttpStatus.SC_OK);

            // 変名前のEntityTypeを取得できないこと
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_NOT_FOUND);
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, HttpStatus.SC_NOT_FOUND);

            // EntityTypeの一覧取得で変名後のEntityTypeが取得できること
            res = EntityTypeUtils.list(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, "?\\$filter=Name+eq+'" + entityTypeReName2 + "'", HttpStatus.SC_OK);
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("Name", entityTypeReName2);
            ODataCommon.checkResponseBodyList(res.bodyAsJson(), null, EntityTypeUtils.NAMESPACE, additional);

            // 最初の名前でEntityTypeを作成できること
            res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeを取得できること
            res = EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_OK);
            additional = new HashMap<String, Object>();
            additional.put("Name", entityTypeName);
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, EntityTypeUtils.NAMESPACE, additional);

        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeReName, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeReName2, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameに空文字を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameに空文字を指定した場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameにアンダーバー始まりの文字列を指定した場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "_testEntity";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameにハイフン始まりの文字列を指定した場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "-testEntity";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameにスラッシュを含む文字列を指定した場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "test/Entity";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameに指定可能な文字数の最小値を指定した場合204になること.
     */
    @Test
    public final void EntityTypeの更新時Nameに指定可能な文字数の最小値を指定した場合204になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "1";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(更新可能であること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // 変名後のEntityTypeを取得できること
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, HttpStatus.SC_OK);

            // 変名前のEntityTypeを取得できないこと
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_NOT_FOUND);

        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeReName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameに指定可能な文字数の最大値を指定した場合204になること.
     */
    @Test
    public final void EntityTypeの更新時Nameに指定可能な文字数の最大値を指定した場合204になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(更新可能であること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // 変名後のEntityTypeを取得できること
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, HttpStatus.SC_OK);

            // 変名前のEntityTypeを取得できないこと
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, HttpStatus.SC_NOT_FOUND);

        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeReName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameに指定可能な文字数の最大値をオーバーした場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameに指定可能な文字数の最大値をオーバーした場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameに日本語を指定した場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameに日本語を指定した場合400になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "日本語";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.getCode(),
                    DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの更新時Nameが指定されていない場合400になること.
     */
    @Test
    public final void EntityTypeの更新時Nameが指定されていない場合400になること() {
        String entityTypeName = "testDataEntityName";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(400になること)
            String entityTypeBody = "{}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_BAD_REQUEST);
            res.checkErrorResponse(DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.getCode(),
                    DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params("Name").getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 存在しないEntityTypeを指定して更新した場合404になること.
     */
    @Test
    public final void 存在しないEntityTypeを指定して更新した場合404になること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "testDataEntityName";

        try {
            // EntityTypeの更新(404になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            TResponse res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NOT_FOUND);
            res.checkErrorResponse(DcCoreException.OData.NO_SUCH_ENTITY.getCode(),
                    DcCoreException.OData.NO_SUCH_ENTITY.getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 変名後のEntityTypeが存在する場合409になること.
     */
    @Test
    public final void 変名後のEntityTypeが存在する場合409になること() {
        String entityTypeName1 = "testDataEntityName1";
        String entityTypeName2 = "testDataEntityName2";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName1, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの作成
            res = createEntityType(entityTypeName2, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(409になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeName1 + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityTypeName2, entityTypeBody, HttpStatus.SC_CONFLICT);
            res.checkErrorResponse(DcCoreException.OData.ENTITY_ALREADY_EXISTS.getCode(),
                    DcCoreException.OData.ENTITY_ALREADY_EXISTS.getMessage());
        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName1, Setup.TEST_CELL1, -1);
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName2, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * 同じ名前でEntityTypeを更新できること.
     */
    @Test
    public final void 同じ名前でEntityTypeを更新できること() {
        String entityTypeName = "testDataEntityName";
        String entityTypeReName = "testDataEntityName";

        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // EntityTypeの更新(204になること)
            String entityTypeBody = "{\"Name\": \"" + entityTypeReName + "\"}";
            res = EntityTypeUtils.update(MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA,
                    entityTypeName, entityTypeBody, HttpStatus.SC_NO_CONTENT);

            // EntityTypeを取得できること
            EntityTypeUtils.get(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeReName, HttpStatus.SC_OK);

        } finally {
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの削除時にAssociationEndがあると409になること.
     */
    @Test
    public final void EntityTypeの削除時にAssociationEndがあると409になること() {
        String entityTypeName = "testDataEntity";

        String associationEndName = "deleteAssociationEnd";
        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // AssociationEndの作成
            AssociationEndUtils.create(MASTER_TOKEN_NAME, "*", Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    HttpStatus.SC_CREATED, associationEndName, entityTypeName);

            // EntityTypeの削除(409になること)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_CONFLICT);

            // AssociationEndの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName,
                    Setup.TEST_BOX1, associationEndName, HttpStatus.SC_NO_CONTENT);

            // EntityTypeの削除(削除可能なこと)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
        } finally {
            // AssociationEndの削除
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_ODATA,
                    entityTypeName,
                    Setup.TEST_BOX1, associationEndName, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeの削除時にPropertyがあると409になること.
     */
    @Test
    public final void EntityTypeの削除時にPropertyがあると409になること() {
        String entityTypeName = "testDataEntity";

        String propertyName = "deleteProperty";
        try {
            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_CREATED);

            // Propertyの作成
            PropertyUtils.create(BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName,
                    propertyName, "Edm.String", true, null, "None", false, null, HttpStatus.SC_CREATED);

            // EntityTypeの削除(409になること)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_CONFLICT);

            // Propertyの削除
            PropertyUtils.delete(BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName,
                    propertyName, HttpStatus.SC_NO_CONTENT);

            // EntityTypeの削除(削除可能なこと)
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
        } finally {
            // Propertyの削除
            PropertyUtils.delete(BEARER_MASTER_TOKEN, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    entityTypeName,
                    propertyName, -1);

            // EntityTypeの削除
            EntityTypeUtils.delete(Setup.TEST_ODATA, MASTER_TOKEN_NAME, ACCEPT, entityTypeName,
                    Setup.TEST_CELL1, -1);
        }
    }

    /**
     * DateフィールドOFFの確認. 日付型でユーザデータを登録し、文字列で完全一致検索を行い、ヒットすること
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void DateフィールドOFFの確認() {
        String odataName = "dateOdata";
        String entityTypeName = "testDateEntity";

        String useDataId = "entityTypeDel";
        try {
            // odataコレクションの作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, odataName);

            // EntityTypeの作成
            TResponse res = createEntityType(entityTypeName, odataName);
            res.statusCode(HttpStatus.SC_CREATED);

            // userDataの作成
            JSONObject body = new JSONObject();
            // 日付型のデータ登録
            body.put("__id", useDataId);
            body.put("date", "2012/09/21");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    Setup.TEST_CELL1, Setup.TEST_BOX1, odataName, entityTypeName);

            // $filterで完全一致検索ができること
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", odataName)
                    .with("entityType", entityTypeName)
                    .with("query", "?\\$filter=date+eq+'2012/09/21'")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            // 取得件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
        } finally {
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, entityTypeName, useDataId, odataName);
            EntityTypeUtils.delete(odataName, MASTER_TOKEN_NAME,
                    ACCEPT, entityTypeName, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, odataName, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * EntityTypeを上限値を超える数作成した場合にエラーが返却される.
     */
    @Test
    public final void EntityTypeを上限値を超える数作成した場合にエラーが返却される() {
        try {
            for (int i = 0; i < DcCoreConfig.getUserdataMaxEntityCount(); i++) {
                String entityTypeName = String.format("testEntity%03d", i);
                createEntityType(entityTypeName, Setup.TEST_ODATA);
            }
            String entityTypeName = "testEntityExceedsTheLimit";
            TResponse res = createEntityType(entityTypeName, Setup.TEST_ODATA);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            for (int i = 0; i < DcCoreConfig.getUserdataMaxEntityCount(); i++) {
                String entityTypeName = String.format("testEntity%03d", i);
                EntityTypeUtils.delete(Setup.TEST_ODATA,
                        MASTER_TOKEN_NAME, "application/json", entityTypeName, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            }
            String entityTypeName = "testEntityExceedsTheLimit";
            EntityTypeUtils.delete(Setup.TEST_ODATA,
                    MASTER_TOKEN_NAME, "application/json", entityTypeName, Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
        }
    }

    /**
     * EntityTypeを作成する.
     * @param name EntityTypeのName
     * @param odataName oadataコレクション名
     * @return レスポンス
     */
    private TResponse createEntityType(String name, String odataName) {
        return Http.request("box/entitySet-post.txt")
                .with("cellPath", "testcell1")
                .with("boxPath", "box1")
                .with("odataSvcPath", odataName)
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", "Bearer " + DcCoreConfig.getMasterToken())
                .with("Name", name)
                .returns()
                .debug();
    }
}
