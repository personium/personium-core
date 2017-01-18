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
package com.fujitsu.dc.core.eventbus;

import org.codehaus.jackson.annotate.JsonProperty;

import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.Event;

/**
 * barファイル内のRole定義用JSONファイル読み込み用Mapping定義クラス.
 */
public class JSONEvent {

    /**
     * levelプロパティ.
     */
    @JsonProperty("level")
    private Event.LEVEL level;

    /**
     * actionプロパティ.
     */
    @JsonProperty("action")
    private String action;

    /**
     * objectプロパティ.
     */
    @JsonProperty("object")
    private String object;

    /**
     * resultプロパティ.
     */
    @JsonProperty("result")
    private String result;


    /**
     * levelプロパティの取得.
     * @return the level
     */
    public Event.LEVEL getLevel() {
        return level;
    }


    /**
     * @param levelValue the level to set
     */
    public void setLevel(final Event.LEVEL levelValue) {
        this.level = Event.LEVEL.valueOf(levelValue.toString().toUpperCase());
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
     * levelプロパティのバリデート.
     * @param levelValue levelプロパティの値
     * @return 正しい場合はtrueを、それ以外はfalseを返す。
     */
    public static boolean validateLevel(final Event.LEVEL levelValue) {
        boolean retValue = true;
        if (Event.LEVEL.INFO != levelValue
                && Event.LEVEL.WARN != levelValue
                && Event.LEVEL.ERROR != levelValue) {
            retValue = false;
        }
        return retValue;
    }

    /**
     * actionプロパティのバリデート.
     * @param actionValue actionプロパティの値
     * @return 正しい場合はtrueを、それ以外はfalseを返す。
     */
    public static boolean validateAction(final String actionValue) {
        return validateStringValue(actionValue);
    }

    /**
     * objectプロパティのバリデート.
     * @param objectValue objectプロパティの値
     * @return 正しい場合はtrueを、それ以外はfalseを返す。
     */
    public static boolean validateObject(final String objectValue) {
        return validateStringValue(objectValue);
    }

    /**
     * resultプロパティのバリデート.
     * @param resultValue resultプロパティの値
     * @return 正しい場合はtrueを、それ以外はfalseを返す。
     */
    public static boolean validateResult(final String resultValue) {
        return validateStringValue(resultValue);
    }

    /**
     * 文字列型プロパティのバリデート.
     * @param value 文字列プロパティの値
     * @return 正しい場合はtrueを、それ以外はfalseを返す。
     */
    static boolean validateStringValue(final String value) {
        boolean retValue = true;
        if (value == null) {
            retValue = false;
        } else if (value.length() > Common.MAX_EVENT_VALUE_LENGTH) {
            retValue = false;
        }
        return retValue;
    }
}
