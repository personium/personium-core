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
package com.fujitsu.dc.core.bar.jackson;

import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONObject;

/**
 * barファイル内のRole定義用JSONファイル読み込み用Mapping定義クラス.
 */
public class JSONRoles implements JSONMappedObject {

    /**
     * Name.
     */
    @JsonProperty("Name")
    private String name;

    /**
     * Nameプロパティの取得.
     * @return Name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Nameプロパティの設定.
     * @param name Name.
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("Name", this.name);
        return json;
    }
}
