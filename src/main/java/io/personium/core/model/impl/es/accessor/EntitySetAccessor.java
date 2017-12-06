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

import java.util.Map;

import io.personium.common.es.response.PersoniumDeleteResponse;
import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumIndexResponse;
import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * ODataEntitySetに対するアクセッサーのインターフェースクラス.
 */
public interface EntitySetAccessor {

    /**
     * ドキュメントを取得する.
     * @param id ドキュメントのID
     * @return 応答
     */
    PersoniumGetResponse get(String id);

    /**
     * UUIDでデータ登録を行う.
     * @param docHandler 登録データ
     * @return 登録結果
     */
    PersoniumIndexResponse create(EntitySetDocHandler docHandler);

    /**
     * ID指定ありでデータ登録を行う.
     * @param id 登録ID
     * @param docHandler 登録データ
     * @return 登録結果
     */
    PersoniumIndexResponse create(String id, EntitySetDocHandler docHandler);

    /**
     * バージョン指定ありでデータ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @param version バージョン情報
     * @return 更新結果
     */
    PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler, long version);

    /**
     * データ更新を行う.
     * @param id 更新データのID
     * @param docHandler 登録データ
     * @return 更新結果
     */
    PersoniumIndexResponse update(String id, EntitySetDocHandler docHandler);

    /**
     * データ削除を行う.
     * @param docHandler 削除データ
     * @return 削除結果
     */
    PersoniumDeleteResponse delete(EntitySetDocHandler docHandler);

    /**
     * ドキュメントの件数を取得.
     * @param query クエリ情報
     * @return ES応答
     */
    long count(Map<String, Object> query);

    /**
     * ドキュメントを検索.
     * @param query クエリ情報
     * @return ES応答
     */
    PersoniumSearchResponse search(Map<String, Object> query);

    /**
     * データ削除を行う.
     * @param docHandler 削除データ
     * @param version バージョン
     * @return 削除結果
     */
    PersoniumDeleteResponse delete(EntitySetDocHandler docHandler, long version);

    /**
     * Typeを取得する.
     * @return 応答
     */
    String getType();

}
