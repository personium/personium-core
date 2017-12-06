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
package io.personium.test.jersey.box.odatacol.batch;

import static io.personium.test.utils.BatchUtils.BOUNDARY;
import static io.personium.test.utils.BatchUtils.END_BOUNDARY;
import static io.personium.test.utils.BatchUtils.START_BOUNDARY;
import static io.personium.test.utils.BatchUtils.retrieveGetBody;
import static io.personium.test.utils.BatchUtils.retrievePostWithBody;
import static io.personium.test.utils.BatchUtils.retrievePutBody;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.UserDataUtils;

/**
 * UserData$batchのテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchDynamicPropertyTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchDynamicPropertyTest() {
        super();
    }

    /**
     * $batchで新規DynamicPropertyを含むユーザデータを更新後ユーザデータの登録更新をしてPropertyが1件のみ追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchで新規DynamicPropertyを含むユーザデータを更新後ユーザデータの登録更新をしてPropertyが1件のみ追加されること() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // 更新対象のユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", "001");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, colPath, entityTypeName);

            // $batchリクエスト
            JSONObject putBody = new JSONObject();
            putBody.put("__id", "001");
            putBody.put("dynamicProp1", "dynamicPropValue");

            JSONObject postBody = new JSONObject();
            postBody.put("__id", "002");
            postBody.put("dynamicProp1", "dynamicPropValue");

            String batchBody = START_BOUNDARY + retrievePutBody(entityTypeName + "('001')", putBody)
                    + START_BOUNDARY + retrievePutBody(entityTypeName + "('001')", putBody)
                    + START_BOUNDARY + retrievePostWithBody(entityTypeName, postBody)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 1);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, putBody, null, null);
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "002"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, postBody, null, null);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * $batchで新規DynamicPropertyを含むユーザデータを登録後ユーザデータの登録をしてPropertyが1件のみ追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchで新規DynamicPropertyを含むユーザデータを登録後ユーザデータの登録をしてPropertyが1件のみ追加されること() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // $batchリクエスト
            JSONObject postBody1 = new JSONObject();
            postBody1.put("__id", "001");
            postBody1.put("dynamicProp1", "dynamicPropValue");

            JSONObject postBody2 = new JSONObject();
            postBody2.put("__id", "002");
            postBody2.put("dynamicProp1", "dynamicPropValue");

            String batchBody = START_BOUNDARY + retrievePostWithBody(entityTypeName, postBody1)
                    + START_BOUNDARY + retrievePostWithBody(entityTypeName, postBody2)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 1);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, postBody1, null, null);
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "002"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, postBody2, null, null);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * $batchで新規DynamicPropertyを含むユーザデータを登録後ユーザデータの更新をしてPropertyが1件のみ追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchで新規DynamicPropertyを含むユーザデータを登録後ユーザデータの更新をしてPropertyが1件のみ追加されること() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // $batchリクエスト
            JSONObject batchBody1 = new JSONObject();
            batchBody1.put("__id", "001");
            batchBody1.put("dynamicProp1", "dynamicPropValue");

            JSONObject batchBody2 = new JSONObject();
            batchBody2.put("__id", "001");
            batchBody2.put("dynamicProp1", "dynamicPropValue2");

            String batchBody = START_BOUNDARY + retrievePostWithBody(entityTypeName, batchBody1)
                    + START_BOUNDARY + retrievePutBody(entityTypeName + "('001')", batchBody2)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 1);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody2, null, null);
        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * $batchで登録済みDynamicPropertyを含むユーザデータを更新してPropertyが追加されないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchで登録済みDynamicPropertyを含むユーザデータを更新してPropertyが追加されないこと() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // 更新対象のユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", "001");
            body.put("dynamicProp1", "dynamicPropValue");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, colPath, entityTypeName);

            // $batchリクエスト
            JSONObject batchBody1 = new JSONObject();
            batchBody1.put("__id", "001");
            batchBody1.put("dynamicProp1", "dynamicPropValue2");

            String batchBody = START_BOUNDARY
                    + retrievePutBody(entityTypeName + "('001')", batchBody1)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 1);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody1, null, null);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * $batchで登録済みDynamicPropertyを含むユーザデータを登録してPropertyが追加されないこと.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchで登録済みDynamicPropertyを含むユーザデータを登録してPropertyが追加されないこと() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // ユーザデータ作成
            JSONObject body = new JSONObject();
            body.put("__id", "001");
            body.put("dynamicProp1", "dynamicPropValue");
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, Setup.TEST_CELL1,
                    Setup.TEST_BOX1, colPath, entityTypeName);

            // $batchリクエスト
            JSONObject batchBody1 = new JSONObject();
            batchBody1.put("__id", "002");
            batchBody1.put("dynamicProp1", "dynamicPropValue2");

            String batchBody = START_BOUNDARY
                    + retrievePostWithBody(entityTypeName, batchBody1)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 1);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, body, null, null);
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "002"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody1, null, null);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * $batchでクエリオペレーションを間に含むユーザデータ登録で必要なPropertyのみ追加されること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchでクエリオペレーションを間に含むユーザデータ登録で必要なPropertyのみ追加されること() {
        String colPath = "batchDynamicTest";
        String entityTypeName = "entityTypeBatch";
        try {

            // コレクション作成
            DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, Setup.TEST_CELL1,
                    Setup.TEST_BOX1,
                    colPath);

            // EntityType作成
            EntityTypeUtils.create(Setup.TEST_CELL1, MASTER_TOKEN_NAME, Setup.TEST_BOX1, colPath,
                    entityTypeName, HttpStatus.SC_CREATED);

            // $batchリクエスト
            JSONObject batchBody1 = new JSONObject();
            batchBody1.put("__id", "001");
            batchBody1.put("dynamicProp1", "dynamicPropValue1"); // 新規Property使用

            JSONObject batchBody2 = new JSONObject();
            batchBody2.put("__id", "002");
            batchBody2.put("dynamicProp1", "dynamicPropValue1"); // 既存Property使用

            JSONObject batchBody3 = new JSONObject();
            batchBody3.put("__id", "003");
            batchBody3.put("dynamicProp2", "dynamicPropValue1"); // 新規Property使用

            String batchBody = START_BOUNDARY
                    + retrievePostWithBody(entityTypeName, batchBody1)
                    + START_BOUNDARY + retrieveGetBody(entityTypeName + "('001')")
                    + START_BOUNDARY + retrievePostWithBody(entityTypeName, batchBody2)
                    + START_BOUNDARY + retrievePostWithBody(entityTypeName, batchBody3)
                    + END_BOUNDARY;
            Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colPath)
                    .with("boundary", BOUNDARY)
                    .with("token", PersoniumUnitConfig.getMasterToken())
                    .with("body", batchBody)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // Property一覧
            String locationUrlGet = UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, null, null);
            PersoniumRequest req = PersoniumRequest.get(locationUrlGet + "?$inlinecount=allpages");
            req.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            PersoniumResponse res = request(req);
            ODataCommon.checkResponseBodyCount(res.bodyAsJson(), 2);

            // UserData取得
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "001"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody1, null, null);
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "002"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody2, null, null);
            res = ODataCommon.getOdataResource(UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath,
                    entityTypeName, "003"));
            ODataCommon.checkResponseBody(res.bodyAsJson(), null, UserDataODataProducer.USER_ODATA_NAMESPACE + "."
                    + entityTypeName, batchBody3, null, null);

        } finally {
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "001",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "002",
                    PersoniumUnitConfig.getMasterToken(), -1);
            deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, entityTypeName, "003",
                    PersoniumUnitConfig.getMasterToken(), -1);
            EntityTypeUtils.delete(colPath, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityTypeName,
                    Setup.TEST_BOX1, Setup.TEST_CELL1, -1);
            DavResourceUtils.deleteCollection(Setup.TEST_CELL1, Setup.TEST_BOX1, colPath, MASTER_TOKEN_NAME, -1);
        }
    }
}
