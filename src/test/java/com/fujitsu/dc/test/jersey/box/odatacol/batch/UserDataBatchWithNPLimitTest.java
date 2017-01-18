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
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveLinksPostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrieveLinksPostResBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostBody;
import static com.fujitsu.dc.test.utils.BatchUtils.retrievePostResBody;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreConfig;
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
 * UserData $batchのNavigationProperty経由登録の上限値テスト. たくさんデータを登録するので、「Integration.class, Regression.class」では動作しない。
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class UserDataBatchWithNPLimitTest extends AbstractUserDataBatchTest {
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages", "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    String masterToken = Setup.MASTER_TOKEN_NAME;
    String cellName = "userDataBatchWithNPLimitTestCell";
    String boxName = "box";
    String colName = "col";

    String parentEntityTypeName = "parentEntity";
    String childEntityTypeName = "childEntity";
    String srcEntityTypeName = "srcEntity";
    String targetEntityTypeName = "targetEntity";

    String parentAssociationEndName = "parentAssociation";
    String childAssociationEndName = "childAssociation";
    String srcAssociationEndName = "srcAssociation";
    String targetAssociationEndName = "targetAssociation";

    String srcId = "id";

    // 登録するユーザODataの件数（N:Nの$linksで登録可能な上限値）
    int maxUserDataCount = DcCoreConfig.getLinksNtoNMaxSize();
    int registUserDataCount = maxUserDataCount - 10;

    /**
     * コンストラクタ.
     */
    public UserDataBatchWithNPLimitTest() {
        super(new WebAppDescriptor.Builder(UserDataBatchWithNPLimitTest.INIT_PARAMS).build());
    }

    /**
     * ソース側のリンク数が上限に満たない場合登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ソース側のリンク数が上限に満たない場合登録できること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;

            // $batch
            String path = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            index = registUserDataCount + 1;
            String registeredId1 = String.format("id%03d", index++);
            String registeredId2 = String.format("id%03d", index++);
            String registeredId3 = String.format("id%03d", index++);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, registeredId1)
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, registeredId2)
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, registeredId3)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が3つ増加していることのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            response = Http.request("box/odatacol/list.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('" + srcId + "')")
                    .with("entityType", "_" + targetEntityTypeName).with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON).with("token", masterToken).returns()
                    .statusCode(HttpStatus.SC_OK).debug();

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), registUserDataCount + 3);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }
        }
    }

    /**
     * ソース側のリンク数が上限値マイナス1の場合登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ソース側のリンク数が上限値マイナス1の場合登録できること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値マイナス1まで登録)
            for (; index <= maxUserDataCount - 1; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $batch
            String path = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String registeredId = String.format("id%03d", maxUserDataCount);
            String expectedBody = START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, registeredId)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が１つ増加していることのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            response = Http.request("box/odatacol/list.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('" + srcId + "')")
                    .with("entityType", "_" + targetEntityTypeName).with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON).with("token", masterToken).returns()
                    .statusCode(HttpStatus.SC_OK).debug();

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), maxUserDataCount);

            // 1つめのリクエストで登録したデータが登録されていること
            UserDataUtils.get(cellName, masterToken, boxName, colName, targetEntityTypeName, registeredId,
                    HttpStatus.SC_OK);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }
        }
    }

    /**
     * ソース側のリンク数が上限値以上の場合400エラーとなること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ソース側のリンク数が上限値以上の場合400エラーとなること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $batch
            String path = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + START_BOUNDARY
                    + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していないことのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            response = Http.request("box/odatacol/list.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('" + srcId + "')")
                    .with("entityType", "_" + targetEntityTypeName).with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON).with("token", masterToken).returns()
                    .statusCode(HttpStatus.SC_OK).debug();

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), maxUserDataCount);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }
        }
    }

    /**
     * ターゲット側のひとつのEntityTypeが上限値に達していても他のEntityTypeは登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ターゲット側のひとつのEntityTypeが上限値に達していても他のEntityTypeは登録できること()
            throws ParseException {
        String additionalEntityType = "additionalEntityType";
        String additionalAssociationEnd1 = "additionalAssociation1";
        String additionalAssociationEnd2 = "additionalAssociation2";
        String additionalUserDataId = String.format("id%03d", 1);
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // スキーマlink情報追加
            // EntityType
            EntityTypeUtils.create(cellName, masterToken, boxName, colName,
                    additionalEntityType, HttpStatus.SC_CREATED);
            // AssociationEnd
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    additionalAssociationEnd1, additionalEntityType);
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    additionalAssociationEnd2, srcEntityTypeName);

            // AssociationEnd - AssociationEnd $links
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    additionalEntityType, srcEntityTypeName, additionalAssociationEnd1, additionalAssociationEnd2,
                    HttpStatus.SC_NO_CONTENT);

            // $batch
            String path1 = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String path2 = srcEntityTypeName + "('" + srcId + "')/_" + additionalEntityType;
            String body = START_BOUNDARY + retrievePostBody(path1, String.format("id%03d", index++))
                    + START_BOUNDARY + retrievePostBody(path2, additionalUserDataId)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, additionalEntityType, additionalUserDataId)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していないことのチェック
            checkLinkCount(srcEntityTypeName, srcId, targetEntityTypeName, maxUserDataCount);

            // 他のEntityTypeが登録できていることのチェック
            checkLinkCount(srcEntityTypeName, srcId, additionalEntityType, 1);

        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }

            deleteUserDataWithLink(srcEntityTypeName, srcId, additionalEntityType, 1);

            String key = "Name='" + additionalAssociationEnd1 + "',_EntityType.Name='" + additionalEntityType + "'";
            String navKey = "Name='" + additionalAssociationEnd2 + "',_EntityType.Name='" + srcEntityTypeName + "'";
            AssociationEndUtils.deleteLink(cellName, colName, boxName, key, navKey, -1);

            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, additionalEntityType,
                    boxName, additionalAssociationEnd1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, srcEntityTypeName, boxName,
                    additionalAssociationEnd2, -1);

            EntityTypeUtils.delete(colName, masterToken, MediaType.APPLICATION_JSON, additionalEntityType, boxName,
                    cellName, -1);
        }
    }

    /**
     * ひとつのUserDataに対するリンクが上限値に達していても他のUserDataに対するリンクは登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ひとつのUserDataに対するリンクが上限値に達していても他のUserDataに対するリンクは登録できること()
            throws ParseException {
        String additionalUserDataId = String.format("id%03d", 999);
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // ソース側ユーザOData追加
            JSONObject additionalBody = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + additionalUserDataId
                    + "\",\"name\":\"pochi\"}");
            UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, additionalBody, cellName, boxName, colName,
                    srcEntityTypeName);

            // $batch
            String path1 = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String path2 = srcEntityTypeName + "('" + additionalUserDataId + "')/_" + targetEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path1, String.format("id%03d", index++))
                    + START_BOUNDARY + retrievePostBody(path2, additionalUserDataId)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, additionalUserDataId)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していないことのチェック
            checkLinkCount(srcEntityTypeName, srcId, targetEntityTypeName, maxUserDataCount);

            // 他のEntityTypeが登録できていることのチェック
            checkLinkCount(srcEntityTypeName, additionalUserDataId, targetEntityTypeName, 1);

        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }

            deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, 999);
        }
    }

    /**
     * ソース側のひとつのEntityTypeが上限値に達していても他のEntityTypeは登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ソース側のひとつのEntityTypeが上限値に達していても他のEntityTypeは登録できること()
            throws ParseException {
        String additionalEntityType = "additionalEntityType";
        String additionalAssociationEnd1 = "additionalAssociation1";
        String additionalAssociationEnd2 = "additionalAssociation2";
        String additionalUserDataId = String.format("id%03d", 999);
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // スキーマlink情報追加
            // EntityType
            EntityTypeUtils.create(cellName, masterToken, boxName, colName,
                    additionalEntityType, HttpStatus.SC_CREATED);
            // AssociationEnd
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    additionalAssociationEnd1, additionalEntityType);
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    additionalAssociationEnd2, targetEntityTypeName);

            // AssociationEnd - AssociationEnd $links
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    additionalEntityType,
                    targetEntityTypeName, additionalAssociationEnd1, additionalAssociationEnd2,
                    HttpStatus.SC_NO_CONTENT);

            // ソース側の追加のEntityTypeにユーザOData登録
            JSONObject additionalBody = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + additionalUserDataId
                    + "\",\"name\":\"pochi\"}");
            UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, additionalBody, cellName, boxName, colName,
                    additionalEntityType);

            // $batch
            String path1 = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String path2 = additionalEntityType + "('" + additionalUserDataId + "')/_" + targetEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path1, String.format("id%03d", index++))
                    + START_BOUNDARY + retrievePostBody(path2, additionalUserDataId)
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, targetEntityTypeName, additionalUserDataId)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していないことのチェック
            checkLinkCount(srcEntityTypeName, srcId, targetEntityTypeName, maxUserDataCount);

            // 他のEntityTypeが登録できていることのチェック
            checkLinkCount(additionalEntityType, additionalUserDataId, targetEntityTypeName, 1);

        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }

            deleteUserDataWithLink(additionalEntityType, additionalUserDataId, targetEntityTypeName, 999);
            UserDataUtils.delete(masterToken, -1, cellName, boxName, colName, additionalEntityType,
                    additionalUserDataId);

            String key = "Name='" + additionalAssociationEnd1 + "',_EntityType.Name='" + additionalEntityType + "'";
            String navKey = "Name='" + additionalAssociationEnd2 + "',_EntityType.Name='" + targetEntityTypeName + "'";
            AssociationEndUtils.deleteLink(cellName, colName, boxName, key, navKey, -1);

            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, additionalEntityType,
                    boxName, additionalAssociationEnd1, -1);
            AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, colName, targetEntityTypeName,
                    boxName, additionalAssociationEnd2, -1);

            EntityTypeUtils.delete(colName, masterToken, MediaType.APPLICATION_JSON, additionalEntityType, boxName,
                    cellName, -1);
        }
    }

    /**
     * ONE対AST_ソース側のリンク数が上限値以上の場合でも登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ONE対AST_ソース側のリンク数が上限値以上の場合でも登録できること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, parentEntityTypeName, srcId,
                        childEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $batch
            String path = parentEntityTypeName + "('" + srcId + "')/_" + childEntityTypeName;
            String body = START_BOUNDARY + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + START_BOUNDARY
                    + retrievePostBody(path, String.format("id%03d", index++)) + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            index = maxUserDataCount + 1;
            String expectedBody = START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, childEntityTypeName,
                            String.format("id%03d", index++))
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, childEntityTypeName,
                            String.format("id%03d", index++))
                    + START_BOUNDARY
                    + retrievePostResBody(cellName, boxName, colName, childEntityTypeName,
                            String.format("id%03d", index++))
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していることのチェック
            checkLinkCount(parentEntityTypeName, srcId, childEntityTypeName, maxUserDataCount + 3);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(parentEntityTypeName, srcId, childEntityTypeName, index);
            }
        }
    }

    /**
     * ターゲット側に存在しないEntityType名を指定した場合404エラーとなること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ターゲット側に存在しないEntityType名を指定した場合404エラーとなること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName, srcEntityTypeName, srcId,
                        targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $batch
            String path1 = srcEntityTypeName + "('" + srcId + "')/_" + targetEntityTypeName;
            String path2 = srcEntityTypeName + "('" + srcId + "')/_" + "dummyEntityType";
            String body = START_BOUNDARY + retrievePostBody(path1, String.format("id%03d", index))
                    + START_BOUNDARY + retrievePostBody(path2, String.format("id%03d", 1))
                    + END_BOUNDARY;
            TResponse response = Http.request("box/odatacol/batch.txt").with("cell", cellName).with("box", boxName)
                    .with("collection", colName).with("boundary", BOUNDARY)
                    .with("token", DcCoreConfig.getMasterToken()).with("body", body).returns().debug()
                    .statusCode(HttpStatus.SC_ACCEPTED);

            // レスポンスボディのチェック
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_NOT_FOUND)
                    + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 1; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }
        }
    }

    // -------------------------------

    /**
     * $batchでAST対ASTのUserDataの$links登録_上限値以上の$linksが存在する場合400エラーとなること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void $batchでAST対ASTのUserDataの$links登録_上限値以上の$linksが存在する場合400エラーとなること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                        srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);
            }
            // ベースデータ追加($linksの上限値を超える分)
            // target側
            int linkedIndex = index;
            for (; index <= maxUserDataCount + 3; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                        targetEntityTypeName);
            }

            // $batch
            String path = srcEntityTypeName + "('" + srcId + "')/\\$links/_" + targetEntityTypeName;
            String body = "";
            StringBuilder bodyBuilder = new StringBuilder();
            for (; linkedIndex <= maxUserDataCount + 3; linkedIndex++) {
                String linksBody = "{\"uri\":\""
                        + UrlUtils.userdata(cellName, boxName, colName,
                                targetEntityTypeName, String.format("id%03d", linkedIndex)) + "\"}";
                bodyBuilder.append(START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, linksBody));
            }
            bodyBuilder.append(END_BOUNDARY);
            body = bodyBuilder.toString();
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
            String expectedBody = START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST)
                    + START_BOUNDARY + retrieveChangeSetResErrorBody(HttpStatus.SC_BAD_REQUEST) + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していないことのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('" + srcId + "')")
                    .with("entityType", "_" + targetEntityTypeName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), maxUserDataCount);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(srcEntityTypeName, srcId, targetEntityTypeName, index);
            }
        }
    }

    /**
     * $batchでONE対ASTのUserDataの$links登録_上限値以上の$linksが存在する場合登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void $batchでONE対ASTのUserDataの$links登録_上限値以上の$linksが存在する場合登録できること()
            throws ParseException {
        try {
            int index = registUserDataCount + 1;
            // ベースデータ追加($linksの上限値まで登録)
            for (; index <= maxUserDataCount; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                        parentEntityTypeName, srcId, childEntityTypeName, HttpStatus.SC_CREATED);
            }
            // ベースデータ追加($linksの上限値を超える分)
            // target側
            int linkedIndex = index;
            for (; index <= maxUserDataCount + 3; index++) {
                String id = String.format("id%03d", index);
                JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body, cellName, boxName, colName,
                        childEntityTypeName);
            }

            // $batch
            String path = parentEntityTypeName + "('" + srcId + "')/\\$links/_" + childEntityTypeName;
            String body = "";
            StringBuilder bodyBuilder = new StringBuilder();
            for (; linkedIndex <= maxUserDataCount + 3; linkedIndex++) {
                String linksBody = "{\"uri\":\""
                        + UrlUtils.userdata(cellName, boxName, colName,
                                childEntityTypeName, String.format("id%03d", linkedIndex)) + "\"}";
                bodyBuilder.append(START_BOUNDARY + retrieveLinksPostBody(HttpMethod.POST, path, linksBody));
            }
            bodyBuilder.append(END_BOUNDARY);
            body = bodyBuilder.toString();
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
            String expectedBody = START_BOUNDARY + retrieveLinksPostResBody()
                    + START_BOUNDARY + retrieveLinksPostResBody()
                    + START_BOUNDARY + retrieveLinksPostResBody() + END_BOUNDARY;
            checkBatchResponseBody(response, expectedBody);

            // 登録済み件数が増加していることのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + parentEntityTypeName + "('" + srcId + "')")
                    .with("entityType", "_" + childEntityTypeName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(0, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), maxUserDataCount + 3);
        } finally {
            // 追加したデータの削除
            for (int index = registUserDataCount + 1; index <= maxUserDataCount + 3; index++) {
                deleteUserDataWithLink(parentEntityTypeName, srcId, childEntityTypeName, index);
            }
        }
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
        EntityTypeUtils.create(cellName, masterToken, boxName, colName, parentEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(cellName, masterToken, boxName, colName, childEntityTypeName, HttpStatus.SC_CREATED);

        // AssociationEnd
        AssociationEndUtils.create(masterToken, "0..1", cellName, boxName, colName, HttpStatus.SC_CREATED,
                parentAssociationEndName, parentEntityTypeName);
        AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                childAssociationEndName, childEntityTypeName);
        AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                srcAssociationEndName, srcEntityTypeName);
        AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                targetAssociationEndName, targetEntityTypeName);

        // AssociationEnd - AssociationEnd $links
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                parentEntityTypeName,
                childEntityTypeName, parentAssociationEndName, childAssociationEndName, HttpStatus.SC_NO_CONTENT);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName, srcEntityTypeName,
                targetEntityTypeName, srcAssociationEndName, targetAssociationEndName, HttpStatus.SC_NO_CONTENT);

        // 0..1:N関連
        // src側(1)
        JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + srcId + "\",\"name\":\"pochi\"}");
        UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, parentEntityTypeName);

        // target側(*)
        for (int i = 1; i <= registUserDataCount; i++) {
            String id = String.format("id%03d", i);
            body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    parentEntityTypeName, srcId, childEntityTypeName, HttpStatus.SC_CREATED);
        }

        // N:N関連
        // src側(*)
        body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + srcId + "\",\"name\":\"pochi\"}");
        UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body,
                cellName, boxName, colName, srcEntityTypeName);

        // target側(*)
        for (int i = 1; i <= registUserDataCount; i++) {
            String id = String.format("id%03d", i);
            body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);
        }

    }

    /**
     * 全テストの後に１度だけ実行する処理.
     */
    @After
    public final void after() {
        // Cellの再帰的削除
        Setup.cellBulkDeletion(cellName);
    }

    private void deleteUserDataWithLink(
            String srcEntityType,
            String srcUserDataId,
            String targetEntityType,
            int index) {
        String id = String.format("id%03d", index);
        UserDataUtils.deleteLinks(cellName, boxName, colName,
                srcEntityType, srcUserDataId, targetEntityType, id, -1);
        UserDataUtils.delete(masterToken, -1, cellName, boxName, colName, targetEntityType, id);
    }

    private void checkLinkCount(String srcEntityType, String srcUserDataId, String targetEntityType, int expectCount) {
        TResponse response;
        String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
        response = Http.request("box/odatacol/list.txt").with("cell", cellName).with("box", boxName)
                .with("collection", colName + "/" + srcEntityType + "('" + srcUserDataId + "')")
                .with("entityType", "_" + targetEntityType).with("query", query)
                .with("accept", MediaType.APPLICATION_JSON).with("token", masterToken).returns()
                .statusCode(HttpStatus.SC_OK).debug();

        // __countのチェック
        ODataCommon.checkResponseBodyCount(response.bodyAsJson(), expectCount);
    }

}
