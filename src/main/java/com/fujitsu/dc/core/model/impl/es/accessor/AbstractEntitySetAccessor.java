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

import com.fujitsu.dc.common.es.EsIndex;
import com.fujitsu.dc.common.es.response.DcDeleteResponse;
import com.fujitsu.dc.common.es.response.DcIndexResponse;
import com.fujitsu.dc.common.es.util.DcUUID;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * ODataEntityのアクセス処理の抽象クラス.
 */
public abstract class AbstractEntitySetAccessor extends DataSourceAccessor implements EntitySetAccessor {

    /**
     * データ登録時に発生するSQLExceptionのSQLState.
     */
    protected static final String MYSQL_BAD_TABLE_ERROR = "42S02";

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingId
     */
    protected AbstractEntitySetAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * マスターデータを登録する.
     * @param docHandler 登録データ
     */
    protected abstract void createAds(EntitySetDocHandler docHandler);

    /**
     * マスターデータを更新する.
     * @param docHandler 登録データ
     * @param version Elasticsearchに登録されたドキュメントのバージョン
     */
    protected abstract void updateAds(EntitySetDocHandler docHandler, long version);

    /**
     * マスタデータを削除する.
     * @param docHandler 削除データ
     * @param version 削除したデータのバージョン
     */
    protected abstract void deleteAds(EntitySetDocHandler docHandler, long version);

    /**
     * UUIDでODataEntityのデータ登録を行う.
     * @param docHandler 登録データ
     * @return 登録結果
     */
    @Override
    public DcIndexResponse create(final EntitySetDocHandler docHandler) {
        String id = DcUUID.randomUUID();
        return create(id, docHandler);
    }

    /**
     * ODataEntityのデータ登録を行う.
     * @param id 登録データのID
     * @param docHandler 登録データ
     * @return 登録結果
     */
    public DcIndexResponse create(String id, EntitySetDocHandler docHandler) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(getIndex().getName());
        docHandler.setId(id);
        DcIndexResponse response = create(id, docHandler.getSource(), docHandler);
        createAds(docHandler);
        return response;
    }

    /**
     * Cellのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @return 更新結果
     */
    @Override
    public DcIndexResponse update(String id, EntitySetDocHandler docHandler) {
        return this.update(id, docHandler, -1);
    }

    /**
     * バージョン指定ありでODataEntityのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @param version バージョン情報
     * @return 更新結果
     */
    public DcIndexResponse update(String id, EntitySetDocHandler docHandler, long version) {
        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        prepareDataUpdate(getIndex().getName());
        DcIndexResponse response = update(id, docHandler.getSource(), version);
        updateAds(docHandler, response.getVersion());
        return response;
    }

    /**
     * ODataEntityのデータ削除を行う.
     * @param docHandler 削除データ
     * @return 削除結果
     */
    @Override
    public DcDeleteResponse delete(final EntitySetDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * ODataEntityのデータ削除を行う.
     * @param docHandler 削除データ
     * @param version バージョン情報
     * @return 削除結果
     */
    @Override
    public DcDeleteResponse delete(EntitySetDocHandler docHandler, long version) {
        String id = docHandler.getId();

        // マスタ書き込みでエラーが発生したためES更新を不可能とする
        super.prepareDataUpdate(getIndex().getName());
        DcDeleteResponse response = super.delete(id, version);
        deleteAds(docHandler, response.getVersion());
        return response;
    }
}
