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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.es.EsType;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.core.model.impl.es.ads.Ads;
import com.fujitsu.dc.core.model.impl.es.ads.AdsConnectionException;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.ads.JdbcAds;
import com.fujitsu.dc.core.model.impl.es.repair.AdsAccessor;
import com.fujitsu.dc.core.model.impl.es.repair.EsAccessor;
import com.fujitsu.dc.test.categories.Unit;

/**
 * EsAcessorユニットテストクラス.
 */
@Category({Unit.class })
public class AdsAcessorTest {
    private static final String ROUTING_ID = "routingId";

    static Logger log = LoggerFactory.getLogger(AdsAcessorTest.class);

    private String idxName = DcCoreConfig.getEsUnitPrefix() + "_anon";
    private String[] idList = {"documentId1" };
    private Ads ads;

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     * @throws AdsConnectionException AdsConnectionException
     */
    @Before
    @SuppressWarnings("unchecked")
    public void before() throws InterruptedException, AdsConnectionException {
        ads = new JdbcAds();

        // Typeの定義
        // （Type 名に # は使えないっぽい。）
        EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
        // ドキュメント登録
        JSONObject json1 = new JSONObject();
        json1.put("c", ROUTING_ID);
        json1.put("p", Long.parseLong("1406595596944"));
        json1.put("u", Long.parseLong("1406595596944"));
        DcIndexResponse res1 = type.create("documentId1", json1);
        assertEquals(idList[0], res1.getId());
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @After
    public void after() throws InterruptedException {
        // 作成したインデックスを消す
        EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
        type.delete("documentId1");
    }

    /**
     * ADSにCellを登録できること.
     * @throws AdsException AdsException
     */
    @Test
    public void ADSにCellを登録できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへリペアを実施する
            AdsAccessor.createAds(idxName, "Cell", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchCellList(idxName, list);
            assertEquals(1, adsResponce.size());
        } finally {
            AdsAccessor.deleteAds(idxName, "Cell", "documentId1");
        }
    }

