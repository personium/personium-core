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
package com.fujitsu.dc.core.rs.odata;

import java.util.Map;

import com.fujitsu.dc.common.es.EsBulkRequest;
import com.fujitsu.dc.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * BatchCreateRequestクラス.
 */
public class BulkRequest implements EsBulkRequest {

    private BatchBodyPart bodyPart;
    private String entitySetName;
    private EntitySetDocHandler docHandler;
    private Exception error;

    /**
     * コンストラクタ.
     */
    public BulkRequest() {
    }

    /**
     * コンストラクタ.
     * @param bodyPart BatchBodyPart
     */
    public BulkRequest(BatchBodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    /**
     * 登録先のタイプを取得する.
     * @return Type名
     */
    public String getType() {
        return docHandler.getType();
    }

    /**
     * 登録データのIDを取得する.
     * @return ID
     */
    public String getId() {
        return docHandler.getId();
    }

    /**
     * 登録データを取得する.
     * @return 登録データのHashMap
     */
    public Map<String, Object> getSource() {
        return docHandler.getSource();
    }

    /**
     * BodyPartのゲッター.
     * @return BatchBodyPart
     */
    public BatchBodyPart getBodyPart() {
        return bodyPart;
    }

    /**
     * bodyPartのセッター.
     * @param bodyPart BatchBodyPart
     */
    public void setBodyPart(BatchBodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    /**
     * EntitySetNameのゲッター.
     * @return EntitySetName
     */
    public String getEntitySetName() {
        return entitySetName;
    }

    /**
     * EntitySetNameのセッター.
     * @param entitySetName EntitySetName
     */
    public void setEntitySetName(String entitySetName) {
        this.entitySetName = entitySetName;
    }

    /**
     * DocHandlerのゲッター.
     * @return EntitySetDocHandler
     */
    public EntitySetDocHandler getDocHandler() {
        return docHandler;
    }

    /**
     * DocHandlerのセッター.
     * @param docHandler EntitySetDocHandler
     */
    public void setDocHandler(EntitySetDocHandler docHandler) {
        this.docHandler = docHandler;
    }

    /**
     * Errorのゲッター.
     * @return Exception
     */
    public Exception getError() {
        return error;
    }

    /**
     * Errorのセッター.
     * @param error Exception
     */
    public void setError(Exception error) {
        this.error = error;
    }

    @Override
    public BULK_REQUEST_TYPE getRequestType() {
        return BULK_REQUEST_TYPE.INDEX;
    }

}
