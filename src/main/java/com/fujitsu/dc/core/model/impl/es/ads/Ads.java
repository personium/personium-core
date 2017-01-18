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
package com.fujitsu.dc.core.model.impl.es.ads;

import java.util.List;

import org.json.simple.JSONObject;

import com.fujitsu.dc.core.model.impl.es.DavNode;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;
import com.fujitsu.dc.core.model.impl.es.doc.LinkDocHandler;

/**
 * ES のADS ( Authentic Data Store ) のインターフェース.
 * ひとつのIndexに対して、一つのADSが対応する。
 */
public interface Ads {
    /**
     * Entity Document生成に伴い、Adsの対応レコード生成を行う.
     * @param index index
     * @param oedh OEntityDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void createEntity(String index, EntitySetDocHandler oedh) throws AdsException;

    /**
     * Entity Document更新に伴い、Adsの対応レコード更新を行う.
     * @param index index
     * @param oedh OEntityDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void updateEntity(String index, EntitySetDocHandler oedh) throws AdsException;

    /**
     * Entity Document削除に伴い、Adsの対応レコード削除を行う.
     * @param index index
     * @param id id
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void deleteEntity(String index, String id) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するEntity件数を取得する。
     * @param index index
     * @return 件数
     * @throws AdsException 取得失敗時
     */
    long countEntity(String index) throws AdsException;

    /**
     * Entity Document一括生成に伴い、Adsの対応レコード一括生成を行う.
     * @param index index
     * @param bulkRequestList 一括生成データ
     * @throws AdsException 処理失敗時発生
     */
    void bulkEntity(String index, List<EntitySetDocHandler> bulkRequestList) throws AdsException;

    /**
     * Entity Document一括更新に伴い、Adsの対応レコード一括更新を行う.
     * @param index index
     * @param bulkRequestList 一括更新データ
     * @throws AdsException 処理失敗時発生
     */
    void bulkUpdateEntity(String index, List<EntitySetDocHandler> bulkRequestList) throws AdsException;

    /**
     * Dav Document一括更新に伴い、Adsの対応レコード一括更新を行う.
     * @param index index
     * @param bulkRequestList 一括更新データ
     * @throws AdsException 処理失敗時発生
     */
    void bulkUpdateDav(String index, List<DavNode> bulkRequestList) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するEntityのリストを取得する。
     * @param index index
     * @param offset 取得開始レコード(最初から取るときは0)
     * @param size 取得件数
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> getEntityList(String index, long offset, long size) throws AdsException;

    /**
     * 引数で渡されたuuidをもとに、ADS内の特定indexに対応するデータを検索する。
     * @param index DataBundle名
     * @param idList 検索対象のuuidリスト
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> searchEntityList(String index, List<String> idList) throws AdsException;

    /**
     * Cell Document生成に伴い、Adsの対応レコード生成を行う.
     * @param index index
     * @param docHandler CellDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void createCell(String index, EntitySetDocHandler docHandler) throws AdsException;

    /**
     * Cell Document更新に伴い、Adsの対応レコード更新を行う.
     * @param index index
     * @param docHandler CellDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void updateCell(String index, EntitySetDocHandler docHandler) throws AdsException;

    /**
     * Cell Document削除に伴い、Adsの対応レコード削除を行う.
     * @param index index
     * @param id id
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void deleteCell(String index, String id) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するCell件数を取得する。
     * @param index index
     * @return 件数
     * @throws AdsException 取得失敗時
     */
    long countCell(String index) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するCellのリストを取得する。
     * @param index index
     * @param offset 取得開始レコード(最初から取るときは0)
     * @param size 取得件数
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> getCellList(String index, long offset, long size) throws AdsException;

