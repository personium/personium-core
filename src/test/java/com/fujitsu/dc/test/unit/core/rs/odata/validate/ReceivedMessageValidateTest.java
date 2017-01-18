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
import com.fujitsu.dc.core.model.ctl.ReceivedMessage;
import com.fujitsu.dc.core.rs.cell.MessageODataResource;
import com.fujitsu.dc.core.rs.odata.AbstractODataResource;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;

/**
 * Messageバリデートテスト.
 */
@Category({ Unit.class })
public class ReceivedMessageValidateTest extends AbstractODataResource {
    /**
     * コンストラクタ.
     */
    public ReceivedMessageValidateTest() {
    }

    /**
     * idが31文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void idが31文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_ID.build(),
                ReceivedMessage.P_ID.getName(),
                OProperties.string(ReceivedMessage.P_ID.getName(), "1234567890123456789012345678901"));
    }

    /**
     * idが32文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void idが32文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_ID.build(),
                ReceivedMessage.P_ID.getName(),
                OProperties.string(ReceivedMessage.P_ID.getName(), "12345678901234567890123456789012"));
    }

    /**
     * idが33文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void idが33文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_ID.build(),
                ReceivedMessage.P_ID.getName(),
                OProperties.string(ReceivedMessage.P_ID.getName(), "123456789012345678901234567890123"));
    }

    /**
     * idがNullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void idがNullの場合にDcCoreExceptionが発生すること() {
        this.setDefaultValue(ReceivedMessage.P_ID.build(),
                ReceivedMessage.P_ID.getName(),
                OProperties.string(ReceivedMessage.P_ID.getName(), ""));
    }

    /**
     * InReplyToが31文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void InReplyToが31文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_IN_REPLY_TO.build(),
                ReceivedMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(ReceivedMessage.P_IN_REPLY_TO.getName(), "1234567890123456789012345678901"));
    }

    /**
     * InReplyToが32文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void InReplyToが32文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_IN_REPLY_TO.build(),
                ReceivedMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(ReceivedMessage.P_IN_REPLY_TO.getName(), "12345678901234567890123456789012"));
    }

    /**
     * InReplyToが33文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void InReplyToが33文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_IN_REPLY_TO.build(),
                ReceivedMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(ReceivedMessage.P_IN_REPLY_TO.getName(), "123456789012345678901234567890123"));
    }

    /**
     * InReplyToがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void InReplyToがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(ReceivedMessage.P_IN_REPLY_TO.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(ReceivedMessage.P_IN_REPLY_TO.build(),
                ReceivedMessage.P_IN_REPLY_TO.getName(),
                OProperties.string(ReceivedMessage.P_IN_REPLY_TO.getName(), ""));
        assertEquals(expected.getValue(), result.getValue());

    }

    /**
     * FromがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void FromがURL形式の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_FROM.build(),
                ReceivedMessage.P_FROM.getName(),
                OProperties.string(ReceivedMessage.P_FROM.getName(), "http://example.com/test"));
    }

    /**
     * FromがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void FromがURL形式でない場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_FROM.build(),
                ReceivedMessage.P_FROM.getName(),
                OProperties.string(ReceivedMessage.P_FROM.getName(), "ftp://example.com/test"));
    }

    /**
     * FromがNullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void FromがNullの場合にDcCoreExceptionが発生すること() {
        this.setDefaultValue(ReceivedMessage.P_FROM.build(),
                ReceivedMessage.P_FROM.getName(),
                OProperties.string(ReceivedMessage.P_FROM.getName(), ""));
    }

    /**
     * Typeがmessageの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがmessageの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_TYPE.build(),
                ReceivedMessage.P_TYPE.getName(),
                OProperties.string(ReceivedMessage.P_TYPE.getName(), "message"));
    }

    /**
     * Typeがreq.relation.buildの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_buildeの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_TYPE.build(),
                ReceivedMessage.P_TYPE.getName(),
                OProperties.string(ReceivedMessage.P_TYPE.getName(), "req.relation.build"));
    }

    /**
     * Typeがreq.relation.breakの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_breakの場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_TYPE.build(),
                ReceivedMessage.P_TYPE.getName(),
                OProperties.string(ReceivedMessage.P_TYPE.getName(), "req.relation.break"));
    }

    /**
     * Typeがsocial_messageの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがsocial_messageの場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_TYPE.build(),
                ReceivedMessage.P_TYPE.getName(),
                OProperties.string(ReceivedMessage.P_TYPE.getName(), "social.message"));
    }

    /**
     * Typeがnullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがnullの場合にDcCoreExceptionが発生すること() {
        this.setDefaultValue(ReceivedMessage.P_TYPE.build(),
                ReceivedMessage.P_TYPE.getName(),
                OProperties.string(ReceivedMessage.P_TYPE.getName(), ""));
    }

    /**
     * Titleが0文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが0文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                ReceivedMessage.P_TITLE.build(),
                ReceivedMessage.P_TITLE.getName(),
                OProperties.string(ReceivedMessage.P_TITLE.getName(), ""));
    }

    /**
     * Titleが256文字の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Titleが256文字の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(
                ReceivedMessage.P_TITLE.build(),
                ReceivedMessage.P_TITLE.getName(),
                OProperties.string(ReceivedMessage.P_TITLE.getName(), AbstractCase.STRING_LENGTH_128
                        + AbstractCase.STRING_LENGTH_128));
    }

    /**
     * Titleが257文字の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Titleが257文字の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                ReceivedMessage.P_TITLE.build(),
                ReceivedMessage.P_TITLE.getName(),
                OProperties.string(ReceivedMessage.P_TITLE.getName(), AbstractCase.STRING_LENGTH_128
                        + AbstractCase.STRING_LENGTH_129));
    }

    /**
     * Titleがnullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Titleがnullの場合にDcCoreExceptionが発生すること() {
        this.setDefaultValue(ReceivedMessage.P_TITLE.build(),
                ReceivedMessage.P_TITLE.getName(),
                OProperties.string(ReceivedMessage.P_TITLE.getName(), ""));
    }

    /**
     * Priorityが0の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Priorityが0の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 0));
    }

    /**
     * Priorityが1の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが1の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 1));
    }

    /**
     * Priorityが2の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが2の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 2));
    }

    /**
     * Priorityが3の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが3の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 3));
    }

    /**
     * Priorityが4の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが4の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 4));
    }

    /**
     * Priorityが5の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Priorityが5の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 5));
    }

    /**
     * Priorityが6の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Priorityが6の場合にDcCoreExceptionが発生すること() {
        this.validateProperty(
                ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.int32(ReceivedMessage.P_PRIORITY.getName(), 6));
    }

    /**
     * Priorityがnullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Priorityがnullの場合にDcCoreExceptionが発生すること() {
        this.setDefaultValue(ReceivedMessage.P_PRIORITY.build(),
                ReceivedMessage.P_PRIORITY.getName(),
                OProperties.string(ReceivedMessage.P_PRIORITY.getName(), ""));
    }

    /**
     * RequestRelationがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void RequestRelationがURL形式の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_REQUEST_RELATION.build(),
                ReceivedMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(ReceivedMessage.P_REQUEST_RELATION.getName(), "http://example.com/test"));
    }

    /**
     * RequestRelationがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void RequestRelationがURL形式でない場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_REQUEST_RELATION.build(),
                ReceivedMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(ReceivedMessage.P_REQUEST_RELATION.getName(), "ftp://example.com/test"));
    }

    /**
     * RequestRelationがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void RequestRelationがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(ReceivedMessage.P_REQUEST_RELATION.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(ReceivedMessage.P_REQUEST_RELATION.build(),
                ReceivedMessage.P_REQUEST_RELATION.getName(),
                OProperties.string(ReceivedMessage.P_REQUEST_RELATION.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * RequestRelationTargetがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void RequestRelationTargetがURL形式の場合にDcCoreExceptionが発生しないこと() {
        this.validateProperty(ReceivedMessage.P_REQUEST_RELATION_TARGET.build(),
                ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "http://example.com/test"));
    }

    /**
     * RequestRelationTargetがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void RequestRelationTargetがURL形式でない場合にDcCoreExceptionが発生すること() {
        this.validateProperty(ReceivedMessage.P_REQUEST_RELATION_TARGET.build(),
                ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(), "ftp://example.com/test"));
    }

    /**
     * RequestRelationTargetがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void RequestRelationTargetがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(),
                EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(ReceivedMessage.P_REQUEST_RELATION_TARGET.build(),
                ReceivedMessage.P_REQUEST_RELATION_TARGET.getName(),
                OProperties.string(ReceivedMessage.P_MULTICAST_TO.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * MulticastToがURL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void MulticastToがURL形式の場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), "http://example.com/test");
    }

    /**
     * MulticastToがCSV複数URL形式の場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void MulticastToがCSV複数URL形式の場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(),
                "http://example.com/test,http://example.com/test");
    }

    /**
     * MulticastToがURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void MulticastToがURL形式でない場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(), "ftp://example.com/test");

    }

    /**
     * MulticastToがCSV複数URL形式とURL形式でない場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void MulticastToがCSV複数URL形式とURL形式でない場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(),
                "http://example.com/test,ftp://example.com/test");
    }

    /**
     * MulticastToが不正なCSV形式の場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void MulticastToが不正なCSV形式の場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateUriCsv(ReceivedMessage.P_MULTICAST_TO.getName(),
                "http://example.com/test,,http://example.com/test");
    }

    /**
     * MulticastToがNullの場合にNullOPropertyが返却されること.
     */
    @Test
    public final void MulticastToがNullの場合にNullOPropertyが返却されること() {
        OProperty<?> expected = OProperties.null_(ReceivedMessage.P_MULTICAST_TO.getName(), EdmSimpleType.STRING);
        OProperty<?> result = this.setDefaultValue(ReceivedMessage.P_MULTICAST_TO.build(),
                ReceivedMessage.P_MULTICAST_TO.getName(),
                OProperties.string(ReceivedMessage.P_MULTICAST_TO.getName(), AbstractCase.STRING_LENGTH_129));
        assertEquals(expected.getValue(), result.getValue());
    }

