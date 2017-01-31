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
package io.personium.core.model.impl.es.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.es.EsClient;
import io.personium.common.es.EsIndex;
import io.personium.common.es.response.DcIndexResponse;
import io.personium.core.DcCoreConfig;
import io.personium.core.model.impl.es.ads.AdsException;
import io.personium.core.model.impl.es.ads.JdbcAds;
import io.personium.core.model.impl.es.doc.LinkDocHandler;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.DcRunner;

/**
 * ODataLinkAccessorTestの単体テストケース.
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class ODataLinkAccessorTest {

    private static final String UNIT_PREFIX = DcCoreConfig.getEsUnitPrefix();
    private static final String UNIT_USER_NAME = "index_for_test";
    private static final String INDEX_NAME = UNIT_PREFIX + "_" + UNIT_USER_NAME;
    private static final String TYPE_NAME = "TypeForTest";
    private static final String ROUTING_ID = "RoutingIdTest";

    private static EsClient esClient;

    /**
     * 各テスト実行前の初期化処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @Before
    public void setUp() throws Exception {
        esClient = new EsClient(DcCoreConfig.getEsClusterName(), DcCoreConfig.getEsHosts());
    }

    /**
     * 各テスト実行後のクリーンアップ処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @After
    public void tearDown() throws Exception {
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        try {
            index.delete();
            JdbcAds ads = new JdbcAds();
            ads.deleteIndex(INDEX_NAME);
        } catch (Exception ex) {
            System.out.println("");
        }
    }

    /**
     * 例外用Mock.
     * @author Administrator
     */
    class JdbcAdsMock extends JdbcAds {

        JdbcAdsMock() throws Exception {
            super();
        }

        @Override
        public void createLink(String index, LinkDocHandler docHandler) throws AdsException {
            throw new AdsException("MockErrorCreare");
        }

        @Override
        public void updateLink(String index, LinkDocHandler docHandler) throws AdsException {
            throw new AdsException("MockErrorUpdate");
        }

        @Override
        public void deleteLink(String index, String id) throws AdsException {
            throw new AdsException("MockErrorDelete");
        }
    }

    /**
     * create処理が正常に終了する.
     */
    @Test
    public void create処理が正常に終了する() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();

        // データ登録実行
        DcIndexResponse response = linkAccessor.create(docHandler);

        // レスポンスのチェック
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // マスタにデータが登録されていることを確認
        if (linkAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(1, ads.countLink(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        linkAccessor.delete(docHandler);
    }

    /**
     * create処理にてAdsが例外を上げた場合でも正常に終了すること.
     */
    @Test
    public void create処理にてAdsが例外を上げた場合でも正常に終了すること() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();
        try {
            linkAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データ登録実行
        DcIndexResponse response = linkAccessor.create(docHandler);

        // レスポンスのチェック
        assertNotNull(response);
        assertFalse(response.getId().equals(""));
    }

    /**
     * update処理が正常に終了する.
     */
    @Test
    public void update処理が正常に終了する() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();
        DcIndexResponse createResponse = linkAccessor.create(docHandler);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));

        // データ更新実行
        String updateString = "updatedEnt1Key";
        docHandler.setEnt1Key(updateString);

        DcIndexResponse updateResponse = linkAccessor.update(createResponse.getId(), docHandler);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(createResponse.getId(), updateResponse.getId());

        // マスタにデータが更新されていることを確認
        if (linkAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                List<JSONObject> list = ads.getLinkList(INDEX_NAME, 0, 1);
                assertEquals(1, list.size());
                assertEquals(updateString, ((JSONObject) list.get(0).get("source")).get("k1"));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        linkAccessor.delete(docHandler);
    }

    /**
     * update処理にてAdsが例外を上げた場合でも正常に終了すること.
     */
    @Test
    public void update処理にてAdsが例外を上げた場合でも正常に終了すること() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();
        DcIndexResponse createResponse = linkAccessor.create(docHandler);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));
        try {
            linkAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データ更新実行
        String updateString = "updatedEnt1Key";
        docHandler.setEnt1Key(updateString);

        DcIndexResponse updateResponse = linkAccessor.update(createResponse.getId(), docHandler);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(createResponse.getId(), updateResponse.getId());
    }

    /**
     * delete処理が正常に終了する.
     */
    @Test
    public void delete処理が正常に終了する() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();
        DcIndexResponse response = linkAccessor.create(docHandler);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // データを削除する
        linkAccessor.delete(docHandler);

        // データが削除されていることを確認する
        if (linkAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(0, ads.countLink(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }
    }

    /**
     * delete処理にてAdsが例外を上げた場合でも正常に終了すること.
     */
    @Test
    public void delete処理にてAdsが例外を上げた場合でも正常に終了すること() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        ODataLinkAccessor linkAccessor = new ODataLinkAccessor(index, TYPE_NAME, ROUTING_ID);
        LinkDocHandler docHandler = createTestLinkDocHandler();
        DcIndexResponse response = linkAccessor.create(docHandler);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));
        try {
            linkAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データを削除する
        linkAccessor.delete(docHandler);
    }

    /**
     * LinkDocHandlerを生成する.
     * @return
     */
    private LinkDocHandler createTestLinkDocHandler() {
        long dateTime = new Date().getTime();
        LinkDocHandler docHandler = new LinkDocHandler();
        docHandler.setBoxId("testBoxId");
        docHandler.setCellId("testCellId");
        docHandler.setNodeId("testNodeId");
        docHandler.setPublished(dateTime);
        docHandler.setUpdated(dateTime);
        docHandler.setEnt1Key("ent1Key");
        docHandler.setEnt1Type("ent1Type");
        docHandler.setEnt2Key("ent2Key");
        docHandler.setEnt2Type("ent2Type");
        return docHandler;
    }
}