    /**
     * 引数で渡されたuuidをもとに、ADS内の特定indexに対応するデータを検索する。
     * @param index DataBundle名
     * @param idList 検索対象のuuidリスト
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> searchCellList(String index, List<String> idList) throws AdsException;

    /**
     * Link Document生成に伴い、Adsの対応レコード生成を行う.
     * @param index index
     * @param ldh LinkDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void createLink(String index, LinkDocHandler ldh) throws AdsException;

    /**
     * Link Document生成に伴い、Adsの一括登録用のレコード生成を行う.
     * @param index index
     * @param bulkRequestList 一括登録するLinkDocHandlerのリスト
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void bulkCreateLink(String index, List<LinkDocHandler> bulkRequestList) throws AdsException;

    /**
     * Link Document更新に伴い、Adsの対応レコード更新を行う.
     * @param index index
     * @param ldh LinkDocHandler
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void updateLink(String index, LinkDocHandler ldh) throws AdsException;

    /**
     * Link Document削除に伴い、Adsの対応レコード削除を行う.
     * @param index index
     * @param id id
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void deleteLink(String index, String id) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するLink件数を取得する。
     * @param index index
     * @return 件数
     * @throws AdsException 取得失敗時
     */
    long countLink(String index) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するLinkのリストを取得する。
     * @param index index
     * @param offset 取得開始レコード(最初から取るときは0)
     * @param size 取得件数
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> getLinkList(String index, long offset, long size) throws AdsException;

    /**
     * 引数で渡されたuuidをもとに、ADS内の特定indexに対応するデータを検索する。
     * @param index DataBundle名
     * @param idList 検索対象のuuidリスト
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> searchLinkList(String index, List<String> idList) throws AdsException;

    /**
     * DavNode生成に伴い、Adsの対応レコード生成を行う.
     * @param index index
     * @param davNode DavNode
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void createDavNode(String index, DavNode davNode) throws AdsException;

    /**
     * DavNode更新に伴い、Adsの対応レコード更新を行う.
     * @param index index
     * @param davNode DavNode
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void updateDavNode(String index, DavNode davNode) throws AdsException;

    /**
     * DavNode削除に伴い、Adsの対応レコード削除を行う.
     * @param index index
     * @param id id
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void deleteDavNode(String index, String id) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するDavNode件数を取得する。
     * @param index index
     * @return 件数
     * @throws AdsException 取得失敗時
     */
    long countDavNode(String index) throws AdsException;

    /**
     * リカバリ用に、ADS内の特定indexに対応するDavNodeのリストを取得する。
     * @param index index
     * @param offset 取得開始レコード(最初から取るときは0)
     * @param size 取得件数
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> getDavNodeList(String index, long offset, long size) throws AdsException;

    /**
     * 引数で渡されたuuidをもとに、ADS内の特定indexに対応するデータを検索する。
     * @param index DataBundle名
     * @param idList 検索対象のuuidリスト
     * @return JSONObjectのリスト.
     * @throws AdsException 取得失敗時
     */
    List<JSONObject> searchDavNodeList(String index, List<String> idList) throws AdsException;

    /**
     * 指定されたIDのCellのリソースをENTITYテーブルから削除する.
     * @param index 対象インデックス
     * @param cellId 対象セルID
     * @throws AdsException 削除失敗時
     */
    void deleteCellResourceFromEntity(String index, String cellId) throws AdsException;

    /**
     * 指定されたIDのCellのリソースをDAV_NODEテーブルから削除する.
     * @param index 対象インデックス
     * @param cellId 対象セルID
     * @throws AdsException 削除失敗時
     */
    void deleteCellResourceFromDavNode(String index, String cellId) throws AdsException;

    /**
     * 指定されたIDのCellのリソースをLINKテーブルから削除する.
     * @param index 対象インデックス
     * @param cellId 対象セルID
     * @throws AdsException 削除失敗時
     */
    void deleteCellResourceFromLink(String index, String cellId) throws AdsException;

    /**
     * Es Index生成に伴い、Adsの対応空間を生成する.
     * @param index index
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void createIndex(String index) throws AdsException;

    /**
     * PCS管理用DBを作成する.
     * @throws AdsException 管理用DBの作成に失敗
     */
    void createManagementDatabase() throws AdsException;

    /**
     * Es Index削除に伴い、Adsの対応空間を削除する.
     * @param index index
     * @throws AdsException 同期失敗時発生。（処理結果が１件でない場合にも発生する）
     */
    void deleteIndex(String index) throws AdsException;

    /**
     * セル削除用管理テーブルに削除するセル情報を登録する.
     * @param dbName セル削除対象のDB名
     * @param cellId セル削除対象のセルID
     * @throws AdsException 登録失敗時
     */
    void insertCellDeleteRecord(String dbName, String cellId) throws AdsException;

}
