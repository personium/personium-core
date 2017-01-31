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
package io.personium.core.model.ctl;

import org.json.simple.JSONObject;

/**
 * barファイル内のRole定義用JSONファイル読み込み用Mapping定義クラス.
 */
public class Event {

    /**
     * イベントのログ出力レベル.
     * 小文字でのinfo/warn/errorは旧APIとの互換を保つために置かれている.
     * このため、通常は大文字のINFO/WARN/ERRORを使用すること.
     */
    public enum LEVEL {
        /** INFOレベル . */
        INFO,
        /** INFOレベル . */
        info,
        /** WARNレベル . */
        WARN,
        /** WARNレベル . */
        warn,
        /** ERRORレベル . */
        ERROR,
        /** ERRORレベル . */
        error;
    }

    /**
     * levelプロパティ.
     */
    private LEVEL level;

    /**
     * RequestKeyプロパティ.
     */
    private String requestKey;

    /**
     * nameプロパティ.
     */
    private String name;

    /**
     * schemaプロパティ.
     */
    private String schema;

    /**
     * subjectプロパティ.
     */
    private String subject;

    /**
     * actionプロパティ.
     */
    private String action;

    /**
     * objectプロパティ.
     */
    private String object;

    /**
     * resultプロパティ.
     */
    private String result;

    /**
     * @return the requestKey
     */
    public String getRequestKey() {
        return requestKey;
    }


    /**
     * @param requestKey the requestKey to set
     */
    public void setRequestKey(String requestKey) {
        this.requestKey = requestKey;
    }


    /**
     * @return the name
     */
    public String getName() {
        return name;
    }


    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }


    /**
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }


    /**
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }


    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }


    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }


    /**
     * levelプロパティの取得.
     * @return the level
     */
    public LEVEL getLevel() {
        return level;
    }


    /**
     * @param levelValue the level to set
     */
    public void setLevel(final LEVEL levelValue) {
        this.level = levelValue;
    }


    /**
     * levelプロパティの取得.
     * @return the action
     */
    public String getAction() {
        return action;
    }


    /**
     * @param actionValue the action to set
     */
    public void setAction(final String actionValue) {
        this.action = actionValue;
    }


    /**
     * levelプロパティの取得.
     * @return the object
     */
    public String getObject() {
        return object;
    }


    /**
     * @param objectValue the object to set
     */
    public void setObject(final String objectValue) {
        this.object = objectValue;
    }


    /**
     * levelプロパティの取得.
     * @return the result
     */
    public String getResult() {
        return result;
    }


    /**
     * @param resultValue the result to set
     */
    public void setResult(final String resultValue) {
        this.result = resultValue;
    }

    /**
     * EventオブジェクトのJSON表現を取得する.
     * @return JSONオブジェクト
     */
    @SuppressWarnings("unchecked")
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("level", this.level.toString());
        json.put("action", this.action);
        json.put("object", this.object);
        json.put("result", this.result);
        return json;
    }

}
