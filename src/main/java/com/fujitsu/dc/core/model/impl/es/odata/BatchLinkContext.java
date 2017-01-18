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
package com.fujitsu.dc.core.model.impl.es.odata;

import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * ユーザODataの$batch内のリンク情報を保持するためのクラス.
 */
public class BatchLinkContext {

    private EntitySetDocHandler sourceDocHandler;
    private String targetEntityTypeName;
    private String targetEntityTypeId;
    private long existsCount;
    private long requestCount;

    BatchLinkContext(EntitySetDocHandler sourceDocHandler, String targetEntityType, String targetEntityTypeId) {
        this.sourceDocHandler = sourceDocHandler;
        this.targetEntityTypeName = targetEntityType;
        this.targetEntityTypeId = targetEntityTypeId;
    }

    /**
     * ソース側のDocHandlerを取得する.
     * @return ソース側のDocHandler
     */
    EntitySetDocHandler getSourceDocHandler() {
        return sourceDocHandler;
    }

    /**
     * ターゲット側のEntityType名を取得する.
     * @return ターゲット側のEntityType名
     */
    String getTargetEntityTypeName() {
        return targetEntityTypeName;
    }

    /**
     * ターゲット側のEntityTypeIDを取得する.
     * @return ターゲット側のEntityTypeID
     */
    String getTargetEntityTypeId() {
        return targetEntityTypeId;
    }

    /**
     * これから登録する件数（DBに登録済み件数+リクエスト内の解析済み件数）.
     * @return これから登録する件数
     */
    long getRegistCount() {
        return this.existsCount + this.requestCount;
    }

    /**
     * DBに登録済みの件数を設定する.
     * @param existsCount DBに登録済みの件数
     */
    void setExistsCount(long existsCount) {
        this.existsCount = existsCount;
    }

    /**
     * リクエスト内の解析済み件数をインクリメントする.
     */
    void incrementRegistCount() {
        this.requestCount++;
    }

}
