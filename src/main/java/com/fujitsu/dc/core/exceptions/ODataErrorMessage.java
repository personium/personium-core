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
package com.fujitsu.dc.core.exceptions;

import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * ODataのエラーメッセージ.
 * JAXBオブジェクトなので、JAX-RSに渡されると、Acceptヘッダに合わせて適宜XML/JSON変形されて返される.
 */
@XmlRootElement(name = "error")
public final class ODataErrorMessage {
    /**
     * デフォルト言語タグ.
     */
    public static final String DEFAULT_LANG_TAG = Locale.ENGLISH.toLanguageTag();
    String code;
    Message message;
    /**
     * デフォルトコンストラクタ.
     * これがないとJAXBとして困る
     */
    public ODataErrorMessage() {
    }

    /**
     * コンストラクタ.
     * @param paramCode コード
     * @param messageLang メッセージの言語タグ RFC4646 / RFC 5646?
     * @param messageValue メッセージ
     */
    public ODataErrorMessage(final String paramCode,
            final String messageLang,
            final String messageValue) {
        this.code = paramCode;
        this.message = new Message(messageLang, messageValue);
    }
    /**
     * コンストラクタ.
     * @param paramCode コード
     * @param messageValue メッセージ
     */
    public ODataErrorMessage(final String paramCode,
            final String messageValue) {
        this.code = paramCode;
        this.message = new Message(DEFAULT_LANG_TAG, messageValue);
    }
    /**
     * @return the code
     */
    @XmlElement(name = "code")
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * @return the message
     */
    @XmlElement(name = "message")
    public Message getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(final String message) {
        this.message = new Message(DEFAULT_LANG_TAG, message);
    }

    /**
     */
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlType(name = "message", propOrder = { "lang", "value" })
    static final class Message {
        String lang;
        String value;
        /**
         * デフォルトコンストラクタ.
         * これがないとJAXBとして困る
         */
        Message() {
        }
        Message(String lang, String value) {
            this.lang = lang;
            this.value = value;
        }
        @XmlElement(name = "lang")
        String getLang() {
            return this.lang;
        }
        @XmlElement(name = "value")
        String getValue() {
            return this.value;
        }
    }
}
