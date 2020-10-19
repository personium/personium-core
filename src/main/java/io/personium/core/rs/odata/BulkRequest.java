/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.rs.odata;

import java.util.Map;

import io.personium.common.es.EsBulkRequest;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;

/**
 * BatchCreateRequest class.
 */
public class BulkRequest implements EsBulkRequest {

    private BatchBodyPart bodyPart;
    private String entitySetName;
    private EntitySetDocHandler docHandler;
    private Exception error;

    /**
     * constructor.
     */
    public BulkRequest() {
    }

    /**
     * constructor.
     * @param bodyPart BatchBodyPart
     */
    public BulkRequest(BatchBodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    /**
     * Get the type of registration destination.
     * @return Type Name
     */
    public String getType() {
        return docHandler.getType();
    }

    /**
     * Get the ID of the registration data.
     * @return ID
     */
    public String getId() {
        return docHandler.getId();
    }

    /**
     * Acquire registration data.
     * @return HashMap of registered data
     */
    public Map<String, Object> getSource() {
        return docHandler.getSource();
    }

    /**
     * Getter of BodyPart.
     * @return BatchBodyPart
     */
    public BatchBodyPart getBodyPart() {
        return bodyPart;
    }

    /**
     * Setter of bodyPart.
     * @param bodyPart BatchBodyPart
     */
    public void setBodyPart(BatchBodyPart bodyPart) {
        this.bodyPart = bodyPart;
    }

    /**
     * Getter of EntitySetName.
     * @return EntitySetName
     */
    public String getEntitySetName() {
        return entitySetName;
    }

    /**
     * Setter of EntitySetName.
     * @param entitySetName EntitySetName
     */
    public void setEntitySetName(String entitySetName) {
        this.entitySetName = entitySetName;
    }

    /**
     * Docherand getter.
     * @return EntitySetDocHandler
     */
    public EntitySetDocHandler getDocHandler() {
        return docHandler;
    }

    /**
     * DocHandler's setter.
     * @param docHandler EntitySetDocHandler
     */
    public void setDocHandler(EntitySetDocHandler docHandler) {
        this.docHandler = docHandler;
    }

    /**
     * Getter of Error.
     * @return Exception
     */
    public Exception getError() {
        return error;
    }

    /**
     * Error setter.
     * @param error Exception
     */
    public void setError(Exception error) {
        this.error = error;
    }

    @Override
    public BulkRequestType getRequestType() {
        return BulkRequestType.INDEX;
    }

}
