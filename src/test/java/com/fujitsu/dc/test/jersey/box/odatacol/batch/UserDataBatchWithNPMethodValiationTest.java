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
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveDeleteResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePutBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePutResBody;

import java.util.ArrayList;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
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
 * UserData$batchのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataBatchWithNPMethodValiationTest extends AbstractUserDataBatchTest {

    /**
     * コンストラクタ.
     */
    public UserDataBatchWithNPMethodValiationTest() {
        super();
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録1_登録2_削除1_登録1_の順で実行した場合正常に終了すること.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録1_登録2_削除1_登録1_の順で実行した場合正常に終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String delPath = "SalesDetail('npTest1')";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrievePostBody(path, "npTest2")
                    + START_BOUNDARY + retrieveDeleteBody(delPath)
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest2", true)
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest1", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest2", HttpStatus.SC_OK);
            // リンク情報のチェック(SalesDetail→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey", "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest1"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest2"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(SalesDetail('npTest1')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "npTest1",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(SalesDetail('npTest2')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "npTest2",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "npTest1", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "npTest2", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest1", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest2", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録1_登録2_削除1_登録3_の順で実行した場合正常に終了すること.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録1_登録2_削除1_登録3_の順で実行した場合正常に終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String delPath = "SalesDetail('npTest1')";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrievePostBody(path, "npTest2")
                    + START_BOUNDARY + retrieveDeleteBody(delPath)
                    + START_BOUNDARY + retrievePostBody(path, "npTest3")
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest2", true)
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest3", true)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest2", HttpStatus.SC_OK);
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest3", HttpStatus.SC_OK);
            // リンク情報のチェック(SalesDetail→Sales)
            TResponse resList = UserDataUtils.listLink(cellName, boxName, colName,
                    "Sales", "srcKey", "SalesDetail");
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest2"));
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "SalesDetail", "npTest3"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);

            // リンク情報のチェック(SalesDetail('npTest1')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "npTest2",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
            // リンク情報のチェック(SalesDetail('npTest2')→Sales)
            resList = UserDataUtils.listLink(cellName, boxName, colName, "SalesDetail", "npTest3",
                    "Sales");
            expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, "Sales", "srcKey"));
            ODataCommon.checkLinResponseBody(resList.bodyAsJson(), expectedUriList);
        } finally {
            ResourceUtils.deleteUserDataLinks("srcKey", "npTest2", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            ResourceUtils.deleteUserDataLinks("srcKey", "npTest3", "SalesDetail", cellName, boxName, colName,
                    "Sales", -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest2", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "SalesDetail", "npTest3", DcCoreConfig.getMasterToken(), -1);
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録1_削除1_登録1_削除1_の順で実行した場合正常に終了すること.
     */
    @Test
    public final void $batchのNavPro経由ユーザデータ登録で登録1_削除1_登録1_削除1_の順で実行した場合正常に終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String delPath = "SalesDetail('npTest1')";
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrieveDeleteBody(delPath)
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrieveDeleteBody(delPath)
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
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest1", true)
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録1_更新1_削除1_の順で実行した場合正常に終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録で登録1_更新1_削除1_の順で実行した場合正常に終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String singlePathToNp = "SalesDetail('npTest1')";
            JSONObject bulkBody = new JSONObject();
            bulkBody.put("Name", "nameTest");
            bulkBody.put("age", 15);
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrievePutBody(singlePathToNp, bulkBody)
                    + START_BOUNDARY + retrieveDeleteBody(singlePathToNp)
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
                    + START_BOUNDARY + retrievePutResBody()
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }

    /**
     * $batchのNavPro経由ユーザデータ登録で登録1_登録2_更新2_更新1_削除2_削除1_の順で実行した場合正常に終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void $batchのNavPro経由ユーザデータ登録で登録1_登録2_更新2_更新1_削除2_削除1_の順で実行した場合正常に終了すること() {
        try {
            String path = "Sales('srcKey')/_SalesDetail";
            String singlePathToNp1 = "SalesDetail('npTest1')";
            String singlePathToNp2 = "SalesDetail('npTest2')";
            JSONObject bulkBody = new JSONObject();
            bulkBody.put("Name", "nameTest");
            bulkBody.put("age", 15);
            String body = START_BOUNDARY + retrievePostBody("Sales", "srcKey")
                    + START_BOUNDARY + retrievePostBody(path, "npTest1")
                    + START_BOUNDARY + retrievePostBody(path, "npTest2")
                    + START_BOUNDARY + retrievePutBody(singlePathToNp2, bulkBody)
                    + START_BOUNDARY + retrievePutBody(singlePathToNp1, bulkBody)
                    + START_BOUNDARY + retrieveDeleteBody(singlePathToNp2)
                    + START_BOUNDARY + retrieveDeleteBody(singlePathToNp1)
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
                    + START_BOUNDARY + retrievePostResBodyToSetODataCol("SalesDetail", "npTest2", true)
                    + START_BOUNDARY + retrievePutResBody()
                    + START_BOUNDARY + retrievePutResBody()
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + START_BOUNDARY + retrieveDeleteResBody()
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // ユーザデータ取得
            UserDataUtils.get(cellName, DcCoreConfig.getMasterToken(), boxName, colName,
                    "SalesDetail", "npTest1", HttpStatus.SC_NOT_FOUND);
        } finally {
            deleteUserData(cellName, boxName, colName, "Sales", "srcKey", DcCoreConfig.getMasterToken(), -1);
        }
    }
}
