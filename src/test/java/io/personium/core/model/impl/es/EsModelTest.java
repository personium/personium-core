/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.model.impl.es;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.EsIndex;
import io.personium.common.es.EsType;
import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.accessor.ODataLinkAccessor;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;

/**
 * EsModelの単体テストケース.
 */
@Ignore
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class })
public class EsModelTest {
    static Logger log = LoggerFactory.getLogger(EsModelTest.class);

    /**
     * テストケース共通の初期化処理. テスト用のElasticsearchのNodeを初期化する
     * @throws Exception 異常が発生した場合の例外
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * テストケース共通のクリーンアップ処理. テスト用のElasticsearchのNodeをクローズする
     * @throws Exception 異常が発生した場合の例外
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
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
    }

    /**
     * idxAdminメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void idxAdminメソッドでオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxAdmin();
        assertNotNull(index);
        assertEquals(EsIndex.CATEGORY_AD, index.getCategory());
    }

    /**
     * idxUserメソッドでnullを指定した場合にオブジェクトが正常に取得できる.
     */
    @Test
    public void idxUserメソッドでnullを指定した場合にオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxUser(null);
        assertNotNull(index);
        assertTrue(index.getName().endsWith("anon"));
        assertEquals(EsIndex.CATEGORY_USR, index.getCategory());
    }

    /**
     * idxUserメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void idxUserメソッドでオブジェクトが正常に取得できる() {
        EsIndex index = EsModel.idxUser("UriStringForTest");
        assertNotNull(index);
        assertTrue(index.getName().endsWith("UriStringForTest"));
        assertEquals(EsIndex.CATEGORY_USR, index.getCategory());
    }

    /**
     * cellメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void cellメソッドでオブジェクトが正常に取得できる() {
        EntitySetAccessor cell = EsModel.cell();
        assertNotNull(cell);
        assertEquals("Cell", cell.getType());
    }

    /**
     * boxメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void boxメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        EntitySetAccessor box = EsModel.box(null);
        assertNotNull(box);
    }

    /**
     * unitCtlメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void unitCtlメソッドでオブジェクトが正常に取得できる() {
        EntitySetAccessor unitCtl = EsModel.unitCtl("TypeStringForTest", "TestCellID");
        assertNotNull(unitCtl);
        assertEquals("TypeStringForTest", unitCtl.getType());
    }

    /**
     * cellCtlメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void cellCtlメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        EntitySetAccessor cellCtl = EsModel.cellCtl(null, "TypeStringForTest");
        assertNotNull(cellCtl);
    }

    /**
     * unitCtlLinkメソッドでオブジェクトが正常に取得できる.
     */
    @Test
    public void unitCtlLinkメソッドでオブジェクトが正常に取得できる() {
        ODataLinkAccessor unitCtlLink = EsModel.unitCtlLink("TestCellID");
        assertNotNull(unitCtlLink);
        assertEquals("link", unitCtlLink.getType());
    }

    /**
     * cellCtlLinkメソッドでnullを指定した場合にNullPointerExceptionをスローする.
     */
    @Test(expected = NullPointerException.class)
    public void cellCtlLinkメソッドでnullを指定した場合にNullPointerExceptionをスローする() {
        ODataLinkAccessor cellCtlLink = EsModel.cellCtlLink(null);
        assertNotNull(cellCtlLink);
    }


    /**
     * EsModelの基礎的なテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    @Ignore
    public void EsModelの基礎的なテスト() {
        String idxName = "test" + UUID.randomUUID().toString();
        EsIndex idx = EsModel.idxUser(idxName);

        // Typeの定義
        // （Type 名に # は使えないっぽい。）
        EsType typ = EsModel.type(idx.getName(), "tType2", null, 0, 0);

        // いきなりドキュメントを取得しようとするとFALSEになるテスト
        PersoniumGetResponse getResp1 = typ.get("doc4");
        assertNull(getResp1);

        // いきなりドキュメントを検索しようとすると０件になるテスト
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> query2 = new HashMap<String, Object>();
        Map<String, Object> query3 = new HashMap<String, Object>();
        query3.put("key1", "value1");
        query2.put("term", query3);
        query.put("query", query2);
        PersoniumSearchResponse searchResp = typ.search(query);
        log.debug("search performed.. ");
        assertNull(searchResp);

        // ドキュメント登録
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", "value2");
        PersoniumIndexResponse res = typ.create("doc5", json);

        // String id = typ.post(json);
        String id = res.getId();

        log.debug("doc [" + id + "] created.. ");
        log.debug("  version=" + res.version());

        // ドキュメント登録したものを
        // 一件取得
        PersoniumGetResponse getResp = typ.get(id);
        log.debug("doc [" + id + "] retrieved.. ");
        log.debug(getResp.sourceAsString());
        assertTrue(getResp.exists());

        // 検索
        // Map<String, Object> query = new HashMap<String, Object>();
        // Map<String, Object> query2 = new HashMap<String, Object>();
        // Map<String, Object> query3 = new HashMap<String, Object>();
        // query3.put("key1", "value1");
        // query2.put("term", query3);
        // query.put("query", query2);
        searchResp = typ.search(query);
        log.debug("search performed.. ");
        log.debug("  " + searchResp.getHits().getCount() + " hits");
        log.debug(searchResp.toString());
        assertEquals(1, searchResp.getHits().getCount());

        // 消す

        PersoniumDeleteResponse delResp = typ.delete(id);
        assertFalse(delResp.isNotFound());
        if (delResp.isNotFound()) {
            log.debug(" doc [" + id + "] not found.. ");
        } else {
            log.debug(" doc [" + id + "] deleted.. ");
        }

        idx.delete();
    }


}
