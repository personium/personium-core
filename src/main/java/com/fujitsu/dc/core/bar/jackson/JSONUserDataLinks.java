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

import java.util.Map;

import org.codehaus.jackson.annotate.JsonProperty;
import org.json.simple.JSONObject;

/**
 * barファイル内の$links定義用JSONファイル(70_$links_json)読み込み用Mapping定義クラス.
 */
public class JSONUserDataLinks implements JSONMappedObject {

    /**
     * FromType.
     */
    @JsonProperty("FromType")
    private String fromType;

    /**
     * FromId.
     */
    @JsonProperty("FromId")
    private Map<String, String> fromId;

    /**
     * ToType.
     */
    @JsonProperty("ToType")
    private String toType;

    /**
     * ToId.
     */
    @JsonProperty("ToId")
    private Map<String, String> toId;

    /**
     * FromTypeプロパティの取得.
     * @return FromType名
     */
    public String getFromType() {
        return this.fromType;
    }

    /**
     * FromTypeプロパティの設定.
     * @param fromType FromType.
     */
    public void setFromType(String fromType) {
        this.fromType = fromType;
    }

    /**
     * FromId名プロパティの取得.
     * @return FromId
     */
    public Map<String, String> getFromId() {
        return this.fromId;
    }

    /**
     * FromId名プロパティの設定.
     * @param fromIdValue FromId
     */
    public void setFromId(Map<String, String> fromIdValue) {
        this.fromId = fromIdValue;
    }

    /**
     * ToTypeプロパティの取得.
     * @return ToType名
     */
    public String getToType() {
        return this.toType;
    }

    /**
     * ToTypeプロパティの設定.
     * @param toType ToType.
     */
    public void setToType(String toType) {
        this.toType = toType;
    }

    /**
     * ToId名プロパティの取得.
     * @return ToId
     */
    public Map<String, String> getToId() {
        return this.toId;
    }

    /**
     * ToId名プロパティの設定.
     * @param toIdValue ToId
     */
    public void setToId(Map<String, String> toIdValue) {
        this.toId = toIdValue;
    }

    /**
     * NavigatioinProperty形式のToTypeプロパティの取得.
     * @return NavigatioinProperty形式のToType名
     */
    public String getNavPropToType() {
        return "_" + this.toType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("FromType", this.fromType);

        JSONObject fromIdJson = new JSONObject();
        for (String name : this.fromId.keySet()) {
            fromIdJson.put(name, this.fromId.get(name));
        }
        json.put("FromId", fromIdJson);

        json.put("ToType", this.toType);

        JSONObject toIdJson = new JSONObject();
        for (String name : this.toId.keySet()) {
            toIdJson.put(name, this.toId.get(name));
        }
        json.put("ToId", toIdJson);

        return json;
    }
}
