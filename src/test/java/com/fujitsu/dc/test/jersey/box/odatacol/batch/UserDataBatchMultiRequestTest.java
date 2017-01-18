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
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveGetBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveGetResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
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
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;

/**
 * UserData$batch内に複数のリクエストが指定された場合のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchMultiRequestTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchMultiRequestTest() {
        super();
    }

    /**
     * 正しいリクエストが2つ指定された場合全て正常終了すること.
     */
    @Test
    public final void 正しいリクエストが2つ指定された場合全て正常終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest1", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_OK);
            // リンク情報のチェック(SalesDetail→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey", "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest1"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(SalesDetail('npTest1')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "npTest1",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "npTest1", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * 正しいリクエストが1つと文法に誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること.
     */
    @Test
    public final void 正しいリクエストが1つと文法に誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること() {
        String path = "Sales('srcKey')/";
        String code = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + path + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                + START_BOUNDARY + retrievePostBody(path, "npTest1")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);

        // ユーザデータ取得
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey", HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 正しいリクエストが１つとデータに誤りのあるリクエストが1つ指定された場合1件のみエラーとなること.
     */
    @Test
    public final void 正しいリクエストが１つとデータに誤りのあるリクエストが1つ指定された場合1件のみエラーとなること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "__npTest1")
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
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "Sales", "srcKey", HttpStatus.SC_OK);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(),
                    HttpStatus.SC_NO_CONTENT);
        }
    }

    /**
     * 文法に誤りのあるリクエストが1つとデータに誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること.
     */
    @Test
    public final void 文法に誤りのあるリクエストが1つとデータに誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること() {
        String path = "Sales('srcKey')/";
        String code = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + path + " HTTP/1.1")
                .getMessage();
        String body = START_BOUNDARY + retrievePostBody("Sales", "__srcKey")
                + START_BOUNDARY + retrievePostBody(path, "npTest1")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);
    }

    /**
     * 正しいリクエストが10個指定された場合全て正常終了すること.
     */
    @Test
    public final void 正しいリクエストが10個指定された場合全て正常終了すること() {
        try {
            String path1 = "Sales('srcKey1')/_SalesDetail";
            String path2 = "Sales('srcKey2')/_SalesDetail";
            String path3 = "Sales('srcKey3')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey1")
                    + START_BOUNDARY + retrievePostBody("Sales", "srcKey2")
                    + START_BOUNDARY + retrievePostBody("Sales", "srcKey3")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey1')")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey2')")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey3')")
                    + START_BOUNDARY + retrievePostBody(path1, "npTest1")
                    + START_BOUNDARY + retrievePostBody(path2, "npTest2")
                    + START_BOUNDARY + retrievePostBody(path3, "npTest3")
                    + START_BOUNDARY + retrieveGetBody("SalesDetail('npTest1')")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey1", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey2", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey3", true)
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey1")
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey2")
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey3")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest1", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest2", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest3", true)
                    + START_BOUNDARY + retrieveGetResBody("SalesDetail", "npTest1")
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック(Sales→SalesDetail)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey1", "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest1"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey2", "SalesDetail");
            expectedUriList.clear();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest2"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey3", "SalesDetail");
            expectedUriList.clear();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest3"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            // リンク情報の削除
            ResourceUtils.deleteUserDataLinks("srcKey1", "npTest1", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey2", "npTest2", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey3", "npTest3", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);

            // UserODataの削除
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest2", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest3", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey2", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey3", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * 正しいリクエストが9個と文法に誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること.
     */
    @Test
    public final void 正しいリクエストが9個と文法に誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること() {
        String path1 = "Sales('srcKey1')/_SalesDetail";
        String invalidPath = "Sales('srcKey2')/";
        String path3 = "Sales('srcKey3')/_SalesDetail";
        String code = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + invalidPath + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey1")
                + START_BOUNDARY + retrievePostBody("Sales", "srcKey2")
                + START_BOUNDARY + retrievePostBody("Sales", "srcKey3")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey1')")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey2')")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey3')")
                + START_BOUNDARY + retrievePostBody(path1, "npTest1")
                + START_BOUNDARY + retrievePostBody(invalidPath, "npTest2")
                + START_BOUNDARY + retrievePostBody(path3, "npTest3")
                + START_BOUNDARY + retrieveGetBody("SalesDetail('npTest1')")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);

        // ユーザデータが作成されていないことを確認
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey1", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey2", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey3", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest2", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest3", HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 正しいリクエストが9個とデータに誤りのあるリクエストが1つ指定された場合1件のみエラーとなること.
     */
    @Test
    public final void 正しいリクエストが9個とデータに誤りのあるリクエストが1つ指定された場合1件のみエラーとなること() {
        try {
            String path1 = "Sales('srcKey1')/_SalesDetail";
            String invalidEntityPath = "Sales('srcKey2')/_InvalidEntity";
            String path3 = "Sales('srcKey3')/_SalesDetail";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey1")
                    + START_BOUNDARY + retrievePostBody("Sales", "srcKey2")
                    + START_BOUNDARY + retrievePostBody("Sales", "srcKey3")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey1')")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey2')")
                    + START_BOUNDARY + retrieveGetBody("Sales('srcKey3')")
                    + START_BOUNDARY + retrievePostBody(path1, "npTest1")
                    + START_BOUNDARY + retrievePostBody(invalidEntityPath, "npTest2")
                    + START_BOUNDARY + retrievePostBody(path3, "npTest3")
                    + START_BOUNDARY + retrieveGetBody("SalesDetail('npTest1')")
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
            String expectedBody = START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey1", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey2", true)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("Sales", "srcKey3", true)
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey1")
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey2")
                    + START_BOUNDARY + retrieveGetResBody("Sales", "srcKey3")
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest1", true)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest3", true)
                    + START_BOUNDARY + retrieveGetResBody("SalesDetail", "npTest1")
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // リンク情報のチェック(Sales→SalesDetail)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey1", "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest1"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey2", "SalesDetail");
            expectedUriList.clear();
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey3", "SalesDetail");
            expectedUriList.clear();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest3"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            // リンク情報の削除
            ResourceUtils.deleteUserDataLinks("srcKey1", "npTest1", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey3", "npTest3", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);

            // UserODataの削除
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest3", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey2", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey3", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * 正しいリクエストが8個と文法に誤りのあるリクエストが1つとデータに誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること.
     */
    @Test
    public final void 正しいリクエストが8個と文法に誤りのあるリクエストが1つとデータに誤りのあるリクエストが1つ指定された場合$batch全体が400エラーとなること() {
        String path1 = "Sales('srcKey1')/_SalesDetail";
        String invalidEntityPath = "Sales('srcKey3')/_InvalidEntity";
        String invalidPath = "Sales('srcKey2')/";
        String code = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.getCode();
        String err = DcCoreException.OData.BATCH_BODY_FORMAT_PATH_ERROR.params("POST " + invalidPath + " HTTP/1.1")
                .getMessage();

        String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey1")
                + START_BOUNDARY + retrievePostBody("Sales", "srcKey2")
                + START_BOUNDARY + retrievePostBody("Sales", "srcKey3")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey1')")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey2')")
                + START_BOUNDARY + retrieveGetBody("Sales('srcKey3')")
                + START_BOUNDARY + retrievePostBody(path1, "npTest1")
                + START_BOUNDARY + retrievePostBody(invalidEntityPath, "npTest2")
                + START_BOUNDARY + retrievePostBody(invalidPath, "npTest3")
                + START_BOUNDARY + retrieveGetBody("SalesDetail('npTest1')")
                + END_BOUNDARY;
        Http.request("box/odatacol/batch.txt")
                .with("cell", cellName)
                .with("box", boxName)
                .with("collection", colName)
                .with("boundary", BOUNDARY)
                .with("token", DcCoreConfig.getMasterToken())
                .with("body", body)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .checkErrorResponse(code, err);

        // ユーザデータが作成されていないことを確認
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey1", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey2", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "Sales", "srcKey3", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest2", HttpStatus.SC_NOT_FOUND);
        UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                "SalesDetail", "npTest3", HttpStatus.SC_NOT_FOUND);
    }

}
