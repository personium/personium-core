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
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBodyNoId;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
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
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData$batchNP経由登録 関係性のバリエーションのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchWithNPRelationTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchWithNPRelationTest() {
        super();
    }

    /**
     * $batchで0_1対0_1のユーザデータをNavPro経由で登録できること.
     */
    @Test
    public final void $batchで0_1対0_1のユーザデータをNavPro経由で登録できること() {
        try {
            String path = "Price('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Price", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Price", "srcKey")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001")
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0002",
                    HttpStatus.SC_NOT_FOUND);

            // リンク情報のチェック(Sales→Price)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "id0001",
                    "Price");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Price", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(Price→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Price", "srcKey",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "id0001", "Sales", cellName, boxName, colName, "Price", -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Price", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで0_1対1のユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで0_1対1のユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Supplier('srcKey')/_Product";
            String body = START_BOUNDARY + retrievePostBody("Supplier", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "srcKey")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", "id0001")
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Product", "id0002",
                    HttpStatus.SC_NOT_FOUND);

            // リンク情報のチェック(Supplier→Product)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "srcKey", "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(Product→Supplier)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", "id0001", "Supplier");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Product", cellName, boxName, colName, "Supplier", -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで0_1対NのユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで0_1対NのユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Sales('srcKey')/_Supplier";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "id0001", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "id0002", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Supplier", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Supplier", "id0002",
                    HttpStatus.SC_OK);

            // リンク情報のチェック(Sales→Supplier)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Supplier");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier('id0001')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Supplier", "id0001",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier('id0002')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Supplier", "id0002",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Supplier", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "Supplier", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで1対0_1のユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで1対0_1のユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Product('srcKey')/_Supplier";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "id0001", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Supplier", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Supplier", "id0002",
                    HttpStatus.SC_NOT_FOUND);

            // リンク情報のチェック(Product→Supplier)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", "srcKey", "Supplier");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "id0001", "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Supplier", cellName, boxName, colName, "Product", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "Supplier", cellName, boxName, colName, "Product", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで1対NのユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで1対NのユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Sales('srcKey')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "id0001", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "id0002", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "SalesDetail",
                    "id0001", HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "SalesDetail",
                    "id0002", HttpStatus.SC_OK);

            // リンク情報のチェック(Sales→SalesDetail)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(SalesDetail('id0001')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "id0001",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(SalesDetail('id0002')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "id0002",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "SalesDetail", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "SalesDetail", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでN対0_1のユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchでN対0_1のユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Supplier('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Supplier", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0002",
                    HttpStatus.SC_NOT_FOUND);

            // リンク情報のチェック(Supplier→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "srcKey", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales→Supplier)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "id0001", "Supplier");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Sales", cellName, boxName, colName, "Supplier", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "Sales", cellName, boxName, colName, "Supplier", -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでN対1のユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchでN対1のユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "SalesDetail('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("SalesDetail", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0002",
                    HttpStatus.SC_NOT_FOUND);

            // リンク情報のチェック(SalesDetail→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "SalesDetail", "srcKey", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales→SalesDetail)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "id0001", "SalesDetail");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Sales", cellName, boxName, colName, "SalesDetail", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "Sales", cellName, boxName, colName, "SalesDetail", -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでN対NのユーザデータをNavPro経由で登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchでN対NのユーザデータをNavPro経由で登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "id0001")
                    + START_BOUNDARY + retrievePostBody(path, "id0002")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0001", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "id0002", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0001",
                    HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "Sales", "id0002",
                    HttpStatus.SC_OK);

            // リンク情報のチェック(Product→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Product", "srcKey",
                    "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0002"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0001')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "id0001",
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0002')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "id0002",
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0001", "Sales", cellName, boxName, colName, "Product", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", "id0002", "Sales", cellName, boxName, colName, "Product", -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで0_1対0_1のユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchで0_1対0_1のユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id = null;
        try {
            String path = "Price('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Price", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Price", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Sales", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック(Sales→Price)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", id,
                    "Price");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Price", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(Price→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Price", "srcKey",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", id, "Sales", cellName, boxName, colName, "Price", -1);
            deleteUserData(cellName, boxName, colName, "Sales", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Price", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで0_1対1のユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchで0_1対1のユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id = null;
        try {
            String path = "Supplier('srcKey')/_Product";
            String body = START_BOUNDARY + retrievePostBody("Supplier", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Product", ".*", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Product", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック(Supplier→Product)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "srcKey", "Product");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(Product→Supplier)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", id, "Supplier");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id, "Product", cellName, boxName, colName, "Supplier", -1);
            deleteUserData(cellName, boxName, colName, "Product", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで0_1対NのユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchで0_1対NのユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id1 = null;
        String id2 = null;
        try {
            String path = "Sales('srcKey')/_Supplier";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", ".*", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", ".*", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Supplier", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            id1 = (String) ((JSONObject) results.get(0)).get("__id");
            id2 = (String) ((JSONObject) results.get(1)).get("__id");

            // リンク情報のチェック(Sales→Supplier)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", "srcKey",
                    "Supplier");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", id1));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", id2));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier('id0001')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Supplier", id1,
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier('id0002')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Supplier", id2,
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id1, "Supplier", cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id2, "Supplier", cellName, boxName, colName, "Sales", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", id1, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", id2, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで1対0_1のユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchで1対0_1のユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id = null;
        try {
            String path = "Product('srcKey')/_Supplier";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", ".*", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Supplier", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック(Product→Supplier)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", "srcKey", "Supplier");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Supplier→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", id, "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id, "Supplier", cellName, boxName, colName, "Product", -1);
            deleteUserData(cellName, boxName, colName, "Supplier", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchで1対NのユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchで1対NのユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id1 = null;
        String id2 = null;
        try {
            // 事前準備
            // EntityType作成
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "srcEntity",
                    HttpStatus.SC_CREATED);
            EntityTypeUtils.create(cellName, DcCoreConfig.getMasterToken(), boxName, colName, "tgtEntity",
                    HttpStatus.SC_CREATED);
            // AssociationEnd作成
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "1", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "ae1", "srcEntity");
            AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", cellName, boxName, colName,
                    HttpStatus.SC_CREATED, "ae2", "tgtEntity");
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                    colName, "srcEntity", "tgtEntity", "ae1", "ae2", HttpStatus.SC_NO_CONTENT);

            // ユーザデータ（ソース側）作成
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");
            super.createUserData(srcBody, HttpStatus.SC_CREATED, cellName, boxName, colName, "srcEntity");

            // $batch
            String path = "srcEntity('srcKey')/_tgtEntity";
            String body = START_BOUNDARY
                    + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY
                    + retrievePostBodyNoId(path, HttpMethod.POST)
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
                    + retrievePostResBodyToSetODataCol("tgtEntity", ".*", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("tgtEntity", ".*", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "tgtEntity", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            id1 = (String) ((JSONObject) results.get(0)).get("__id");
            id2 = (String) ((JSONObject) results.get(1)).get("__id");

            // リンク情報のチェック
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "srcEntity", "srcKey", "tgtEntity");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "tgtEntity", id1));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "tgtEntity", id2));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", id1, "tgtEntity", cellName, boxName, colName,
                    "srcEntity", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", id2, "tgtEntity", cellName, boxName, colName,
                    "srcEntity", -1);
            deleteUserData(cellName, boxName, colName, "tgtEntity", id1, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "tgtEntity", id2, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "srcEntity", "srcKey", DcCoreConfig.getMasterToken(), -1);
            // AssociationEndの削除
            String url = UrlUtils
                    .associationEndLink(cellName, boxName, colName, "ae1", "srcEntity", "ae2", "tgtEntity");
            ODataCommon.deleteOdataResource(url);
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cellName, boxName, colName, "ae1", "srcEntity"));
            ODataCommon.deleteOdataResource(UrlUtils.associationEnd(cellName, boxName, colName, "ae2", "tgtEntity"));
            // EntityTypeの削除
            Setup.entityTypeDelete(colName, "srcEntity", cellName, boxName);
            Setup.entityTypeDelete(colName, "tgtEntity", cellName, boxName);
        }
    }

    /**
     * $batchでN対0_1のユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchでN対0_1のユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id = null;
        try {
            String path = "Supplier('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Supplier", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Supplier", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Sales", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック(Supplier→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "srcKey", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales→Supplier)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", id, "Supplier");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id, "Sales", cellName, boxName, colName, "Supplier", -1);
            deleteUserData(cellName, boxName, colName, "Sales", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでN対1のユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchでN対1のユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id = null;
        try {
            String path = "SalesDetail('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("SalesDetail", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "srcKey", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true)
                    + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_CONFLICT) // id0002: 作成
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Sales", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(1, results.size());
            id = (String) ((JSONObject) results.get(0)).get("__id");

            // リンク情報のチェック(SalesDetail→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "SalesDetail", "srcKey", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales→SalesDetail)
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", id, "SalesDetail");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id, "Sales", cellName, boxName, colName, "SalesDetail", -1);
            deleteUserData(cellName, boxName, colName, "Sales", id, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchでN対NのユーザデータをNavPro経由で__id指定なしで登録できること.
     */
    @Test
    public final void $batchでN対NのユーザデータをNavPro経由で__id指定なしで登録できること() {
        String id1 = null;
        String id2 = null;
        try {
            String path = "Product('srcKey')/_Sales";
            String body = START_BOUNDARY + retrievePostBody("Product", "srcKey")
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
                    + START_BOUNDARY + retrievePostBodyNoId(path, HttpMethod.POST)
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", ".*", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ一覧取得
            response = UserDataUtils.list(cellName, boxName, colName, "Sales", "",
                    DcCoreConfig.getMasterToken(), HttpStatus.SC_OK);
            // ID取得
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, results.size());
            id1 = (String) ((JSONObject) results.get(0)).get("__id");
            id2 = (String) ((JSONObject) results.get(1)).get("__id");

            // リンク情報のチェック(Product→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName, "Product", "srcKey",
                    "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id1));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", id2));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0001')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", id1,
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(Sales('id0002')→Product)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "Sales", id2,
                    "Product");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Product", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id1, "Sales", cellName, boxName, colName, "Product", -1);
            ResourceUtils.deleteUserDataLinks(
                    "srcKey", id2, "Sales", cellName, boxName, colName, "Product", -1);
            deleteUserData(cellName, boxName, colName, "Sales", id1, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", id2, DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で色々な関連を含んでいる場合正常に登録できること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録で色々な関連を含んでいる場合正常に登録できること() {
        try {
            JSONObject srcBody = new JSONObject();
            srcBody.put("__id", "srcKey");
            srcBody.put("Name", "key0001");

            String body = START_BOUNDARY
                    + retrievePostBody("Price", "srcKey001")
                    + START_BOUNDARY
                    + retrievePostBody("Product", "srcKey002")
                    + START_BOUNDARY
                    + retrievePostBody("Sales", "srcKey003")
                    + START_BOUNDARY
                    + retrievePostBody("Supplier", "srcKey004")
                    // 0..1:0..1
                    + START_BOUNDARY
                    + retrievePostBody("Price('srcKey001')/_Sales", "id0001")
                    // N:N
                    + START_BOUNDARY
                    + retrievePostBody("Product('srcKey002')/_Sales", "id0011")
                    // 0..1:N
                    + START_BOUNDARY
                    + retrievePostBody("Sales('srcKey003')/_Supplier", "id0031")
                    // N:N
                    + START_BOUNDARY
                    + retrievePostBody("Product('srcKey002')/_Sales", "id0012")
                    // N:0..1
                    + START_BOUNDARY
                    + retrievePostBody("Supplier('srcKey004')/_Sales", "id0021")
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
                    + retrievePostResBodyToSetODataCol("Price", "srcKey001", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Product", "srcKey002", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "srcKey003", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Supplier", "srcKey004", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "id0001", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "id0011", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Supplier", "id0031", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "id0012", true)
                    + START_BOUNDARY
                    + retrievePostResBodyToSetODataCol("Sales", "id0021", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック Price('srcKey')/_Sales
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Price", "srcKey001", "Sales");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0001"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック Product('srcKey')/_Sales
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Product", "srcKey002", "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0011"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0012"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック Sales('srcKey')/_Supplier
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey003", "Supplier");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Supplier", "id0031"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック Supplier('srcKey')/_Sales
            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Supplier", "srcKey004", "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "id0021"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey001", "id0001", "Sales",
                    cellName, boxName, colName, "Price", -1);
            ResourceUtils.deleteUserDataLinks("srcKey002", "id0011", "Sales",
                    cellName, boxName, colName, "Product", -1);
            ResourceUtils.deleteUserDataLinks("srcKey002", "id0012", "Sales",
                    cellName, boxName, colName, "Product", -1);
            ResourceUtils.deleteUserDataLinks("srcKey003", "id0031", "Supplier",
                    cellName, boxName, colName, "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey004", "id0021", "Sales",
                    cellName, boxName, colName, "Supplier", -1);

            deleteUserData(cellName, boxName, colName, "Sales", "id0001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0011", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0012", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "id0031", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "id0021", DcCoreConfig.getMasterToken(), -1);

            deleteUserData(cellName, boxName, colName, "Price", "srcKey001", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Product", "srcKey002", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey003", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Supplier", "srcKey004", DcCoreConfig.getMasterToken(), -1);

        }
    }

}
