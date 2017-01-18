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
package com.fujitsu.dc.core.model.impl.es.repair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.ads.AdsWriteFailureLogInfo;
import com.fujitsu.dc.common.es.response.DcSearchHit;
import com.fujitsu.dc.common.es.response.DcSearchResponse;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Cell;
import com.fujitsu.dc.core.model.impl.es.DavNode;
import com.fujitsu.dc.core.model.impl.es.ads.Ads;
import com.fujitsu.dc.core.model.impl.es.ads.AdsException;
import com.fujitsu.dc.core.model.impl.es.ads.JdbcAds;
import com.fujitsu.dc.core.model.impl.es.doc.CellDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.OEntityDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.UserDataDocHandler;
import com.fujitsu.dc.core.model.impl.es.odata.UserDataODataProducer;

/**
 * データストア層(ADS)への操作処理を実装したクラス.
 */
public class AdsAccessor {

    /** ログ用オブジェクト. */
    static Logger logger = LoggerFactory.getLogger(AdsAccessor.class);

    private static Ads ads;

    /**
     * デフォルトコンストラクタ（使用不可）.
     */
    private AdsAccessor() {
    }

    /**
     * ADSを初期化する.
     * この際、接続チェックも行う。
     * @return 初期化に成功した場合は trueを、それ以外は falseを返す。
     */
    public static boolean initializedAds() {
        try {
            ads = new JdbcAds();
            ((JdbcAds) ads).checkConnection();
        } catch (AdsException e) {
            logger.error("Ads connection failed.", e);
            return false;
        }
        return true;
    }

    /**
     * ADSからリペア対象のデータが存在するか検索をする.
     * @param logInfo ログから読み込んだADS書き込み失敗情報
     * @return リペア対象のデータの検索結果
     * @throws AdsException AdsException
     */
    public static List<JSONObject> getIdListOnAds(AdsWriteFailureLogInfo logInfo) throws AdsException {
        List<String> idList = new ArrayList<String>();
        idList.add(logInfo.getUuid());

        String indexName = logInfo.getIndexName();
        String type = logInfo.getType();
        if (Cell.EDM_TYPE_NAME.equals(type)) {
            return ads.searchCellList(indexName, idList);
        } else if ("link".equals(type)) {
            return ads.searchLinkList(indexName, idList);
        } else if ("dav".equals(type)) {
            return ads.searchDavNodeList(indexName, idList);
        } else {
            return ads.searchEntityList(indexName, idList);
        }
    }

    /**
     * リペア対象のデータをADSに登録する.
     * @param indexName リペア対象インデックス名
     * @param type リペア対象のESのタイプ名
     * @param esResponse ESから取得したリペア対象のデータ情報
     * @throws AdsException AdsException
     */
    public static void createAds(String indexName, String type,
            DcSearchResponse esResponse) throws AdsException {
        // MySQLへデータ登録処理
        DcSearchHit[] dcSearchHit = esResponse.getHits().getHits();
        try {
            if (Cell.EDM_TYPE_NAME.equals(type)) {
                // CELLテーブルに登録
                EntitySetDocHandler oedh = new CellDocHandler(dcSearchHit[0]);
                ads.createCell(indexName, oedh);
            } else if ("link".equals(type)) {
                // LINKテーブルに登録
                LinkDocHandler ldh = new LinkDocHandler(dcSearchHit[0]);
                ads.createLink(indexName, ldh);
            } else if ("dav".equals(type)) {
                // DAV_NODEテーブルに登録
                DavNode davNode = DavNode.createFromJsonString(dcSearchHit[0].getId(),
                        dcSearchHit[0].sourceAsString());
                ads.createDavNode(indexName, davNode);
            } else {
                // ENTITYテーブルに登録
                EntitySetDocHandler oedh;
                if (type.equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
                    oedh = new UserDataDocHandler(dcSearchHit[0]);
                } else {
                    oedh = new OEntityDocHandler(dcSearchHit[0]);
                }

                if (oedh.getDynamicFields() == null) {
                    // リペア対象のデータのDynamicFieldsが空の場合にAdsのデータがnullになってしまうので空オブジェクトを挿入
                    // DocHandlerでの修正も検討したが、既存のcore側の処理に影響を与えないようここで修正
                    oedh.setDynamicFields(new HashMap<String, Object>());
                }

                ads.createEntity(indexName, oedh);
            }
        } catch (DcCoreException e) {
            throw new AdsException(e);
        }
    }

    /**
     * リペア対象のデータをADSに更新する.
     * @param indexName リペア対象インデックス名
     * @param type リペア対象タイプ名
     * @param esResponse ESから取得したリペア対象のデータ情報
     * @throws AdsException AdsException
     */
    public static void updateAds(String indexName, String type,
            DcSearchResponse esResponse) throws AdsException {
        try {
            // MySQLへデータ更新処理
            DcSearchHit[] dcSearchHit = esResponse.getHits().getHits();
            if (Cell.EDM_TYPE_NAME.equals(type)) {
                // CELLテーブルに更新
                EntitySetDocHandler oedh = new CellDocHandler(dcSearchHit[0]);
                ads.updateCell(indexName, oedh);
            } else if ("link".equals(type)) {
                // LINKテーブルに更新
                LinkDocHandler ldh = new LinkDocHandler(dcSearchHit[0]);
                ads.updateLink(indexName, ldh);
            } else if ("dav".equals(type)) {
                // DAV_NODEテーブルに更新
                DavNode davNode = DavNode.createFromJsonString(dcSearchHit[0].getId(),
                        dcSearchHit[0].sourceAsString());
                ads.updateDavNode(indexName, davNode);
            } else {
                // ENTITYテーブルに更新
                EntitySetDocHandler oedh;
                if (type.equals(UserDataODataProducer.USER_ODATA_NAMESPACE)) {
                    oedh = new UserDataDocHandler(dcSearchHit[0]);
                } else {
                    oedh = new OEntityDocHandler(dcSearchHit[0]);
                }

                if (oedh.getDynamicFields() == null) {
                    // リペア対象のデータのDynamicFieldsが空の場合にAdsのデータがnullになってしまうので空オブジェクトを挿入
                    // DocHandlerでの修正も検討したが、既存のcore側の処理に影響を与えないようここで修正
                    oedh.setDynamicFields(new HashMap<String, Object>());
                }

                ads.updateEntity(indexName, oedh);
            }
        } catch (DcCoreException e) {
            throw new AdsException(e);
        }

    }

    /**
     * リペア対象のデータをADSに削除する.
     * @param indexName リペア対象インデックス名
     * @param type リペア対象タイプ名
     * @param idList リペア対象のuuid
     * @throws AdsException AdsException
     */
    public static void deleteAds(String indexName, String type,
            String idList) throws AdsException {
        // MySQLへデータ削除処理
        if (Cell.EDM_TYPE_NAME.equals(type)) {
            // CELLテーブルに削除
            ads.deleteCell(indexName, idList);
        } else if ("link".equals(type)) {
            // LINKテーブルに削除
            ads.deleteLink(indexName, idList);
        } else if ("dav".equals(type)) {
            // DAV_NODEテーブルに削除
            ads.deleteDavNode(indexName, idList);
        } else {
            // ENTITYテーブルに削除
            ads.deleteEntity(indexName, idList);
        }
    }
}
