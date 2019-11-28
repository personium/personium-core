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
package io.personium.core.model.lock;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.test.categories.Unit;

/**
 * AccountValidAuthnIntervalLockManager unit test class.
 */
@Category({ Unit.class })
public class AccountValidAuthnIntervalLockManagerTest {

    /**
     * before.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * after.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * Tests that create and update.
     */
    @Test
    public void create_and_update() {
        try {
            AccountValidAuthnIntervalLockManager.registLockObject("AccountId");
            AccountValidAuthnIntervalLockManager.registLockObject("AccountId");
        } catch (PersoniumCoreException e) {
            fail();
        }
    }
}
