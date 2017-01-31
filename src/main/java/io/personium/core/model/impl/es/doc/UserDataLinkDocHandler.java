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
package io.personium.core.model.impl.es.doc;

import java.util.TreeMap;

import io.personium.common.es.response.DcSearchHit;

/**
 * ESでN:Nリンクを扱う. リンクは２つのタイプの間に張られている。 EsLinkHandler elh = new EsLinkHandler(type1, type2); 双方のキーを指定して、リンクドキュメントを作成する。
 * 双方のキーを指定して、リンクドキュメントのキーを作成する。 片側のTypeのキーを指定してもう片方のTypeの一覧を取得する。
 */
public class UserDataLinkDocHandler extends LinkDocHandler {

    /**
     * コンストラクタ.
     */
    public UserDataLinkDocHandler() {
        super();
    }

    /**
     * コンストラクタ.
     * @param srcHandler OEntityDocHandler
     * @param tgtHandler OEntityDocHandler
     */
    public UserDataLinkDocHandler(final EntitySetDocHandler srcHandler, final EntitySetDocHandler tgtHandler) {
        super(srcHandler, tgtHandler);

        String entityTypeId = srcHandler.getEntityTypeId();
        String srcId = srcHandler.getId();
        String tgtentityTypeId = tgtHandler.getEntityTypeId();
        String tgtId = tgtHandler.getId();

        // ES 保存時の一意キー作成
        TreeMap<String, String> tm = new TreeMap<String, String>();
        tm.put(entityTypeId, srcId);
        tm.put(tgtentityTypeId, tgtId);

        setEnt1Type(tm.firstKey());
        setEnt2Type(tm.lastKey());
        setEnt1Key(tm.get(getEnt1Type()));
        setEnt2Key(tm.get(getEnt2Type()));

        setId(this.createLinkId());
    }

    /**
     * コンストラクタ.
     * @param searchHit 検索結果
     */
    public UserDataLinkDocHandler(DcSearchHit searchHit) {
        super(searchHit);
    }

}
