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

import java.util.List;

import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.odata.OEntityWrapper;

/**
 * Preprocessing and postprocessing for ODataResource.
 */
public class ODataCtlResource {
    /**
     * Preprocessing process.
     * @param oEntityWrapper OEntityWrapper object
     */
    public void beforeCreate(final OEntityWrapper oEntityWrapper) {
    }

    /**
     * Pre-update processing.
     * @param oEntityWrapper OEntityWrapper object
     * @param oEntityKey The entityKey to delete
     */
    public void beforeUpdate(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
    }

    /**
     * Partial update preprocessing.
     * @param oEntityWrapper OEntityWrapper object
     * @param oEntityKey The entityKey to delete
     */
    public void beforeMerge(final OEntityWrapper oEntityWrapper, final OEntityKey oEntityKey) {
        beforeUpdate(oEntityWrapper, oEntityKey);
    }

    /**
     * Pre-deletion process.
     * @param entitySetName entitySet name
     * @param oEntityKey The entityKey to delete
     */
    public void beforeDelete(final String entitySetName, final OEntityKey oEntityKey) {
    }

    /**
     * Processing after deletion.
     * @param entitySetName entitySet name
     * @param oEntityKey The entityKey to delete
     */
    public void afterDelete(final String entitySetName, final OEntityKey oEntityKey) {
    }

    /**
     * Link registration preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    public void beforeLinkCreate(OEntityId sourceEntity, String targetNavProp) {
        //$ Links specification of ExtRole and _Relation is not allowed (Relation: ExtRole is 1: N relation)
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * Link acquisition preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    public void beforeLinkGet(OEntityId sourceEntity, String targetNavProp) {
    }

    /**
     * Link deletion preprocessing.
     * @param sourceEntity Linked entity
     * @param targetNavProp Navigation property to be linked
     */
    public void beforeLinkDelete(OEntityId sourceEntity, String targetNavProp) {
        //$ Links specification of ExtRole and _Relation is not allowed (Relation: ExtRole is 1: N relation)
        checkNonSupportLinks(sourceEntity.getEntitySetName(), targetNavProp);
    }

    /**
     * Check processing other than p: Format.
     * @param entitySetName target entityset name
     * @param props property list
     */
    public void validate(String entitySetName, List<OProperty<?>> props) {
    }

    /**
     * Post event to EventBus.
     * @param entitySetName the name of the entityset of processing object
     * @param object string of processing object
     * @param info string of the information about processing
     * @param op kind of operation
     */
    public void postEvent(String entitySetName, String object, String info, String op) {
    }

    private void checkNonSupportLinks(String sourceEntity, String targetNavProp) {
        if (targetNavProp.startsWith("_")) {
            targetNavProp = targetNavProp.substring(1);
        }
        if ((sourceEntity.equals(ExtRole.EDM_TYPE_NAME) //NOPMD -To maintain readability
                        && targetNavProp.equals(Relation.EDM_TYPE_NAME)) //NOPMD
                || (sourceEntity.equals(Relation.EDM_TYPE_NAME) //NOPMD
                        && targetNavProp.equals(ExtRole.EDM_TYPE_NAME))) { //NOPMD
            throw PersoniumCoreException.OData.NO_SUCH_ASSOCIATION;
        }
    }
}
