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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
import io.personium.common.es.response.DcActionResponse;
import io.personium.common.es.response.DcGetResponse;
import io.personium.common.es.response.DcIndexResponse;
import io.personium.common.es.response.EsClientException;
import io.personium.common.es.util.DcUUID;
import io.personium.core.DcCoreConfig;
import io.personium.core.model.file.BinaryDataAccessException;
import io.personium.core.model.file.BinaryDataAccessor;
import io.personium.core.model.impl.es.DavNode;
import io.personium.core.model.impl.es.ads.AdsException;
import io.personium.core.model.impl.es.ads.JdbcAds;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.DcRunner;

/**
 * DavNodeAccessorTestの単体テストケース.
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class DavNodeAccessorTest {

    private static final String UNIT_USER_NAME = "index_for_test";
    private static final String UNIT_PREFIX = DcCoreConfig.getEsUnitPrefix();
    private static final String INDEX_NAME = UNIT_PREFIX + "_" + UNIT_USER_NAME;
    private static final String TYPE_NAME = "TypeForTest";
    private static final String ROUTING_ID = "RoutingIdTest";

    private BinaryDataAccessor binaryDataAccessor = new BinaryDataAccessor(DcCoreConfig.getBlobStoreRoot(),
            "index_for_test", DcCoreConfig.getFsyncEnabled());
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
        public void createDavNode(String index, DavNode davNode) throws AdsException {
            throw new AdsException("MockErrorCreare");
        }

        @Override
        public void updateDavNode(String index, DavNode davNode) throws AdsException {
            throw new AdsException("MockErrorUpdate");
        }

        @Override
        public void deleteDavNode(String index, String id) throws AdsException {
            throw new AdsException("MockErrorDelete");
        }
    }

    /**
     * 例外用Mock.
     * @author Administrator
     */
    class JdbcAdsMockDavNodeAccessor extends DavNodeAccessor {

        JdbcAdsMockDavNodeAccessor(EsIndex index, String name, String routingId) throws Exception {
            super(index, name, routingId);
        }

        @Override
        public DcActionResponse createForFile(String id, DavNode davNode) {
            throw new EsClientException(id);
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
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();

        // データ登録実行
        DcIndexResponse response = davNodeAccessor.create(davNode);

        // レスポンスのチェック
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // マスタにデータが登録されていることを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(1, ads.countDavNode(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
    }

    /**
     * create処理にてAdsが例外を上げた場合でも正常に終了すること.
     */
    @Test
    public void create処理にてAdsが例外を上げた場合でも正常に終了すること() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();
        try {
            davNodeAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データ登録実行
        DcIndexResponse response = davNodeAccessor.create(davNode);

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
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();
        DcIndexResponse createResponse = davNodeAccessor.create(davNode);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));

        // データ更新実行
        long dateTime = new Date().getTime();
        davNode.setUpdated(dateTime);

        DcIndexResponse updateResponse = davNodeAccessor.update(createResponse.getId(), davNode);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(createResponse.getId(), updateResponse.getId());

        // マスタにデータが更新されていることを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                List<JSONObject> list = ads.getDavNodeList(INDEX_NAME, 0, 1);
                assertEquals(1, list.size());
                assertEquals(dateTime, Long.parseLong((String) ((JSONObject) list.get(0).get("source")).get("u")));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
    }

    /**
     * update処理にてAdsが例外を上げた場合でも正常に終了すること.
     */
    @Test
    public void update処理にてAdsが例外を上げた場合でも正常に終了すること() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();
        DcIndexResponse createResponse = davNodeAccessor.create(davNode);
        assertNotNull(createResponse);
        assertFalse(createResponse.getId().equals(""));
        try {
            davNodeAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データ更新実行
        long dateTime = new Date().getTime();
        davNode.setUpdated(dateTime);

        DcIndexResponse updateResponse = davNodeAccessor.update(createResponse.getId(), davNode);

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
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();
        DcIndexResponse response = davNodeAccessor.create(davNode);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));

        // データを削除する
        davNodeAccessor.delete(davNode);

        // データが削除されていることを確認する
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(0, ads.countDavNode(INDEX_NAME));
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
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();
        DcIndexResponse response = davNodeAccessor.create(davNode);
        assertNotNull(response);
        assertFalse(response.getId().equals(""));
        try {
            davNodeAccessor.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
    }

    /**
     * ファイルありcreate処理が正常に終了する.
     */
    @Test
    public void ファイルありcreate処理が正常に終了する() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);

        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);

        // データ登録実行
        DcActionResponse response = davNodeAccessor.createForFile(id, davNode);

        // レスポンスのチェック
        assertNotNull(response);
        String resId = null;
        if (response instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) response).getId();
        } else if (response instanceof DcGetResponse) {
            resId = ((DcGetResponse) response).getId();
        }
        assertEquals(id, resId);

        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // マスタにデータが登録されていることを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(1, ads.countDavNode(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
        deleteTestTmpFile(id);
    }

    /**
     * ファイルありcreate処理でESエラーが発生した場合ファイルが存在しないこと.
     */
    @Test
    public void ファイルありcreate処理でESエラーが発生した場合ファイルが存在しないこと() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);

        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);

        // データ登録実行
        DcActionResponse response = davNodeAccessor.createForFile(id, davNode);

        // レスポンスのチェック
        assertNotNull(response);
        String resId = null;
        if (response instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) response).getId();
        } else if (response instanceof DcGetResponse) {
            resId = ((DcGetResponse) response).getId();
        }
        assertEquals(id, resId);
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // マスタにデータが登録されていることを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(1, ads.countDavNode(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // テスト用にファイル削除
        deleteTestTmpFile(id);

        try {
            // データ登録実行
            // Mockを使用してESClientExceptionを発生させる
            new JdbcAdsMockDavNodeAccessor(index, TYPE_NAME, ROUTING_ID).createForFile(id, davNode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertFalse(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // データを削除する
        davNodeAccessor.delete(davNode);
    }

    /**
     * ファイルありcreate処理でMySQLエラーが発生した場合ファイルが存在しないこと.
     */
    @Test
    public void ファイルありcreate処理でMySQLエラーが発生した場合ファイルが存在しないこと() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);

        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);

        DavNodeAccessor davNodeAccessorTest = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        try {
            davNodeAccessorTest.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }

        // データ登録実行
        DcActionResponse response = davNodeAccessorTest.createForFile(id, davNode);

        // レスポンスのチェック
        assertNotNull(response);
        String resId = null;
        if (response instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) response).getId();
        } else if (response instanceof DcGetResponse) {
            resId = ((DcGetResponse) response).getId();
        }
        assertEquals(id, resId);
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // マスタにデータが登録されていないことを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(0, ads.countDavNode(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
        deleteTestTmpFile(id);
    }

    /**
     * ファイルありcreate処理で一時ファイルのコピーに失敗した場合ファイルが存在しないこと.
     */
    @Test
    public void ファイルありcreate処理で一時ファイルのコピーに失敗した場合ファイルが存在しないこと() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);

        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode();

        try {
            // データ登録実行
            davNodeAccessor.createForFile(id, davNode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // レスポンスのチェック
        assertFalse(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // マスタにデータが登録されていないことを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                assertEquals(0, ads.countDavNode(INDEX_NAME));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
    }

    /**
     * ファイルありupdate処理が正常に終了する.
     */
    @Test
    public void ファイルありupdate処理が正常に終了する() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);
        DcActionResponse createResponse = davNodeAccessor.createForFile(id, davNode);
        assertNotNull(createResponse);
        String resId = null;
        if (createResponse instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) createResponse).getId();
        } else if (createResponse instanceof DcGetResponse) {
            resId = ((DcGetResponse) createResponse).getId();
        }
        assertFalse(resId.equals(""));

        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // データ更新実行
        long dateTime = new Date().getTime();
        davNode.setUpdated(dateTime);
        createTestTmpFile(id, "DavNodeAccessorTest Update!");

        DcIndexResponse updateResponse = davNodeAccessor.updateForFile(resId, davNode, -1);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(resId, updateResponse.getId());
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));
        assertEquals("DavNodeAccessorTest Update!", readFile(id));

        // マスタにデータが更新されていることを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                List<JSONObject> list = ads.getDavNodeList(INDEX_NAME, 0, 1);
                assertEquals(1, list.size());
                assertEquals(dateTime, Long.parseLong((String) ((JSONObject) list.get(0).get("source")).get("u")));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
        deleteTestTmpFile(id);
    }

    /**
     * ファイルありupdate処理でMySQLエラーが発生した場合ファイルが更新されないこと.
     */
    @Test
    public void ファイルありupdate処理でMySQLエラーが発生した場合ファイルが更新されないこと() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);
        DcActionResponse createResponse = davNodeAccessor.createForFile(id, davNode);
        assertNotNull(createResponse);
        String resId = null;
        if (createResponse instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) createResponse).getId();
        } else if (createResponse instanceof DcGetResponse) {
            resId = ((DcGetResponse) createResponse).getId();
        }
        assertFalse(resId.equals(""));
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // データ更新実行
        long dateTime = new Date().getTime();
        davNode.setUpdated(dateTime);
        createTestTmpFile(id, "DavNodeAccessorTest Update!");

        DavNodeAccessor davNodeAccessorUp = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        try {
            davNodeAccessorUp.setAds(new JdbcAdsMock());
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        DcIndexResponse updateResponse = davNodeAccessorUp.updateForFile(resId, davNode, -1);

        // レスポンスのチェック
        assertNotNull(updateResponse);
        assertEquals(resId, updateResponse.getId());
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));
        assertEquals("DavNodeAccessorTest Update!", readFile(id));

        // マスタにデータが更新されていないことを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                List<JSONObject> list = ads.getDavNodeList(INDEX_NAME, 0, 1);
                assertEquals(1, list.size());
                assertFalse(Long.toString(dateTime).equals((String) ((JSONObject) list.get(0).get("source")).get("u")));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
        deleteTestTmpFile(id);
    }

    /**
     * ファイルありupdate処理で一時ファイルのコピーに失敗した場合ファイルが更新されないこと.
     */
    @Test
    public void ファイルありupdate処理で一時ファイルのコピーに失敗した場合ファイルが更新されないこと() {
        // 事前準備
        EsIndex index = esClient.idxUser(UNIT_PREFIX, UNIT_USER_NAME);
        assertNotNull(index);
        String id = DcUUID.randomUUID();
        DavNodeAccessor davNodeAccessor = new DavNodeAccessor(index, TYPE_NAME, ROUTING_ID);
        DavNode davNode = createTestDavNode(id);
        DcActionResponse createResponse = davNodeAccessor.createForFile(id, davNode);
        assertNotNull(createResponse);
        String resId = null;
        if (createResponse instanceof DcIndexResponse) {
            resId = ((DcIndexResponse) createResponse).getId();
        } else if (createResponse instanceof DcGetResponse) {
            resId = ((DcGetResponse) createResponse).getId();
        }
        assertFalse(resId.equals(""));
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));

        // データ更新実行
        long dateTime = new Date().getTime();
        davNode.setUpdated(dateTime);

        try {
            davNodeAccessor.updateForFile(resId, davNode, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // レスポンスのチェック
        assertTrue(binaryDataAccessor.existsForFilename(id));
        assertFalse(binaryDataAccessor.existsForFilename(id + ".tmp"));
        assertEquals("DavNodeAccessorTest", readFile(id));

        // マスタにデータが更新されていないことを確認
        if (davNodeAccessor.getAds() != null) {
            JdbcAds ads = null;
            try {
                ads = new JdbcAds();
                List<JSONObject> list = ads.getDavNodeList(INDEX_NAME, 0, 1);
                assertEquals(1, list.size());
                assertFalse(Long.toString(dateTime).equals((String) ((JSONObject) list.get(0).get("source")).get("u")));
            } catch (Exception e) {
                fail(e.getMessage());
            }
        }

        // データを削除する
        davNodeAccessor.delete(davNode);
        deleteTestTmpFile(id);
    }

    /**
     * DavCmpEsImplを生成する.
     * @return DavCmpEsImpl
     */
    private DavNode createTestDavNode() {
        DavNode davNode = new DavNode();
        return davNode;
    }

    /**
     * DavCmpEsImplを生成する.
     * @return DavCmpEsImpl
     */
    private DavNode createTestDavNode(String id) {
        DavNode davNode = new DavNode();
        // 一時ファイル作成
        createTestTmpFile(id);
        return davNode;
    }

    private void createTestTmpFile(String id) {
        createTestTmpFile(id, "DavNodeAccessorTest");
    }

    private void createTestTmpFile(String id, String body) {
        InputStream is = new ByteArrayInputStream(body.getBytes());
        BufferedInputStream bufferedInput = new BufferedInputStream(is);

        try {
            binaryDataAccessor.create(bufferedInput, id);
        } catch (BinaryDataAccessException e) {
            fail(e.getMessage());
        }
    }

    private void deleteTestTmpFile(String id) {
        try {
            binaryDataAccessor.deletePhysicalFile(id);
        } catch (BinaryDataAccessException e) {
            fail(e.getMessage());
        }
    }

    private String readFile(String filename) {
        FileReader f = null;
        BufferedReader b = null;
        StringBuffer sb = new StringBuffer();

        String filepath = binaryDataAccessor.getFilePath(filename);
        try {
            f = new FileReader(filepath);
            b = new BufferedReader(f);
            String s;
            while ((s = b.readLine()) != null) {
                sb.append(s);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (b != null) {
                try {
                    b.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}
