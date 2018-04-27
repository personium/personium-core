/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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
package io.personium.core.bar;

import java.text.MessageFormat;

import io.personium.core.PersoniumCoreMessageUtils;
import io.personium.core.utils.EscapeControlCode;

/**
 * Personium exception for bar installation.
 */
public class PersoniumBarException extends RuntimeException {

//    public static final PersoniumBarException BAR_INSTALLATION_FAILED = create("PL-BI-0001");
    public static final PersoniumBarException INSTALLATION_FAILED = create("PL-BI-1004");
//    public static final PersoniumBarException UNKNOWN_ERROR = create("PL-BI-1005");

    private String code;
    private String path;
    private String message;

    private PersoniumBarException(String code, String path, String message) {
        this.code = code;
        this.path = path;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    /**
     * ファクトリーメソッド.
     * @param code メッセージコード
     * @return PersoniumCoreException
     */
    private static PersoniumBarException create(String code) {
        // ログメッセージの取得
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumBarException(code, null, message);
    }

    public PersoniumBarException path(String path) {
        return new PersoniumBarException(this.code, path, this.message);
    }

    public PersoniumBarException detail(String detailMessage) {
        // 置換メッセージ作成
        String ms = MessageFormat.format(this.message, detailMessage);
        // 制御コードのエスケープ処理
        ms = EscapeControlCode.escape(ms);
        // メッセージ置換クローンを作成
        return new PersoniumBarException(this.code, this.path, ms);
    }

    public PersoniumBarException detail(PersoniumBarException.Detail detail) {
        // 置換メッセージ作成
        String ms = MessageFormat.format(this.message, detail.getDetailMessage());
        // 制御コードのエスケープ処理
        ms = EscapeControlCode.escape(ms);
        // メッセージ置換クローンを作成
        return new PersoniumBarException(this.code, this.path, ms);
    }

    /**
     *
     */
    public static class Detail {
        private String detailCode;
        private String detailMessage;

        public Detail(String detailCode) {
            this.detailCode = detailCode;
            detailMessage = PersoniumCoreMessageUtils.getMessage(detailCode);
        }

        public Detail(String detailCode, Object... params) {
            this.detailCode = detailCode;
            String message = PersoniumCoreMessageUtils.getMessage(detailCode);
            // 置換メッセージ作成
            message = MessageFormat.format(message, params);
            // 制御コードのエスケープ処理
            detailMessage = EscapeControlCode.escape(message);
        }

        public String getDetailCode() {
            return detailCode;
        }

        public String getDetailMessage() {
            return detailMessage;
        }
    }
}
