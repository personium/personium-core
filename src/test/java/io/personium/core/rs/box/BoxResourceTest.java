/**
 * personium.io
 * Copyright 2018 FUJITSU LIMITED
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

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Link.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
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
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
import io.personium.core.model.DavCmp;
import io.personium.core.model.ModelFactory;
import io.personium.test.categories.Unit;

/**
 * BoxResource unit test classs.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BoxResource.class, ModelFactory.class })
@Category({ Unit.class })
public class BoxResourceTest {

    /** Target class of unit test. */
    private BoxResource boxResource;

    /**
     * Create and return spy BoxResource.
     * @return spy BoxResource
     * @throws Exception Unexpected error.
     */
    private BoxResource createSpyBoxResource() throws Exception {
        // Mock settings
        Cell cell = mock(Cell.class);
        Box box = mock(Box.class);
        doReturn(box).when(cell).getBoxForName("boxName");
        BoxCmp boxCmp = mock(BoxCmp.class);
        PowerMockito.mockStatic(ModelFactory.class);
        PowerMockito.doReturn(boxCmp).when(ModelFactory.class, "boxCmp", anyObject());
        PowerMockito.whenNew(BoxRsCmp.class).withAnyArguments().thenReturn(null);

        return spy(new BoxResource(cell, "boxName", null, null, null, null));
    }

    /**
     * Test recursiveDelete().
     * normal.
     * @throws Exception Unexpected error.
     */
    @Test
    public void recursiveDelete_Normal() throws Exception {
        // Test method args
        String recursiveHeader = "true";

        // Mock settings
        boxResource = createSpyBoxResource();
        BoxRsCmp boxRsCmp = mock(BoxRsCmp.class);
        boxResource.boxRsCmp = boxRsCmp;

        AccessContext accessContext = mock(AccessContext.class);
        doReturn(accessContext).when(boxRsCmp).getAccessContext();
        doNothing().when(boxRsCmp).checkAccessContext(anyObject(), anyObject());

        DavCmp davCmp = mock(DavCmp.class);
        doReturn(davCmp).when(boxRsCmp).getDavCmp();
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
            @Override
            public boolean bufferEntity() {
                return false;
            }
            @Override
            public void close() {
            }
            @Override
            public Set<String> getAllowedMethods() {
                return null;
            }
            @Override
            public Map<String, NewCookie> getCookies() {
                return null;
            }
            @Override
            public Date getDate() {
                return null;
            }
            @Override
            public EntityTag getEntityTag() {
                return null;
            }
            @Override
            public String getHeaderString(String arg0) {
                return null;
            }
            @Override
            public Locale getLanguage() {
                return null;
            }
            @Override
            public Date getLastModified() {
                return null;
            }
            @Override
            public int getLength() {
                return 0;
            }
            @Override
            public Link getLink(String arg0) {
                return null;
            }
            @Override
            public Builder getLinkBuilder(String arg0) {
                return null;
            }
            @Override
            public Set<Link> getLinks() {
                return null;
            }
            @Override
            public URI getLocation() {
                return null;
            }
            @Override
            public MediaType getMediaType() {
                return null;
            }
            @Override
            public StatusType getStatusInfo() {
                return null;
            }
            @Override
            public MultivaluedMap<String, String> getStringHeaders() {
                return null;
            }
            @Override
            public boolean hasEntity() {
                return false;
            }
            @Override
            public boolean hasLink(String arg0) {
                return false;
            }
            @Override
            public <T> T readEntity(Class<T> arg0) {
                return null;
            }
            @Override
            public <T> T readEntity(GenericType<T> arg0) {
                return null;
            }
            @Override
            public <T> T readEntity(Class<T> arg0, Annotation[] arg1) {
                return null;
            }
            @Override
            public <T> T readEntity(GenericType<T> arg0, Annotation[] arg1) {
                return null;
            }
        };
        doReturn(res).when(builder).build();

        // Expected result
        Response expected = res;

        // Run method
        Response actual = boxResource.recursiveDelete(recursiveHeader);

        // Confirm result
        assertThat(actual.getStatus(), is(expected.getStatus()));
    }

    /**
     * Test recursiveDelete().
     * error.
     * recursiveHeader is unexpected value.
     * @throws Exception Unexpected error.
     */
    @Test
    public void recursiveDelete_Error_recursiveHeader_is_unexpected() throws Exception {
        // Test method args
        String recursiveHeader = "foo";

        // Mock settings
        boxResource = createSpyBoxResource();

        // Expected result
        // None.

        // Run method
        try {
            boxResource.recursiveDelete(recursiveHeader);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test recursiveDelete().
     * error.
     * recursiveHeader is false.
     * @throws Exception Unexpected error.
     */
    @Test
    public void recursiveDelete_Error_recursiveHeader_is_false() throws Exception {
        // Test method args
        String recursiveHeader = "false";

        // Mock settings
        boxResource = createSpyBoxResource();

        // Expected result
        // None.

        // Run method
        try {
            boxResource.recursiveDelete(recursiveHeader);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test recursiveDelete().
     * error.
     * recursiveHeader is null.
     * @throws Exception Unexpected error.
     */
    @Test
    public void recursiveDelete_Error_recursiveHeader_is_null() throws Exception {
        // Test method args
        String recursiveHeader = null;

        // Mock settings
        boxResource = createSpyBoxResource();

        // Expected result
        // None.

        // Run method
        try {
            boxResource.recursiveDelete(recursiveHeader);
            fail("Not throws exception.");
        } catch (PersoniumCoreException e) {
            // Confirm result
            PersoniumCoreException expected = PersoniumCoreException.Misc.PRECONDITION_FAILED.params(
                    PersoniumCoreUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }
}
