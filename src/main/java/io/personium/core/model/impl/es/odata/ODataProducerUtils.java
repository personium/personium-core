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
package io.personium.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OEntityId;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumSearchHits;
import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.auth.AuthUtils;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.impl.es.doc.EntitySetDocHandler;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.core.utils.ODataUtils;

/**
 * ODataProducerUtils.
 */
public final class ODataProducerUtils {

    /**
     * log.
     */
    static Logger log = LoggerFactory.getLogger(ODataProducerUtils.class);

    private ODataProducerUtils() {
    }

    /**
     * Perform uniqueness check of data at Entity registration / update.
     * @param producer
     * @param newEntity Entity to register / update newly
     * @param originalEntity original Entity
     * @param originalKey The key name specified in the update request
     */
    static void checkUniqueness(EsODataProducer producer, OEntityWrapper newEntity,
            OEntityWrapper originalEntity, OEntityKey originalKey) {
        boolean needsPkCheck = false;
        if (originalEntity == null) {
            needsPkCheck = true;
        } else {
            //Only when the key of oew is changed from originalKey, the influence by key change is confirmed.
            OEntityKey normNewKey = AbstractODataResource.normalizeOEntityKey(newEntity.getEntityKey(),
                    newEntity.getEntitySet());
            if (null == originalKey) {
                originalKey = originalEntity.getEntityKey();
            }
            OEntityKey normOrgKey = AbstractODataResource.normalizeOEntityKey(
                    originalKey, newEntity.getEntitySet());
            String newKeyStr = normNewKey.toKeyStringWithoutParentheses();
            String orgKeyStr = normOrgKey.toKeyStringWithoutParentheses();
            //We have to normalize KEY and compare.
            log.debug("NWKEY:" + newKeyStr);
            log.debug("ORKEY:" + orgKeyStr);
            if (!newKeyStr.equals(orgKeyStr)) {
                needsPkCheck = true;
            }
        }
        if (needsPkCheck) {
            //Perform a search with the primary key.
            EntitySetDocHandler hit = producer.retrieveWithKey(newEntity.getEntitySet(), newEntity.getEntityKey());
            if (hit != null) {
                //If data exists, it will be a CONFLICT error
                throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
            }
        }

        //Uniqueness check by UK constraint
        //UK constraint extraction processing
        //Caching (together with TODO schema information)
        Map<String, List<String>> uks = new HashMap<String, List<String>>();
        List<EdmProperty> listEdmProperties = newEntity.getEntityType().getProperties().toList();
        for (EdmProperty edmProp : listEdmProperties) {
            Iterable<? extends NamespacedAnnotation<?>> anots = edmProp.getAnnotations();
            for (NamespacedAnnotation<?> anot : anots) {
                if ("Unique".equals(anot.getName())
                        && PersoniumCoreUtils.XmlConst.NS_PERSONIUM.equals(anot.getNamespace().getUri())) {
                    String ukName = (String) anot.getValue();
                    List<String> ukProps = uks.get(ukName);
                    if (ukProps == null) {
                        ukProps = new ArrayList<String>();
                    }
                    ukProps.add(edmProp.getName());
                    uks.put(ukName, ukProps);
                }
            }
        }

        //Here we perform a search with all the unique keys and confirm that there is no data
        for (Map.Entry<String, List<String>> uk : uks.entrySet()) {
            log.debug("checking uk : [" + uk.getKey() + "] = ");
            List<String> ukProps = uk.getValue();
            Set<OProperty<?>> ukSet = new HashSet<OProperty<?>>();
            //Since UK ensures the uniqueness of non-null items, any number of items can be used in which all items are null.
            boolean allNull = true;
            //When there is no change in all the items making up the UK, there is no need to check it.
            boolean changed = false;
            for (String k : ukProps) {
                log.debug("              - [" + k + "]");
                OProperty<?> oProp = newEntity.getProperty(k);
                if (oProp.getValue() != null) {
                    allNull = false;
                    if (originalEntity != null) {
                        OProperty<?> origProp = originalEntity.getProperty(k);
                        if (!oProp.getValue().equals(origProp.getValue())) {
                            changed = true;
                        }
                    }
                }
                ukSet.add(oProp);
            }
            //When all items constituting UK are Null, there is no need to check
            boolean needsUkCheck = !allNull;
            if (originalEntity != null && !changed) {
                //With the change, when there is no change of all the items making up the UK, there is no need to check.
                needsUkCheck = false;
            }

            //Check only when there is a change, but not AllNull after change key.
            if (needsUkCheck) {
                EntitySetDocHandler edh = producer.retrieveWithKey(newEntity.getEntitySet(), ukSet, null);
                if (edh != null) {
                    //If data exists, it will be a CONFLICT error
                    throw PersoniumCoreException.OData.ENTITY_ALREADY_EXISTS;
                }
            }
        }
    }

