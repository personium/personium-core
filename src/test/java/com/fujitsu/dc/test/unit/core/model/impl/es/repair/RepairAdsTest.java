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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogException;
import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.es.EsType;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.EsModel;
import com.fujitsu.dc.core.model.impl.es.ads.Ads;
import com.fujitsu.dc.core.model.impl.es.ads.AdsConnectionException;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.ads.JdbcAds;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.model.impl.es.repair.AdsAccessor;
import com.fujitsu.dc.core.model.impl.es.repair.EsAccessor;
import com.fujitsu.dc.core.model.impl.es.repair.RepairAds;
import com.fujitsu.dc.core.model.impl.es.repair.RepairAdsException;
import com.fujitsu.dc.test.categories.Unit;

/**
 * RepairAdsTest ユニットテストクラス.
 */
@Category({Unit.class })
public class RepairAdsTest {

    private static final String ROUTING_ID = "routingId";

    static Logger log = LoggerFactory.getLogger(RepairAdsTest.class);

    private String idxName = DcCoreConfig.getEsUnitPrefix() + "_anon";
    private String id = "repair_" + DcUUID.randomUUID();
    private String[] idList = {id };
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
        DcIndexResponse res1 = type.create(id, json1);
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
        type.delete(id);
    }

    /**
     * Journalログのディレクトリが不正の場合_DcRepairAdsExceptionが発生すること.
     * @throws RepairAdsException DcRepairAdsException
     */
    @Ignore
    @Test
    public void Journalログのディレクトリが不正の場合_DcRepairAdsExceptionが発生すること() throws RepairAdsException {
        // RepairAds repair = RepairAds.getInstance();
        // repair.readProperties();
        // repair.correctAdsWriteFailureLogFiles();
    }

    /**
     * Journalログのローテートファイルが存在しない場合_DcRepairAdsExceptionが発生すること.
     */
    @Ignore
    @Test
    public void Journalログのローテートファイルが存在しない場合_DcRepairAdsExceptionが発生すること() {
    }

    /**
     * Elasticsearchの検索結果が2件以上の場合_AdsExceptionが発生すること.
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws AdsException AdsException
     */
    @Ignore
    // ESへ同じUUIDを登録することが不可能のため。パワーモックを使用すると行けるかも。
    @Test
    public void Elasticsearchの検索結果が2件以上の場合_AdsExceptionが発生すること()
            throws RepairAdsException, AdsWriteFailureLogException, AdsException {
        try {
            AdsAccessor.initializedAds();
            RepairAds repair = RepairAds.getInstance();

            StringBuilder sbuf = new StringBuilder();
            sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
            sbuf.append("ComplexTypeProperty\t");
            sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
            sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
            sbuf.append(id + "\t");
            sbuf.append("CREATE\t");
            sbuf.append("1\t");
            sbuf.append("1408595358931");
            AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

            List<String> list = new ArrayList<String>();
            list.add("abc");
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            List<JSONObject> adsResponse = AdsAccessor.getIdListOnAds(logInfo);
            repair.repairToAds(logInfo, esResponse, adsResponse);
            fail();
        } catch (AdsException e) {
            System.out.println("OK");
        }
    }

    /**
     * ADSの検索結果が2件以上の場合_AdsExceptionが発生すること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws AdsException AdsException
     */
    @Ignore
    // ADSへ同じUUIDを登録することが不可能のため。パワーモックを使用すると行けるかも。
    @Test
    public void ADSの検索結果が2件以上の場合_AdsExceptionが発生すること() throws AdsWriteFailureLogException, AdsException {
        try {
            AdsAccessor.initializedAds();

            RepairAds repair = RepairAds.getInstance();

            // AdsWriteFailureLogInfoを生成
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
            sbuf.append("ComplexTypeProperty\t");
            sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
            sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
            sbuf.append(id + "\t");
            sbuf.append("CREATE\t");
            sbuf.append("1\t");
            sbuf.append("1408595358931");
            AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

            // 前準備で登録したデータをESから取得する
            List<String> list = new ArrayList<String>();
            list.add(id);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSへ同じデータを2件登録する
            AdsAccessor.createAds(idxName, "Cell", esResponse);
            // AdsAccessor.createAds(idxName, "Cell", esResponse);

            // ADSから対象データを検索する(2件ヒット)
            List<JSONObject> adsResponse = AdsAccessor.getIdListOnAds(logInfo);

            // DcRepairAdsExceptionが発生すること
            repair.repairToAds(logInfo, esResponse, adsResponse);
            fail();
        } catch (RepairAdsException e) {
            System.out.println("OK");
        } finally {
            // ADSのデータを削除
            ads.deleteCell(idxName, id);
            // ads.deleteCell(idxName, id);
        }
    }

    /**
     * リペア対象のデータがESに存在し、ADSに存在しない場合にADSに登録処理がされること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsException AdsException
     */
    @Test
    public void リペア対象のデータがESに存在_ADSに存在しない場合にADSに登録処理がされること()
            throws AdsWriteFailureLogException, RepairAdsException, AdsException {
        try {
            AdsAccessor.initializedAds();

            RepairAds repair = RepairAds.getInstance();

            // AdsWriteFailureLogInfoを生成
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
            sbuf.append("ComplexTypeProperty\t");
            sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
            sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
            sbuf.append(id + "\t");
            sbuf.append("CREATE\t");
            sbuf.append("1\t");
            sbuf.append("1408595358931");
            AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

            // 前準備で登録したデータをESから取得する
            List<String> list = new ArrayList<String>();
            list.add(id);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSから対象データを検索する
            List<JSONObject> adsResponseBefore = AdsAccessor.getIdListOnAds(logInfo);

            // ADSにデータが登録されること
            repair.repairToAds(logInfo, esResponse, adsResponseBefore);

            // ADSから対象データを検索する
            List<JSONObject> adsResponseAfter = AdsAccessor.getIdListOnAds(logInfo);

            // ADSのデータが1件であること
            assertEquals(1, adsResponseAfter.size());
        } finally {
            ads.deleteEntity(idxName, id);
        }
    }

    /**
     * リペア対象のデータがESとADSに存在し、ジャーナルログのデータバージョンとESのデータバージョンが同じ場合_更新処理がされること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsException AdsException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void リペア対象のデータがESとADSに存在かつジャーナルログのデータバージョンとESのデータバージョンが同じ場合_更新処理がされること()
            throws AdsWriteFailureLogException, RepairAdsException, AdsException {
        try {
            AdsAccessor.initializedAds();

            RepairAds repair = RepairAds.getInstance();

            // ADSにデータを登録する
            List<String> list = new ArrayList<String>();
            list.add(id);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");
            AdsAccessor.createAds(idxName, "Entity", esResponse);

            // ESにデータ更新をする(バージョンが2)
            EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
            // ドキュメント登録
            JSONObject json1 = new JSONObject();
            json1.put("c", ROUTING_ID);
            json1.put("p", Long.parseLong("1406595596955"));
            json1.put("u", Long.parseLong("1406595596955"));
            type.update(id, json1);

            // AdsWriteFailureLogInfoを生成
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
            sbuf.append("ComplexTypeProperty\t");
            sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
            sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
            sbuf.append(id + "\t");
            sbuf.append("UPDATE\t");
            sbuf.append("2\t");
            sbuf.append("1408595358931");
            AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

            // 前準備で登録したデータをESから取得する
            esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSから対象データを検索する(ヒット)
            List<JSONObject> adsResponseBefore = AdsAccessor.getIdListOnAds(logInfo);

            // ADSのデータが更新されること
            repair.repairToAds(logInfo, esResponse, adsResponseBefore);

            // ADSから対象データを検索する
            List<JSONObject> adsResponseAfter = AdsAccessor.getIdListOnAds(logInfo);

            // チェック
            // 登録前と登録後のデータ取得件数が等しい
            assertEquals(adsResponseBefore.size(), adsResponseAfter.size());

            // データ更新日時が更新前と異なること
            JSONObject jsonBefore = (JSONObject) adsResponseBefore.get(0).get("source");
            JSONObject jsonAfter = (JSONObject) adsResponseAfter.get(0).get("source");
            if (jsonBefore.get("u").equals(jsonAfter.get("u"))) {
                fail();
            }
        } finally {
            ads.deleteEntity(idxName, id);
        }
    }

    /**
     * リペア対象のデータがADSに存在し、ESに存在しない場合にADSに削除処理がされること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsException AdsException
     */
    @Test
    public void リペア対象のデータがADSに存在しESに存在しない場合にADSに削除処理がされること()
            throws AdsWriteFailureLogException, RepairAdsException, AdsException {
        AdsAccessor.initializedAds();

        RepairAds repair = RepairAds.getInstance();

        // AdsWriteFailureLogInfoを生成
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
        sbuf.append("ComplexTypeProperty\t");
        sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
        sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
        sbuf.append(id + "\t");
        sbuf.append("CREATE\t");
        sbuf.append("1\t");
        sbuf.append("1408595358931");
        AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

        // 前準備で登録したデータをESから取得する
        List<String> list = new ArrayList<String>();
        list.add(id);
        DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

        // ADSにデータを登録
        EntitySetDocHandler oedh = new OEntityDocHandler(esResponse.getHits().getHits()[0]);
        ads.createEntity(idxName, oedh);

        // ESからデータを削除する
        EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
        type.delete(id);

        // ADSから対象データを検索する(ヒット)
        List<JSONObject> adsResponseBefore = AdsAccessor.getIdListOnAds(logInfo);
        // ESから対象データを検索する(0件)
        esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

        // ADSにデータが削除されること
        repair.repairToAds(logInfo, esResponse, adsResponseBefore);

        // ADSから対象データを検索する
        List<JSONObject> adsResponseAfter = AdsAccessor.getIdListOnAds(logInfo);
        // ADSのデータが0件であること
        assertEquals(0, adsResponseAfter.size());
    }

    /**
     * リペア対象のデータがADSとESに存在しない場合にリペア処理が無視されること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsException AdsException
     */
    @Test
    public void リペア対象のデータがADSとESに存在しない場合にリペア処理が無視されること()
            throws AdsWriteFailureLogException, RepairAdsException, AdsException {
        AdsAccessor.initializedAds();

        RepairAds repair = RepairAds.getInstance();

        // AdsWriteFailureLogInfoを生成
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
        sbuf.append("ComplexTypeProperty\t");
        sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
        sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
        sbuf.append("dummy_repair\t");
        sbuf.append("CREATE\t");
        sbuf.append("1\t");
        sbuf.append("1408595358931");
        AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

        // 前準備で登録したデータをESから取得する
        List<String> list = new ArrayList<String>();
        list.add("dummy_repair");

        // ADSから対象データを検索する(0件)
        List<JSONObject> adsResponseBefore = AdsAccessor.getIdListOnAds(logInfo);
        // ESから対象データを検索する(0件)
        DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

        // 対象データのリペア処理が無視されること
        repair.repairToAds(logInfo, esResponse, adsResponseBefore);

        // ADSから対象データを検索する
        List<JSONObject> adsResponseAfter = AdsAccessor.getIdListOnAds(logInfo);
        // ADSのデータが0件であること
        assertEquals(0, adsResponseAfter.size());
    }

    /**
     * リペア対象のデータがESとADSに存在し、ジャーナルログのデータバージョンとESのデータバージョンが異なる場合_処理が無視されること.
     * @throws AdsWriteFailureLogException AdsWriteFailureLogException
     * @throws RepairAdsException DcRepairAdsException
     * @throws AdsException AdsException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void リペア対象のデータがESとADSに存在してジャーナルログのデータバージョンとESのデータバージョンが異なる場合_処理が無視されること()
            throws AdsWriteFailureLogException, RepairAdsException, AdsException {
        try {
            AdsAccessor.initializedAds();

            RepairAds repair = RepairAds.getInstance();

            // ADSにデータを登録する
            List<String> list = new ArrayList<String>();
            list.add(id);
            DcSearchResponse esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");
            AdsAccessor.createAds(idxName, "Entity", esResponse);

            // ESにデータ更新をする(バージョンが2)
            EsType type = EsModel.type(idxName, "UserData", ROUTING_ID, 0, 0);
            // ドキュメント登録
            JSONObject json1 = new JSONObject();
            json1.put("c", ROUTING_ID);
            json1.put("p", Long.parseLong("1406595596955"));
            json1.put("u", Long.parseLong("1406595596955"));
            type.update(id, json1);

            // AdsWriteFailureLogInfoを生成
            StringBuilder sbuf = new StringBuilder();
            sbuf.append(DcCoreConfig.getEsUnitPrefix() + "_anon\t");
            sbuf.append("ComplexTypeProperty\t");
            sbuf.append("odata-gsX3t2q3Qz6jdIn30fFMaQ\t");
            sbuf.append("aCUuueHzTKCPchE0yxTZZA\t");
            sbuf.append(id + "\t");
            sbuf.append("UPDATE\t");
            sbuf.append("1\t");
            sbuf.append("1408595358931");
            AdsWriteFailureLogInfo logInfo = AdsWriteFailureLogInfo.parse(sbuf.toString());

            // 前準備で登録したデータをESから取得する
            esResponse = EsAccessor.search(idxName, ROUTING_ID, list, "UserData");

            // ADSから対象データを検索する(ヒット)
            List<JSONObject> adsResponseBefore = AdsAccessor.getIdListOnAds(logInfo);

            // ADSのデータが更新されること
            repair.repairToAds(logInfo, esResponse, adsResponseBefore);

            // ADSから対象データを検索する
            List<JSONObject> adsResponseAfter = AdsAccessor.getIdListOnAds(logInfo);

            // チェック
            // 登録前と登録後のデータ取得件数が等しい
            assertEquals(adsResponseBefore.size(), adsResponseAfter.size());

            // データ更新日時が更新前と異なること
            JSONObject jsonBefore = (JSONObject) adsResponseBefore.get(0).get("source");
            JSONObject jsonAfter = (JSONObject) adsResponseAfter.get(0).get("source");
            assertEquals(jsonBefore.get("u"), jsonAfter.get("u"));
        } finally {
            ads.deleteEntity(idxName, id);
        }
    }

}
