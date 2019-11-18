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
package io.personium.test.jersey.concurrent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.setup.Setup;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * Dav系APIへの同時リクエストテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ConcurrentDavRequestTest extends PersoniumTest {
    private static final String CELL_NAME = "testcell1";
    private static final String COL_NAME = "colForConcurrencyTest";
    private static final String FILE_NAME = "fileForConcurrencyTest.txt";
    private static final String FILE_BODY = "fileBodyForConcurrencyTest";
    private static final int NUM_CONCURRENCY = 10;

    /** ログオブジェクト. */
    static Log log = LogFactory.getLog(ConcurrentDavRequestTest.class);;

    /**
     * コンストラクタ.
     */
    public ConcurrentDavRequestTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * カウンタ.
     */
    static class Counters {
        int countSuccess = 0;
        int countMoreSuccess = 0;
        int countFailure = 0;
        int countOverflow = 0;

        synchronized void incSuccess() {
            this.countSuccess++;
        }

        synchronized void incMoreSuccess() {
            this.countMoreSuccess++;
        }

        synchronized void incFailure() {
            this.countFailure++;
        }

        synchronized void incOverflow() {
            this.countOverflow++;
        }

        void assertSuccessCount(int expectedSuccessCount) {
            assertEquals(expectedSuccessCount, this.countSuccess);
        }

        void assertMoreSuccessCount(int expectedMoreSuccessCount) {
            assertEquals(expectedMoreSuccessCount, this.countMoreSuccess);
        }

        void assertFailureCount(int expectedFailureCount) {
            assertEquals(expectedFailureCount, this.countFailure);
        }

        void assertTotalCount(int expectedTotalCount) {
            assertEquals(expectedTotalCount, this.countSuccess + this.countMoreSuccess + this.countFailure
                    + this.countOverflow);
        }

        void assertOverflowNonZero() {
            assertTrue(this.countOverflow > 0);
        }

        void debugPrint() {
            log.debug("    Success: " + countSuccess);
            log.debug("MoreSuccess: " + countMoreSuccess);
            log.debug("    Failure: " + countFailure);
            log.debug("   Overflow: " + countOverflow);
        }
    }

    /**
     * 同一名称コレクション同時作成がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称コレクション同時作成がひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.createColRequest(COL_NAME);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // リクエストを発行
                    TResponse resp = theReq.returns();
                    log.debug("Status Code = " + resp.getStatusCode());
                    // 結果をカウントアップ
                    if (HttpStatus.SC_CREATED == resp.getStatusCode()) {
                        counters.incSuccess();
                    } else if (HttpStatus.SC_SERVICE_UNAVAILABLE == resp.getStatusCode()) {
                        counters.incOverflow();
                    } else if (HttpStatus.SC_METHOD_NOT_ALLOWED == resp.getStatusCode()) {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        try {
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 結果の簡易出力
            counters.debugPrint();
            // 全部の処理が戻ってきて
            counters.assertTotalCount(NUM_CONCURRENCY);
            // そのうち成功は１件のみである
            counters.assertSuccessCount(1);
        } finally {
            // テストで作成したEntityの削除
            this.deleteColRequest(COL_NAME).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一名称ファイル同時作成がすべて成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイル同時作成がすべて成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // リクエストを発行
                    TResponse resp = theReq.returns();
                    log.debug("Status Code = " + resp.getStatusCode());
                    // 結果をカウントアップ
                    if (HttpStatus.SC_CREATED == resp.getStatusCode()
                            || HttpStatus.SC_NO_CONTENT == resp.getStatusCode()) {
                        counters.incSuccess();
                    } else if (HttpStatus.SC_SERVICE_UNAVAILABLE == resp.getStatusCode()) {
                        counters.incOverflow();
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        try {
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 結果の簡易出力
            counters.debugPrint();
            // 全部の処理が戻ってきて
            counters.assertTotalCount(NUM_CONCURRENCY);
            // 全件成功となる(失敗はゼロ件）
            counters.assertFailureCount(0);
        } finally {
            // テストで作成したEntityの削除
            this.deleteFileRequest(FILE_NAME).returns()
                    .statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一名称ファイル同時削除がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイル同時削除がひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.deleteFileRequest(FILE_NAME);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // リクエストを発行
                    TResponse resp = theReq.returns();
                    log.debug("Status Code = " + resp.getStatusCode());
                    // 結果をカウントアップ
                    if (HttpStatus.SC_NO_CONTENT == resp.getStatusCode()) {
                        counters.incSuccess();
                    } else if (HttpStatus.SC_SERVICE_UNAVAILABLE == resp.getStatusCode()) {
                        counters.incOverflow();
                    } else if (HttpStatus.SC_NOT_FOUND == resp.getStatusCode()) {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 結果の簡易出力
            counters.debugPrint();
            // 全部の処理が戻ってきて
            counters.assertTotalCount(NUM_CONCURRENCY);
            // そのうち成功は１件のみである
            counters.assertSuccessCount(1);
        } finally {
            // テストで作成したEntityの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称コレクションから同一名称コレクションへの同時移動がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称コレクションから同一名称コレクションへの同時移動がひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> moveCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = new DavMultiThreadRunner(moveFileRequest(COL_NAME, COL_NAME + "dest", "T"),
                    moveCounters);
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.createColRequest(COL_NAME).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : moveCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, moveCounters.get(respCode)));
                total += moveCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode) {
                    assertEquals(1, (int) moveCounters.get(respCode));
                    // 400: TODO ロック待ちによってコレクションが移動されてしまった場合（コレクションの上書き対応時には不要となる
                    // 404: リクエスト受け付け時に既にコレクションが移動されてしまった場合
                    // 503: ロック待ちがタイムアウトした場合
                } else if (HttpStatus.SC_BAD_REQUEST != respCode && HttpStatus.SC_NOT_FOUND != respCode
                        && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            moveCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したリソースの削除
            this.deleteFileRequest(COL_NAME + "dest")
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 複数のファイルから同一名称ファイルへの同時移動がすべて成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 複数のファイルから同一名称ファイルへの同時移動がすべて成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.moveFileRequest(FILE_NAME + i, FILE_NAME + "_moved", "T");
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    // リクエストを発行
                    TResponse resp = theReq.returns();
                    log.debug("Status Code = " + resp.getStatusCode());
                    // 結果をカウントアップ
                    if (HttpStatus.SC_CREATED == resp.getStatusCode()) {
                        counters.incSuccess();
                    } else if (HttpStatus.SC_NO_CONTENT == resp.getStatusCode()) {
                        counters.incMoreSuccess();
                    } else if (HttpStatus.SC_SERVICE_UNAVAILABLE == resp.getStatusCode()) {
                        counters.incOverflow();
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                this.putFileRequest(FILE_NAME + i, Setup.TEST_BOX1, FILE_BODY).returns();
            }
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 結果の簡易出力
            counters.debugPrint();
            // 全部の処理が戻ってきて
            counters.assertTotalCount(NUM_CONCURRENCY);
            // そのうち成功(201)は１件のみである
            counters.assertSuccessCount(1);
            counters.assertMoreSuccessCount(NUM_CONCURRENCY - 1);
            counters.assertFailureCount(0);
        } finally {
            // テストで作成したリソースの削除
            this.deleteFileRequest(FILE_NAME + "_moved").returns().statusCode(-1);
            // 移動元のリソースは１件（任意）だけ移動されているため、ステータスコードはチェックしない。
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                this.deleteFileRequest(FILE_NAME + i).returns().statusCode(-1);

            }
        }
    }

    /**
     * 同一名称ファイルへのPROPFIND中のDELETEリクエストがひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのPROPFIND中のDELETEリクエストがひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.propfindFileRequest(FILE_NAME), counters);
            } else {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(FILE_NAME), counters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : counters.keySet()) {
                log.debug(String.format("%d: %d", respCode, counters.get(respCode)));
                total += counters.get(respCode);
                if (HttpStatus.SC_MULTI_STATUS == respCode || HttpStatus.SC_NOT_FOUND == respCode) {
                    assertFalse(0 == counters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertEquals(1, (int) counters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, counters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したEntityの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * コレクションのPROPFIND中に子要素のDELETEを実行した場合リクエストがすべて成功すること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void コレクションのPROPFIND中に子要素のDELETEを実行した場合リクエストがすべて成功すること() throws InterruptedException {
        String cell = Setup.TEST_CELL1;
        String box = Setup.TEST_BOX1;
        String col = "concurrentDavRequestTestCol";
        try {
            // テスト用コレクション準備
            DavResourceUtils.createWebDavCollection(
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cell, box, col);
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                DavResourceUtils.createWebDavFile(AbstractCase.MASTER_TOKEN_NAME, cell,
                        box + "/" + col + "/file_" + i, "body", MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);
            }

            // カウンタを準備
            final Map<Integer, Integer> propfindCounters = new HashMap<Integer, Integer>();
            final Map<Integer, Integer> deleteCounters = new HashMap<Integer, Integer>();

            // 同時リクエスト用スレッドの準備
            List<Thread> listThread = new ArrayList<Thread>();
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                DavMultiThreadRunner runner = null;
                if (i % 2 == 0) {
                    runner = new DavMultiThreadRunner(this.propfindRequest(cell, col), propfindCounters);
                } else {
                    runner = new DavMultiThreadRunner(this.deleteRequest(cell, col + "/file_" + i), deleteCounters);
                }
                Thread t = new Thread(runner);
                listThread.add(t);
            }
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;

            // PROPFINDのレスポンス確認
            for (Integer respCode : propfindCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, propfindCounters.get(respCode)));
                total += propfindCounters.get(respCode);
                if (HttpStatus.SC_MULTI_STATUS == respCode) {
                    assertFalse(0 == propfindCounters.get(respCode));
                } else {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            propfindCounters.get(respCode)));
                }
            }

            // DELETEのレスポンス確認
            for (Integer respCode : deleteCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, deleteCounters.get(respCode)));
                total += deleteCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertFalse(0 == deleteCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            deleteCounters.get(respCode)));
                }
            }

            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                DavResourceUtils.deleteWebDavFile(cell, AbstractCase.MASTER_TOKEN_NAME, box, col + "/file_" + i);
            }
            DavResourceUtils.deleteCollection(cell, box, col, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 同一名称ファイルへのACL中のDELETEリクエストがひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称コレクションへのACL中のDELETEリクエストがひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (NUM_CONCURRENCY / 2 + 1 == i) {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(COL_NAME), counters);
            } else {
                runner = new DavMultiThreadRunner(this.aclCollectionRequest(COL_NAME), counters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用コレクション準備
            this.createColRequest(COL_NAME).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : counters.keySet()) {
                log.debug(String.format("%d: %d", respCode, counters.get(respCode)));
                total += counters.get(respCode);
                if (HttpStatus.SC_OK == respCode || HttpStatus.SC_NOT_FOUND == respCode) {
                    assertFalse(0 == counters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertEquals(1, (int) counters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, counters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            this.deleteColRequest(COL_NAME).returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのPROPPATCH中のDELETEリクエストがひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのPROPPATCH中のDELETEリクエストがひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY * 5; i++) {
            DavMultiThreadRunner runner = null;
            if (NUM_CONCURRENCY / 2 + 1 == i) {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(FILE_NAME), counters);
            } else {
                runner = new DavMultiThreadRunner(this.proppatchFileRequest(FILE_NAME), counters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : counters.keySet()) {
                log.debug(String.format("%d: %d", respCode, counters.get(respCode)));
                total += counters.get(respCode);
                if (HttpStatus.SC_MULTI_STATUS == respCode || HttpStatus.SC_NOT_FOUND == respCode) {
                    assertFalse(0 == counters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertEquals(1, (int) counters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, counters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY * 5, total);
        } finally {
            // テストで作成したEntityの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのPUT中のDELETEリクエストが204もしくは404になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのPUT中のDELETEリクエストが204もしくは404になること() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> counters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY), counters);
            } else {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(FILE_NAME), counters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : counters.keySet()) {
                log.debug(String.format("%d: %d", respCode, counters.get(respCode)));
                total += counters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode || HttpStatus.SC_NO_CONTENT == respCode) {
                    assertFalse(0 == counters.get(respCode));
                } else if (HttpStatus.SC_NOT_FOUND != respCode && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, counters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのPUT中のMOVEリクエストが201もしくは404になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのPUT中のMOVEリクエストが201もしくは404になること() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> putCounters = new HashMap<Integer, Integer>();
        final Map<Integer, Integer> moveCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY),
                        putCounters);
            } else {
                runner = new DavMultiThreadRunner(this.moveFileRequest(FILE_NAME, FILE_NAME + "_moved", "T"),
                        moveCounters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : putCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, putCounters.get(respCode)));
                total += putCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode) {
                    assertTrue(0 < (int) putCounters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertTrue(0 < (int) putCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, putCounters.get(respCode)));
                }
            }
            for (Integer respCode : moveCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, moveCounters.get(respCode)));
                total += moveCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode) {
                    assertEquals(1, (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertTrue(0 < (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_NOT_FOUND != respCode && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, moveCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
            this.deleteFileRequest(FILE_NAME + "_moved")
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのGET中のDELETEリクエストがひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのGET中のDELETEリクエストがひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> getCounters = new HashMap<Integer, Integer>();
        final Map<Integer, Integer> deleteCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.getFileRequest(FILE_NAME), getCounters);
            } else {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(FILE_NAME), deleteCounters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : getCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, getCounters.get(respCode)));
                total += getCounters.get(respCode);
                if (HttpStatus.SC_OK != respCode && HttpStatus.SC_NOT_FOUND != respCode
                        && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, getCounters.get(respCode)));
                }
            }
            for (Integer respCode : deleteCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, deleteCounters.get(respCode)));
                total += deleteCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertEquals(1, (int) deleteCounters.get(respCode));
                } else if (HttpStatus.SC_NOT_FOUND == respCode) {
                    assertEquals(4, (int) deleteCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            deleteCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのGET中のMOVEリクエストがひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのGET中にMOVEリクエストが1回だけ201になること() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> getCounters = new HashMap<Integer, Integer>();
        final Map<Integer, Integer> moveCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.getFileRequest(FILE_NAME), getCounters);
            } else {
                runner = new DavMultiThreadRunner(this.moveFileRequest(FILE_NAME, FILE_NAME + "_moved", "T"),
                        moveCounters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : getCounters.keySet()) {
                log.debug(String.format("  get result%d: %d", respCode, getCounters.get(respCode)));
                total += getCounters.get(respCode);
                if (HttpStatus.SC_OK != respCode && HttpStatus.SC_NOT_FOUND != respCode
                        && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode, getCounters.get(respCode)));
                }
            }
            for (Integer respCode : moveCounters.keySet()) {
                log.debug(String.format("move result: %d: %d", respCode, moveCounters.get(respCode)));
                total += moveCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode) {
                    assertEquals(1, (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_NOT_FOUND == respCode) {
                    assertEquals(4, (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            moveCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
            this.deleteFileRequest(FILE_NAME + "_moved")
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへのDELETE中のMOVEリクエストが201もしくは404になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへのDELETE中のMOVEリクエストが201もしくは404になること() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> deleteCounters = new HashMap<Integer, Integer>();
        final Map<Integer, Integer> moveCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = null;
            if (i % 2 == 0) {
                runner = new DavMultiThreadRunner(this.deleteFileRequest(FILE_NAME), deleteCounters);
            } else {
                runner = new DavMultiThreadRunner(this.moveFileRequest(FILE_NAME, FILE_NAME + "_moved", "T"),
                        moveCounters);
            }
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            this.putFileRequest(FILE_NAME, Setup.TEST_BOX1, FILE_BODY).returns();
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : deleteCounters.keySet()) {
                log.debug(String.format("deleted %d: %d", respCode, deleteCounters.get(respCode)));
                total += deleteCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT != respCode && HttpStatus.SC_NOT_FOUND != respCode
                        && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            deleteCounters.get(respCode)));
                }
            }
            for (Integer respCode : moveCounters.keySet()) {
                log.debug(String.format("moved %d: %d", respCode, moveCounters.get(respCode)));
                total += moveCounters.get(respCode);
                if (HttpStatus.SC_CREATED != respCode && HttpStatus.SC_NOT_FOUND != respCode
                        && HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            moveCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            this.deleteFileRequest(FILE_NAME)
                    .returns()
                    .statusCode(-1);
            this.deleteFileRequest(FILE_NAME + "_moved")
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称ファイルへの複数のMOVEリクエストが201もしくは204になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称ファイルへの複数のMOVEリクエストが201もしくは204になること() throws InterruptedException {
        // カウンタを準備
        final Map<Integer, Integer> moveCounters = new HashMap<Integer, Integer>();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            DavMultiThreadRunner runner = new DavMultiThreadRunner(this.moveFileRequest(FILE_NAME + i, FILE_NAME
                    + "_moved", "T"),
                    moveCounters);
            Thread t = new Thread(runner);
            listThread.add(t);
        }
        try {
            // テスト用ファイル準備
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                this.putFileRequest(FILE_NAME + i, Setup.TEST_BOX1, FILE_BODY).returns();
            }
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : moveCounters.keySet()) {
                log.debug(String.format("%d: %d", respCode, moveCounters.get(respCode)));
                total += moveCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode) {
                    assertEquals(1, (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_NO_CONTENT == respCode) {
                    assertEquals(NUM_CONCURRENCY - 1, (int) moveCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("request failed: respcode=%d, count:%d", respCode,
                            moveCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            // テストで作成したファイルの削除
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                this.deleteFileRequest(FILE_NAME + i).returns().statusCode(-1);
            }
            this.deleteFileRequest(FILE_NAME + "_moved")
                    .returns()
                    .statusCode(-1);
        }
    }

    /**
     * 同一名称コレクションの作成時に親コレクションが存在しない場合に409になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称コレクションの作成時に親コレクションが存在しない場合に409になること() throws InterruptedException {
        final String cell = "concurrencyTestCell";
        final String box = "box1";
        final String col = "col";

        try {
            // テスト用コレクション準備
            CellUtils.create(cell, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cell, "box1", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cell, box, col);
            // カウンタを準備
            final Map<Integer, Integer> mkcolCounters = new HashMap<Integer, Integer>();
            final Map<Integer, Integer> deleteCounters = new HashMap<Integer, Integer>();

            // 同時リクエスト用スレッドの準備
            List<Thread> listThread = new ArrayList<Thread>();
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                DavMultiThreadRunner runner = null;
                if (i % 2 == 0) {
                    runner = new DavMultiThreadRunner(this.deleteColRequest(cell, col), deleteCounters);
                } else {
                    runner = new DavMultiThreadRunner(this.createColRequest(cell, col + "/" + COL_NAME), mkcolCounters);
                }
                Thread t = new Thread(runner);
                listThread.add(t);
            }
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : deleteCounters.keySet()) {
                log.debug(String.format("delete %d: %d", respCode, deleteCounters.get(respCode)));
                total += deleteCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT == respCode || HttpStatus.SC_NOT_FOUND == respCode
                        || HttpStatus.SC_FORBIDDEN == respCode) {
                    assertTrue(0 < deleteCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("delete request failed: respcode=%d, count:%d",
                            respCode, deleteCounters.get(respCode)));
                }
            }
            for (Integer respCode : mkcolCounters.keySet()) {
                log.debug(String.format("mkcol  %d: %d", respCode, mkcolCounters.get(respCode)));
                total += mkcolCounters.get(respCode);
                if (HttpStatus.SC_CREATED == respCode || HttpStatus.SC_METHOD_NOT_ALLOWED == respCode
                        || HttpStatus.SC_CONFLICT == respCode) {
                    assertTrue(0 < mkcolCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("mkcol  request failed: respcode=%d, count:%d",
                            respCode, mkcolCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            DavResourceUtils.deleteCollection(cell, box, col + "/" + COL_NAME, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(cell, box, col, AbstractCase.MASTER_TOKEN_NAME, -1);
            BoxUtils.delete(cell, AbstractCase.MASTER_TOKEN_NAME, box, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, -1);
        }
    }

    /**
     * 同一名称コレクションの削除時に親コレクションが存在しない場合に404になること.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一名称コレクションの削除時に親コレクションが存在しない場合に404になること() throws InterruptedException {
        final String cell = "concurrencyTestCell";
        final String box = "box1";

        try {
            // テスト用コレクション準備
            CellUtils.create(cell, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            BoxUtils.create(cell, "box1", AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
            DavResourceUtils.createWebDavCollection(
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, cell, box, COL_NAME);
            // カウンタを準備
            final Map<Integer, Integer> targetCounters = new HashMap<Integer, Integer>();
            final Map<Integer, Integer> parentCounters = new HashMap<Integer, Integer>();

            // 同時リクエスト用スレッドの準備
            List<Thread> listThread = new ArrayList<Thread>();
            for (int i = 0; i < NUM_CONCURRENCY; i++) {
                DavMultiThreadRunner runner = null;
                if (NUM_CONCURRENCY / 2 + 1 == i) {
                    runner = new DavMultiThreadRunner(this.deleteColRequest(cell, COL_NAME), targetCounters);
                } else {
                    runner = new DavMultiThreadRunner(this.bulkDeleteCellRequest(cell), parentCounters);
                }
                Thread t = new Thread(runner);
                listThread.add(t);
            }
            // 全部走らせてから
            for (Thread t : listThread) {
                t.start();
            }
            // 全部待つ
            for (Thread t : listThread) {
                t.join();
            }
            // 全部の処理が戻ってきて
            int total = 0;
            for (Integer respCode : parentCounters.keySet()) {
                log.debug(String.format("parent %d: %d", respCode, parentCounters.get(respCode)));
                total += parentCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT == respCode || HttpStatus.SC_NOT_FOUND == respCode
                        || HttpStatus.SC_CONFLICT == respCode) {
                    assertTrue(0 < parentCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("delete request failed: respcode=%d, count:%d",
                            respCode, parentCounters.get(respCode)));
                }
            }
            for (Integer respCode : targetCounters.keySet()) {
                log.debug(String.format("target %d: %d", respCode, targetCounters.get(respCode)));
                total += targetCounters.get(respCode);
                if (HttpStatus.SC_NO_CONTENT == respCode || HttpStatus.SC_NOT_FOUND == respCode) {
                    assertTrue(0 < targetCounters.get(respCode));
                } else if (HttpStatus.SC_SERVICE_UNAVAILABLE != respCode) {
                    fail(String.format("target request failed: respcode=%d, count:%d",
                            respCode, targetCounters.get(respCode)));
                }
            }
            assertEquals(NUM_CONCURRENCY, total);
        } finally {
            DavResourceUtils.deleteCollection(cell, box, COL_NAME, AbstractCase.MASTER_TOKEN_NAME, -1);
            BoxUtils.delete(cell, AbstractCase.MASTER_TOKEN_NAME, box, -1);
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cell, -1);
        }
    }

    /**
     * マルチスレッド実行用クラス.
     */
    public class DavMultiThreadRunner implements Runnable {

        Http request;
        Map<Integer, Integer> counters = null;

        @SuppressWarnings("unused")
        private DavMultiThreadRunner() {
        }

        /**
         * コンストラクタ.
         * @param req Httpリクエスト
         * @param counters カウンタ
         */
        public DavMultiThreadRunner(Http req, Map<Integer, Integer> counters) {
            this.request = req;
            this.counters = counters;
        }

        @Override
        public void run() {
            // リクエストを発行
            TResponse resp = request.returns();
            log.debug("Status Code = " + resp.getStatusCode());
            // ステータスコード別に件数をカウントアップ
            int respCode = resp.getStatusCode();
            synchronized (counters) {
                if (null == counters.get(respCode)) {
                    counters.put(respCode, 1);
                } else {
                    counters.put(respCode, counters.get(respCode) + 1);
                }
            }
            log.debug("response: " + respCode);
            if (respCode == 500) {
                log.debug("Status 500 errro: " + resp.getBody());
            }
        }
    }

    /**
     * セル再帰的削除リクエストを生成.
     * @param cellName セル名
     * @return リクエストオブジェクト
     */
    private Http bulkDeleteCellRequest(String celllName) {
        return Http.request("cell/cell-bulk-delete.txt")
                .with("cell", celllName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME);
    }

    /**
     * Col削除リクエストを生成.
     * @param col ロール名
     * @return リクエストオブジェクト
     */
    private Http deleteColRequest(String colName) {
        return deleteColRequest(CELL_NAME, colName);
    }

    private Http deleteColRequest(String cellName, String colName) {
        return Http.request("box/delete-col.txt")
                .with("cellPath", cellName)
                .with("box", "box1")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", colName);
    }

    /**
     * Col作成リクエストを生成.
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    private Http createColRequest(String colName) {
        return createColRequest(CELL_NAME, colName);
    }

    private Http createColRequest(String cellName, String colName) {
        return Http.request("box/mkcol-normal.txt")
                .with("cellPath", cellName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", colName);
    }

    /**
     * File削除リクエストを生成.
     * @param fileName ファイル名
     * @return リクエストオブジェクト
     */
    Http deleteFileRequest(String fileName) {
        return deleteRequest(CELL_NAME, fileName);
    }

    /**
     * 削除リクエストを生成.
     * @param cell セル名
     * @param path パス（Boxは含めない）
     * @return リクエストオブジェクト
     */
    Http deleteRequest(String cell, String path) {
        return Http.request("box/dav-delete.txt")
                .with("cellPath", cell)
                .with("box", Setup.TEST_BOX1)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", path);
    }

    /**
     * File作成リクエストを生成.
     * @param boxName box名
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http putFileRequest(String fileName, String boxName, String fileBody) {
        return Http.request("box/dav-put.txt")
                .with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", fileName)
                .with("box", boxName)
                .with("contentType", javax.ws.rs.core.MediaType.TEXT_PLAIN)
                .with("source", fileBody);
    }

    /**
     * File作成リクエストを生成.
     * @param boxName box名
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http getFileRequest(String fileName) {
        return Http.request("box/dav-get.txt")
                .with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("box", Setup.TEST_BOX1)
                .with("path", fileName);
    }

    /**
     * File移動リクエストを生成.
     * @param srcFileName 移動元ファイル名
     * @param dstFileName 移動先ファイル名
     * @param overwrite "F" or "T"
     * @return リクエストオブジェクト
     */
    Http moveFileRequest(String srcFileName, String dstFileName, String overwrite) {
        return Http.request("box/dav-move-with-header.txt")
                .with("cellPath", CELL_NAME)
                .with("authorization", AbstractCase.BEARER_MASTER_TOKEN)
                .with("path", Setup.TEST_BOX1 + "/" + srcFileName)
                .with("destination", UrlUtils.box(CELL_NAME, Setup.TEST_BOX1, dstFileName))
                .with("overWrite", overwrite)
                .with("ifMatch", "*")
                .with("depth", "infinity");
    }

    /**
     * FileへのPROPPATCHリクエストを生成.
     * @param fileName ファイル名
     * @return リクエストオブジェクト
     */
    Http proppatchFileRequest(String fileName) {
        return Http.request("box/proppatch.txt")
                .with("cell", CELL_NAME)
                .with("token", AbstractCase.BEARER_MASTER_TOKEN)
                .with("path", fileName)
                .with("box", Setup.TEST_BOX1)
                .with("author1", "hoge")
                .with("hoge", "hoge");
    }

    /**
     * FileへのPROPFINDリクエストを生成.
     * @param fileName ファイル名
     * @return リクエストオブジェクト
     */
    Http propfindFileRequest(String fileName) {
        return propfindRequest(CELL_NAME, fileName);
    }

    /**
     * PROPFINDリクエストを生成.
     * @param cellName セル名
     * @param path パス（Boxは含めない）
     * @return リクエストオブジェクト
     */
    Http propfindRequest(String cellName, String path) {
        return Http.request("box/propfind-col.txt")
                .with("cell", cellName)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("path", path)
                .with("box", Setup.TEST_BOX1)
                .with("depth", "0");
    }

    /**
     * CollectionへのACLリクエストを生成.
     * @param colName コレクション名
     * @return リクエストオブジェクト
     */
    Http aclCollectionRequest(String colName) {
        return Http.request("box/acl-all.txt")
                .with("cell", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("resourcePath", colName)
                .with("box", Setup.TEST_BOX1)
                .with("level", "")
                .with("roleBaseUrl", UrlUtils.roleResource(CELL_NAME, null, ""));
    }

}
