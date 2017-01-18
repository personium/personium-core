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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * $expandクエリ指定のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataExpandTest extends AbstractUserDataTest {

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();

    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put(
                "com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    private String toEntityTypeName = "toEntity";
    private String fromEntityTypeName = "fromEntity";
    private String toUserDataId = "toEntitySet";
    private String fromUserDataId = "fromEntitySet";
    private String toUserDataId2 = "toEntitySet2";
    private String fromUserDataId2 = "fromEntitySet2";

    /**
     * コンストラクタ.
     */
    public UserDataExpandTest() {
        super(new WebAppDescriptor.Builder(UserDataExpandTest.INIT_PARAMS).build());
    }

    /**
     * Keyあり取得時にexpandに指定したリソースの情報が展開されること.
     */
    @Test
    public final void Keyあり取得時にexpandに指定したリソースの情報が展開されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=" + "_" + toEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", fromUserDataId);

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional);

            // toEntity($expandで指定)のデータチェック
            JSONObject dResults = (JSONObject) ((JSONObject) response.bodyAsJson().get("d"));
            JSONObject resultsResults = (JSONObject) ((JSONObject) dResults.get("results"));
            JSONArray expandResults = (JSONArray) resultsResults.get("_" + toEntityTypeName);
            assertEquals(1, expandResults.size());
            nameSpace = getNameSpace(toEntityTypeName);
            Map<String, Object> expandAdditional = new HashMap<String, Object>();
            expandAdditional.put("__id", toUserDataId);
            ODataCommon.checkResults((JSONObject) expandResults.get(0), null, nameSpace, expandAdditional);

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandに指定したリソースの情報が展開されること.
     */
    @Test
    public final void Keyなし取得時にexpandに指定したリソースの情報が展開されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=" + "_" + toEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalforId1 = new HashMap<String, Object>();
            Map<String, Object> additionalforId2 = new HashMap<String, Object>();
            additional.put(fromUserDataId, additionalforId1);
            additional.put(fromUserDataId2, additionalforId2);
            additionalforId1.put("__id", fromUserDataId);
            additionalforId2.put("__id", fromUserDataId2);

            // EntityのURLを組み立てる
            Map<String, String> url = new HashMap<String, String>();
            url.put(fromUserDataId, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId));
            url.put(fromUserDataId2, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId2));

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), url, nameSpace, additional, "__id");

            // toEntity($expandで指定)のデータチェック
            JSONObject dResults = (JSONObject) response.bodyAsJson().get("d");
            JSONArray resultsResults = (JSONArray) dResults.get("results");
            HashMap<String, String> idList = new HashMap<String, String>();
            idList.put(fromUserDataId, toUserDataId);
            idList.put(fromUserDataId2, toUserDataId2);
            for (Object results : resultsResults) {
                JSONArray expandResults = (JSONArray) ((JSONObject) results).get("_" + toEntityTypeName);
                assertEquals(1, expandResults.size());
                nameSpace = getNameSpace(toEntityTypeName);
                Map<String, Object> expandAdditional = new HashMap<String, Object>();
                expandAdditional.put("__id", idList.get(((JSONObject) results).get("__id")));
                ODataCommon.checkResults((JSONObject) expandResults.get(0), null, nameSpace, expandAdditional);
            }
        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandとfilterを指定してexpand指定したリソースの情報が展開されること.
     */
    @Test
    public final void Keyなし取得時にexpandとfilterを指定してexpand指定したリソースの情報が展開されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query",
                            "?\\$filter=__id+eq+%27" + fromUserDataId + "%27&\\$expand=" + "_" + toEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalforId1 = new HashMap<String, Object>();
            additional.put(fromUserDataId, additionalforId1);
            additionalforId1.put("__id", fromUserDataId);

            // EntityのURLを組み立てる
            Map<String, String> url = new HashMap<String, String>();
            url.put(fromUserDataId, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId));

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), url, nameSpace, additional, "__id");

            // toEntity($expandで指定)のデータチェック
            JSONObject dResults = (JSONObject) response.bodyAsJson().get("d");
            JSONArray resultsResults = (JSONArray) dResults.get("results");
            HashMap<String, String> idList = new HashMap<String, String>();
            idList.put(fromUserDataId, toUserDataId);
            for (Object results : resultsResults) {
                JSONArray expandResults = (JSONArray) ((JSONObject) results).get("_" + toEntityTypeName);
                assertEquals(1, expandResults.size());
                nameSpace = getNameSpace(toEntityTypeName);
                Map<String, Object> expandAdditional = new HashMap<String, Object>();
                expandAdditional.put("__id", idList.get(((JSONObject) results).get("__id")));
                ODataCommon.checkResults((JSONObject) expandResults.get(0), null, nameSpace, expandAdditional);
            }
        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * $linksしていないユーザーデータが存在している場合にKeyなし取得するとexpandに指定したリソースの情報が展開されること.
     */
    @Test
    public final void $linksしていないユーザーデータが存在している場合にKeyなし取得するとexpandに指定したリソースの情報が展開されること() {

        try {
            // データ作成
            createData();
            createData2();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt").with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1).with("collection", Setup.TEST_ODATA).with("entityType", navPropName)
                    .with("query", "?\\$expand=" + "_" + toEntityTypeName).with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken()).returns().statusCode(HttpStatus.SC_OK).debug();

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalforId1 = new HashMap<String, Object>();
            Map<String, Object> additionalforId2 = new HashMap<String, Object>();
            Map<String, Object> additionalNoneNp = new HashMap<String, Object>();
            additional.put(fromUserDataId, additionalforId1);
            additional.put(fromUserDataId2, additionalforId2);
            additional.put("noneNP", additionalNoneNp);
            additionalforId1.put("__id", fromUserDataId);
            additionalforId2.put("__id", fromUserDataId2);
            additionalNoneNp.put("__id", "noneNP");

            // EntityのURLを組み立てる
            Map<String, String> url = new HashMap<String, String>();
            url.put(fromUserDataId, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId));
            url.put(fromUserDataId2, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId2));
            url.put("noneNP", UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, "noneNP"));

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            ODataCommon.checkResponseBodyList(response.bodyAsJson(), url, nameSpace, additional, "__id");

            // toEntity($expandで指定)のデータチェック
            JSONObject dResults = (JSONObject) response.bodyAsJson().get("d");
            JSONArray resultsResults = (JSONArray) dResults.get("results");
            HashMap<String, String> idList = new HashMap<String, String>();
            idList.put(fromUserDataId, toUserDataId);
            idList.put(fromUserDataId2, toUserDataId2);
            String id = null;
            for (Object results : resultsResults) {
                JSONArray expandResults = (JSONArray) ((JSONObject) results).get("_" + toEntityTypeName);
                id = ((JSONObject) results).get("__id").toString();
                if (id.equals("noneNP")) {
                    assertEquals(0, expandResults.size());
                } else {
                    assertEquals(1, expandResults.size());
                    nameSpace = getNameSpace(toEntityTypeName);
                    Map<String, Object> expandAdditional = new HashMap<String, Object>();
                    expandAdditional.put("__id", idList.get(id));
                    ODataCommon.checkResults((JSONObject) expandResults.get(0), null, nameSpace, expandAdditional);
                }
            }
        } finally {
            // データ削除
            deleteData2();
            deleteData();
        }
    }

    /**
     * Keyあり取得時にexpandに空文字を指定した場合にexpand指定なしと同じになること.
     */
    @Test
    public final void Keyあり取得時にexpandに空文字を指定した場合にexpand指定なしと同じになること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", fromUserDataId);

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            Map<String, String> np = new HashMap<String, String>();
            np.put("_" + toEntityTypeName, UrlUtils.userdataNP(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                    navPropName, fromUserDataId, toEntityTypeName));
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, nameSpace, additional, np);
        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandに空文字を指定した場合にexpand指定なしと同じになること.
     */
    @Test
    public final void Keyなし取得時にexpandに空文字を指定した場合にexpand指定なしと同じになること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalforId1 = new HashMap<String, Object>();
            Map<String, Object> additionalforId2 = new HashMap<String, Object>();
            additional.put(fromUserDataId, additionalforId1);
            additional.put(fromUserDataId2, additionalforId2);
            additionalforId1.put("__id", fromUserDataId);
            additionalforId2.put("__id", fromUserDataId2);

            // EntityのURLを組み立てる
            Map<String, String> url = new HashMap<String, String>();
            url.put(fromUserDataId, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId));
            url.put(fromUserDataId2, UrlUtils.userdata(Setup.TEST_CELL1, Setup.TEST_BOX1,
                    Setup.TEST_ODATA, navPropName, fromUserDataId2));

            // fromEntityのデータチェック
            String nameSpace = getNameSpace(fromEntityTypeName);
            Map<String, Map<String, String>> np = new HashMap<String, Map<String, String>>();
            Map<String, String> npforId1 = new HashMap<String, String>();
            Map<String, String> npforId2 = new HashMap<String, String>();
            np.put(fromUserDataId, npforId1);
            np.put(fromUserDataId2, npforId2);
            npforId1.put("_" + toEntityTypeName,
                    UrlUtils.userdataNP(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            navPropName, fromUserDataId, toEntityTypeName));
            npforId2.put("_" + toEntityTypeName,
                    UrlUtils.userdataNP(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                            navPropName, fromUserDataId2, toEntityTypeName));

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), url, nameSpace, additional, "__id",
                    ODataCommon.COUNT_NONE, np, null);
        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyあり取得時にexpandに存在するリソースかつアンダースコアなし指定の場合に400が返却されること.
     */
    @Test
    public final void Keyあり取得時にexpandに存在するリソースかつアンダースコアなし指定の場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=" + toEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandに存在するリソースかつアンダースコアなし指定の場合に400が返却されること.
     */
    @Test
    public final void Keyなし取得時にexpandに存在するリソースかつアンダースコアなし指定の場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=" + toEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyあり取得時にexpandにアンダースコアのみを指定した場合に400が返却されること.
     */
    @Test
    public final void Keyあり取得時にexpandにアンダースコアのみを指定した場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=_")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandにアンダースコアのみを指定した場合に400が返却されること.
     */
    @Test
    public final void Keyなし取得時にexpandにアンダースコアのみを指定した場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=_")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyあり取得時にexpandに存在しないリースを指定した場合に400が返却されること.
     */
    @Test
    public final void Keyあり取得時にexpandに存在しないリースを指定した場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=_test")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得時にexpandに存在しないリースを指定した場合に400が返却されること.
     */
    @Test
    public final void Keyなし取得時にexpandに存在しないリースを指定した場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=_test")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyあり取得かつexpand複数指定時に存在しないリソースが含まれる場合に400が返却されること.
     */
    @Test
    public final void Keyあり取得かつexpand複数指定時に存在しないリソースが含まれる場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName + "('" + fromUserDataId + "')")
                    .with("query", "?\\$expand=" + "_" + toEntityTypeName + ",_test")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * Keyなし取得かつexpand複数指定時に存在しないリソースが含まれる場合に400が返却されること.
     */
    @Test
    public final void Keyなし取得かつexpand複数指定時に存在しないリソースが含まれる場合に400が返却されること() {

        try {
            // データ作成
            createData();

            // $expandを指定してデータを取得
            Http.request("box/odatacol/list.txt")
                    .with("cell", Setup.TEST_CELL1)
                    .with("box", Setup.TEST_BOX1)
                    .with("collection", Setup.TEST_ODATA)
                    .with("entityType", navPropName)
                    .with("query", "?\\$expand=" + "_" + toEntityTypeName + ",_test")
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST)
                    .debug();

        } finally {
            // データ削除
            deleteData();
        }
    }

    /**
     * データ作成処理.
     */
    @SuppressWarnings("unchecked")
    public final void createData() {
        navPropName = fromEntityTypeName;
        JSONObject body = new JSONObject();
        JSONObject linkBody = new JSONObject();

        // エンティティタイプを作成
        EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                Setup.TEST_ODATA, toEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(Setup.TEST_CELL1, DcCoreConfig.getMasterToken(),
                Setup.TEST_ODATA, navPropName, HttpStatus.SC_CREATED);

        // AssociationEndを作成
        AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "AssociationEnd", toEntityTypeName);
        AssociationEndUtils.create(DcCoreConfig.getMasterToken(), "*", Setup.TEST_CELL1,
                Setup.TEST_BOX1, Setup.TEST_ODATA, HttpStatus.SC_CREATED, "LinkAssociationEnd", navPropName);

        // AssociationEndを関連付け
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1, Setup.TEST_BOX1,
                Setup.TEST_ODATA, toEntityTypeName, navPropName, "AssociationEnd", "LinkAssociationEnd",
                HttpStatus.SC_NO_CONTENT);

        // ユーザデータを作成
        body.put("__id", toUserDataId);
        linkBody.put("__id", fromUserDataId);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        body.put("__id", toUserDataId2);
        linkBody.put("__id", fromUserDataId2);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName);
        createUserData(linkBody, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName);

        // ユーザデータ-ユーザデータの$links作成
        String targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId + "')")
                .with("trgPath", navPropName)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);

        targetUri = UrlUtils.cellRoot(Setup.TEST_CELL1) + Setup.TEST_BOX1 + "/"
                + Setup.TEST_ODATA + "/" + navPropName + "('" + fromUserDataId2 + "')";
        Http.request("link-userdata-userdata.txt")
                .with("cellPath", Setup.TEST_CELL1)
                .with("boxPath", Setup.TEST_BOX1)
                .with("colPath", Setup.TEST_ODATA)
                .with("srcPath", toEntityTypeName + "('" + toUserDataId2 + "')")
                .with("trgPath", navPropName)
                .with("token", DcCoreConfig.getMasterToken())
                .with("trgUserdataUrl", targetUri)
                .returns()
                .debug()
                .statusCode(HttpStatus.SC_NO_CONTENT);
    }

    /**
     * データ削除処理.
     */
    public final void deleteData() {
        entityTypeName = toEntityTypeName;

        // ユーザデータ-ユーザデータの$links削除
        deleteUserDataLinks(toUserDataId, fromUserDataId);
        deleteUserDataLinks(toUserDataId2, fromUserDataId2);

        // ユーザデータを削除
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName, toUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName, fromUserDataId, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                toEntityTypeName, toUserDataId2, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                navPropName, fromUserDataId2, DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

        // AssociationEndLinkを削除
        AssociationEndUtils.deleteLink(Setup.TEST_CELL1, Setup.TEST_ODATA,
                Setup.TEST_BOX1, "Name='AssociationEnd',_EntityType.Name='" + toEntityTypeName + "'",
                "Name='LinkAssociationEnd',_EntityType.Name='" + navPropName + "'", HttpStatus.SC_NO_CONTENT);

        // AssociationEndを削除
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                Setup.TEST_ODATA, toEntityTypeName, Setup.TEST_BOX1, "AssociationEnd", HttpStatus.SC_NO_CONTENT);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                Setup.TEST_ODATA, navPropName, Setup.TEST_BOX1, "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);

        // エンティティタイプを削除
        EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                "application/json", toEntityTypeName, Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
        EntityTypeUtils.delete(Setup.TEST_ODATA, DcCoreConfig.getMasterToken(),
                "application/json", navPropName, Setup.TEST_CELL1, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * データ削除処理.
     */
    public final void deleteData2() {

        entityTypeName = toEntityTypeName;

        // ユーザデータを削除
        deleteUserData(Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA, fromEntityTypeName, "noneNP",
                DcCoreConfig.getMasterToken(), HttpStatus.SC_NO_CONTENT);

    }

    /**
     * データ作成処理.
     */
    @SuppressWarnings("unchecked")
    public final void createData2() {
        navPropName = fromEntityTypeName;
        JSONObject body = new JSONObject();

        // ユーザデータを作成
        body.put("__id", "noneNP");
        // linkBody.put("__id", fromUserDataId);
        createUserData(body, HttpStatus.SC_CREATED, Setup.TEST_CELL1, Setup.TEST_BOX1, Setup.TEST_ODATA,
                fromEntityTypeName);
    }

    /**
     * $expandによるデータ一件取得が正常に行えること.
     */
    @Test
    public final void $expandによるデータ一件取得が正常に行えること() {
        final int top = 10;
        final String baseEntity = "baseEntityType";
        final String target0101Entity = "target01-01EntityType";
        final String target01NEntity = "target01-NEntityType";
        final String targetN01Entity = "targetN-01EntityType";
        final String targetNNEntity = "targetN-NEntityType";

        try {
            // データ作成
            createExpandData("expandTestCell", "box", "odata");

            // $expandを指定してデータを取得
            // プロパティの上限数に合わせて2プロパティずつ確認
            String expands = String.format("?\\$expand=_%s,_%s", target0101Entity, target01NEntity);
            TResponse response = UserDataUtils.getWithQuery("expandTestCell", MASTER_TOKEN_NAME, "box", "odata",
                    baseEntity, expands, "fromId1", HttpStatus.SC_OK);
            JSONObject body = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            assertEquals("fromId1", body.get("__id"));
            checkResponseBodyAsExpand("fromId1", target0101Entity, body, 1);
            checkResponseBodyAsExpand("fromId1", target01NEntity, body, top);

            // $expandを指定してデータを取得
            expands = String.format("?\\$expand=_%s,_%s", targetN01Entity, targetNNEntity);
            response = UserDataUtils.getWithQuery("expandTestCell", MASTER_TOKEN_NAME, "box", "odata",
                    baseEntity, expands, "fromId1", HttpStatus.SC_OK);
            body = (JSONObject) ((JSONObject) response.bodyAsJson().get("d")).get("results");

            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            assertEquals("fromId1", body.get("__id"));
            checkResponseBodyAsExpand("fromId1", targetN01Entity, body, 1);
            checkResponseBodyAsExpand("fromId1", targetNNEntity, body, top);

            // $expandクエリとその他のクエリを組み合わせても取得可能か
            expands = String.format("\\$expand=_%s,_%s", targetN01Entity, targetNNEntity);
            String query = "?\\$select=__id&" + expands;
            response = UserDataUtils.getWithQuery("expandTestCell", MASTER_TOKEN_NAME, "box", "odata",
                    baseEntity, query, "fromId1", HttpStatus.SC_OK);

        } finally {
            Setup.cellBulkDeletion("expandTestCell");
        }
    }

    /**
     * $expandによるデータ一覧取得が正常に行えること.
     */
    @Test
    public final void $expandによるデータ一覧取得が正常に行えること() {
        final int top = 10;
        final String baseEntity = "baseEntityType";
        final String target0101Entity = "target01-01EntityType";
        final String target01NEntity = "target01-NEntityType";
        final String targetN01Entity = "targetN-01EntityType";
        final String targetNNEntity = "targetN-NEntityType";

        try {
            // データ作成
            createExpandData("expandTestCell", "box", "odata");

            // $expandを指定してデータを取得
            // プロパティの上限数に合わせて2プロパティずつ確認
            String expands = String.format("_%s,_%s", target0101Entity, target01NEntity);
            TResponse response = listUserODataWithExpand("expandTestCell", "box", "odata", baseEntity,
                    expands, top, HttpStatus.SC_OK);
            JSONArray body = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, body.size());

            // ベース側1つ目(fromId1)の取得結果をチェックする。
            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            JSONObject fromId1Body = (JSONObject) body.get(0);
            assertEquals("fromId1", fromId1Body.get("__id"));
            checkResponseBodyAsExpand("fromId1", target0101Entity, fromId1Body, 1);
            checkResponseBodyAsExpand("fromId1", target01NEntity, fromId1Body, top);

            // ベース側2つ目(fromId2)の取得結果をチェックする。
            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            JSONObject fromId2Body = (JSONObject) body.get(1);
            assertEquals("fromId2", fromId2Body.get("__id"));
            checkResponseBodyAsExpand("fromId2", target0101Entity, fromId2Body, 1);
            checkResponseBodyAsExpand("fromId2", target01NEntity, fromId2Body, top);

            // $expandを指定してデータを取得
            expands = String.format("_%s,_%s", targetN01Entity, targetNNEntity);
            response = listUserODataWithExpand("expandTestCell", "box", "odata", baseEntity,
                    expands, top, HttpStatus.SC_OK);
            body = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(2, body.size());

            // ベース側1つ目(fromId1)の取得結果をチェックする。
            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            fromId1Body = (JSONObject) body.get(0);
            assertEquals("fromId1", fromId1Body.get("__id"));
            checkResponseBodyAsExpand("fromId1", targetN01Entity, fromId1Body, 1);
            checkResponseBodyAsExpand("fromId1", targetNNEntity, fromId1Body, top);

            // ベース側2つ目(fromId2)の取得結果をチェックする。
            // 取得したデータの"__id" がユーザOData一覧で取得すべきデータであることを確認
            fromId2Body = (JSONObject) body.get(1);
            assertEquals("fromId2", fromId2Body.get("__id"));
            checkResponseBodyAsExpand("fromId2", targetN01Entity, fromId2Body, 1);
            checkResponseBodyAsExpand("fromId2", targetNNEntity, fromId2Body, top);

            // $expandクエリとその他のクエリを組み合わせても取得可能か
            String query = "?\\$orderby=__id&\\$top=10&\\$select=__id&"
                    + "\\$filter=startswith(__id,%27fromId1%27)&\\$inlinecount=allpages&\\$expand=" + expands;
            UserDataUtils
                    .list("expandTestCell", "box", "odata", baseEntity, query, MASTER_TOKEN_NAME, HttpStatus.SC_OK);

        } finally {
            Setup.cellBulkDeletion("expandTestCell");
        }
    }

    /**
     * 一覧取得でexpandに空文字を指定した場合200が返却されること.
     */
    @Test
    public final void 一覧取得でexpandに空文字を指定した場合200が返却されること() {
        final int top = 25;

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String fromEntity = "Sales";
        String userDataId = "id000000";
        try {
            // ユーザOData追加
            UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, "{\"__id\":\"" + userDataId + "\"}",
                    cellName,
                    boxName, colName, fromEntity);

            // $expandを指定してデータを取得
            String expands = "";
            listUserODataWithExpand(cellName, boxName, colName, fromEntity, expands, top, HttpStatus.SC_OK);
        } finally {
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, cellName, boxName, colName, fromEntity, userDataId);
        }
    }

    /**
     * 一覧取得でexpandに最大プロパティ数を指定した場合正常に取得できること.
     */
    @Test
    public final void 一覧取得でexpandに最大プロパティ数を指定した場合正常に取得できること() {
        final int top = 25;

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        // $expandを指定してデータを取得
        String expands = String.format("_%s,_%s",
                targetEntity1, targetEntity2);
        listUserODataWithExpand(cellName, boxName, colName, fromEntity, expands, top, HttpStatus.SC_OK);
    }

    /**
     * 一覧取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること.
     */
    @Test
    public final void 一覧取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること() {
        final int top = 25;

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";
        String targetEntity3 = "Supplier";

        // $expandを指定してデータを取得
        String expands = String.format("_%s,_%s,_%s",
                targetEntity1, targetEntity2, targetEntity3);
        TResponse response = listUserODataWithExpand(cellName, boxName, colName, fromEntity, expands, top,
                HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(response, DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getCode(),
                DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getMessage());

    }

    /**
     * 一覧取得でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること.
     */
    @Test
    public final void 一覧取得でexpand指定時にtopに取得件数最大数を指定した場合正常に取得できること() {
        final int top = DcCoreConfig.getMaxExpandSizeForList();

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        // $expandを指定してデータを取得
        String expands = String.format("_%s,_%s",
                targetEntity1, targetEntity2);
        listUserODataWithExpand(cellName, boxName, colName, fromEntity, expands, top, HttpStatus.SC_OK);
    }

    /**
     * 一覧取得でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること.
     */
    @Test
    public final void 一覧取得でexpand指定時にtopに取得件数最大数を超える値を指定した場合400エラーとなること() {
        final int top = DcCoreConfig.getMaxExpandSizeForList() + 1;

        String cellName = Setup.TEST_CELL1;
        String boxName = Setup.TEST_BOX1;
        String colName = Setup.TEST_ODATA;
        String fromEntity = "Sales";
        String targetEntity1 = "Price";
        String targetEntity2 = "Product";

        // $expandを指定してデータを取得
        String expands = String.format("_%s,_%s",
                targetEntity1, targetEntity2);
        TResponse response = listUserODataWithExpand(cellName, boxName, colName, fromEntity, expands, top,
                HttpStatus.SC_BAD_REQUEST);

        ODataCommon.checkErrorResponseBody(response,
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getCode(),
                DcCoreException.OData.QUERY_INVALID_ERROR.params("$top", String.valueOf(top)).getMessage());

    }

    /**
     * 一件取得でexpandに最大プロパティ数を指定した場合正常に取得できること.
     */
    @Test
    public final void 一件取得でexpandに最大プロパティ数を指定した場合正常に取得できること() {
        String token = MASTER_TOKEN_NAME;
        String cellName = "expand_property_max_num_test_cell";
        String boxName = "box";
        String colName = "col";
        String fromEntity = "fromEntity";
        String toEntityPrefix = "toEntity";

        String userDataId = "userdata000";

        int maxPropertyNum = DcCoreConfig.getExpandPropertyMaxSizeForRetrieve();

        try {
            // 事前準備
            CellUtils.create(cellName, token, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);

            // EntityType作成
            EntityTypeUtils.create(cellName, token, boxName, colName, fromEntity, HttpStatus.SC_CREATED);
            for (int i = 0; i < maxPropertyNum; i++) {
                String toEntityType = String.format("%s_%04d", toEntityPrefix, i);
                EntityTypeUtils.create(cellName, token, boxName, colName, toEntityType, HttpStatus.SC_CREATED);
            }

            // AssociationEnd作成（リンク含む）
            for (int i = 0; i < maxPropertyNum; i++) {
                String fromAssociationEnd = String.format("fromAssociationEnd_%04d", i);
                String toAssociationEnd = String.format("toAssociationEnd_%04d", i);
                String toEntityType = String.format("%s_%04d", toEntityPrefix, i);

                AssociationEndUtils.create(token, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                        fromAssociationEnd, fromEntity);
                AssociationEndUtils.createViaNP(token, cellName, boxName, colName,
                        fromAssociationEnd, fromEntity,
                        toAssociationEnd, "*", toEntityType, HttpStatus.SC_CREATED);
            }

            // ユーザOData登録
            UserDataUtils.create(token, HttpStatus.SC_CREATED, "{\"__id\":\"" + userDataId + "\"}",
                    cellName, boxName, colName, fromEntity);

            // $expandを指定してデータを取得
            StringBuilder expandBuilder = new StringBuilder("?\\$expand=");
            for (int i = 0; i < maxPropertyNum; i++) {
                String toEntityType = String.format("_%s_%04d", toEntityPrefix, i);
                expandBuilder.append(toEntityType);
                if (i != maxPropertyNum - 1) {
                    expandBuilder.append(",");
                }
            }
            String expands = expandBuilder.toString();
            UserDataUtils.getWithQuery(cellName, MASTER_TOKEN_NAME, boxName, colName,
                    fromEntity, expands, userDataId, HttpStatus.SC_OK);

        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * 一件取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること.
     */
    @Test
    public final void 一件取得でexpandに最大プロパティ数を超える値を指定した場合400エラーとなること() {
        String token = MASTER_TOKEN_NAME;
        String cellName = "expand_property_max_num_test_cell";
        String boxName = "box";
        String colName = "col";
        String fromEntity = "fromEntity";
        String toEntityPrefix = "toEntity";

        String userDataId = "userdata000";

        int maxPropertyNum = DcCoreConfig.getExpandPropertyMaxSizeForRetrieve() + 1;

        try {
            // 事前準備
            CellUtils.create(cellName, token, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, token, HttpStatus.SC_CREATED);
            DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cellName, boxName, colName);

            // EntityType作成
            EntityTypeUtils.create(cellName, token, boxName, colName, fromEntity, HttpStatus.SC_CREATED);
            for (int i = 0; i < maxPropertyNum; i++) {
                String toEntityType = String.format("%s_%04d", toEntityPrefix, i);
                EntityTypeUtils.create(cellName, token, boxName, colName, toEntityType, HttpStatus.SC_CREATED);
            }

            // AssociationEnd作成（リンク含む）
            for (int i = 0; i < maxPropertyNum; i++) {
                String fromAssociationEnd = String.format("fromAssociationEnd_%04d", i);
                String toAssociationEnd = String.format("toAssociationEnd_%04d", i);
                String toEntityType = String.format("%s_%04d", toEntityPrefix, i);

                AssociationEndUtils.create(token, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                        fromAssociationEnd, fromEntity);
                AssociationEndUtils.createViaNP(token, cellName, boxName, colName,
                        fromAssociationEnd, fromEntity,
                        toAssociationEnd, "*", toEntityType, HttpStatus.SC_CREATED);
            }

            // ユーザOData登録
            UserDataUtils.create(token, HttpStatus.SC_CREATED, "{\"__id\":\"" + userDataId + "\"}",
                    cellName, boxName, colName, fromEntity);

            // $expandを指定してデータを取得
            StringBuilder expandBuilder = new StringBuilder("?\\$expand=");
            for (int i = 0; i < maxPropertyNum; i++) {
                String toEntityType = String.format("_%s_%04d", toEntityPrefix, i);
                expandBuilder.append(toEntityType);
                if (i != maxPropertyNum - 1) {
                    expandBuilder.append(",");
                }
            }
            String expands = expandBuilder.toString();
            TResponse res = UserDataUtils.getWithQuery(cellName, MASTER_TOKEN_NAME, boxName, colName,
                    fromEntity, expands, userDataId, HttpStatus.SC_BAD_REQUEST);
            ODataCommon.checkErrorResponseBody(res, DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getCode(),
                    DcCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED.getMessage());

        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    private void checkResponseBodyAsExpand(String baseEntityId, final String targetEntity,
            JSONObject fromIdBody, int top) {

        // 0..1 : 0..1 取得したユーザODataのリンク先データとして、EntitySetの定義が存在し、1件のみ取得できることを確認
        JSONArray fromId1Expands = (JSONArray) fromIdBody.get("_" + targetEntity);
        assertNotNull(fromId1Expands);
        assertEquals(top, fromId1Expands.size());
        for (Object item : fromId1Expands) {
            assertNotNull(((JSONObject) item).get("__id"));
        }
        // expand内のデータが作成日の降順で作成されていることを確認する
        for (int i = top - 1, idx = 0; i >= 0; i--, idx++) {
            JSONObject item = (JSONObject) fromId1Expands.get(idx);
            String actualId = (String) item.get("__id");
            assertEquals(String.format("%s_%s_%04d", baseEntityId, targetEntity, i), actualId);
        }
    }

    /**
     * データ作成処理（$expandの0-1:0-1,0-1:N,N:0-1,N:N用データの作成）.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     */
    private void createExpandData(String cell, String box, String col) {
        final String token = DcCoreConfig.getMasterToken();
        // 1. 制御オブジェクトの作成
        CellUtils.create(cell, token, HttpStatus.SC_CREATED);
        BoxUtils.create(cell, box, token, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, cell, box, col);

        // 2. ユーザスキーマの作成
        String baseEntity = "baseEntityType";
        EntityTypeUtils.create(cell, token, box, col, baseEntity,
                HttpStatus.SC_CREATED);

        // 2.1 0..1 : 0..1 のEntityType作成
        String target0101Entity = "target01-01EntityType";
        EntityTypeUtils.create(cell, token, box, col, target0101Entity,
                HttpStatus.SC_CREATED);
        AssociationEndUtils.create(token, "0..1", cell, box, col,
                HttpStatus.SC_CREATED, "ae1-1", baseEntity);
        AssociationEndUtils.create(token, "0..1", cell, box, col,
                HttpStatus.SC_CREATED, "ae1-2", target0101Entity);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box, col,
                baseEntity, target0101Entity, "ae1-1", "ae1-2", HttpStatus.SC_NO_CONTENT);

        // 2.2 0..1 : N のEntityType作成
        String target01NEntity = "target01-NEntityType";
        EntityTypeUtils.create(cell, token, box, col, target01NEntity,
                HttpStatus.SC_CREATED);
        AssociationEndUtils.create(token, "0..1", cell, box, col, HttpStatus.SC_CREATED, "ae2-1", baseEntity);
        AssociationEndUtils.create(token, "*", cell, box, col, HttpStatus.SC_CREATED, "ae2-2", target01NEntity);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                col, baseEntity, target01NEntity, "ae2-1", "ae2-2", HttpStatus.SC_NO_CONTENT);

        // 2.3 N : 0..1 のEntityType作成
        String targetN01Entity = "targetN-01EntityType";
        EntityTypeUtils.create(cell, token, box, col, targetN01Entity,
                HttpStatus.SC_CREATED);
        AssociationEndUtils.create(token, "*", cell, box, col, HttpStatus.SC_CREATED, "ae3-1", baseEntity);
        AssociationEndUtils.create(token, "0..1", cell, box, col, HttpStatus.SC_CREATED, "ae3-2", targetN01Entity);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                col, baseEntity, targetN01Entity, "ae3-1", "ae3-2", HttpStatus.SC_NO_CONTENT);

        // 2.4 N : N のEntityType作成
        String targetNNEntity = "targetN-NEntityType";
        EntityTypeUtils.create(cell, token, box, col, targetNNEntity,
                HttpStatus.SC_CREATED);
        AssociationEndUtils.create(token, "*", cell, box, col, HttpStatus.SC_CREATED, "ae4-1", baseEntity);
        AssociationEndUtils.create(token, "*", cell, box, col, HttpStatus.SC_CREATED, "ae4-2", targetNNEntity);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cell, box,
                col, baseEntity, targetNNEntity, "ae4-1", "ae4-2", HttpStatus.SC_NO_CONTENT);

        // 3. ユーザODataの作成
        // 3.1 ベース側
        UserDataUtils.create(token, HttpStatus.SC_CREATED,
                "{\"__id\":\"" + "fromId1" + "\",\"name\":\"fromUserOData_1\"}", cell, box, col, baseEntity);
        UserDataUtils.create(token, HttpStatus.SC_CREATED,
                "{\"__id\":\"" + "fromId2" + "\",\"name\":\"fromUserOData_2\"}", cell, box, col, baseEntity);
        // 3.2 ターゲット側
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId1", target0101Entity, 1);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId1", target01NEntity, 10);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId1", targetN01Entity, 1);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId1", targetNNEntity, 10);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId2", target0101Entity, 1);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId2", target01NEntity, 10);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId2", targetN01Entity, 1);
        createExpandUserODataByNP(cell, box, col, baseEntity, "fromId2", targetNNEntity, 10);

    }

    /**
     * ユーザOData作成処理（$expandのベース側データの作成）.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param baseEntity ベース側EntitySet名
     * @param id ベース側ID
     * @param navPropName ターゲット側EntitySet名
     * @param records 作成レコード数
     */
    @SuppressWarnings("unchecked")
    private void createExpandUserODataByNP(String cell, String box, String col,
            String baseEntity, String id, String navPropName, int records) {
        final String token = DcCoreConfig.getMasterToken();

        // 引数で渡された件数分のユーザODataを作成する。
        for (int i = 0; i < records; i++) {
            JSONObject body = new JSONObject();
            body.put("__id", String.format("%s_%s_%04d", id, navPropName, i));
            body.put(String.format("property_%04d", i),
                    String.format("value_%04d", i));
            UserDataUtils.createViaNP(token, body, cell, box, col, baseEntity, id, navPropName, HttpStatus.SC_CREATED);
        }
    }

    /**
     * ユーザODataの一覧取得を$expandクエリを使用して呼び出す.
     * @param cell セル名
     * @param box ボックス名
     * @param col コレクション名
     * @param baseEntity ベース側EntitySet名
     * @param expands $expandクエリの値
     * @param top $topの値
     * @param status ステータスコード
     * @return レスポンス
     */
    private TResponse listUserODataWithExpand(String cell, String box,
            String col, String baseEntity, String expands, int top, int status) {
        return Http.request("box/odatacol/list.txt").with("cell", cell)
                .with("box", box).with("collection", col)
                .with("entityType", baseEntity)
                .with("query", "?\\$expand=" + expands + "&\\$top=" + top + "&\\$orderby=__id")
                .with("accept", MediaType.APPLICATION_JSON)
                .with("token", DcCoreConfig.getMasterToken())
                .returns()
                .statusCode(status)
                .debug();
    }
}
