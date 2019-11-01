/**
 * Personium
 * Copyright 2019 Personium Project
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
package io.personium.core.rs.box;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.common.base.Charsets;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Unit;

/**
 * DavFileResource unit test class.
 */
@Category({ Unit.class })
public class DavFileResourceTest {

    /** Target class of unit test. */
    private DavFileResource davFileResource;

    /**
     * Test delete().
     * error.
     * recursiveHeader is unexpected value.
     */
    @Test
    public void put_IfNoneMatchWildcard_ShouldFail() {

        // Mock settings
        davFileResource = new DavFileResource(null, null);

        // Run method
        try {
        	InputStream is = new ByteArrayInputStream("{test:1}".getBytes(Charsets.UTF_8));
            davFileResource.put(org.apache.http.entity.ContentType.APPLICATION_JSON.toString(), null, "*", is);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreException.Dav.ETAG_MATCH;
            assertEquals(expected.getCode(), e.getCode());
        }
    }


}
