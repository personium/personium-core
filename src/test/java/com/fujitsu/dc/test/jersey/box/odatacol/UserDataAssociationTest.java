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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.odata.UserDataODataProducer;
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
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataの同一名AssociationEndが定義されている場合の動作テスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class UserDataAssociationTest extends JerseyTest {
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages", "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    String masterToken = Setup.MASTER_TOKEN_NAME;
    String cellName = "userDataAssociationTestCell";
    String boxName = "box";
    String colName = "col";

    String srcEntityTypeName = "entity";
    String targetEntityTypeName = "entity2";

    String associationEndName = "Association";

    String sourceNameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + srcEntityTypeName;
    String targetNameSpace = UserDataODataProducer.USER_ODATA_NAMESPACE + "." + targetEntityTypeName;

    // 登録するユーザODataの件数（N:Nの$linksで登録可能な上限値）
    int registUserDataCount = DcCoreConfig.getLinksNtoNMaxSize();

    /**
     * コンストラクタ.
     */
    public UserDataAssociationTest() {
        super(new WebAppDescriptor.Builder(UserDataAssociationTest.INIT_PARAMS).build());
    }

    /**
     * 全テストの前に１度だけ実行する処理.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Before
    public final void before() throws ParseException {
        // 事前にデータを登録する
        CellUtils.create(cellName, masterToken, HttpStatus.SC_CREATED);
        BoxUtils.create(cellName, boxName, masterToken, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(masterToken, HttpStatus.SC_CREATED, cellName, boxName, colName);

        // EntityType
        EntityTypeUtils.create(cellName, masterToken, boxName, colName, srcEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils
                .create(cellName, masterToken, boxName, colName, targetEntityTypeName, HttpStatus.SC_CREATED);

        // AssociationEnd
        AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                associationEndName, srcEntityTypeName);
        AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                associationEndName, targetEntityTypeName);

        // AssociationEnd - AssociationEnd $links
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, srcEntityTypeName,
                targetEntityTypeName, associationEndName, associationEndName, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 全テストの後に１度だけ実行する処理.
     */
    @After
    public final void after() {
        // Cellの再帰的削除
        Setup.cellBulkDeletion(cellName);
    }

    /**
     * 同名のAssociationEndで関連を結んだEntityType間でユーザODataNP経由登録を実施して正常に登録できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 同名のAssociationEndで関連を結んだEntityType間でユーザODataNP経由登録を実施して正常に登録できること() {
        String userDataId = "id001";
        String userDataNpId = "targetid001";

        // リクエスト実行
        try {
            // sourceへユーザOData登録
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, srcEntityTypeName);

            // targetへユーザODataNP経由登録
            JSONObject npBody = new JSONObject();
            npBody.put("__id", userDataNpId);
            npBody.put("target", "target");
            TResponse response = UserDataUtils.createViaNP(AbstractCase.MASTER_TOKEN_NAME, npBody,
                    cellName, boxName, colName,
                    srcEntityTypeName, userDataId, targetEntityTypeName, HttpStatus.SC_CREATED);
            // レスポンスヘッダーのチェック
            String location = UrlUtils.userData(cellName, boxName, colName, targetEntityTypeName
                    + "('" + userDataNpId + "')");
            ODataCommon.checkCommonResponseHeader(response, location);

            // レスポンスボディーのチェック
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, targetNameSpace, npBody, null, null);

            // sourceへユーザODataNP経由登録
            npBody = new JSONObject();
            npBody.put("__id", userDataNpId);
            npBody.put("source", "source");

            response = UserDataUtils.createViaNP(AbstractCase.MASTER_TOKEN_NAME, npBody,
                    cellName, boxName, colName,
                    targetEntityTypeName, userDataNpId, srcEntityTypeName, HttpStatus.SC_CREATED);
            // レスポンスヘッダーのチェック
            location = UrlUtils.userData(cellName, boxName, colName, srcEntityTypeName
                    + "('" + userDataNpId + "')");
            ODataCommon.checkCommonResponseHeader(response, location);

            // レスポンスボディーのチェック
            ODataCommon.checkResponseBody(response.bodyAsJson(), location, sourceNameSpace, npBody, null, null);

            // targetをNP経由で一覧取得する
            response = UserDataUtils.listViaNP(cellName, boxName, colName, srcEntityTypeName, userDataId,
                    targetEntityTypeName, "", HttpStatus.SC_OK);

            // レスポンスボディのチェック
            Map<String, String> uri = new HashMap<String, String>();
            uri.put(userDataNpId, UrlUtils.userData(cellName, boxName,
                    colName, targetEntityTypeName + "('" + userDataNpId + "')"));

            Map<String, Map<String, Object>> additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop = new HashMap<String, Object>();
            additional.put(userDataNpId, additionalprop);
            additionalprop.put("target", "target");

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, targetNameSpace, additional, "__id", null,
                    null);

            // sourceをNP経由で一覧取得する
            response = UserDataUtils.listViaNP(cellName, boxName, colName, targetEntityTypeName, userDataNpId,
                    srcEntityTypeName, "", HttpStatus.SC_OK);

            // レスポンスボディのチェック
            uri = new HashMap<String, String>();
            uri.put(userDataId, UrlUtils.userData(cellName, boxName,
                    colName, srcEntityTypeName + "('" + userDataId + "')"));
            uri.put(userDataNpId, UrlUtils.userData(cellName, boxName,
                    colName, srcEntityTypeName + "('" + userDataNpId + "')"));

            additional = new HashMap<String, Map<String, Object>>();
            Map<String, Object> additionalprop1 = new HashMap<String, Object>();
            additional.put(userDataId, additionalprop1);
            Map<String, Object> additionalprop2 = new HashMap<String, Object>();
            additional.put(userDataNpId, additionalprop2);
            additionalprop.put("source", "source");

            ODataCommon.checkResponseBodyList(response.bodyAsJson(), uri, sourceNameSpace, additional, "__id", null,
                    null);

        } finally {
            UserDataUtils.deleteLinks(cellName, boxName, colName, srcEntityTypeName, userDataId, targetEntityTypeName,
                    userDataNpId, -1);
            UserDataUtils.deleteLinks(cellName, boxName, colName, srcEntityTypeName, userDataNpId,
                    targetEntityTypeName,
                    userDataNpId, -1);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, srcEntityTypeName,
                    userDataId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, srcEntityTypeName,
                    userDataNpId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, targetEntityTypeName,
                    userDataNpId);
        }
    }

    /**
     * 同名のAssociationEndで関連を結んだEntityType間でユーザODataの$links登録を実施して$links一覧取得できること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 同名のAssociationEndで関連を結んだEntityType間でユーザODataの$links登録を実施して$links一覧取得できること() {
        String userDataId = "id001";
        String userDataNpId = "targetid001";

        // リクエスト実行
        try {
            // sourceへユーザOData登録
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("source", "source");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, srcEntityTypeName);

            // targetへユーザOData登録
            body = new JSONObject();
            body.put("__id", userDataNpId);
            body.put("target", "target");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, targetEntityTypeName);

            // ユーザOData$links登録
            UserDataUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, srcEntityTypeName,
                    userDataId, targetEntityTypeName, userDataNpId, HttpStatus.SC_NO_CONTENT);

            // $links一覧取得(tartget)
            TResponse res = UserDataUtils.listLink(cellName, boxName, colName, srcEntityTypeName, userDataId,
                    targetEntityTypeName);

            // レスポンスボディのチェック
            ArrayList<String> uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName,
                    colName, targetEntityTypeName + "('" + userDataNpId + "')"));
            ODataCommon.checkLinResponseBody(res.bodyAsJson(), uri);

            // $links一覧取得(source)
            res = UserDataUtils.listLink(cellName, boxName, colName, targetEntityTypeName, userDataNpId,
                    srcEntityTypeName);

            // レスポンスボディのチェック
            uri = new ArrayList<String>();
            uri.add(UrlUtils.userData(cellName, boxName,
                    colName, srcEntityTypeName + "('" + userDataId + "')"));
            ODataCommon.checkLinResponseBody(res.bodyAsJson(), uri);
        } finally {
            UserDataUtils.deleteLinks(cellName, boxName, colName, srcEntityTypeName, userDataId, targetEntityTypeName,
                    userDataNpId, -1);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, srcEntityTypeName,
                    userDataId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, targetEntityTypeName,
                    userDataNpId);
        }
    }

    /**
     * 同名のAssociationEndで関連を結んだEntityType間でクエリ$expandを指定してユーザOData一覧取得ができること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void 同名のAssociationEndで関連を結んだEntityType間でクエリ$expandを指定してユーザOData一覧取得ができること() {
        String userDataId = "id001";
        String userDataNpId = "targetid001";

        // リクエスト実行
        try {
            // sourceへユーザOData登録
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);
            body.put("source", "source");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, srcEntityTypeName);

            // targetへユーザOData登録
            body = new JSONObject();
            body.put("__id", userDataNpId);
            body.put("target", "target");
            UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, targetEntityTypeName);

            // ユーザOData$links登録
            UserDataUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, srcEntityTypeName,
                    userDataId, targetEntityTypeName, userDataNpId, HttpStatus.SC_NO_CONTENT);

            // $expandを指定してデータを取得
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", srcEntityTypeName + "('" + userDataId + "')")
                    .with("query", "?\\$expand=" + "_" + targetEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", userDataId);

            // fromEntityのデータチェック
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, sourceNameSpace, additional);

            // toEntity($expandで指定)のデータチェック
            JSONObject dResults = (JSONObject) ((JSONObject) response.bodyAsJson().get("d"));
            JSONObject resultsResults = (JSONObject) ((JSONObject) dResults.get("results"));
            JSONArray expandResults = (JSONArray) resultsResults.get("_" + targetEntityTypeName);
            assertEquals(1, expandResults.size());
            Map<String, Object> expandAdditional = new HashMap<String, Object>();
            expandAdditional.put("__id", userDataNpId);
            ODataCommon.checkResults((JSONObject) expandResults.get(0), null, targetNameSpace, expandAdditional);

            // $expandを指定してデータを取得
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName)
                    .with("entityType", targetEntityTypeName + "('" + userDataNpId + "')")
                    .with("query", "?\\$expand=" + "_" + srcEntityTypeName)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", DcCoreConfig.getMasterToken())
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディーのチェック
            additional = new HashMap<String, Object>();
            additional.put("__id", userDataNpId);

            // fromEntityのデータチェック
            ODataCommon.checkResponseBody(response.bodyAsJson(), null, targetNameSpace, additional);

            // toEntity($expandで指定)のデータチェック
            dResults = (JSONObject) ((JSONObject) response.bodyAsJson().get("d"));
            resultsResults = (JSONObject) ((JSONObject) dResults.get("results"));
            expandResults = (JSONArray) resultsResults.get("_" + srcEntityTypeName);
            assertEquals(1, expandResults.size());
            expandAdditional = new HashMap<String, Object>();
            expandAdditional.put("__id", userDataId);
            ODataCommon.checkResults((JSONObject) expandResults.get(0), null, sourceNameSpace, expandAdditional);
        } finally {
            UserDataUtils.deleteLinks(cellName, boxName, colName, srcEntityTypeName, userDataId, targetEntityTypeName,
                    userDataNpId, -1);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, srcEntityTypeName,
                    userDataId);
            UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, -1, cellName, boxName, colName, targetEntityTypeName,
                    userDataNpId);
        }
    }
}
