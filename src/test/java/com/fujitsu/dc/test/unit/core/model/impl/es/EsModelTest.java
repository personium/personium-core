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
package com.fujitsu.dc.test.unit.core.model.impl.es;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.EsType;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcGetResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.test.categories.Unit;

/**
 * EsModelユニットテストクラス.
 */
@Category({ Unit.class })
public class EsModelTest {
    static Logger log = LoggerFactory.getLogger(EsModelTest.class);

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
        DcGetResponse getResp1 = typ.get("doc4");
        assertNull(getResp1);

        // いきなりドキュメントを検索しようとすると０件になるテスト
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> query2 = new HashMap<String, Object>();
        Map<String, Object> query3 = new HashMap<String, Object>();
        query3.put("key1", "value1");
        query2.put("term", query3);
        query.put("query", query2);
        DcSearchResponse searchResp = typ.search(query);
        log.debug("search performed.. ");
        assertNull(searchResp);

        // ドキュメント登録
        JSONObject json = new JSONObject();
        json.put("key1", "value1");
        json.put("key2", "value2");
        DcIndexResponse res = typ.create("doc5", json);

        // String id = typ.post(json);
        String id = res.getId();

        log.debug("doc [" + id + "] created.. ");
        log.debug("  version=" + res.version());

        // ドキュメント登録したものを
        // 一件取得
        DcGetResponse getResp = typ.get(id);
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

        DcDeleteResponse delResp = typ.delete(id);
        assertFalse(delResp.isNotFound());
        if (delResp.isNotFound()) {
            log.debug(" doc [" + id + "] not found.. ");
        } else {
            log.debug(" doc [" + id + "] deleted.. ");
        }

        idx.delete();
    }

}
