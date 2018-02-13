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
package io.personium.core.event;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for PersoniumEvent.
 */
@Category({ Unit.class })
public class PersoniumEventTest {

    /**
     * Test getDateTimeRFC1123().
     * Normal test.
     */
    @Test
    public void getDateTimeRFC1123_Normal() {
        // --------------------
        // Test method args
        // --------------------
        PersoniumEvent event = new PersoniumEvent();
        String time = "2018-02-13T15:55:00.111Z";
        event.setDateTime(time);

        // --------------------
        // Expected result
        // --------------------
        String expected = "Tue, 13 Feb 2018 15:55:00 GMT";

        // --------------------
        // Run method
        // --------------------
        String ret = event.getDateTimeRFC1123();

        // --------------------
        // Confirm result
        // --------------------
        assertThat(ret, is(expected));
    }

}
