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
package com.fujitsu.dc.test.jersey.box.odatacol.batch;

import static com.fujitsu.dc.test.utils.BatchUtils.BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.END_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.START_BOUNDARY;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveChangeSetResErrorBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveGetBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveListBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBodyOfProperty;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostNoneBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostWithBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePutBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePutResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveQueryOperationResErrorBody;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.CompareJSON;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.odatacol.schema.property.PropertyUtils;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData$batchのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchWithNPTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchWithNPTest() {
        super();
    }

    /**
     * $batchで既存のユーザデータからNavPro経由で別のユーザデータが登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで既存のユーザデータからNavPro経由で別のユーザデータが登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");

            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0002")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0002", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで新規のユーザデータからNavPro経由で別のユーザデータが登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで新規のユーザデータからNavPro経由で別のユーザデータが登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0002")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0002", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで存在しないユーザデータからNavPro経由で別のユーザデータが登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで存在しないユーザデータからNavPro経由で別のユーザデータが登録できないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0002")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータが登録されていないことの確認
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0001",
                    HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0002",
                    HttpStatus.SC_NOT_FOUND);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで存在しないEntityのユーザデータからNavPro経由で別のユーザデータが登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで存在しないEntityのユーザデータからNavPro経由で別のユーザデータが登録できないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "XXXXXSales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0002")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータが登録されていないことの確認
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0001",
                    HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0002",
                    HttpStatus.SC_NOT_FOUND);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで既存のユーザデータからNavPro経由で同じ__idのユーザデータが登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで既存のユーザデータからNavPro経由で同じ__idのユーザデータが登録できないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");
            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで既存のユーザデータからNavPro経由で登録済みの__idを指定した場合ユーザデータが登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで既存のユーザデータからNavPro経由で登録済みの__idを指定した場合ユーザデータが登録できないこと() {
        try {
            // 事前準備
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");

            srcBody = new JSONObject();
            srcBody.put("__id", "id0001");
            srcBody.put("Name", "key0002");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Product");

            // $batch前のデータ取得
            TResponse original = UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Product", "id0001", HttpStatus.SC_OK);

            // $batch
            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得 書き換わっていない
            TResponse after = UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Product", "id0001", HttpStatus.SC_OK);
            CompareJSON.Result res = CompareJSON.compareJSON(original.bodyAsJson(), after.bodyAsJson());
            assertNull(res);

            // リンク情報が追加されていない
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで既存のユーザデータからNavPro経由で最大プロパティ数のユーザデータが登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで既存のユーザデータからNavPro経由で最大プロパティ数のユーザデータが登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");

            int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType();
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, Setup.TEST_ENTITYTYPE_MN);
            String path = Setup.TEST_ENTITYTYPE_MN + "('srcKey')/_" + Setup.TEST_ENTITYTYPE_MDP;
            String body = START_BOUNDARY
                    + retrievePostBodyOfProperty(path, "id0001", maxPropNum)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol(Setup.TEST_ENTITYTYPE_MDP, "id0001", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(
                    cellName, boxName, colName,
                    Setup.TEST_ENTITYTYPE_MN, "srcKey",
                    Setup.TEST_ENTITYTYPE_MDP);
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, Setup.TEST_ENTITYTYPE_MDP, "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", Setup.TEST_ENTITYTYPE_MDP, cellName, boxName,
                    colName, Setup.TEST_ENTITYTYPE_MN, -1);
            deleteUserData(cellName, boxName, colName, Setup.TEST_ENTITYTYPE_MDP, "id0001",
                    DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, Setup.TEST_ENTITYTYPE_MN, "srcKey",
                    DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで既存のユーザデータからNavPro経由で最大プロパティ数を超えたユーザデータが登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで既存のユーザデータからNavPro経由で最大プロパティ数を超えたユーザデータが登録できないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");

            int maxPropNum = DcCoreConfig.getMaxPropertyCountInEntityType() + 1;
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");
            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBodyOfProperty(path, "id0001", maxPropNum)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでNavPro経由でのGETができないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchでNavPro経由でのGETができないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "Sales");
            String body = START_BOUNDARY
                    + retrieveListBody("Sales('srcKey')/_Product")
                    + START_BOUNDARY + retrievePostBody("Sales('srcKey')/_Product('id0001')", "id0001")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey')/_Product('id0001')")
                    + START_BOUNDARY + retrievePutBody("Sales('srcKey')/_Product('id0001')")
                    + START_BOUNDARY + retrieveDeleteBody("Sales('srcKey')/_Product('id0001')")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrieveQueryOperationResErrorBody(HttpStatus.SC_NOT_IMPLEMENTED)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveQueryOperationResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で別のメソッドをはさんだ場合正常に登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録で別のメソッドをはさんだ場合正常に登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");

            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePutBody("Product('id0001')")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0002")
                    + END_BOUNDARY;

            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                    + START_BOUNDARY
                    + retrievePutResBody()
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0002", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で別のメソッドをはさんで同じIDを登録した場合409になること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録で別のメソッドをはさんで同じIDを登録した場合409になること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");

            String path = "Sales('srcKey')/_Product";
            String body = START_BOUNDARY
                    + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + START_BOUNDARY
                    + retrievePutBody("Product('id0001')")
                    + START_BOUNDARY
                    + retrievePostBody(path, "id0001")
                    + END_BOUNDARY;

            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "id0001", true)
                    + START_BOUNDARY
                    + retrievePutResBody()
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Product", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0002", "Product", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で異なるEntityTypeに対して同じ__idを指定して作成できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で異なるEntityTypeに対して同じ__idを指定して作成できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody("Sales('srcKey')/_Supplier", "id0001")
                    + START_BOUNDARY + retrievePostBody("Product('srcKey')/_Sales", "id0001")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "id0001", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Supplier", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);

            // リンク情報のチェック(Sales→Supplier)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Supplier");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Supplier", "id0001",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Product→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Product", "srcKey",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "id0001",
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Supplier", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Sales", cellName, boxName, colName, "Product", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでリンク関係にないユーザデータがNavPro経由で登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchでリンク関係にないユーザデータがNavPro経由で登録できないこと() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Product('srcKey')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND) // id0001: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "id0001", HttpStatus.SC_NOT_FOUND);

        } finally {
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データの__idが不正文字で指定された場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データの__idが不正文字で指定された場合登録できないこと() {
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "src/Key")
                    + START_BOUNDARY + retrievePostBody(path, "src@Key")
                    + START_BOUNDARY + retrievePostBody(path, "_srcKey")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "src/Key", HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "src@Key", HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "_srcKey", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データに空白文字を含む__idを指定された場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データに空白文字を含む__idを指定された場合登録できないこと() {
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "src Key")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "src%20Key", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データに空文字で__idを指定された場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データに空文字で__idを指定された場合登録できないこと() {
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データの__idが200文字指定された場合登録できること.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データの__idが200文字指定された場合登録できること() {
        String id = "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890";

        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, id)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", id, true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", id, HttpStatus.SC_OK);
            // リンク情報のチェック(Product→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Product", "srcKey",
                    "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0001')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", id,
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", id, "Sales", cellName, boxName, colName,
                    "Product", -1);
            deleteUserData(cellName, boxName, colName, "Sales", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データの__idが201文字指定された場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データの__idが201文字指定された場合登録できないこと() {
        String id = "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890"
                + "12345678901234567890123456789012345678901234567890a";

        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, id)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", id, HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録データの__idにnullが指定された場合登録できること.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録データの__idにnullが指定された場合登録できること() {
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, null)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "null", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "null", HttpStatus.SC_OK);
            // リンク情報のチェック(Product→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Product", "srcKey",
                    "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "null"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0001')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "null",
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "null", "Sales", cellName, boxName, colName,
                    "Product", -1);
            deleteUserData(cellName, boxName, colName, "Sales", "null", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録でリクエストボディが指定されていない場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録でリクエストボディが指定されていない場合登録できないこと() {
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostNoneBody(path)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録でリクエストボディが空の場合登録できないこと.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録でリクエストボディが空の場合登録できないこと() {
        String id = null;
        try {
            String path = "Product('srcKey')/_Sales";
            JSONObject batchBody = new JSONObject();
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostWithBody(path, batchBody)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Sales", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", "srcKey", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", id, "Sales", cellName, boxName, colName,
                    "Product", -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", id, DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録でリクエストボディのプロパティ型が不正の場合登録できないこと.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録でリクエストボディのプロパティ型が不正の場合登録できないこと() {
        String propName = "p_name_" + String.valueOf(System.currentTimeMillis());
        String locationUrl =
                UrlUtils.property(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, propName,
                        "Sales");
        try {
            // リクエストパラメータ設定
            DcRequest req = DcRequest.post(PropertyUtils.REQUEST_URL);
            req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN);
            req.addJsonBody(PropertyUtils.PROPERTY_NAME_KEY, propName);
            req.addJsonBody(PropertyUtils.PROPERTY_ENTITYTYPE_NAME_KEY, "Sales");
            req.addJsonBody(PropertyUtils.PROPERTY_TYPE_KEY, EdmSimpleType.BOOLEAN.getFullyQualifiedTypeName());

            // リクエスト実行
            request(req);

            JSONObject batchBody = new JSONObject();
            batchBody.put("__id", "test_invalid_propValue");
            batchBody.put(propName, "string");
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostWithBody(path, batchBody)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "test_invalid_propValue", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
            // 作成したPropertyを削除
            assertEquals(HttpStatus.SC_NO_CONTENT, ODataCommon.deleteOdataResource(locationUrl).getStatusCode());
        }
    }

    /**
     * $batchのNP経由ユーザデータ登録でNavProp名が指定されていない場合400レスポンスが返却されること.
     */
    @Test
    public final void $batchのNP経由ユーザデータ登録でNavProp名が指定されていない場合400レスポンスが返却されること() {
        String path = "Sales('srcKey')/";
        String code = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + path + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY
                + retrievePostBody(path, "id0001")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err)
                .debug();
    }

    /**
     * $batchのNP経由ユーザデータ登録でNavProp名が_のみの場合404となること.
     */
    @Test
    public final void $batchのNP経由ユーザデータ登録でNavProp名が_のみの場合404となること() {
        String path = "Sales('srcKey')/_";

        String body = START_BOUNDARY
                + retrievePostBody(path, "id0001")
                + END_BOUNDARY;
        TResponse response = Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .statusCode(HttpStatus.SC_ACCEPTED)
                .debug();

        // レスポンスボディのチェック
        String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                + END_BOUNDARY;
        checkBatchResponseBody(response, expectedBody);

    }

    /**
     * $batchのNP経由ユーザデータ登録でNavProp名に異常な文字列を指定した場合404となること.
     */
    @Test
    public final void $batchのNP経由ユーザデータ登録でNavProp名に異常な文字列を指定した場合404となること() {
        String path = "Sales('srcKey')/_SalesDe test";

        try {
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken())
                    .with("body", body)
                    .returns()
                    .statusCode(HttpStatus.SC_ACCEPTED)
                    .debug();

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }

    }

}
