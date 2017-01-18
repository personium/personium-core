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
package com.fujitsu.dc.core.model.lock;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;

/**
 * LockManagerユニットテストクラス.
 */
@RunWith(DcRunner.class)
@Category({ Unit.class })
public class AccountLockManagerTest {

    /**
     * 前処理.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
    }

    /**
     * 後処理.
     */
    @After
    public void after() {
        LockManager.deleteAllLocks();
    }

    /**
     * AccountLock作成＆更新ができること.
     */
    @Test
    public void AccountLock作成_更新ができること() {
        try {
            AccountLockManager.registAccountLockObjct("AccountId");
            AccountLockManager.registAccountLockObjct("AccountId");
        } catch (DcCoreException e) {
            fail();
        }
    }
}
