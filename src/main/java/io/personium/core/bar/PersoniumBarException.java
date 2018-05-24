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

    /** Bar installation failed. */
    public static final PersoniumBarException INSTALLATION_FAILED = create("PL-BI-1004");

    /** Erro code. */
    private String code;
    /** Path of the file where error occurred. */
    private String path;
    /** Erro message. */
    private String message;

    /**
     * Constructor.
     * @param code Erro code
     * @param path Path of the file where error occurred
     * @param message Erro message
     */
    private PersoniumBarException(String code, String path, String message) {
        this.code = code;
        this.path = path;
        this.message = message;
    }

    /**
     * Get code.
     * @return code
     */
    public String getCode() {
        return code;
    }

    /**
     * Get path.
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Get message.
     * @return message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Factory method.
     * @param code Error code
     * @return PersoniumBarException
     */
    private static PersoniumBarException create(String errorCode) {
        String message = PersoniumCoreMessageUtils.getMessage(errorCode);
        return new PersoniumBarException(errorCode, "", message);
    }

    /**
     * Set PersoniumBarException to path and return it.
     * @param filePath Path of the file where error occurred
     * @return PersoniumBarException
     */
    public PersoniumBarException path(String filePath) {
        return new PersoniumBarException(this.code, filePath, this.message);
    }

    /**
     * Set PersoniumBarException to error message and return it.
     * @param detailMessage Error message
     * @return PersoniumBarException
     */
    public PersoniumBarException detail(String detailMessage) {
        // 置換メッセージ作成
        String ms = MessageFormat.format(this.message, detailMessage);
        // 制御コードのエスケープ処理
        ms = EscapeControlCode.escape(ms);
        // メッセージ置換クローンを作成
        return new PersoniumBarException(this.code, this.path, ms);
    }

    /**
     * Set PersoniumBarException to error message and return it.
     * @param detail Detail message object
     * @return PersoniumBarException
     */
    public PersoniumBarException detail(PersoniumBarException.Detail detail) {
        // 置換メッセージ作成
        String ms = MessageFormat.format(this.message, detail.getMessage());
        // 制御コードのエスケープ処理
        ms = EscapeControlCode.escape(ms);
        // メッセージ置換クローンを作成
        return new PersoniumBarException(this.code, this.path, ms);
    }

    /**
     * Class for storing error details.
     */
    public static class Detail {
        /** Error code. */
        private String code;
        /** Error message. */
        private String message;

        /**
         * Constructor.
         * @param code Error code
         */
        public Detail(String code) {
            this.code = code;
            message = PersoniumCoreMessageUtils.getMessage(code);
        }

        /**
         * Constructor.
         * @param code Error code
         * @param params Error message parameter
         */
        public Detail(String code, Object... params) {
            this.code = code;
            String ms = PersoniumCoreMessageUtils.getMessage(code);
            // 置換メッセージ作成
            ms = MessageFormat.format(message, params);
            // 制御コードのエスケープ処理
            message = EscapeControlCode.escape(ms);
        }

        /**
         * Get code.
         * @return code
         */
        public String getCode() {
            return code;
        }

        /**
         * Get message.
         * @return message
         */
        public String getMessage() {
            return message;
        }
    }
}
