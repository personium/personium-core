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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.AssociationEndUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.EntityTypeUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataのNavigationProperty経由一覧のテスト.
 * たくさんデータを登録するので、「Integration.class, Regression.class」では動作しない。
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class UserDataListWithNPLimitTest extends JerseyTest {
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages", "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    String masterToken = Setup.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ.
     */
    public UserDataListWithNPLimitTest() {
        super(new WebAppDescriptor.Builder(UserDataListWithNPLimitTest.INIT_PARAMS).build());
    }

    /**
     * ONE対ASTのUserDataをNavigationProperty経由でtopにデフォルト取得件数プラス1を指定した場合指定した件数分データが取得できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ONE対ASTのUserDataをNavigationProperty経由でtopにデフォルト取得件数プラス1を指定した場合指定した件数分データが取得できること()
            throws ParseException {
        String cellName = "userDataListWithNpLimitTestCell";
        String boxName = "box";
        String colName = "col";
        String srcEntityTypeName = "srcEntity";
        String targetEntityTypeName = "targetEntity";
        String srcAssociationEndName = "srcAssociation";
        String targetAssociationEndName = "targetAssociation";

        int registUserDataCount = 26;

        try {
            // 事前にデータを登録する
            CellUtils.create(cellName, masterToken, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, masterToken, HttpStatus.SC_CREATED);
            Http.request("box/mkcol-odata.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("path", colName)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // EntityType
            EntityTypeUtils.create(cellName, masterToken, boxName, colName, srcEntityTypeName, HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, masterToken, boxName, colName, targetEntityTypeName, HttpStatus.SC_CREATED);

            // AssociationEnd
            AssociationEndUtils.create(masterToken, "0..1", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    srcAssociationEndName, srcEntityTypeName);
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    targetAssociationEndName, targetEntityTypeName);

            // AssociationEnd - AssociationEnd $links
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    srcEntityTypeName, targetEntityTypeName, srcAssociationEndName, targetAssociationEndName,
                    HttpStatus.SC_NO_CONTENT);

            // src側(1)
            JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"id\",\"name\":\"pochi\"}");
            UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, srcEntityTypeName);

            // target側(*) 26件
            for (int i = 0; i < registUserDataCount; i++) {
                String id = String.format("id%03d", i);
                body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                        srcEntityTypeName, "id", targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // ユーザデータの一覧取得
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", registUserDataCount);
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('id')")
                    .with("entityType", "_" + targetEntityTypeName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(registUserDataCount, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), registUserDataCount);

        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * AST対ASTのUserDataをNavigationProperty経由でtopにデフォルト取得件数プラス1を指定した場合指定した件数分データが取得できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void AST対ASTのUserDataをNavigationProperty経由でtopにデフォルト取得件数プラス1を指定した場合指定した件数分データが取得できること()
            throws ParseException {
        String cellName = "userDataListWithNpLimitTestCell";
        String boxName = "box";
        String colName = "col";
        String srcEntityTypeName = "srcEntity";
        String targetEntityTypeName = "targetEntity";
        String srcAssociationEndName = "srcAssociation";
        String targetAssociationEndName = "targetAssociation";

        int registUserDataCount = 26;

        try {
            // 事前にデータを登録する
            CellUtils.create(cellName, masterToken, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, masterToken, HttpStatus.SC_CREATED);
            Http.request("box/mkcol-odata.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("path", colName)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_CREATED);

            // EntityType
            EntityTypeUtils.create(cellName, masterToken, boxName, colName, srcEntityTypeName, HttpStatus.SC_CREATED);
            EntityTypeUtils
                    .create(cellName, masterToken, boxName, colName, targetEntityTypeName, HttpStatus.SC_CREATED);

            // AssociationEnd
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    srcAssociationEndName, srcEntityTypeName);
            AssociationEndUtils.create(masterToken, "*", cellName, boxName, colName, HttpStatus.SC_CREATED,
                    targetAssociationEndName, targetEntityTypeName);

            // AssociationEnd - AssociationEnd $links
            AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName, colName,
                    srcEntityTypeName,
                    targetEntityTypeName, srcAssociationEndName, targetAssociationEndName, HttpStatus.SC_NO_CONTENT);

            // src側(1)
            JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"id\",\"name\":\"pochi\"}");
            UserDataUtils.create(masterToken, HttpStatus.SC_CREATED, body,
                    cellName, boxName, colName, srcEntityTypeName);

            // target側(*) 26件
            for (int i = 0; i < registUserDataCount; i++) {
                String id = String.format("id%03d", i);
                body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
                UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                        srcEntityTypeName, "id", targetEntityTypeName, HttpStatus.SC_CREATED);
            }

            // ユーザデータの一覧取得
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", registUserDataCount);
            TResponse response = Http.request("box/odatacol/list.txt")
                    .with("cell", cellName)
                    .with("box", boxName)
                    .with("collection", colName + "/" + srcEntityTypeName + "('id')")
                    .with("entityType", "_" + targetEntityTypeName)
                    .with("query", query)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .with("token", masterToken)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();

            // レスポンスヘッダーのチェック
            ODataCommon.checkCommonResponseHeader(response);

            // レスポンスボディの件数のチェック
            JSONArray results = (JSONArray) ((JSONObject) response.bodyAsJson().get("d")).get("results");
            assertEquals(registUserDataCount, results.size());

            // __countのチェック
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), registUserDataCount);

        } finally {
            // Cellの再帰的削除
            Setup.cellBulkDeletion(cellName);
        }
    }
}
