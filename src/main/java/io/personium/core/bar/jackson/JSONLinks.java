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

import java.util.Map;

import org.json.simple.JSONObject;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * barファイル内の$links定義用JSONファイル(70_$links_json)読み込み用Mapping定義クラス.
 */
public class JSONLinks implements JSONMappedObject {

    /**
     * FromType.
     */
    @JsonProperty("FromType")
    private String fromType;

    /**
     * FromName.
     */
    @JsonProperty("FromName")
    private Map<String, String> fromName;

    /**
     * ToType.
     */
    @JsonProperty("ToType")
    private String toType;

    /**
     * ToName .
     */
    @JsonProperty("ToName")
    private Map<String, String> toName;

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
     * FromName名プロパティの取得.
     * @return FromName
     */
    public Map<String, String> getFromName() {
        return this.fromName;
    }

    /**
     * FromName名プロパティの設定.
     * @param fromName FromName
     */
    public void setFromName(Map<String, String> fromName) {
        this.fromName = fromName;
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
     * ToName名プロパティの取得.
     * @return ToName
     */
    public Map<String, String> getToName() {
        return this.toName;
    }

    /**
     * ToName名プロパティの設定.
     * @param toName ToName
     */
    public void setToName(Map<String, String> toName) {
        this.toName = toName;
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

        JSONObject fromNameJson = new JSONObject();
        for (String name : this.fromName.keySet()) {
            fromNameJson.put(name, this.fromName.get(name));
        }
        json.put("FromName", fromNameJson);

        json.put("ToType", this.toType);

        JSONObject toNameJson = new JSONObject();
        for (String name : this.toName.keySet()) {
            toNameJson.put(name, this.toName.get(name));
        }
        json.put("ToName", toNameJson);

        return json;
    }
}
