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
package io.personium.test.jersey;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * DAOで発生するException.
 */
public class PersoniumException extends Exception {
    /** serialVersionID. */
    private static final long serialVersionUID = 1L;

    /**
     * コンストラクタ.
     * @param msg メッセージ
     * @param t thorowable
     */
    public PersoniumException(final String msg, final Throwable t) {
        super(msg, t);
    }

    /**
     * コンストラクタ.
     * @param msg メッセージ
     */
    public PersoniumException(final String msg) {
        super(msg);
    }

    /**
     * DaoExceptionの生成.
     * @param msg メッセージ
     * @param c ステータスコード
     * @return DaoExceptionオブジェクト
     */
    public static PersoniumException create(final String msg, final int c) {
        String str = "";
        if (msg.startsWith("{")) {
            str = "{\"msg\":" + msg + ",\"code\":\"" + Integer.toString(c) + "\"}";
        } else {
            str = "{\"msg\":\"" + msg + "\",\"code\":\"" + Integer.toString(c) + "\"}";
        }
        return new PersoniumException(str);
    }

    /**
     * 例外発生時のステータスコードを取得.
     * @return ステータスコード
     */
    public final String getCode() {
        String code = "";
        String msg = this.getMessage();
        JSONObject json = (JSONObject) JSONValue.parse(msg);
        code = (String) json.get("code");
        return code;
    }
}
