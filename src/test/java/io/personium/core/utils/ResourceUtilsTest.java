/**
 * personium.io
 * Copyright 2017 FUJITSU LIMITED
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
package io.personium.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Unit;

/**
 * Unit Test class for ResourceUtils.
 */
@Category({ Unit.class })
public class ResourceUtilsTest {

    /**
     * ContentLength1バイト以上かつTransferEncodingありの場合にtrueとなること.
     */
    @Test
    public void ContentLength1バイト以上かつTransferEncodingありの場合にtrueとなること() {
        Long contentLength = 1L;
        String transferEncodeing = "chunked";
        assertTrue(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * ContentLength1バイト以上かつTransferEncodingなしの場合にtrueとなること.
     */
    @Test
    public void ContentLength1バイト以上かつTransferEncodingなしの場合にtrueとなること() {
        Long contentLength = 1L;
        String transferEncodeing = null;
        assertTrue(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * ContentLength0バイトかつTransferEncodingありの場合にtrueとなること.
     */
    @Test
    public void ContentLength0バイトかつTransferEncodingありの場合にtrueとなること() {
        Long contentLength = 0L;
        String transferEncodeing = "chunked";
        assertTrue(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * ContentLength0バイトかつTransferEncodingなしの場合にfalseとなること.
     */
    @Test
    public void ContentLength0バイトかつTransferEncodingなしの場合にfalseとなること() {
        Long contentLength = 0L;
        String transferEncodeing = null;
        assertFalse(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * ContentLengthなしかつTransferEncodingありの場合にtrueとなること.
     */
    @Test
    public void ContentLengthなしかつTransferEncodingありの場合にtrueとなること() {
        Long contentLength = null;
        String transferEncodeing = "chunked";
        assertTrue(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * ContentLengthなしかつTransferEncodingなしの場合にtrueとなること.
     */
    @Test
    public void ContentLengthなしかつTransferEncodingなしの場合にtrueとなること() {
        Long contentLength = null;
        String transferEncodeing = null;
        assertFalse(ResourceUtils.hasApparentlyRequestBody(contentLength, transferEncodeing));
    }

    /**
     * validateXPersoniumRequestKey_When_NullGiven_Then_ShouldReturn_DefaultPattern.
     */
    @Test
    public void validateXPersoniumRequestKey_When_NullGiven_Then_ShouldReturn_DefaultPattern() {
        String result = ResourceUtils.validateXPersoniumRequestKey(null);
        assertEquals(23, result.length());
        assertTrue(result.matches("^[A-Za-z0-9-_]{4}_[A-Za-z0-9-_]{18}$"));
    }

    /**
     * ヘッダに空文字を指定した場合空文字が入ること.
     */
    @Test
    public void validateXPersoniumRequestKey_Normal_key_is_empty_string() {
        String result = ResourceUtils.validateXPersoniumRequestKey("");
        assertNotNull(result);
        assertEquals(0, result.length());
    }

    /**
     * ヘッダに1文字の文字列を指定した場合正しく扱われること.
     */
    @Test
    public void validateXPersoniumRequestKey_Normal_key_is_a_char() {
        String result = ResourceUtils.validateXPersoniumRequestKey("a");
        assertEquals("a", result);
    }

    /**
     * ヘッダに最大長の文字列を指定した場合正しく扱われること.
     */
    @Test
    public void validateXPersoniumRequestKey_Normal_key_is_max_length() {
        String maxHeaderStr128 = "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 40char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 80char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 120char
                "12345678";

        String result = ResourceUtils.validateXPersoniumRequestKey(maxHeaderStr128);
        assertEquals(maxHeaderStr128, result);
    }

    /**
     * ヘッダに最大長を超えた文字列を指定した場合エラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void validateXPersoniumRequestKey_Error_key_is_too_long() {
        String maxHeaderStr128 = "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 40char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 80char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 120char
                "12345678";

        ResourceUtils.validateXPersoniumRequestKey(maxHeaderStr128 + "X");
    }

    /**
     * ヘッダに不正な文字種を指定した場合エラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void validateXPersoniumRequestKey_Error_key_has_invalid_char() {
        ResourceUtils.validateXPersoniumRequestKey("abc-012#");
    }

}
