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
import io.personium.core.model.impl.es.doc.LinkDocHandler;

/**
 * ODataLink情報のアクセス処理を実装したクラス.
 */
public class ODataLinkAccessor extends DataSourceAccessor {

    /**
     * コンストラクタ.
     * @param index インデックス
     * @param name タイプ名
     * @param routingId routingId
     */
    public ODataLinkAccessor(EsIndex index, String name, String routingId) {
        super(index, name, routingId);
    }

    /**
     * ODataLinkのデータ登録を行う.
     * @param id 登録データのID
     * @param docHandler 登録データ
     * @return 登録結果
     */
    public PersoniumIndexResponse create(String id, LinkDocHandler docHandler) {
        docHandler.setId(id);
        PersoniumIndexResponse response = super.create(id, docHandler.createLinkDoc());
        return response;
    }

    /**
     * Delete a document.
     * @param docHandler 削除データ
     * @return 応答
     */
    public PersoniumDeleteResponse delete(final LinkDocHandler docHandler) {
        return this.delete(docHandler, -1);
    }

    /**
     * ODataLinkのデータ削除を行う.
     * @param docHandler 削除データ
     * @param version バージョン情報
     * @return 削除結果
     */
    public PersoniumDeleteResponse delete(final LinkDocHandler docHandler, long version) {
        String id = docHandler.getId();

        PersoniumDeleteResponse response = super.delete(id, version);
        return response;
    }

}
