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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
 * CellLockManagerユニットテストクラス.
 */
@RunWith(DcRunner.class)
@Category({ Unit.class })
public class CellLockManagerTest {
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
     * 参照カウントオブジェクトが存在しない場合に取得結果として-1が返却される.
     */
    @Test
    public void Cell参照カウントが存在しない場合() {
        try {
            long count = CellLockManager.getReferenceCount("TestingCellId");
            assertEquals(-1, count);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * 参照カウントオブジェクトを初期値1で新規作成できることの確認.
     */
    @Test
    public void Cell参照カウントを初期値1で新規作成する() {
        try {
            long createdResult = CellLockManager.incrementReferenceCount("TestingCellId");
            assertEquals(1, createdResult);
            long fetchedCount = CellLockManager.getReferenceCount("TestingCellId");
            assertEquals(1, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * 初期化後の参照カウントをインクリメントすると2になることの確認.
     */
    @Test
    public void 初期化後の参照カウントをインクリメントすると2になる() {
        try {
            long incrementResult = CellLockManager.incrementReferenceCount("TestingCellId");
            assertEquals(1, incrementResult);

            incrementResult = CellLockManager.incrementReferenceCount("TestingCellId");
            assertEquals(2, incrementResult);

            long fetchedCount = CellLockManager.getReferenceCount("TestingCellId");
            assertEquals(2, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * 参照カウントが存在しない状態でデクリメントすると0が返却されることの確認.
     */
    @Test
    public void 参照カウントが存在しない状態でデクリメントすると0が返却される() {
        try {
            long decrementResult = CellLockManager.decrementReferenceCount("TestingCellId");
            assertEquals(0, decrementResult);

            long fetchedCount = CellLockManager.getReferenceCount("TestingCellId");
            assertEquals(-1, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * 参照カウントをインクリメント、デクリメントしてクリアされることの確認.
     */
    @Test
    public void 参照カウントをインクリメント後デクリメントするとクリアされる() {
        try {
            long incrementResult = CellLockManager.incrementReferenceCount("TestingCellId");
            assertEquals(1, incrementResult);

            incrementResult = CellLockManager.incrementReferenceCount("TestingCellId");
            assertEquals(2, incrementResult);

            long decrementResult = CellLockManager.decrementReferenceCount("TestingCellId");
            assertEquals(1, decrementResult);

            decrementResult = CellLockManager.decrementReferenceCount("TestingCellId");
            assertEquals(0, decrementResult);

            long fetchedCount = CellLockManager.getReferenceCount("TestingCellId");
            assertEquals(-1, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * セルの処理状態を一括削除処理中に設定できることを確認.
     */
    @Test
    public void セルの処理状態を一括削除処理中に設定する() {
        try {
            Boolean createdResult = CellLockManager.setBulkDeletionStatus("TestingCellId");
            assertTrue(createdResult);

            long fetchedCount = CellLockManager.getCellStatus("TestingCellId");
            assertEquals(1, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * セルの処理状態を一括削除してない中に設定できることを確認.
     */
    @Test
    public void セルの処理状態を一括削除してない中に設定する() {
        try {
            Boolean createdResult = CellLockManager.resetBulkDeletionStatus("TestingCellId");
            assertTrue(createdResult);

            long fetchedCount = CellLockManager.getCellStatus("TestingCellId");
            assertEquals(0, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }

    /**
     * セルの処理状態が一括削除してない中の場合に状態を一括削除処理中に更新できることを確認.
     */
    @Test
    public void セルの処理状態が一括削除してない中の場合に状態を一括削除処理中に更新する() {
        try {
            Boolean result = CellLockManager.resetBulkDeletionStatus("TestingCellId");
            assertTrue(result);

            long fetchedCount = CellLockManager.getCellStatus("TestingCellId");
            assertEquals(0, fetchedCount);

            Boolean updateResult = CellLockManager.setBulkDeletionStatus("TestingCellId");
            assertTrue(updateResult);

            fetchedCount = CellLockManager.getCellStatus("TestingCellId");
            assertEquals(1, fetchedCount);
        } catch (DcCoreException e) {
            fail();
        }
    }
}
