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
package io.personium.core.rs.box;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.model.DavCmp;
import io.personium.core.model.DavRsCmp;
import io.personium.test.categories.Unit;

/**
 * DavCollectionResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AccessContext.class})
@Category({ Unit.class })
public class DavCollectionResourceTest {

    /** Target class of unit test. */
    private DavCollectionResource davCollectionResource;

    /**
     * Test delete().
     * normal.
     */
    @Test
    public void delete_Normal() {
        // Test method args
        String recursiveHeader = "true";

        // Mock settings
        davCollectionResource = spy(new DavCollectionResource(null, null));
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        davCollectionResource.davRsCmp = davRsCmp;

        doReturn(PowerMockito.mock(AccessContext.class)).when(davRsCmp).getAccessContext();
        DavRsCmp parent = mock(DavRsCmp.class);
        doReturn(parent).when(davRsCmp).getParent();
        doNothing().when(parent).checkAccessContext(anyObject(), anyObject());

        DavCmp davCmp = mock(DavCmp.class);
        doReturn(davCmp).when(davRsCmp).getDavCmp();
        doReturn(false).when(davCmp).isEmpty();

        ResponseBuilder builder = mock(ResponseBuilder.class);
        doReturn(builder).when(davCmp).delete(null, true);
        Response res = new Response() {
            public int getStatus() {
                return 100;
            }
            public MultivaluedMap<String, Object> getMetadata() {
                return null;
            }
            public Object getEntity() {
                return null;
            }
        };
        doReturn(res).when(builder).build();

        // Expected result
        Response expected = res;

        // Run method
        Response actual = davCollectionResource.delete(recursiveHeader);

        // Confirm result
        assertThat(actual.getStatus(), is(expected.getStatus()));
    }

    /**
     * Test delete().
     * error.
     * recursiveHeader is unexpected value.
     */
    @Test
    public void delete_Error_recursiveHeader_is_unexpected() {
        // Test method args
        String recursiveHeader = "foo";

        // Mock settings
        davCollectionResource = spy(new DavCollectionResource(null, null));

        // Expected result
        // None.

        // Run method
        try {
            davCollectionResource.delete(recursiveHeader);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreException.Dav.INVALID_REQUEST_HEADER.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE, recursiveHeader);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test delete().
     * error.
     * recursiveHeader is false.
     * davCmp is not empty.
     */
    @Test
    public void delete_Error_recursiveHeader_is_false_davCmp_not_empty() {
        // Test method args
        String recursiveHeader = "false";

        // Mock settings
        davCollectionResource = spy(new DavCollectionResource(null, null));
        DavRsCmp davRsCmp = mock(DavRsCmp.class);
        davCollectionResource.davRsCmp = davRsCmp;

        doReturn(PowerMockito.mock(AccessContext.class)).when(davRsCmp).getAccessContext();
        DavRsCmp parent = mock(DavRsCmp.class);
        doReturn(parent).when(davRsCmp).getParent();
        doNothing().when(parent).checkAccessContext(anyObject(), anyObject());

        DavCmp davCmp = mock(DavCmp.class);
        doReturn(davCmp).when(davRsCmp).getDavCmp();
        doReturn(false).when(davCmp).isEmpty();

        // Expected result
        // None.

        // Run method
        try {
            davCollectionResource.delete(recursiveHeader);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            assertThat(e.getCode(), is(PersoniumCoreException.Dav.HAS_CHILDREN.getCode()));
        }
    }
}
