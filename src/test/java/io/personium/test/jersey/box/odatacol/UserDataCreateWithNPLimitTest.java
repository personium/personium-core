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
package io.personium.test.jersey.box.odatacol;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

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

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DcRunner;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * UserDataのNavigationProperty経由登録の上限値テスト.
 * たくさんデータを登録するので、「Integration.class, Regression.class」では動作しない。
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class UserDataCreateWithNPLimitTest extends JerseyTest {
    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages", "io.personium.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "io.personium.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "io.personium.core.jersey.filter.DcCoreContainerFilter");
    }

    String masterToken = Setup.MASTER_TOKEN_NAME;
    String cellName = "userDataListWithNpLimitTestCell";
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
    int registUserDataCount = PersoniumUnitConfig.getLinksNtoNMaxSize();

    /**
     * コンストラクタ.
     */
    public UserDataCreateWithNPLimitTest() {
        super(new WebAppDescriptor.Builder(UserDataCreateWithNPLimitTest.INIT_PARAMS).build());
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
        for (int i = 1; i < registUserDataCount; i++) {
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
        for (int i = 1; i < registUserDataCount; i++) {
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

    /**
     * ONE対ASTのUserDataのNavigationProperty経由登録_上限値以上のデータを登録した場合登録できること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void ONE対ASTのUserDataのNavigationProperty経由登録_上限値以上のデータを登録した場合登録できること()
            throws ParseException {

        try {
            // NP経由登録(上限値)
            String id = String.format("id%03d", registUserDataCount);
            JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    parentEntityTypeName, srcId, childEntityTypeName, HttpStatus.SC_CREATED);

            // NP経由登録(上限値 + 1)
            id = String.format("id%03d", registUserDataCount + 1);
            body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    parentEntityTypeName, srcId, childEntityTypeName, HttpStatus.SC_CREATED);

            // 登録済み件数が増加していることのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            TResponse response = Http.request("box/odatacol/list.txt")
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
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), registUserDataCount + 1);
        } finally {
            // 追加したデータの削除
            deleteUserDataWithLink(parentEntityTypeName, childEntityTypeName, registUserDataCount);
            deleteUserDataWithLink(parentEntityTypeName, childEntityTypeName, registUserDataCount + 1);
        }
    }

    /**
     * AST対ASTのUserDataのNavigationProperty経由登録_上限値以上のデータを登録した場合400エラーとなること.
     * @throws ParseException リクエストボディのパースに失敗
     */
    @Test
    public final void AST対ASTのUserDataのNavigationProperty経由登録_上限値以上のデータを登録した場合400エラーとなること()
            throws ParseException {

        try {
            // NP経由登録(上限値)
            String id = String.format("id%03d", registUserDataCount);
            JSONObject body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_CREATED);

            // NP経由登録(上限値 + 1)
            id = String.format("id%03d", registUserDataCount + 1);
            body = (JSONObject) (new JSONParser()).parse("{\"__id\":\"" + id + "\",\"name\":\"pochi\"}");
            TResponse res = UserDataUtils.createViaNP(masterToken, body, cellName, boxName, colName,
                    srcEntityTypeName, srcId, targetEntityTypeName, HttpStatus.SC_BAD_REQUEST);

            // エラーメッセージのチェック
            ODataCommon.checkErrorResponseBody(res, PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED.getCode(),
                    PersoniumCoreException.OData.LINK_UPPER_LIMIT_RECORD_EXEED.getMessage());

            // 登録済み件数が増加していないことのチェック
            String query = String.format("?\\$top=%s&\\$inlinecount=allpages", 0);
            TResponse response = Http.request("box/odatacol/list.txt")
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
            ODataCommon.checkResponseBodyCount(response.bodyAsJson(), registUserDataCount);
        } finally {
            // 追加したデータの削除
            deleteUserDataWithLink(srcEntityTypeName, targetEntityTypeName, registUserDataCount);
            deleteUserDataWithLink(srcEntityTypeName, targetEntityTypeName, registUserDataCount + 1);
        }
    }

    private void deleteUserDataWithLink(String srcEntityType, String targetEntityType, int index) {
        String id = String.format("id%03d", index);
        UserDataUtils.deleteLinks(cellName, boxName, colName,
                srcEntityType, srcId, targetEntityType, id, -1);
        UserDataUtils.delete(masterToken, -1, cellName, boxName, colName, targetEntityType, id);
    }

}