    /**
     * Search N:N links.
     * @param idvals idvals
     * @param tgtEsType tgtEsType
     * @param queryInfo Request query
     * @return Search result
     */
    public static PersoniumSearchHits searchLinksNN(
            List<String> idvals, EntitySetAccessor tgtEsType, QueryInfo queryInfo) {

        if (idvals.size() == 0) {
            return null;
        }

        // If no query is specified, the default value is set.
        Integer size = PersoniumUnitConfig.getTopQueryDefaultSize();
        Integer from = 0;
        if (queryInfo != null) {
            if (queryInfo.top != null) {
                size = queryInfo.top;
            }
            if (queryInfo.skip != null) {
                from = queryInfo.skip;
            }
        }

        // Obtain target primary key column from target UUID column.
        Map<String, Object> source = new HashMap<String, Object>();
        Map<String, Object> filter = new HashMap<String, Object>();
        Map<String, Object> ids = new HashMap<String, Object>();
        source.put("filter", filter);
        source.put("size", size);
        source.put("from", from);
        filter.put("ids", ids);
        ids.put("values", idvals);

        List<Map<String, Object>> sort = new ArrayList<Map<String, Object>>();
        Map<String, Object> orderByName = new HashMap<String, Object>();
        Map<String, Object> orderById = new HashMap<String, Object>();
        Map<String, Object> order = new HashMap<String, Object>();
        source.put("sort", sort);
        sort.add(orderByName);
        sort.add(orderById);
        order.put("order", "asc");
        order.put("ignore_unmapped", true);
        orderByName.put("s.Name.untouched", order);
        orderById.put("s.__id.untouched", order);

        PersoniumSearchHits sHits = tgtEsType.search(source).hits();
        return sHits;
    }

    /**
     * Compare if linksKey and parent key are equal.
     * @param entity entity
     * @param linksKey linksKey
     * @return boolean
     */
    public static boolean isParentEntity(OEntityId entity, String linksKey) {
        return entity.getEntitySetName().equals(linksKey);
    }

    /**
     * Generate a password change request to ES.
     * @param oedhNew oedhNew
     * @param dcCredHeader dcCredHeader
     */
    public static void createRequestPassword(EntitySetDocHandler oedhNew, String dcCredHeader) {
        //Pre-update processing (obtain Hash string converted password)
        Map<String, String> hashed = AuthUtils.hashPassword(dcCredHeader, oedhNew.getType());
        //Overwrite password to be changed to HashedCredential
        Map<String, Object> hiddenFields = oedhNew.getHiddenFields();
        //Put the value of X-Personium-Credential into the key of HashedCredential
        //If there is no designation, return 400 error
        if (hashed != null) {
            for (Map.Entry<String, String> hashedEntry : hashed.entrySet()) {
                hiddenFields.put(hashedEntry.getKey(), hashedEntry.getValue());
            }
        } else {
            throw PersoniumCoreException.Auth.P_CREDENTIAL_REQUIRED;
        }
        oedhNew.setHiddenFields(hiddenFields);

        //Get current time and overwrite __updated
        long nowTimeMillis = System.currentTimeMillis();
        oedhNew.setUpdated(nowTimeMillis);
    }

    /**
     * Merge the values ​​of dynamic fields and static fields of OEDH specified by argument and update oedhNew.
     * @param oedhExisting base OEDH
     * @param oedhNew Add OEDH
     */
    public static void mergeFields(EntitySetDocHandler oedhExisting, EntitySetDocHandler oedhNew) {
        //Add registered properties to static fields
        oedhNew.setStaticFields(ODataUtils.getMergeFields(oedhExisting.getStaticFields(),
                oedhNew.getStaticFields()));
    }
}
