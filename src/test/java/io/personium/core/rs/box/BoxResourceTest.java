/**
 * Personium
 * Copyright 2018-2022 Personium Project Authors
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.common.utils.CommonUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.BoxCmp;
import io.personium.core.model.BoxRsCmp;
import io.personium.core.model.Cell;
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

        return spy(new BoxResource(cell, "boxName", null, null, null));
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
                    CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
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
                    CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
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
                    CommonUtils.HttpHeaders.X_PERSONIUM_RECURSIVE);
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }
}
