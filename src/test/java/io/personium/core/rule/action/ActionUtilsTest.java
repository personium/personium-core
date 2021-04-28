/**
 * Personium
 * Copyright 2018-2021 Personium Project Authors
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
package io.personium.core.rule.action;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.test.categories.Unit;

/**
 * Unit Test class for ActionUtils.
 */
@Category({ Unit.class })
public class ActionUtilsTest {

    /**
     * Test getUrl().
     * Normal test
     */
    @Test
    public void getUrl_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service#box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/box/col/service";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getUrl().
     * Normal test
     * fqdn base
     */
    @Test
    public void getUrl_Normal_fqdnbase() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://cell.personium/box/col/service#box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://cell.personium/box/col/service";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getUrl().
     * Normal test
     * no fragment
     */
    @Test
    public void getUrl_Normal_no_fragment() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/box/col/service";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getUrl().
     * Normal test
     * url is null
     */
    @Test
    public void getUrl_Normal_url_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String service = null;

        // --------------------
        // Expected result
        // --------------------
        String expected = null;

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getUrl().
     * Normal test
     * url is invalid
     */
    @Test
    public void getUrl_Normal_url_is_invalid() {
        // --------------------
        // Test method args
        // --------------------
        String service = "/personium/cell";

        // --------------------
        // Expected result
        // --------------------
        String expected = "/personium/cell";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test
     */
    @Test
    public void getCellUrl_Normal() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service#box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test
     * fqdn base
     */
    @Test
    public void getCellUrl_Normal_fqdnbase() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://cell.personium/box/col/service#box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://cell.personium/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test
     * no fragment
     */
    @Test
    public void getCellUrl_Normal_no_fragment() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test
     * service with query
     */
    @Test
    public void getCellUrl_Normal_service_with_query() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service?param=hoge#box/col/service";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test
     * service with key
     */
    @Test
    public void getCellUrl_Normal_service_with_key() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/cell/box/col/service('key')#box/col/service('key')";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/cell/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

    /**
     * Test getCellUrl().
     * Normal test.
     * service is invalid.
     */
    @Test
    public void getCellUrl_Normal_service_is_invalid() {
        // --------------------
        // Test method args
        // --------------------
        String service = "http://personium/";

        // --------------------
        // Expected result
        // --------------------
        String expected = "http://personium/";

        // --------------------
        // Mock settings
        // --------------------

        // --------------------
        // Run method
        // --------------------
        String result = ActionUtils.getCellUrl(service);

        // --------------------
        // Confirm result
        // --------------------
        assertThat(result, is(expected));
    }

}
