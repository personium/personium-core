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
package io.personium.core.bar.jackson;

import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONObject;

/**
 * barファイル内のExtRole定義用JSONファイル読み込み用Mapping定義クラス.
 */
public class JSONExtRoles implements JSONMappedObject {

    /**
     * ExtRole.
     */
    @JsonProperty("ExtRole")
    private String extRole;

    /**
     * _Relation.Name.
     */
    @JsonProperty("_Relation.Name")
    private String relation;

    /**
     * ExtRoleプロパティの取得.
     * @return ExtRole名
     */
    public String getExtRole() {
        return this.extRole;
    }

    /**
     * ExtRoleプロパティの設定.
     * @param url extRoleURL.
     */
    public void setExtRole(String url) {
        this.extRole = url;
    }

    /**
     * Relation名プロパティの取得.
     * @return Relation_Name
     */
    public String getRelationName() {
        return this.relation;
    }

    /**
     * Relation名プロパティの設定.
     * @param relationName RelationName
     */
    public void setRelationName(String relationName) {
        this.relation = relationName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("ExtRole", this.extRole);
        json.put("_Relation.Name", this.relation);
        return json;
    }
}
