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
package io.personium.core.event;

/**
 * イベント.
 */
public class PersoniumEvent {
    String name;
    String schema;
    int level;
    String subject;
    String action;
    String object;
    String result;
    /**
     * Event Level Constants.
     */
    public static class Level {
        /**
         * INFO Level.
         */
        public static final int INFO = 1;
        /**
         * WARN Level.
         */
        public static final int WARN = 2;
        /**
         * ERROR Level.
         */
        public static final int ERROR = 3;
    }
    /**
     * コンストラクタ.
     * @param name イベント名
     * @param schema BOXスキーマURI
     * @param level レベル
     * @param subject 主体
     * @param action 行為
     * @param object 対象
     * @param result 結果
     */
    public PersoniumEvent(final String name,
            final String schema,
            final int level,
            final String subject,
            final String action,
            final String object,
            final String result) {
        this.name = name;
        this.schema = schema;
        this.level = level;
        this.subject = subject;
        this.action = action;
        this.object = object;
        this.result = result;
    }

    /**
     * @return the subject
     */
    public final String getSubject() {
        return subject;
    }
    /**
     * @param subject the subject to set
     */
    public final void setSubject(String subject) {
        this.subject = subject;
    }
    /**
     * @return the action
     */
    public final String getAction() {
        return action;
    }
    /**
     * @return the schema
     */
    public final String getSchema() {
        return schema;
    }
    /**
     * @param action the action to set
     */
    public final void setAction(String action) {
        this.action = action;
    }
    /**
     * @return the object
     */
    public final String getObject() {
        return object;
    }
    /**
     * @param object the object to set
     */
    public final void setObject(String object) {
        this.object = object;
    }

    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }
    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the result
     */
    public String getResult() {
        return result;
    }

    /**
     * ログ出力するときの文字列を生成する.
     * @return ログ出力用文字列
     */
    public String toLogMessage() {
        String format = "%s,%s,%s,%s,%s,%s";
        return String.format(format, this.name, this.schema, this.subject, this.action, this.object, this.result);
    }

}
