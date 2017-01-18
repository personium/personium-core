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
package com.fujitsu.dc.core.model.impl.es.accessor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.common.es.EsClient;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.QueryMapFactory;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;

/**
 * DataSourceAccessorの単体テストケース.
 */
@RunWith(DcRunner.class)
@Category({Unit.class })
public class DataSourceAccessorTest {

    private static EsClient esClient;

    /**
     * テストケース共通の初期化処理. テスト用のElasticsearchのNodeを初期化する
     * @throws Exception 異常が発生した場合の例外
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        esClient = new EsClient(DcCoreConfig.getEsClusterName(), DcCoreConfig.getEsHosts());
    }

    /**
     * テストケース共通のクリーンアップ処理. テスト用のElasticsearchのNodeをクローズする
     * @throws Exception 異常が発生した場合の例外
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        esClient.closeConnection();
    }

    /**
     * 各テスト実行前の初期化処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * 各テスト実行後のクリーンアップ処理.
     * @throws Exception 異常が発生した場合の例外
     */
    @After
    public void tearDown() throws Exception {
        EsIndex index = esClient.idxAdmin("index_for_test");
        try {
            index.delete();
        } catch (Exception ex) {
            System.out.println("");
        }
    }

    /**
     * create処理が正常に終了する.
     */
    @Test
    public void create処理が正常に終了する() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        DcIndexResponse response = dsa.create("id00001", new HashMap<Object, Object>());
        assertNotNull(response);
        assertEquals("id00001", response.getId());
    }

    /**
     * ID省略でcreate処理が正常に終了する.
     */
    @Test
    public void ID省略でcreate処理が正常に終了する() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        DcIndexResponse response = dsa.create(new HashMap<Object, Object>());
        assertNotNull(response);
        assertFalse(response.getId().equals(""));
    }

    /**
     * update処理が正常に終了する.
     */
    @Test
    public void update処理が正常に終了する() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        DcIndexResponse response = dsa.update("id00001", new HashMap<Object, Object>());
        assertNotNull(response);
        assertEquals("id00001", response.getId());
    }

    /**
     * search処理が正常に終了する.
     */
    @Test
    public void search処理が正常に終了する() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        DcSearchResponse response = dsa.search(new HashMap<String, Object>());
        assertNotNull(response);
    }

    /**
     * search処理でクエリにサイズを指定した場合指定したサイズのドキュメントが取得できる.
     */
    @Test
    public void search処理でクエリにサイズを指定した場合指定したサイズのドキュメントが取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", new HashMap<Object, Object>());

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("size", 3);
        DcSearchResponse response = dsa.search(query);
        assertEquals(5, response.getHits().getAllPages());
        assertEquals(3, response.getHits().getCount());
    }

    /**
     * search処理でクエリに実データ数以上のサイズを指定した場合実データと同じドキュメント数が取得できる.
     */
    @Test
    public void search処理でクエリに実データ数以上のサイズを指定した場合実データと同じドキュメント数が取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("size", 5);
        DcSearchResponse response = dsa.search(query);
        assertEquals(3, response.getHits().getAllPages());
        assertEquals(3, response.getHits().getCount());
    }

    /**
     * search処理でクエリにサイズ0を指定した場合0件取得できる.
     */
    @Test
    public void search処理でクエリにサイズ0を指定した場合0件取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("size", 0);
        DcSearchResponse response = dsa.search(query);
        assertEquals(3, response.getHits().getAllPages());
        assertEquals(0, response.getHits().getCount());
    }

    /**
     * search処理でクエリにnullを指定した場合全件取得できる.
     */
    @Test
    public void search処理でクエリにnullを指定した場合全件取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", new HashMap<Object, Object>());
        dsa.create("id00006", new HashMap<Object, Object>());
        dsa.create("id00007", new HashMap<Object, Object>());
        dsa.create("id00008", new HashMap<Object, Object>());
        dsa.create("id00009", new HashMap<Object, Object>());
        dsa.create("id00010", new HashMap<Object, Object>());
        dsa.create("id00011", new HashMap<Object, Object>());

        DcSearchResponse response = dsa.search(null);
        assertEquals(11, response.getHits().getAllPages());
        assertEquals(11, response.getHits().getCount());
    }

    /**
     * search処理でクエリにサイズを指定しなかった場合ドキュメントが全件取得できる.
     */
    @Test
    public void search処理でクエリにサイズを指定しなかった場合ドキュメントが全件取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");

        Map<String, String> data = new HashMap<String, String>();
        data.put("test", "value");
        dsa.create("id00001", data);
        dsa.create("id00002", data);
        dsa.create("id00003", data);
        dsa.create("id00004", data);
        dsa.create("id00005", data);
        dsa.create("id00006", data);
        dsa.create("id00007", data);
        dsa.create("id00008", data);
        dsa.create("id00009", data);
        dsa.create("id00010", data);
        dsa.create("id00011", data);

        Map<String, Object> query = new HashMap<String, Object>();
        query.put("query", QueryMapFactory.filteredQuery(null, QueryMapFactory.termQuery("test", "value")));
        DcSearchResponse response = dsa.search(query);
        assertEquals(11, response.getHits().getAllPages());
        assertEquals(11, response.getHits().getCount());
    }

    /**
     * count処理でクエリにnullを指定した場合全件数取得できる.
     */
    @Test
    public void count処理でクエリにnullを指定した場合全件数取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", new HashMap<Object, Object>());
        dsa.create("id00006", new HashMap<Object, Object>());
        dsa.create("id00007", new HashMap<Object, Object>());
        dsa.create("id00008", new HashMap<Object, Object>());
        dsa.create("id00009", new HashMap<Object, Object>());
        dsa.create("id00010", new HashMap<Object, Object>());
        dsa.create("id00011", new HashMap<Object, Object>());

        long response = dsa.count(null);
        assertEquals(11, response);
    }

    /**
     * count処理でクエリに空のクエリを指定した場合全件数取得できる.
     */
    @Test
    public void count処理でクエリに空のクエリを指定した場合全件数取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", new HashMap<Object, Object>());
        dsa.create("id00006", new HashMap<Object, Object>());
        dsa.create("id00007", new HashMap<Object, Object>());
        dsa.create("id00008", new HashMap<Object, Object>());
        dsa.create("id00009", new HashMap<Object, Object>());
        dsa.create("id00010", new HashMap<Object, Object>());
        dsa.create("id00011", new HashMap<Object, Object>());

        long response = dsa.count(new HashMap<String, Object>());
        assertEquals(11, response);
    }

    /**
     * count処理でクエリに3件ヒットするクエリを指定した場合件数が3で取得できる.
     */
    @Test
    public void count処理でクエリに3件ヒットするクエリを指定した場合件数が3で取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("sample", "test");
        dsa.create("id00001", body);
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", body);
        dsa.create("id00006", new HashMap<Object, Object>());
        dsa.create("id00007", new HashMap<Object, Object>());
        dsa.create("id00008", new HashMap<Object, Object>());
        dsa.create("id00009", body);
        dsa.create("id00010", new HashMap<Object, Object>());
        dsa.create("id00011", new HashMap<Object, Object>());

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("query", QueryMapFactory.filteredQuery(null, QueryMapFactory.termQuery("sample", "test")));

        long response = dsa.count(source);
        assertEquals(3, response);
    }

    /**
     * count処理でクエリに3件ヒットするクエリにサイズ2を指定した場合件数が3で取得できる.
     */
    @Test
    public void count処理でクエリに3件ヒットするクエリにサイズ2を指定した場合件数が3で取得できる() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("sample", "test");
        dsa.create("id00001", body);
        dsa.create("id00002", new HashMap<Object, Object>());
        dsa.create("id00003", new HashMap<Object, Object>());
        dsa.create("id00004", new HashMap<Object, Object>());
        dsa.create("id00005", body);
        dsa.create("id00006", new HashMap<Object, Object>());
        dsa.create("id00007", new HashMap<Object, Object>());
        dsa.create("id00008", new HashMap<Object, Object>());
        dsa.create("id00009", body);
        dsa.create("id00010", new HashMap<Object, Object>());
        dsa.create("id00011", new HashMap<Object, Object>());

        Map<String, Object> source = new HashMap<String, Object>();
        source.put("query", QueryMapFactory.filteredQuery(null, QueryMapFactory.termQuery("sample", "test")));
        source.put("size", 2);

        long response = dsa.count(source);
        assertEquals(3, response);
    }

    /**
     * delete処理が正常に終了する.
     */
    @Test
    public void delete処理が正常に終了する() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        dsa.create("id00001", new HashMap<Object, Object>());
        DcDeleteResponse response = dsa.delete("id00001", -1);
        assertNotNull(response);
        assertFalse(response.isNotFound());
    }

    /**
     * インデックスが存在しない場合にdelete処理でnullが返却される.
     */
    @Test
    public void インデックスが存在しない場合にdelete処理でnullが返却される() {
        EsIndex index = esClient.idxAdmin("index_for_test");
        assertNotNull(index);
        DataSourceAccessor dsa = new DataSourceAccessor(index, "TypeForTest", "RoutingIdTest");
        DcDeleteResponse response = dsa.delete("id00001", -1);
        assertNull(response);
    }

}
