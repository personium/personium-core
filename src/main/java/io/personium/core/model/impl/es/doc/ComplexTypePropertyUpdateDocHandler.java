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

import java.util.Map;

import org.odata4j.edm.EdmDataServices;

import io.personium.core.odata.OEntityWrapper;

/**
 * ComplexTypeプロパティを更新する際に使用するDocHandler.
 */
public class ComplexTypePropertyUpdateDocHandler extends ComplexTypePropertyDocHandler {

    /**
     * OEntityWrapperから IDのないDocHandlerをつくるConstructor.
     * @param type ESのtype名
     * @param oEntityWrapper OEntityWrapper
     * @param metadata スキーマ情報
     */
    public ComplexTypePropertyUpdateDocHandler(String type, OEntityWrapper oEntityWrapper, EdmDataServices metadata) {
        super(type, oEntityWrapper, metadata);
    }

    /**
     * ES/MySQL登録用データを取得する.
     * @return 登録用データ
     */
    @Override
    public Map<String, Object> getSource() {
        return getCommonSource();
    }
}
