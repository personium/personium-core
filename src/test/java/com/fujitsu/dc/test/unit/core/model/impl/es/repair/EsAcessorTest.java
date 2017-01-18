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
package com.fujitsu.dc.test.unit.core.model.impl.es.repair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.EsType;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchHit;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.core.model.impl.es.repair.EsAccessor;
import com.fujitsu.dc.test.categories.Unit;

import java.util.Arrays;

/**
 * EsAcessorユニットテストクラス.
 */
@Category({Unit.class })
public class EsAcessorTest {
    private static final String ROUTING_ID = "routingId";

    static Logger log = LoggerFactory.getLogger(EsAcessorTest.class);

    private String idxName = "test" + DcUUID.randomUUID();
    private String[] idList = {"documentId1", "documentId2" };

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    @SuppressWarnings("unchecked")
    public void before() throws InterruptedException {
        // データ検索確認用のテストデータを作成（EsModelのテストをベースにデータ作成）
        EsIndex idx = EsModel.idxUser(idxName);
        // インデックスの作成
        idx.create();
        // Typeの定義
        // （Type 名に # は使えないっぽい。）
        EsType type = EsModel.type(idx.getName(), "tType2", ROUTING_ID, 0, 0);
        // ドキュメント登録
        JSONObject json1 = new JSONObject();
        json1.put("key1-1", "value1");
        json1.put("key1-2", "value2");
        DcIndexResponse res1 = type.create("documentId1", json1);
        assertEquals(idList[0], res1.getId());
        JSONObject json2 = new JSONObject();
        json2.put("key2-1", "value1");
        json2.put("key2-2", "value2");
        DcIndexResponse res2 = type.create("documentId2", json2);
        assertEquals(idList[1], res2.getId());
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @After
    public void after() throws InterruptedException {
        // 作成したインデックスを消す
        EsIndex idx = EsModel.idxUser(idxName);
        idx.delete();
    }

    /**
     * EsAcessorの基礎的なテスト.
     */
    @Test
    public void EsModelの基礎的なテスト() {

        List<String> list = Arrays.asList(idList);
        DcSearchResponse response = EsAccessor.search(idxName, ROUTING_ID, list, "tType2");
        assertEquals(2, response.getHits().getHits().length);
        for (DcSearchHit hit : response.getHits().getHits()) {
            assertTrue(list.contains(hit.getId()));
        }
    }

}
