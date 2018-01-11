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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for UriUtils.
 */
@Category({ Unit.class })
public class UriUtilsTest {

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url starts with uniturl.
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_starts_with_uniturl() {
        String actual = UriUtils.convertSchemeFromHttpToLocalUnit("http://uniturl/", "http://uniturl/cell");
        assertThat(actual, is("personium-localunit:/cell"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url not starts with uniturl.
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_not_starts_with_uniturl() {
        String actual = UriUtils.convertSchemeFromHttpToLocalUnit("http://uniturl/", "http://otheruniturl/cell");
        assertThat(actual, is("http://otheruniturl/cell"));
    }

    /**
     * Test convertSchemeFromHttpToLocalUnit().
     * normal.
     * url is null.
     */
    @Test
    public void convertSchemeFromHttpToLocalUnit_Normal_url_is_null() {
        String actual = UriUtils.convertSchemeFromHttpToLocalUnit("http://uniturl/", null);
        assertNull(actual);
    }
}
