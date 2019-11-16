/**
 * personium.io
 * Copyright 2019 FUJITSU LIMITED
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumUnitConfig;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.PersoniumIntegTestRunner;

/**
 * AccountLockManager unit test class.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({ Unit.class })
public class AccountLockManagerTest {

    /**
     * before class.
     */
    @BeforeClass
    public static void beforeClass() {
        AccountLockManager.accountLockCount = 5;
        AccountLockManager.accountLockTime = 1;
    }

    /**
     * after class.
     */
    @AfterClass
    public static void afterClass() {
        AccountLockManager.accountLockCount = PersoniumUnitConfig.getAccountLockCount();
        AccountLockManager.accountLockTime = PersoniumUnitConfig.getAccountLockTime();
    }

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
     * Tests that countup failed count.
     */
    @Test
    public void countup_failed_count() {
        // before coun up.(If there is no such parameter, it is -1)
        assertThat(AccountLockManager.getFailedCount("account_1"), is(-1L));

        // countup.
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.getFailedCount("account_1"), is(1L));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.getFailedCount("account_1"), is(2L));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.getFailedCount("account_1"), is(3L));

        // countup ohter account failed count.
        AccountLockManager.countupFailedCount("account_2");
        assertThat(AccountLockManager.getFailedCount("account_1"), is(3L));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(1L));
    }

    /**
     * Tests that is locked account.
     */
    @Test
    public void is_locked_account() {
        // It will not be locked until it reaches count.
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));

        // Locked when count is reached.
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(true));
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(true));

        // Other accounts are not locked.
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
    }

    /**
     * Tests that account lock release and failure count reset.
     */
    @Test
    public void release_account_lock() {
        AccountLockManager.countupFailedCount("account_1");
        AccountLockManager.countupFailedCount("account_1");
        AccountLockManager.countupFailedCount("account_1");
        AccountLockManager.countupFailedCount("account_1");
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(true));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(5L));
        AccountLockManager.countupFailedCount("account_2");
        AccountLockManager.countupFailedCount("account_2");
        AccountLockManager.countupFailedCount("account_2");
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(3L));

        // release account lock.
        AccountLockManager.releaseAccountLock("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(-1L));
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(3L));

        AccountLockManager.releaseAccountLock("account_2");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(-1L));
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(-1L));
    }

    /**
     * Tests that account lock life time.
     * @throws Exception Unexpected exception
     */
    @Test
    public void account_lock_life_time() throws Exception {
        // before
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(-1L));
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(-1L));

        // account1
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(1L));
        Thread.sleep(1000 * AccountLockManager.accountLockTime / 2);
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(2L));
        Thread.sleep(1000 * AccountLockManager.accountLockTime / 2);
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(3L));
        Thread.sleep(1000 * AccountLockManager.accountLockTime / 2);
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(4L));
        Thread.sleep(1000 * AccountLockManager.accountLockTime / 2);
        AccountLockManager.countupFailedCount("account_1");
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(true));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(5L));

        // account2
        AccountLockManager.countupFailedCount("account_2");
        AccountLockManager.countupFailedCount("account_2");
        AccountLockManager.countupFailedCount("account_2");
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(3L));

        // wait account lock expiration time (s).
        Thread.sleep(1000 * AccountLockManager.accountLockTime + 1);

        // check ralease account lock and reset failed count.
        assertThat(AccountLockManager.isLockedAccount("account_1"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_1"), is(-1L));
        assertThat(AccountLockManager.isLockedAccount("account_2"), is(false));
        assertThat(AccountLockManager.getFailedCount("account_2"), is(-1L));
    }
}
