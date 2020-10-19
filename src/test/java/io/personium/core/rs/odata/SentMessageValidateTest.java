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
package io.personium.core.rs.odata;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmSimpleType;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.SentMessage;
import io.personium.core.rs.odata.AbstractODataResource;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;

/**
 * Message送信バリデートテスト.
 */
@Category({ Unit.class })
public class SentMessageValidateTest extends AbstractODataResource {
    /**
     * コンストラクタ.
     */
    public SentMessageValidateTest() {
    }

    /**
     * InReplyToが31文字の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void InReplyToが31文字の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), "1234567890123456789012345678901"));
    }

    /**
     * InReplyToが32文字の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void InReplyToが32文字の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), "12345678901234567890123456789012"));
    }

    /**
     * InReplyToが33文字の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void InReplyToが33文字の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), "123456789012345678901234567890123"));
    }

    /**
     * InReplyToがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void InReplyToがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(SentMessage.P_IN_REPLY_TO.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());

    }

    /**
     * ToがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void ToがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(SentMessage.P_TO.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_TO.build(),
                SentMessage.P_TO.getName(),
                OProperties.string(SentMessage.P_TO.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * ToRelationが0文字の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToRelationが0文字の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), ""));
    }

    /**
     * ToRelationが1文字の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが1文字の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "1"));
    }

    /**
     * ToRelationが128文字の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが128文字の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), AbstractCase.STRING_LENGTH_128));
    }

    /**
     * ToRelationが129文字の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToRelationが129文字の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), AbstractCase.STRING_LENGTH_129));
    }

    /**
     * ToRelationが使用可能な文字種の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが使用可能な文字種の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "-_+:"));
    }

    /**
     * ToRelationがアンダーバー始まりの場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToRelationがアンダーバー始まりの場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "_a"));
    }

    /**
     * ToRelationがコロン始まりの場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToRelationがコロン始まりの場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), ":a"));
    }

    /**
     * ToRelationがnullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void ToRelationがnullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(SentMessage.P_TO_RELATION.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * Typeがmessageの場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがmessageの場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "message"));
    }

    /**
     * Typeがrequestの場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがrequestの場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "request"));
    }


    /**
     * Typeがsocial_messageの場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Typeがsocial_messageの場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "social.message"));
    }

    /**
     * Typeがnullの場合にmessageが返却されること.
     */
    @Test
    public final void Typeがnullの場合にmessageが返却されること() {
        OProperty<?> expected = OProperties.string(SentMessage.P_TYPE.getName(), "message");
        OProperty<?> result = this.setDefaultValue(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * Titleが0文字の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが0文字の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), ""));
    }

    /**
     * Titleが256文字の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが256文字の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), AbstractCase.STRING_LENGTH_128
                        + AbstractCase.STRING_LENGTH_128));
    }

    /**
     * Titleが257文字の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Titleが257文字の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), AbstractCase.STRING_LENGTH_128
                        + AbstractCase.STRING_LENGTH_129));
    }

    /**
     * Titleがnullの場合に空文字が返却されること.
     */
    @Test
    public final void Titleがnullの場合に空文字が返却されること() {
        OProperty<?> expected = OProperties.string(SentMessage.P_TITLE.getName(), "");
        OProperty<?> result = this.setDefaultValue(SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * Bodyがnullの場合に空文字が返却されること.
     */
    @Test
    public final void Bodyがnullの場合に空文字が返却されること() {
        OProperty<?> expected = OProperties.string(SentMessage.P_BODY.getName(), "");
        OProperty<?> result = this.setDefaultValue(SentMessage.P_BODY.build(),
                SentMessage.P_BODY.getName(),
                OProperties.string(SentMessage.P_BODY.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * Priorityが0の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Priorityが0の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 0));
    }

    /**
     * Priorityが1の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが1の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 1));
    }

    /**
     * Priorityが2の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが2の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 2));
    }

    /**
     * Priorityが3の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが3の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 3));
    }

    /**
     * Priorityが4の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが4の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 4));
    }

    /**
     * Priorityが5の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが5の場合にPersoniumCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 5));
    }

    /**
     * Priorityが6の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Priorityが6の場合にPersoniumCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 6));
    }

    /**
     * Priorityがnullの場合に3が発生すること.
     */
    @Test
    public final void Priorityがnullの場合に3が発生すること() {
        OProperty<?> expected = OProperties.int32(SentMessage.P_PRIORITY.getName(), 3);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.string(SentMessage.P_PRIORITY.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());
    }
}
