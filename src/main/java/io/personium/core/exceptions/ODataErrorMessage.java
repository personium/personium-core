/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core.exceptions;

import java.util.Locale;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * OData error message.
 * Since it is a JAXB object, when it is passed to JAX-RS, XML / JSON transformation is performed appropriately according to Accept header and it is returned.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "error")
public final class ODataErrorMessage {
    /**
     * Default language tag.
     */
    public static final String DEFAULT_LANG_TAG = Locale.ENGLISH.toLanguageTag();
    @XmlElement(name = "code")
    String code;
    @XmlElement(name = "message")
    Message message;
    /**
     * Default constructor.
     * Without this, I am in trouble as JAXB
     */
    public ODataErrorMessage() {
    }

    /**
     * constructor.
     * @param paramCode code
     * @param messageLang message language tag RFC 4646 / RFC 5646?
     * @param messageValue message
     */
    public ODataErrorMessage(final String paramCode,
            final String messageLang,
            final String messageValue) {
        this.code = paramCode;
        this.message = new Message(messageLang, messageValue);
    }
    /**
     * constructor.
     * @param paramCode code
     * @param messageValue message
     */
    public ODataErrorMessage(final String paramCode,
            final String messageValue) {
        this.code = paramCode;
        this.message = new Message(DEFAULT_LANG_TAG, messageValue);
    }

    /**
     */
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlType(name = "message", propOrder = { "lang", "value" })
    static final class Message {
        @XmlElement(name = "lang")
        String lang;
        @XmlElement(name = "value")
        String value;
        /**
         * Default constructor.
         * Without this, I am in trouble as JAXB
         */
        Message() {
        }
        Message(String lang, String value) {
            this.lang = lang;
            this.value = value;
        }
    }
}
