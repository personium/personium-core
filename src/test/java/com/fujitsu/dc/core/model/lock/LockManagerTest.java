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
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Before;
import org.junit.Ignore;
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
@Category({Unit.class })
public class LockManagerTest {

    /**
     * 前処理.
     */
    @Before
    public void before() {
        LockManager.deleteAllLocks();
        // LockManager.setLockType(LockManager.TYPE_IN_PROCESS);
    }

    /**
     * ロックが取得可能.
     */
    @Test
    public void ロックが取得可能() {
        Lock lock = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
        assertNotNull(lock);
        lock.release();
    }

    /**
     * 同じキー名での取得は２回目以降のものはブロックされる.
     */
    @Test
    public void 同じキー名での取得は２回目以降のものはブロックされる() {
        Lock lock = null;
        Lock lock2 = null;
        String lockName = "lk" + new Date().getTime();
        try {
            lock = LockManager.getLock(Lock.CATEGORY_ODATA, lockName, null, null);
            lock2 = LockManager.getLock(Lock.CATEGORY_ODATA, lockName, null, null);
        } catch (DcCoreException e) {
            assertEquals(DcCoreException.class, e.getClass());
        } finally {
            lock.release();
            if (lock2 != null) {
                lock2.release();
            }
        }
    }

    /**
     * ロック開放後同一カテゴリとキーでロック取得可能.
     */
    @Test
    public void ロック開放後同一カテゴリとキーでロック取得可能() {
        Lock lock = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
        lock.release();
        Lock lock2 = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
        assertNotNull(lock2);
        lock2.release();
    }

    /**
     * ふたつのつの取得が走ったとき最初に取得されたロックが開放されたら後続のリトライが成功する.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void ふたつの取得が走ったとき最初に取得されたロックが開放されたら後続のリトライが成功する() throws InterruptedException {
        final Thread t2 = new Thread(new Runnable() {
            public void run() {
                Lock lock = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
                assertNotNull(lock);
                lock.release();
            }
        });
        Thread t1 = new Thread(new Runnable() {
            public void run() {
                Lock lock = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
                assertNotNull(lock);
                t2.start();
                while (true) {
                    if (t2.isAlive()) {
                        lock.release();
                        break;
                    }
                }
            }
        });
        t1.start();
        t2.join();

    }

    /**
     * 違うカテゴリであれば同キー名での連続取得が可能.
     */
    @Test
    public void 違うカテゴリであれば同キー名での連続取得が可能() {
        Lock lockOData = LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
        Lock lockDav = LockManager.getLock(Lock.CATEGORY_DAV, "aaa", null, null);
        assertNotNull(lockOData);
        assertNotNull(lockDav);
        lockDav.release();
        lockOData.release();
    }

    /**
     * InProcessタイプのテスト.
     * @throws InterruptedException InterruptedException
     */
    @Test
    public void InProcessタイプのテスト() throws InterruptedException {
        LockManager.singleton = new InProcessLockManager();
        this.ロックが取得可能();
        this.同じキー名での取得は２回目以降のものはブロックされる();
        this.ロック開放後同一カテゴリとキーでロック取得可能();
        this.ふたつの取得が走ったとき最初に取得されたロックが開放されたら後続のリトライが成功する();
        this.違うカテゴリであれば同キー名での連続取得が可能();
    }

    /**
     * Retryが指定回数行われる.
     * @throws InterruptedException InterruptedException
     */
    @Test
    @Ignore
    public void Retryが指定回数行われる() throws InterruptedException {
        LockManager originalLm = LockManager.singleton;
        LockManager.singleton = new RetryCountingLockManager();
        RetryCountingLockManager rclm = (RetryCountingLockManager) LockManager.singleton;
        try {
            LockManager.getLock(Lock.CATEGORY_ODATA, "aaa", null, null);
        } catch (DcCoreException e) {
            assertEquals(DcCoreException.class, e.getClass());
        } finally {
            // doGetLock呼び出し回数 は 最初の呼び出しの１回と ＋ リトライ回数 となる
            assertEquals(LockManager.getLockRetryTimes() + 1, rclm.getCount);
            LockManager.singleton = originalLm;
        }
    }

    /**
     * Retryが指定回数行われることを確認するためのLockManager.
     * doGetLock呼び出し回数をカウントする。
     */
    class RetryCountingLockManager extends InProcessLockManager {
        int getCount = 0;

        @Override
        Lock doGetLock(String fullKey) {
            getCount++;
            return new Lock(fullKey, new Date().getTime());
        }
    }

}