    /**
     * TypeがmessageでStatusがunreadの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void TypeがmessageでStatusがunreadの場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateStatus("message", "unread");
    }

    /**
     * TypeがmessageでStatusがnoneの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void TypeがmessageでStatusがnoneの場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateStatus("message", "none");
    }

    /**
     * Typeがreq.relation.buildでStatusがnoneの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_buildでStatusがnoneの場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateStatus("req.relation.build", "none");
    }

    /**
     * Typeがreq.relation.breakでStatusがnoneの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_breakでStatusがnoneの場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateStatus("req.relation.break", "none");
    }

    /**
     * Typeがreq.relation.buildでStatusがunreadの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがreq_relation_buildでStatusがunreadの場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateStatus("req.relation.build", "unread");
    }

    /**
     * Typeがreq.relation.breakでStatusがunreadの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがreq_relation_breakでStatusがunreadの場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateStatus("req.relation.break", "unread");
    }

    /**
     * Typeがreq.relation.breakでRequestRelationとRequestRelationTargetの指定がある場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void Typeがreq_relation_breakでRequestRelationとRequestRelationTargetの指定がある場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateReqRelation("req.relation.break", "http://xxx.com/xx", "http://xxx.com/xx");
    }

    /**
     * Typeがreq.relation.breakでRequestRelationがnullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがreq_relation_breakでRequestRelationがnullの場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateReqRelation("req.relation.break", null, "http://example.com/test");
    }

    /**
     * Typeがreq.relation.breakでRequestRelationTargetがnullの場合にDcCoreExceptionが発生すること.
     */
    @Test(expected = DcCoreException.class)
    public final void Typeがreq_relation_breakでRequestRelationTargetがnullの場合にDcCoreExceptionが発生すること() {
        MessageODataResource.validateReqRelation("req.relation.break", "http://example.com/test", null);
    }

    /**
     * TypeがmessageでRequestRelationがnullの場合にDcCoreExceptionが発生しないこと.
     */
    @Test
    public final void TypeがmessageでRequestRelationがnullの場合にDcCoreExceptionが発生しないこと() {
        MessageODataResource.validateReqRelation("message", null, "http://example.com/test");
    }

}
