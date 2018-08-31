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

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.odata4j.edm.EdmDataServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.es.response.PersoniumGetResponse;
import io.personium.common.es.response.PersoniumSearchHit;
import io.personium.core.odata.OEntityWrapper;

/**
 *DocHandler of Cell.
 */
public class CellDocHandler extends OEntityDocHandler {
    /**
     *A JSON key that stores ACL configuration information for elasticsearch.
     */
    public static final String KEY_ACL_FIELDS = "a";

    Map<String, JSONObject> aclFields;
    static Logger log = LoggerFactory.getLogger(CellDocHandler.class);

    /**
     *@return ACL setting information
     */
    public Map<String, JSONObject> getAclFields() {
        return aclFields;
    }

    /**
     *@ param aclFields ACL setting information
     */
    public void setAclFields(Map<String, JSONObject> aclFields) {
        this.aclFields = aclFields;
    }

    /**
     * Constructor.
     */
    public CellDocHandler() {
        super();
        if (this.aclFields == null) {
            aclFields = new HashMap<String, JSONObject>();
        }
    }

    /**
     * Constructor.
     * @param getResponse GetResponse
     */
    public CellDocHandler(PersoniumGetResponse getResponse) {
        super(getResponse);
        if (this.aclFields == null) {
            aclFields = new HashMap<String, JSONObject>();
        }
    }

    /**
     * Constructor.
     * @param type type
     * @param oEntity OEntityWrapper
     *@ param metadata schema information
     */
    public CellDocHandler(String type, OEntityWrapper oEntity, EdmDataServices metadata) {
        super(type, oEntity, metadata);
        if (this.aclFields == null) {
            aclFields = new HashMap<String, JSONObject>();
        }
    }

    /**
     * Constructor.
     * @param searchHit SearchHit
     */
    public CellDocHandler(PersoniumSearchHit searchHit) {
        super(searchHit);
        if (this.aclFields == null) {
            aclFields = new HashMap<String, JSONObject>();
        }
    }

    /**
     *Parse Source in Map format and map to itself.
     *@ param source mapping format information for mapping
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void parseSource(Map<String, Object> source) {
        super.parseSource(source);
        this.aclFields = (Map<String, JSONObject>) source.get(KEY_ACL_FIELDS);
    }

    /**
     *Get Source of Map as Map format.
     *@return Hash value of CellDocument
     */
    @Override
    public Map<String, Object> getSource() {
        Map<String, Object> ret = super.getSource();
        ret.put(KEY_ACL_FIELDS, this.aclFields);
        //The following is given as prop patch information
        ret.put(KEY_DYNAMIC_FIELDS, this.dynamicFields);
        return ret;
    }
}
