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
package io.personium.test.unit.core.model.impl.es.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.sun.jersey.test.framework.WebAppDescriptor;

import io.personium.common.ads.AdsWriteFailureLogException;
import io.personium.common.ads.AdsWriteFailureLogInfo;
import io.personium.common.ads.AdsWriteFailureLogWriter;
import io.personium.common.es.EsIndex;
import io.personium.common.es.EsType;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.Cell;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.ads.Ads;
import io.personium.core.model.impl.es.ads.AdsException;
import io.personium.core.model.impl.es.ads.JdbcAds;
import io.personium.core.model.impl.es.doc.CellDocHandler;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.model.impl.es.doc.UserDataDocHandler;
import io.personium.core.model.impl.es.odata.UserDataODataProducer;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockKeyComposer;
import io.personium.core.webcontainer.listener.RepairServiceLauncher;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * マスタ自動復旧機能のcoreとの結合テストクラス.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class })
public class RepairAdsIntegrationTest extends AbstractCase {

    private String cellName = "repairAdsTestCell";
    private String owner = "repairadstest";

    private Ads ads;

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "io.personium.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public RepairAdsIntegrationTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
    }

    /**
     * core経由で登録されたCellデータをMySQLに登録できること.
     * @throws AdsException MySQL操作失敗
     */
    @Before
    public void before() throws AdsException {
        try {
            ads = new JdbcAds();
        } catch (AdsException e) {
            return;
        }
    }

    /**
     * personium経由で登録されたCellデータをMySQLに登録できること.
     * @throws AdsException MySQL操作失敗
     */
    @Test
    public void persnoium経由で登録されたCellデータをMySQLに登録できること() throws AdsException {
        String indexName = PersoniumUnitConfig.getEsUnitPrefix() + "_" + IndexNameEncoder.encodeEsIndexName("ad");
        String dbName = PersoniumUnitConfig.getEsUnitPrefix() + "_" + IndexNameEncoder.encodeEsIndexName(owner);

        // ESアクセス情報
        String esTypeName = Cell.EDM_TYPE_NAME;
        String routingId = EsIndex.CELL_ROUTING_KEY_NAME;
        String searchFieldName = "Name";
        String searchFieldValue = cellName;

        try {
            // personium.io経由でCell登録（UnitUser指定）
            CellUtils.create(cellName, MASTER_TOKEN_NAME, owner, HttpStatus.SC_CREATED);

            // ESからデータ取得
            PersoniumSearchHit esHit = searchFromEs(indexName, esTypeName, routingId,
                    searchFieldName, searchFieldValue);
            String id = esHit.getId();
            EntitySetDocHandler esDocument = new CellDocHandler(esHit);

            // MySQLから該当データ検索（比較用に控えておく）
            List<String> idList = new ArrayList<String>();
            idList.add(id);
            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponceBefore = ads.searchCellList(dbName, idList);
            assertEquals(1, adsResponceBefore.size());
            // MySQLから該当データ削除
            ads.deleteCell(dbName, id);

            // 取得したデータを元にジャーナルログ作成
            String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                    esDocument.getCellId(), null, null);
            AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                    esDocument.getUnitUserName(), esDocument.getType(), lockKey,
                    esDocument.getCellId(), esDocument.getId(),
                    AdsWriteFailureLogInfo.OperationKind.CREATE, 1, esDocument.getUpdated());
            recordAdsWriteFailureLog(loginfo);

            // リペア実行
            RepairServiceLauncher.RepairAdsService service = new RepairServiceLauncher.RepairAdsService();
            service.run();

            // データがリペアされていることを確認(データが存在すること)
            List<JSONObject> adsResponceAfter = ads.searchCellList(dbName, idList);

            // データがリペアされていることを確認（削除前とリペア後のCellが一致すること）
            assertEquals(((JSONObject) adsResponceBefore.get(0)).get("id"), ((JSONObject) adsResponceAfter.get(0))
                    .get("id"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("u"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("u"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("d"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("d"));
            assertEquals(
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("s"),
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("s"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("b"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("b"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("c"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("c"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("p"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("p"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("a"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("a"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("n"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("n"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("ll"), ((JSONObject) adsResponceAfter
                    .get(0).get("source")).get("ll"));
            assertEquals(
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("h"),
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("h"));
            assertEquals(
                    ((JSONObject) adsResponceBefore.get(0)).get("type"),
                    ((JSONObject) adsResponceAfter.get(0)).get("type"));

            // personium.io経由でCell削除
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, HttpStatus.SC_NO_CONTENT);
            // データが削除されていること
            adsResponceAfter = ads.searchCellList(dbName, idList);
            assertEquals(0, adsResponceAfter.size());
        } finally {
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * core経由で登録されたODATAデータをMySQLに登録できること.
     * @throws AdsException MySQL操作失敗
     */
    @SuppressWarnings("unchecked")
    @Test
    public void core経由で登録されたODATAデータをMySQLに登録できること() throws AdsException {

        String userDataIndexName = PersoniumUnitConfig.getEsUnitPrefix()
                + "_" + IndexNameEncoder.encodeEsIndexName(owner);

        // ESアクセス情報
        String userDataEsTypeName = UserDataODataProducer.USER_ODATA_NAMESPACE;
        String userDataSearchFieldName = "__id";
        String userDataSearchFieldValue = "userDataId";

        String boxName = "testBox";
        String colName = "testCol";
        String entityType = "testEntityType";
        String userDataId = "userDataId";
        try {
            JSONObject body = new JSONObject();
            body.put("__id", userDataId);

            // 事前にデータ登録
            CellUtils.create(cellName, MASTER_TOKEN_NAME, owner, HttpStatus.SC_CREATED);
            BoxUtils.create(cellName, boxName, MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils
                    .createODataCollection(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cellName, boxName, colName);
            EntityTypeUtils.create(cellName, MASTER_TOKEN_NAME, boxName, colName, entityType,
                    HttpStatus.SC_CREATED);
            UserDataUtils
                    .create(MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, body, cellName, boxName, colName, entityType);

            // ESからデータ取得
            String cellIndexName = PersoniumUnitConfig.getEsUnitPrefix()
                    + "_" + IndexNameEncoder.encodeEsIndexName("ad");
            PersoniumSearchHit cellEsHit = searchFromEs(cellIndexName, Cell.EDM_TYPE_NAME,
                    EsIndex.CELL_ROUTING_KEY_NAME, "Name", cellName);
            String cellId = cellEsHit.getId();
            PersoniumSearchHit userDataEsHit = searchFromEs(userDataIndexName, userDataEsTypeName, cellId,
                    userDataSearchFieldName,
                    userDataSearchFieldValue);
            EntitySetDocHandler esDocument = new UserDataDocHandler(userDataEsHit);

            // MySQLから該当データ検索（比較用に控えておく）
            List<String> idList = new ArrayList<String>();
            idList.add(userDataEsHit.getId());
            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponceBefore = ads.searchEntityList(userDataIndexName, idList);
            assertEquals(1, adsResponceBefore.size());
            // MySQLから該当データ削除
            ads.deleteEntity(userDataIndexName, userDataEsHit.getId());

            // 取得したデータを元にジャーナルログ作成
            String lockKey = LockKeyComposer.fullKeyFromCategoryAndKey(Lock.CATEGORY_ODATA,
                    esDocument.getCellId(), esDocument.getBoxId(), esDocument.getNodeId());
            AdsWriteFailureLogInfo loginfo = new AdsWriteFailureLogInfo(
                    userDataIndexName, esDocument.getType(), lockKey,
                    esDocument.getCellId(), esDocument.getId(),
                    AdsWriteFailureLogInfo.OperationKind.CREATE, 1, esDocument.getUpdated());
            recordAdsWriteFailureLog(loginfo);

            // リペア実行
            RepairServiceLauncher.RepairAdsService service = new RepairServiceLauncher.RepairAdsService();
            service.run();

            // データがリペアされていることを確(データが存在すること)
            List<JSONObject> adsResponceAfter = ads.searchEntityList(userDataIndexName, idList);

            assertEquals(((JSONObject) adsResponceBefore.get(0)).get("id"), ((JSONObject) adsResponceAfter.get(0))
                    .get("id"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("u"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("u"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("d"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("d"));
            assertEquals(
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("s"),
                    ((JSONObject) adsResponceAfter.get(0).get("source")).get("s"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("b"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("b"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("c"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("c"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("p"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("p"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("a"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("a"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("n"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("n"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("ll"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("ll"));
            assertEquals(((JSONObject) adsResponceBefore.get(0).get("source")).get("t"), ((JSONObject)
                    adsResponceAfter
                            .get(0).get("source")).get("t"));
            assertEquals(
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("h"),
                    ((JSONObject) adsResponceBefore.get(0).get("source")).get("h"));
            assertEquals(((JSONObject) adsResponceBefore.get(0)).get("type"),
                    ((JSONObject) adsResponceAfter.get(0)).get("type"));
        } finally {
            UserDataUtils.delete(MASTER_TOKEN_NAME, -1, cellName, boxName, colName, entityType,
                    userDataId);
            EntityTypeUtils.delete(colName, MASTER_TOKEN_NAME, MediaType.APPLICATION_JSON, entityType, boxName,
                    cellName, -1);
            DavResourceUtils.deleteCollection(cellName, boxName, colName, MASTER_TOKEN_NAME, -1);
            BoxUtils.delete(cellName, MASTER_TOKEN_NAME, boxName, -1);
            CellUtils.delete(MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * Elasticsearchから指定のデータを検索する.
     * @param indexName インデックス名
     * @param esTypeName ESのtype名
     * @param routingId ESのルーティングID
     * @param searchFieldName ES検索条件のフィールド名（"untouched"は不要）
     * @param searchFieldValue ES検索条件の値
     * @return 検索結果
     */
    @SuppressWarnings("unchecked")
    PersoniumSearchHit searchFromEs(String indexName,
            String esTypeName,
            String routingId,
            String searchFieldName,
            String searchFieldValue) {
        EsType type = EsModel.type(indexName, esTypeName, routingId, 0, 0);
        String query = "{"
                + "    \"query\": {"
                + "        \"filtered\": {"
                + "           \"query\": {\"match_all\": {}},"
                + "           \"filter\": {\"term\": {"
                + "              \"s." + searchFieldName + ".untouched\": \"" + searchFieldValue + "\""
                + "           }}"
                + "        }"
                + "    }, "
                + "    \"size\": 1"
                + "}";
        PersoniumSearchResponse esResponse;
        try {
            esResponse = type.search((Map<String, Object>) new JSONParser().parse(query));
            assertEquals("Failed to retrieve test data from Elasticsearch.", 1, esResponse.getHits().getCount());
            PersoniumSearchHit esHit = esResponse.getHits().getAt(0);
            return esHit;
        } catch (ParseException e) {
            fail("Failed to parse query for ES. " + e.getMessage());
        }
        return null;
    }

    /**
     * Ads書込み失敗ログ出力.
     * @param loginfo リペア用のエラー情報
     */
    protected void recordAdsWriteFailureLog(AdsWriteFailureLogInfo loginfo) {
        AdsWriteFailureLogWriter adsWriteFailureLogWriter = AdsWriteFailureLogWriter.getInstance(
                PersoniumUnitConfig.getAdsWriteFailureLogDir(),
                PersoniumUnitConfig.getCoreVersion(),
                PersoniumUnitConfig.getAdsWriteFailureLogPhysicalDelete());
        try {
            adsWriteFailureLogWriter.writeActiveFile(loginfo);
        } catch (AdsWriteFailureLogException e2) {
            PersoniumCoreLog.Server.WRITE_ADS_FAILURE_LOG_ERROR.reason(e2).writeLog();
            PersoniumCoreLog.Server.WRITE_ADS_FAILURE_LOG_INFO.params(loginfo.toString());
        }
    }

}
