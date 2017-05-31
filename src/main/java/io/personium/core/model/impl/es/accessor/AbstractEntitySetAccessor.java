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

import io.personium.common.es.EsIndex;
import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.util.PersoniumUUID;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

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
     * UUIDでODataEntityのデータ登録を行う.
     * @param docHandler 登録データ
     * @return 登録結果
     */
    @Override
    public PersoniumIndexResponse create(final EntitySetDocHandler docHandler) {
        String id = PersoniumUUID.randomUUID();
        return create(id, docHandler);
    }

    /**
     * ODataEntityのデータ登録を行う.
     * @param id 登録データのID
     * @param docHandler 登録データ
     * @return 登録結果
     */
    public PersoniumIndexResponse create(String id, EntitySetDocHandler docHandler) {
        docHandler.setId(id);
        PersoniumIndexResponse response = create(id, docHandler.getSource(), docHandler);
        return response;
    }

    /**
     * Cellのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @return 更新結果
     */
    @Override
    public PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler) {
        return this.update(id, docHandler, -1);
    }

    /**
     * バージョン指定ありでODataEntityのデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @param version バージョン情報
     * @return 更新結果
     */
    public PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler, long version) {
        PersoniumIndexResponse response = update(id, docHandler.getSource(), version);
        return response;
    }

    /**
     * ODataEntityのデータ削除を行う.
     * @param docHandler 削除データ
     * @return 削除結果
     */
    @Override
    public PersoniumDeleteResponse delete(final EntitySetDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * ODataEntityのデータ削除を行う.
     * @param docHandler 削除データ
     * @param version バージョン情報
     * @return 削除結果
     */
    @Override
    public PersoniumDeleteResponse delete(EntitySetDocHandler docHandler, long version) {
        String id = docHandler.getId();

        PersoniumDeleteResponse response = super.delete(id, version);
        return response;
    }
}
