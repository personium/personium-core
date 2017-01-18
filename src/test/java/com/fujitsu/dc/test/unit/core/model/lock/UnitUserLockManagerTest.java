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
package com.fujitsu.dc.test.unit.core.model.lock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.lock.UnitUserLockManager;
import com.fujitsu.dc.test.categories.Unit;

/**
 * UnitUserLockManager ユニットテストクラス.
 */
@Category({ Unit.class })
public class UnitUserLockManagerTest {



    /**
     * ロックされている場合にTRUEを返すこと.
     */
    @Test
    public void ロックされている場合にTRUEを返すこと() {
        String unitPrefix = DcCoreConfig.getEsUnitPrefix();
        String unitUserName = unitPrefix + "_unituserlocktest";
        try {
            UnitUserLockManager.registLockObjct(unitUserName);
            assertTrue(UnitUserLockManager.hasLockObject(unitUserName));
        } finally {
            UnitUserLockManager.releaseLockObject(unitUserName);
        }
    }

    /**
     * ロックされていない場合にFALSEを返すこと.
     */
    @Test
    public void ロックされていない場合にFALSEを返すこと() {
        String unitPrefix = DcCoreConfig.getEsUnitPrefix();
        String unitUserName = unitPrefix + "_unituserlocktest";
        assertFalse(UnitUserLockManager.hasLockObject(unitUserName));
    }

    /**
     * 異なるUnitUserでロックされている場合にFALSEを返すこと.
     */
    @Test
    public void 異なるUnitUserでロックされている場合にFALSEを返すこと() {
        String unitPrefix = DcCoreConfig.getEsUnitPrefix();
        String unitUserName = unitPrefix + "_unituserlocktest";
        try {
            UnitUserLockManager.registLockObjct(unitUserName + "XX");
            assertFalse(UnitUserLockManager.hasLockObject(unitUserName));
        } finally {
            UnitUserLockManager.releaseLockObject(unitUserName + "XX");
        }
    }
}
