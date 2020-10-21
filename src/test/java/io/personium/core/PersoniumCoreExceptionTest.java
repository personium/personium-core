/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.core;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.plugin.base.PluginMessageUtils.Severity;
import io.personium.test.categories.Unit;


/**
 * Unit test for PersoniumCoreException class.
 */
@Category({ Unit.class })
public final class PersoniumCoreExceptionTest {

    /**
     * test for getMessage method.
     */
    @Test
    public void getMessage() {
        try {
            throw PersoniumCoreException.OData.JSON_PARSE_ERROR;
        } catch (PersoniumCoreException e) {
            Assert.assertEquals("JSON parse error.", e.getMessage());
        }
    }

    /**
     * Test for decideSeverity method.
     */
    @Test
    public void decideSeverity() {
        // 400 series should return INFO
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(400));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(401));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(405));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(412));
        Assert.assertEquals(Severity.INFO, PersoniumCoreException.decideSeverity(499));

        // 500 series should return WARN
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(500));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(502));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(505));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(512));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(599));

        // Under 400 should return WARN
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(399));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(302));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(300));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(201));
        Assert.assertEquals(Severity.WARN, PersoniumCoreException.decideSeverity(200));
    }

    /**
     * create method should throw IllegalArgumentException if invalid key is given.
     */
    @Test(expected = IllegalArgumentException.class)
    public void create_ShouldThrow_IllegalArgumentException_When_InvalidKey_Given() {
        PersoniumCoreException.create("UNKNOWN");
    }
}
