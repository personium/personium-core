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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.common.es.response.PersoniumSearchResponse;
import io.personium.core.model.impl.es.EsModel;
import io.personium.core.model.impl.es.accessor.EntitySetAccessor;
import io.personium.core.model.lock.Lock;
import io.personium.core.model.lock.LockManager;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.DcRunner;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * OData系APIへの同時リクエストテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ConcurrentODataRequestTest extends JerseyTest {
    private static final String CELL_NAME = "testcell1";
    private static final String BOX_NAME = "box1";
    private static final String NEW_ROLE_NAME = "roleForConcurrentTest";
    private static final String NEW_ROLE_NAME_2 = "roleForConcurrentTest2";
    private static final String NEW_BOX_NAME = "boxForConcurrentTest";
    private static final String NEW_BOX_SCHEMA = "http://example.com/schema1/";
    private static final String NEW_BOX_SCHEMA_2 = "https://example.net/schema2/";
    private static final String ACCOUNT_NAME = "account2";
    private static final String ROLE_NAME = "role1";
    private static final int NUM_CONCURRENCY = 10;

    /** ログオブジェクト. */
    static Log log = LogFactory.getLog(ConcurrentODataRequestTest.class);;

    /**
     * コンストラクタ.
     */
    public ConcurrentODataRequestTest() {
        super("io.personium.core.rs");
    }

    /**
     * カウンタ.
     */
    static class Counters {
        int countSuccess = 0;
        int countFailure = 0;
        int countOverflow = 0;

        synchronized void incSuccess() {
            this.countSuccess++;
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

        void assertFailureCount(int expectedFailureCount) {
            assertEquals(expectedFailureCount, this.countFailure);
        }

        void assertTotalCount(int expectedTotalCount) {
            assertEquals(expectedTotalCount, this.countSuccess + this.countFailure + this.countOverflow);
        }

        void assertOverflowNonZero() {
            assertTrue(this.countOverflow > 0);
        }

        void debugPrint() {
            log.debug(" Success: " + countSuccess);
            log.debug(" Failure: " + countFailure);
            log.debug("Overflow: " + countOverflow);
        }
    }

    /**
     * 同一キーのエンティティ同時追加がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一キーのエンティティ同時追加がひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();

        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.createRoleRequest(NEW_ROLE_NAME, BOX_NAME);
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
            // そのうち成功は１件のみである
            counters.assertSuccessCount(1);
        } finally {
            // テストで作成したEntityの削除
            this.deleteRoleRequest(NEW_ROLE_NAME, BOX_NAME).returns().statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一キーのエンティティ同時削除がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一キーのエンティティ同時削除がひとつだけ成功する() throws InterruptedException {
        // 削除すべきレコードを準備
        this.createRoleRequest(NEW_ROLE_NAME, BOX_NAME).returns();
        // カウンタを準備
        final Counters counters = new Counters();
        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.deleteRoleRequest(NEW_ROLE_NAME, BOX_NAME);
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
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        // 処理の実行
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
            // 後片付け
            this.deleteRoleRequest(NEW_ROLE_NAME, BOX_NAME).returns().statusCode(HttpStatus.SC_NOT_FOUND);
            // 後片付け(本来不要)では404が返るはず。
        }
    }

    /**
     * 同一キーのETagつきエンティティ同時更新がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一キーのETagつきエンティティ同時更新がひとつだけ成功する() throws InterruptedException {
        // 更新すべきレコードを準備
        TResponse resp = this.createBoxRequest(NEW_BOX_NAME, NEW_BOX_SCHEMA).returns();
        final String etag = resp.getHeader(HttpHeaders.ETAG);
        // カウンタを準備
        final Counters counters = new Counters();
        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.updateBoxRequest(NEW_BOX_NAME, NEW_BOX_NAME, NEW_BOX_SCHEMA_2, etag);
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
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        // 処理の実行
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
            // そのうち成功は１件である
            counters.assertSuccessCount(1);
        } finally {
            // 後片付け
            this.deleteBoxRequest(NEW_BOX_NAME).returns().statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一キーのETagなし主キー変更を伴うエンティティ同時更新がひとつだけ成功.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一キーのETagなし主キー変更を伴うエンティティ同時更新がひとつだけ成功() throws InterruptedException {
        // 更新すべきレコードを準備
        this.createRoleRequest(NEW_ROLE_NAME, BOX_NAME).returns();
        // カウンタを準備
        final Counters counters = new Counters();
        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.updateRoleRequest(NEW_ROLE_NAME, BOX_NAME, NEW_ROLE_NAME_2);
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
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        // 処理の実行
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
            // 後片付け
            this.deleteRoleRequest(NEW_ROLE_NAME_2, BOX_NAME).returns().statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一対象リンク同時追加がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一対象リンク同時追加がひとつだけ成功する() throws InterruptedException {
        // カウンタを準備
        final Counters counters = new Counters();
        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.linkAccountRoleRequest(ACCOUNT_NAME, ROLE_NAME, null);
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
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        // 処理の実行
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
            // 後片付け
            this.unlinkAccountRoleRequest(ACCOUNT_NAME, ROLE_NAME, null).returns().statusCode(HttpStatus.SC_NO_CONTENT);
            // 後片付けでは204が返るはず。
        }
    }

    /**
     * 同一対象リンク同時削除がひとつだけ成功する.
     * @throws InterruptedException InterruptedException
     */
    @Ignore
    @Test
    public final void 同一対象リンク同時削除がひとつだけ成功する() throws InterruptedException {
        this.linkAccountRoleRequest(ACCOUNT_NAME, ROLE_NAME, null).returns();
        // カウンタを準備
        final Counters counters = new Counters();
        // 同時リクエスト用スレッドの準備
        List<Thread> listThread = new ArrayList<Thread>();
        for (int i = 0; i < NUM_CONCURRENCY; i++) {
            final Http theReq = this.unlinkAccountRoleRequest(ACCOUNT_NAME, ROLE_NAME, null);
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
                    } else {
                        counters.incFailure();
                    }
                }
            };
            Thread t = new Thread(runnable);
            listThread.add(t);
        }
        // 処理の実行
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
            // 後片付け
            this.unlinkAccountRoleRequest(ACCOUNT_NAME, ROLE_NAME, null).returns()
                    // 正常系で削除は行われるので本来後片付けの必要はない。
                    .statusCode(HttpStatus.SC_NOT_FOUND);
            // そのため、ここでは４０４が返るはず。
        }
    }

    /**
     * 更新ロック取得がタイムアウトした場合に503エラーとなる.
     */
    @Ignore("ビルド環境からIT環境に向けてテストを実施した場合に問題あったため一時的に除外")
    @Test
    public void 更新ロック取得がタイムアウトした場合に503エラーとなる() {
        String accessTargetCellId = getTestingCellId();
        Lock lock = getLockForTimeoutTest(accessTargetCellId);
        try {
            int httpStatusCode = createRoleAndGetStatus();
            assertEquals(HttpStatus.SC_SERVICE_UNAVAILABLE, httpStatusCode);
        } finally {
            releaseLockForTimeoutTest(lock);
        }
    }

    private Lock getLockForTimeoutTest(String cellId) {
        return LockManager.getLock("odata", cellId, null, null);
    }

    private void releaseLockForTimeoutTest(Lock lock) {
        lock.release();
    }

    private int createRoleAndGetStatus() {
        Http request = this.createRoleRequest(NEW_ROLE_NAME, BOX_NAME);
        TResponse response = request.returns();
        return response.getStatusCode();
    }

    private String getTestingCellId() {
        Map<String, Object> query = buildQueryForRetrieveCellByName();
        return retrieveCellId(query);
    }

    private Map<String, Object> buildQueryForRetrieveCellByName() {
        Map<String, Object> termQuery = buildBasicQueryForRetrieveByName();
        Map<String, Object> query = buildQueryMap(termQuery);
        Map<String, Object> filter = buildFilterMap(query);
        return filter;
    }

    private Map<String, Object> buildBasicQueryForRetrieveByName() {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("s.Name", CELL_NAME);
        return query;
    }

    private Map<String, Object> buildQueryMap(Map<String, Object> subQuery) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("term", subQuery);
        return query;
    }

    private Map<String, Object> buildFilterMap(Map<String, Object> subQuery) {
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("filter", subQuery);
        return filter;
    }

    private String retrieveCellId(Map<String, Object> query) {
        EntitySetAccessor ecCells = EsModel.cell();
        PersoniumSearchResponse response = ecCells.search(query);
        if (isRetriveFailed(response)) {
            return null;
        }
        return response.getHits().getAt(0).getId();
    }

    private boolean isRetriveFailed(PersoniumSearchResponse response) {
        return response == null || response.getHits().getCount() == 0;
    }

    /**
     * Role削除リクエストを生成.
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http deleteRoleRequest(String roleName, String boxName) {
        return Http.request("role-delete.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("rolename", roleName)
                .with("boxname", "'" + boxName + "'");
    }

    /**
     * Role作成リクエストを生成.
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    @SuppressWarnings("unchecked")
    Http createRoleRequest(String roleName, String boxName) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);
        body.put("_Box.Name", boxName);
        return Http.request("role-create.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("body", body.toString());
    }

    @SuppressWarnings("unchecked")
    Http updateRoleRequest(String roleName, String boxName, String newRoleName) {
        JSONObject body = new JSONObject();
        body.put("Name", newRoleName);
        body.put("_Box.Name", boxName);
        return Http.request("cell/role-update.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("rolename", roleName)
                .with("boxname", "'" + boxName + "'").with("body", body.toString());
    }

    /**
     * Box削除リクエストを生成.
     * @param name Box名
     * @return リクエストオブジェクト
     */
    Http deleteBoxRequest(String name) {
        return Http.request("cell/box-delete.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("boxPath", name);
    }

    /**
     * Box作成リクエストを生成.
     * @param name Box名
     * @param schema schema url
     * @return リクエストオブジェクト
     */
    Http createBoxRequest(String name, String schema) {
        return Http.request("cell/box-create-with-scheme.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("boxPath", name).with("schema", schema);
    }

    Http updateBoxRequest(String name, String newName, String schema, String etag) {
        return Http.request("cell/box-update.txt").with("cellPath", CELL_NAME).with("boxPath", name)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("etag", etag).with("newBoxPath", newName)
                .with("schema", schema);
    }

    /**
     * Account-RoleのLink作成リクエストを生成.
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http linkAccountRoleRequest(String accountName, String roleName, String boxName) {
        String roleKeyPredicate = "Name='" + roleName + "'";
        if (boxName != null) {
            roleKeyPredicate += ",_Box.Name='" + boxName + "'";
        }
        String roleUrl = UrlUtils.cellCtl(CELL_NAME, "Role");
        roleUrl += "(" + roleKeyPredicate + ")";
        return Http.request("cell/link-account-role.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("username", accountName).with("roleUrl", roleUrl);
    }

    /**
     * Account-RoleのLink削除リクエストを生成.
     * @param accountName Account名
     * @param roleName ロール名
     * @param boxName Box名
     * @return リクエストオブジェクト
     */
    Http unlinkAccountRoleRequest(String accountName, String roleName, String boxName) {
        String roleKeyPredicate = "Name='" + roleName + "'";
        if (boxName != null) {
            roleKeyPredicate += ",_Box.Name='" + boxName + "'";
        }
        return Http.request("cell/unlink-account-role.txt").with("cellPath", CELL_NAME)
                .with("token", AbstractCase.MASTER_TOKEN_NAME).with("username", accountName)
                .with("keyPredicate", roleKeyPredicate);
    }
}
