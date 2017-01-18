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
package com.fujitsu.dc.test.unit.core.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.utils.ResourceUtils;
import com.fujitsu.dc.test.categories.Unit;

/**
 * ResourceUtilユニットテストクラス.
 */
@Category({Unit.class })
public class ResourceUtilTest {

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
}