    /**
     * ADSにLinkを登録できること.
     * @throws AdsException AdsException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ADSにLinkを登録できること() throws AdsException {
        try {
            // Typeの定義
            // （Type 名に # は使えないっぽい。）
            EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
            // ドキュメント登録
            JSONObject json1 = new JSONObject();
            json1.put("c", ROUTING_ID);
            json1.put("t1", "UserData");
            json1.put("t2", "UserData");
            json1.put("k1", "_u8jGStVRNmS2dypzsQAHC");
            json1.put("k2", "_u8jGStVRNmS2dypzsQAHD");
            json1.put("p", Long.parseLong("1406595596944"));
            json1.put("u", Long.parseLong("1406595596944"));
            DcIndexResponse res1 = type.create("documentId2", json1);
            assertEquals("documentId2", res1.getId());

            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = new ArrayList<String>();
            list.add("documentId2");
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへリペアを実施する
            AdsAccessor.createAds(idxName, "link", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchLinkList(idxName, list);
            assertEquals(1, adsResponce.size());
        } finally {
            AdsAccessor.deleteAds(idxName, "link", "documentId2");
        }
    }

    /**
     * ADSにDavを登録できること.
     * @throws AdsException AdsException
     */
    @Test
    public void ADSにDavを登録できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへリペアを実施する
            AdsAccessor.createAds(idxName, "dav", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchDavNodeList(idxName, list);
            assertEquals(1, adsResponce.size());
        } finally {
            AdsAccessor.deleteAds(idxName, "dav", "documentId1");
        }
    }

    /**
     * ADSにEntityを登録できること.
     * @throws AdsException AdsException
     */
    @Test
    public void ADSにEntityを登録できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへリペアを実施する
            AdsAccessor.createAds(idxName, "Box", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchEntityList(idxName, list);
            assertEquals(1, adsResponce.size());
        } finally {
            AdsAccessor.deleteAds(idxName, "Box", "documentId1");
        }
    }

    /**
     * ADSにCellを更新できること.
     * @throws AdsException AdsException
     */
    @Test
    public void ADSにCellを更新できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");
            AdsAccessor.createAds(idxName, "Cell", esResponse);

            Map<String, Object> map = esResponse.getHits().getHits()[0].getSource();
            map.put("b", "box");
            // ADSへリペアを実施する(Update)
            AdsAccessor.updateAds(idxName, "Cell", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchCellList(idxName, list);
            assertEquals(1, adsResponce.size());
            assertEquals("box", ((JSONObject) adsResponce.get(0).get("source")).get("b"));
        } finally {
            AdsAccessor.deleteAds(idxName, "Cell", "documentId1");
        }
    }

    /**
     * ADSにLinkを更新できること.
     * @throws AdsException AdsException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void ADSにLinkを更新できること() throws AdsException {
        try {
            // Typeの定義
            // （Type 名に # は使えないっぽい。）
            EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
            // ドキュメント登録
            JSONObject json1 = new JSONObject();
            json1.put("c", ROUTING_ID);
            json1.put("t1", "UserData");
            json1.put("t2", "UserData");
            json1.put("k1", "_u8jGStVRNmS2dypzsQAHC");
            json1.put("k2", "_u8jGStVRNmS2dypzsQAHD");
            json1.put("p", Long.parseLong("1406595596944"));
            json1.put("u", Long.parseLong("1406595596944"));
            DcIndexResponse res1 = type.create("documentId2", json1);
            assertEquals("documentId2", res1.getId());

            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = new ArrayList<String>();
            list.add("documentId2");
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへリペアを実施する
            AdsAccessor.createAds(idxName, "link", esResponse);

            Map<String, Object> map = esResponse.getHits().getHits()[0].getSource();
            map.put("t1", "Account");
            map.put("t2", "Role");
            AdsAccessor.updateAds(idxName, "link", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchLinkList(idxName, list);
            assertEquals(1, adsResponce.size());
            assertEquals("Account", ((JSONObject) adsResponce.get(0).get("source")).get("t1"));
            assertEquals("Role", ((JSONObject) adsResponce.get(0).get("source")).get("t2"));
        } finally {
            AdsAccessor.deleteAds(idxName, "link", "documentId2");
        }
    }

    /**
     * ADSにDavを更新できること.
     * @throws AdsException AdsException
     */
    @Test
    @Ignore
    // DｃSearchResponseを変更しても、update時にはこのデータを使わないため、テストが出来ない。
    public void ADSにDavを更新できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");
            AdsAccessor.createAds(idxName, "dav", esResponse);

            Map<String, Object> map = esResponse.getHits().getHits()[0].getSource();
            map.put("b", "box");
            // ADSへリペアを実施する
            AdsAccessor.updateAds(idxName, "dav", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchDavNodeList(idxName, list);
            assertEquals(1, adsResponce.size());
            assertEquals("box", ((JSONObject) adsResponce.get(0).get("source")).get("b"));
        } finally {
            AdsAccessor.deleteAds(idxName, "dav", "documentId1");
        }
    }

    /**
     * ADSにEntityを更新できること.
     * @throws AdsException AdsException
     */
    @Test
    public void ADSにEntityを更新できること() throws AdsException {
        try {
            AdsAccessor.initializedAds();
            // Elasticsearchへリペア対象のデータを取得
            List<String> list = Arrays.asList(idList);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");
            AdsAccessor.createAds(idxName, "Box", esResponse);

            Map<String, Object> map = esResponse.getHits().getHits()[0].getSource();
            map.put("b", "box");
            // ADSへリペアを実施する
            AdsAccessor.updateAds(idxName, "Box", esResponse);

            // MySQLにデータが登録されていることを確認
            List<JSONObject> adsResponce = ads.searchEntityList(idxName, list);
            assertEquals(1, adsResponce.size());
            assertEquals("box", ((JSONObject) adsResponce.get(0).get("source")).get("b"));
        } finally {
            AdsAccessor.deleteAds(idxName, "Box", "documentId1");
        }
    }
}
