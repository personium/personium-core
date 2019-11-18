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

import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * ユーザODataの$links取得で、$top・$skipクエリを指定した場合のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class })
// 本テストでは1回につき25件のユーザODataを作成する。IT以降のテストの時間短縮のため、コミットビルドでのみ動作するようにしている。
public class UserDataLinkTopSkipTest extends PersoniumTest {

    private static final String MASTER_TOKEN_NAME = Setup.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ.
     */
    public UserDataLinkTopSkipTest() {
        super(new PersoniumCoreApplication());
    }

    private String toEntityTypeName = "toEntity";
    private String fromEntityTypeName = "fromEntity";
    private String toUserDataId = "toEntitySet";
    private String fromUserDataId = "fromEntitySet";

    /**
     * ユーザデータのlink一覧取得で$topに1$skipに10を指定した場合に11件目のデータが取得されること_AssociationEndがアスタ対アスタ.
     * @throws ParseException リクエストのパースに失敗
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topに1$skipに10を指定した場合に11件目のデータが取得されること_AssociationEndがアスタ対アスタ()
            throws ParseException {
        String cellName = "userdatalinknnskip";
        String boxName = "box1";
        String colName = "col";

        try {
            // 事前準備
            createSchemaandBaseData(cellName, boxName, colName, "*", "*");
            // 検索対象のユーザデータ作成
            for (int i = 0; i < 26; i++) {
                String body = String.format("{\"__id\":\"%s%03d\",\"name\":\"tama\"}", toUserDataId, i);
                UserDataUtils.createViaNP(MASTER_TOKEN_NAME, (JSONObject) new JSONParser().parse(body), cellName,
                        boxName, colName, fromEntityTypeName, fromUserDataId, toEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $links一覧取得(クエリあり)
            String query = "?\\$top=1&\\$skip=10";
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", fromEntityTypeName + "('" + fromUserDataId + "')")
                    .with("trgPath", toEntityTypeName)
                    .with("query", query)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            // 取得内容が正しいかのチェック
            assertEquals(1, results.size());
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, toEntityTypeName, toUserDataId + "010"));
            ODataCommon.checkLinResponseBody(res.bodyAsJson(), expectedUriList);

            // $links一覧取得(クエリなし)
            res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", fromEntityTypeName + "('" + fromUserDataId + "')")
                    .with("trgPath", toEntityTypeName)
                    .with("query", "")
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            // 取得内容が正しいかのチェック
            assertEquals(25, results.size());
        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    /**
     * ユーザデータのlink一覧取得で$topに1$skipに10を指定した場合に11件目のデータが取得されること_AssociationEndがONE対アスタ.
     * @throws ParseException リクエストのパースに失敗
     */
    @Test
    public final void ユーザデータのlink一覧取得で$topに1$skipに10を指定した場合に11件目のデータが取得されること_AssociationEndがONE対アスタ()
            throws ParseException {
        String cellName = "userdatalinknnskip";
        String boxName = "box1";
        String colName = "col";

        try {
            // 事前準備
            createSchemaandBaseData(cellName, boxName, colName, "0..1", "*");
            // 検索対象のユーザデータ作成
            for (int i = 0; i < 26; i++) {
                String body = String.format("{\"__id\":\"%s%03d\",\"name\":\"tama\"}", toUserDataId, i);
                UserDataUtils.createViaNP(MASTER_TOKEN_NAME, (JSONObject) new JSONParser().parse(body), cellName,
                        boxName, colName, fromEntityTypeName, fromUserDataId, toEntityTypeName, HttpStatus.SC_CREATED);
            }

            // $links一覧取得
            String query = "?\\$top=1&\\$skip=10";
            TResponse res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", fromEntityTypeName + "('" + fromUserDataId + "')")
                    .with("trgPath", toEntityTypeName)
                    .with("query", query)
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            JSONArray results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            // 取得内容が正しいかのチェック
            assertEquals(1, results.size());
            ArrayList<String> expectedUriList = new ArrayList<String>();
            expectedUriList.add(UrlUtils.userdata(cellName, boxName, colName, toEntityTypeName, toUserDataId + "010"));
            ODataCommon.checkLinResponseBody(res.bodyAsJson(), expectedUriList);

            // $links一覧取得(クエリなし)
            res = Http.request("box/odatacol/list-link-with-query.txt")
                    .with("cellPath", cellName)
                    .with("boxPath", boxName)
                    .with("colPath", colName)
                    .with("srcPath", fromEntityTypeName + "('" + fromUserDataId + "')")
                    .with("trgPath", toEntityTypeName)
                    .with("query", "")
                    .with("token", MASTER_TOKEN_NAME)
                    .with("accept", MediaType.APPLICATION_JSON)
                    .returns()
                    .statusCode(HttpStatus.SC_OK)
                    .debug();
            results = (JSONArray) ((JSONObject) res.bodyAsJson().get("d")).get("results");
            // 取得内容が正しいかのチェック
            assertEquals(25, results.size());
        } finally {
            Setup.cellBulkDeletion(cellName);
        }
    }

    private void createSchemaandBaseData(String cellName, String boxName, String colName,
            String fromMultiplicity, String toMultiplicity) {
        // cell~スキーマ情報を作成
        CellUtils.create(cellName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        DavResourceUtils.createODataCollection(MASTER_TOKEN_NAME,
                HttpStatus.SC_CREATED, cellName, boxName, colName);
        EntityTypeUtils.create(cellName, PersoniumUnitConfig.getMasterToken(),
                colName, toEntityTypeName, HttpStatus.SC_CREATED);
        EntityTypeUtils.create(cellName, PersoniumUnitConfig.getMasterToken(),
                colName, fromEntityTypeName, HttpStatus.SC_CREATED);
        AssociationEndUtils.create(PersoniumUnitConfig.getMasterToken(), fromMultiplicity, cellName,
                boxName, colName, HttpStatus.SC_CREATED, "AssociationEnd", fromEntityTypeName);
        AssociationEndUtils.create(PersoniumUnitConfig.getMasterToken(), toMultiplicity, cellName,
                boxName, colName, HttpStatus.SC_CREATED, "LinkAssociationEnd", toEntityTypeName);
        AssociationEndUtils.createLink(AbstractCase.MASTER_TOKEN_NAME, cellName, boxName,
                colName, fromEntityTypeName, toEntityTypeName, "AssociationEnd",
                "LinkAssociationEnd", HttpStatus.SC_NO_CONTENT);
        // ベースとなるユーザデータ作成
        UserDataUtils.create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED,
                "{\"__id\":\"" + fromUserDataId + "\",\"name\":\"pochi\"}", cellName, boxName, colName,
                fromEntityTypeName);
    }
}
