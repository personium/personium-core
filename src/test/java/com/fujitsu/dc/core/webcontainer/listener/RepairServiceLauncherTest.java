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
package com.fujitsu.dc.core.webcontainer.listener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * com.fujitsu.dc.core.webcontainer.listener.RepairServiceLauncherのテスト.
 */
@RunWith(JUnit4.class)
public class RepairServiceLauncherTest {

    /**
     * ExecutorServiceの起動_終了テスト.
     */
    @Test
    @Ignore
    public void ExecutorServiceの起動_終了テスト() {
        RepairServiceLauncher launcher = new RepairServiceLauncher();
        assertThat(launcher.executor.getPoolSize(), is(1));
        assertThat(launcher.executor.getTaskCount(), is(1L));

        launcher.shutdown();
        assertThat(launcher.executor.isTerminated(), is(true));
        assertThat(launcher.executor.isShutdown(), is(true));
    }

    /**
     * ExecutorServiceの終了時に実行中のタスクが強制終了されないことを確認.
     * @throws InterruptedException タイムアウト
     */
    @Test
    public void ExecutorServiceの終了時に実行中のタスクが強制終了されないことを確認() throws InterruptedException {
        RepairServiceLauncher launcher = new RepairServiceLauncher();

        // 既存のタスクを中止。
        BlockingQueue<Runnable> queue = launcher.executor.getQueue();
        assertThat(queue.size(), is(1));
        launcher.executor.remove(queue.peek());

        // 数秒かかるタスクを代わりに投入.
        Runnable command = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(8000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        launcher.executor.scheduleWithFixedDelay(command, 0, 10, TimeUnit.SECONDS);

        Thread.sleep(1000);
        System.out.println("Shutting down service.");
        // タスクが即時終了しないことを確認する。
        long tm1 = System.currentTimeMillis();
        launcher.shutdown();
        long tm2 = System.currentTimeMillis();
        assertTrue(tm2 - tm1 > 4000L);

        assertThat(launcher.executor.isTerminated(), is(true));
        assertThat(launcher.executor.isShutdown(), is(true));
    }

    /**
     * ファイルの有無によりAdsRepairの起動が切り替えられること.
     * @throws IllegalAccessException エラー
     * @throws IllegalArgumentException エラー
     * @throws SecurityException エラー
     * @throws NoSuchFieldException エラー
     * @throws IOException エラー
     */
    @Test
    public void ファイルの有無によりAdsRepairの起動が切り替えられること()
            throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, SecurityException, IOException {

        RepairServiceLauncher.RepairAdsService service = new RepairServiceLauncher.RepairAdsService();

        Field flagFileField = service.getClass().getDeclaredField("invocationFlagFile");
        flagFileField.setAccessible(true);
        flagFileField.set(service, "./testFlagFile");

        File testFile = new File("./testFlagFile");
        testFile.deleteOnExit();

        if (testFile.exists()) {
            testFile.delete();
        }

        // ファイルが存在しないため、falseが返る。
        assertFalse(service.shouldInvoke());

        // ディレクトリであるため、falseが返る。
        testFile.mkdir();
        assertFalse(service.shouldInvoke());
        testFile.delete();

        testFile.createNewFile();
        assertTrue(service.shouldInvoke());

        testFile.delete();

        assertFalse(testFile.exists());
    }

}
