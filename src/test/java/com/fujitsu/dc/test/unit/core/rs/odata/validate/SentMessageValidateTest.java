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
package com.fujitsu.dc.test.unit.core.rs.odata.validate;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmSimpleType;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.model.ctl.ReceivedMessage;
import com.fujitsu.dc.core.model.ctl.SentMessage;
import com.fujitsu.dc.core.rs.cell.MessageODataResource;
import com.fujitsu.dc.core.rs.odata.AbstractODataResource;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;

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
     * InReplyToが31文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void InReplyToが31文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), "1234567890123456789012345678901"));
    }

    /**
     * InReplyToが32文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void InReplyToが32文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_IN_REPLY_TO.build(),
                SentMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(SentMessage.P_IN_REPLY_TO.getName(), "12345678901234567890123456789012"));
    }

    /**
     * InReplyToが33文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void InReplyToが33文字の場合にDcCoreExceptionが発生すること() {
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
     * ToがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがURL形式の場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(), "http://example.com/test");
    }

    /**
     * ToがCSV複数URL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがCSV複数URL形式の場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test,http://example.com/test");
    }

    /**
     * ToがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToがURL形式でない場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(), "ftp://example.com/test");

    }

    /**
     * ToがCSV複数URL形式とURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToがCSV複数URL形式とURL形式でない場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test,ftp://example.com/test");
    }

    /**
     * Toが不正なCSV形式の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Toが不正なCSV形式の場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test,,http://example.com/test");
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
     * ToRelationが0文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToRelationが0文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), ""));
    }

    /**
     * ToRelationが1文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが1文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "1"));
    }

    /**
     * ToRelationが128文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが128文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), AbstractCase.STRING_LENGTH_128));
    }

    /**
     * ToRelationが129文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToRelationが129文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), AbstractCase.STRING_LENGTH_129));
    }

    /**
     * ToRelationが使用可能な文字種の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToRelationが使用可能な文字種の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "-_+:"));
    }

    /**
     * ToRelationがアンダーバー始まりの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToRelationがアンダーバー始まりの場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_TO_RELATION.build(),
                SentMessage.P_TO_RELATION.getName(),
                OProperties.string(SentMessage.P_TO_RELATION.getName(), "_a"));
    }

    /**
     * ToRelationがコロン始まりの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToRelationがコロン始まりの場合にDcCoreExceptionが発生すること() {
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
     * Typeがmessageの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがmessageの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "message"));
    }

    /**
     * Typeがreq.relation.buildの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_buildの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "req.relation.build"));
    }

    /**
     * Typeがreq.relation.breakの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_breakの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_TYPE.build(),
                SentMessage.P_TYPE.getName(),
                OProperties.string(SentMessage.P_TYPE.getName(), "req.relation.break"));
    }

    /**
     * Typeがsocial_messageの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがsocial_messageの場合にDcCoreExceptionが発生すること() {
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
     * Titleが0文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが0文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), ""));
    }

    /**
     * Titleが256文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが256文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                SentMessage.P_TITLE.build(),
                SentMessage.P_TITLE.getName(),
                OProperties.string(SentMessage.P_TITLE.getName(), AbstractCase.STRING_LENGTH_128
                        + AbstractCase.STRING_LENGTH_128));
    }

    /**
     * Titleが257文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Titleが257文字の場合にDcCoreExceptionが発生すること() {
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
     * Bodyが0byteの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Bodyが0byteの場合にDcCoreExceptionが発生しないこと() {
        String body = "";
        MessageODataResource.validateBody(body, Common.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Bodyが64Kbyteの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Bodyが64Kbyteの場合にDcCoreExceptionが発生しないこと() {
        char[] buff = new char[65536];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = 0x41;
        }
        String body = String.valueOf(buff);

        MessageODataResource.validateBody(body, Common.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Bodyが64Kbyteを超える場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Bodyが64Kbyteを超える場合にDcCoreExceptionが発生すること() {
        char[] buff = new char[65537];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = 0x41;
        }
        String body = String.valueOf(buff);

        MessageODataResource.validateBody(body, Common.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Priorityが0の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Priorityが0の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 0));
    }

    /**
     * Priorityが1の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが1の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 1));
    }

    /**
     * Priorityが2の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが2の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 2));
    }

    /**
     * Priorityが3の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが3の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 3));
    }

    /**
     * Priorityが4の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが4の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 4));
    }

    /**
     * Priorityが5の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが5の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_PRIORITY.build(),
                SentMessage.P_PRIORITY.getName(),
                OProperties.int32(SentMessage.P_PRIORITY.getName(), 5));
    }

    /**
     * Priorityが6の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Priorityが6の場合にDcCoreExceptionが発生すること() {
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

    /**
     * RequestRelationがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void RequestRelationがURL形式の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_REQUEST_RELATION.build(),
                SentMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION.getName(), "http://example.com/test"));
    }

    /**
     * RequestRelationがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void RequestRelationがURL形式でない場合にDcCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_REQUEST_RELATION.build(),
                SentMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION.getName(), "ftp://example.com/test"));
    }

    /**
     * RequestRelationがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void RequestRelationがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(SentMessage.P_REQUEST_RELATION.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_REQUEST_RELATION.build(),
                SentMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * RequestRelationTargetがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void RequestRelationTargetがURL形式の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(SentMessage.P_REQUEST_RELATION_TARGET.build(),
                SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION_TARGET.getName(), "http://example.com/test"));
    }

    /**
     * RequestRelationTargetがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void RequestRelationTargetがURL形式でない場合にDcCoreExceptionが発生すること() {
        this.validateProperty(SentMessage.P_REQUEST_RELATION_TARGET.build(),
                SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION_TARGET.getName(), "ftp://example.com/test"));
    }

    /**
     * RequestRelationTargetがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void RequestRelationTargetがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(SentMessage.P_REQUEST_RELATION_TARGET.build(),
                SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(SentMessage.P_REQUEST_RELATION_TARGET.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * ToもToRelationも存在しない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void ToもToRelationも存在しない場合にDcCoreExceptionが発生すること() {
        String to = (String) OProperties.null_(SentMessage.P_TO.getName(),
                EdmSimpleType.STRING).getValue();
        String toRelation = (String) OProperties.null_(SentMessage.P_TO_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        MessageODataResource.validateToAndToRelation(to, toRelation);
    }

    /**
     * ToがあってToRelationがない場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがあってToRelationがない場合にDcCoreExceptionが発生しないこと() {
        String to = "http://example.com/toAddress";
        String toRelation = (String) OProperties.null_(SentMessage.P_TO_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        MessageODataResource.validateToAndToRelation(to, toRelation);
    }

    /**
     * ToがなくてToRelationがある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがなくてToRelationがある場合にDcCoreExceptionが発生しないこと() {
        String to = (String) OProperties.null_(SentMessage.P_TO.getName(),
                EdmSimpleType.STRING).getValue();
        String toRelation = "http://example.com/toRelation";
        MessageODataResource.validateToAndToRelation(to, toRelation);
    }

    /**
     * ToとToRelationが両方ある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToとToRelationが両方ある場合にDcCoreExceptionが発生しないこと() {
        String to = "http://example.com/toAddress";
        String toRelation = "http://example.com/toRelation";
        MessageODataResource.validateToAndToRelation(to, toRelation);
    }

    /**
     * Typeが関係登録依頼でRequestRelationがない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeが関係登録依頼でRequestRelationがない場合にDcCoreExceptionが発生すること() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = (String) OProperties.null_(
                SentMessage.P_REQUEST_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係登録依頼でRequestRelationがある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeが関係登録依頼でRequestRelationがある場合にDcCoreExceptionが発生しないこと() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係削除依頼でRequestRelationがない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeが関係削除依頼でRequestRelationがない場合にDcCoreExceptionが発生すること() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = (String) OProperties.null_(
                SentMessage.P_REQUEST_RELATION.getName(),
                EdmSimpleType.STRING).getValue();
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係削除依頼でRequestRelationがある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeが関係削除依頼でRequestRelationがある場合にDcCoreExceptionが発生しないこと() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係登録依頼でRequestRelationTargetがない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeが関係登録依頼でRequestRelationTargetがない場合にDcCoreExceptionが発生すること() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = (String) OProperties.null_(
                SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                EdmSimpleType.STRING).getValue();
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係登録依頼でRequestRelationTargetがある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeが関係登録依頼でRequestRelationTargetがある場合にDcCoreExceptionが発生しないこと() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BUILD;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係削除依頼でRequestRelationTargetがない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeが関係削除依頼でRequestRelationTargetがない場合にDcCoreExceptionが発生すること() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = (String) OProperties.null_(
                SentMessage.P_REQUEST_RELATION_TARGET.getName(),
                EdmSimpleType.STRING).getValue();
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * Typeが関係削除依頼でRequestRelationTargetがある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeが関係削除依頼でRequestRelationTargetがある場合にDcCoreExceptionが発生しないこと() {
        String type = ReceivedMessage.TYPE_REQ_RELATION_BREAK;
        String requestRelation = "http://example.com/reqRelation";
        String requestRelationTarget = "http://example.com/reqRelation";
        MessageODataResource.validateReqRelation(type, requestRelation, requestRelationTarget);
    }

    /**
     * 送信先URLが最大送信許可数を超えている場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void 送信先URLが最大送信許可数を超えている場合にDcCoreExceptionが発生すること() {
        MessageODataResource mor = new MessageODataResource(null, null, null);
        mor.checkMaxDestinationsSize(1001);
    }

    /**
     * 送信先URLが最大送信許可数を超えていない場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void 送信先URLが最大送信許可数を超えていない場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource mor = new MessageODataResource(null, null, null);
        mor.checkMaxDestinationsSize(1000);
    }
}
