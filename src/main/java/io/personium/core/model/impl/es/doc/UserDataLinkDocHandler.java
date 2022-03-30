/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
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
package io.personium.core.model.impl.es.doc;

import java.util.TreeMap;

import io.personium.common.es.response.PersoniumSearchHit;

/**
 * We deal with N: N links with ES. Links are stretched between two types. EsLinkHandler elh = new EsLinkHandler (type 1, type 2); Specify both keys to create a linked document.
 * Designate both keys and create a link document key. Specify the key of Type on one side to obtain the list of the other Type.
 */
public class UserDataLinkDocHandler extends LinkDocHandler {

    /**
     * constructor.
     */
    public UserDataLinkDocHandler() {
        super();
    }

    /**
     * constructor.
     * @param srcHandler OEntityDocHandler
     * @param tgtHandler OEntityDocHandler
     */
    public UserDataLinkDocHandler(final EntitySetDocHandler srcHandler, final EntitySetDocHandler tgtHandler) {
        super(srcHandler, tgtHandler);

        String entityTypeId = srcHandler.getEntityTypeId();
        String srcId = srcHandler.getId();
        String tgtentityTypeId = tgtHandler.getEntityTypeId();
        String tgtId = tgtHandler.getId();

        //Create unique key when saving ES
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
     * constructor.
     * @param searchHit Search result
     */
    public UserDataLinkDocHandler(PersoniumSearchHit searchHit) {
        super(searchHit);
    }

}
