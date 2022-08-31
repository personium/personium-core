/**
 * Personium
 * Copyright 2020-2022 Personium Project Authors
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
package io.personium.core.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for DavCommon.
 */
@Category({ Unit.class })
public class DavCommonTest {
    static Logger log = LoggerFactory.getLogger(CellTest.class);

    @Test
    public void testIsValidResourceName() throws UnsupportedEncodingException {
        // Testing various UTF-8 Chars
        assertTrue(DavCommon.isValidResourceName("abc.def"));
        assertTrue(DavCommon.isValidResourceName("château_café.txt")); // French
        assertTrue(DavCommon.isValidResourceName("マリオの被害状況.xlsx")); // Japanese
        assertTrue(DavCommon.isValidResourceName("명동주변.xml")); // Korean
        assertTrue(DavCommon.isValidResourceName("สูตรผัดไทย.pptx")); // Thai
        assertFalse(DavCommon.isValidResourceName("abc?|rm *")); // Invalid Chars

        String ngString = null;
        String okString = null;
        // Testing for escaped chars
        //  see:  https://www.charset.org/utf-8
        ngString = URLDecoder.decode("%0E%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF", "UTF-8"); // 0x0E Shift out
        log.info(ngString);
        assertFalse(DavCommon.isValidResourceName(ngString)); // Invalid since it includes control chars

        ngString = URLDecoder.decode("abc%C2%8Bd", "UTF-8");   //  0xC28B;  Control character: Partial Line Forward
        log.info(ngString);
        assertFalse(DavCommon.isValidResourceName(ngString)); // Invalid since it includes control chars

        okString = URLDecoder.decode("%C2%A3300", "UTF-8");   //   0xC2A3:  ￡ Pound Sign
        log.info(okString);
        assertTrue(DavCommon.isValidResourceName(okString)); //  Valid since the pound sigin is not a control char
    }
}
