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
package io.personium.core.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreLog;
import io.personium.core.PersoniumUnitConfig;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;

/**
 * 本アプリでのMemcachedアクセスを司るClient.
 * オープンソースのMemcachedClientをラップしており、将来予見されるライブラリ変更のインパクトを本クラス内に収める.
 * Memcachedクライアントのコネクション確立には時間的なコストがかかるためこのクラスのクラス変数にClientを保持しておき、
 * サーバ起動時にコネクション確立し、そのままコネクションを維持する。
 */
public class MemcachedClient implements CacheClient {
    static volatile boolean isReportError = false;
    net.spy.memcached.MemcachedClient spyClient = null;

    private MemcachedClient(String host, String port, long opTimeout) {
        try {
            ConnectionFactoryBuilder cfb = new ConnectionFactoryBuilder(new DefaultConnectionFactory());
            // memcached のタイムアウト時間を設定
            cfb.setOpTimeout(opTimeout);

            List<InetSocketAddress> addrs = new ArrayList<InetSocketAddress>();
            addrs.add(new InetSocketAddress(host, Integer.valueOf(port)));

            this.spyClient = new net.spy.memcached.MemcachedClient(cfb.build(), addrs);
        } catch (NumberFormatException e) {
            PersoniumCoreLog.Server.MEMCACHED_PORT_FORMAT_ERROR.params(e.getMessage()).reason(e).writeLog();
        } catch (IOException e) {
            PersoniumCoreLog.Server.MEMCACHED_CONNECTO_FAIL.params(host, port, e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    private MemcachedClient() {
    }

    /**
     * 指定キーのキャッシュを取得.
     * @param <T> 取得する型
     * @param key キャッシュキー
     * @param clazz 取得する型、 型に問題があるときはClassCastExcetpion発生
     * @return キャッシュされたオブジェクト / null キャッシュが存在しないとき
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            T ret = (T) this.spyClient.get(key);
            if (isReportError) {
                isReportError = false;
            }
            return ret;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーでオブジェクトを一定の有効期限のみキャッシュします.
     * @param key キャッシュのキー
     * @param expiresIn 有効期間
     * @param object キャッシュすべきオブジェクト
     * @return 処理成功時はTrue/失敗時はFalseを返す.
     */
    public Boolean add(String key, int expiresIn, Object object) {
        try {
            return this.spyClient.add(key, expiresIn, object).get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
        return Boolean.FALSE;
    }

    /**
     * 指定キーでオブジェクトをキャッシュします.
     * @param key キャッシュのキー
     * @param object キャッシュすべきオブジェクト
     * @return 処理成功時はTrue/失敗時はFalseを返す.
     */
    public Boolean add(String key, Object object) {
        return this.add(key, 0, object);
    }

    /**
     * 指定キーでオブジェクトを一定の有効期限のみキャッシュします.
     * @param key キャッシュのキー
     * @param expiresIn 有効期間
     * @param object キャッシュすべきオブジェクト
     * @return 処理成功時はTrue/失敗時はFalseを返す.
     */
    @Override
    public Boolean put(String key, int expiresIn, Object object) {
        try {
            if (!this.spyClient.replace(key, expiresIn, object).get()) {
                if (!this.spyClient.add(key, expiresIn, object).get()) { //NOPMD - To maintain readability
                    // Coping with core issue #35.
                    // Correspondence to failure where processing fails when accessing at the same time.
                    return this.spyClient.replace(key, expiresIn, object).get();
                }
            }
            return true;
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_SET_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
        return false;
    }

    /**
     * キャッシュをすべてクリアします.
     */
    public void clear() {
        try {
            this.spyClient.flush().get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_CLEAR_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_CLEAR_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーのキャッシュを削除.
     * @param key キャッシュキー
     */
    @Override
    public void delete(String key) {
        try {
            this.spyClient.delete(key).get();
        } catch (InterruptedException e) {
            PersoniumCoreLog.Server.MEMCACHED_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (ExecutionException e) {
            PersoniumCoreLog.Server.MEMCACHED_DELETE_FAIL.params(e.getMessage()).reason(e).writeLog();
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定したキーのオブジェクトを新規作成する.
     * @param key キャッシュキー
     * @param initValue 初期値
     * @return 作成に成功した場合または既に存在する場合はtrue, 失敗した場合はfalseを返す
     */
    public Boolean createLongValue(String key, long initValue) {
        try {
            long count = this.spyClient.incr(key, 0, initValue);
            return count == initValue;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーの値を返す.
     * @param key キャッシュキー
     * @return 指定キーの値
     */
    public long getLongValue(String key) {
        try {
            // 増分0でインクリメントすることで現在の設定値を取得する
            return this.spyClient.incr(key, 0);
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーの値をインクリメント.
     * @param key キャッシュキー
     * @return インクリメント後の値
     */
    public long incrementLongValue(String key) {
        try {
            return this.spyClient.incr(key, 1, 1);
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーの値をデクリメント.
     * @param key キャッシュキー
     * @return デクリメント後の値
     */
    public long decrementLongValue(String key) {
        try {
            long count = this.spyClient.decr(key, 1);
            if (count == 0) {
                delete(key);
            }
            return count;
        } catch (RuntimeException e) {
            log.info(e.getMessage(), e);
            throw new MemcachedClientException(e);
        }
    }

    /**
     * 指定キーの値を削除する.
     * @param key キャッシュキー
     */
    public void deleteLongValue(String key) {
        delete(key);
    }

    static Logger log = LoggerFactory.getLogger(MemcachedClient.class);

    static {
        if ("memcached".equals(PersoniumUnitConfig.getCacheType())) {
            cacheClient = new MemcachedClient(PersoniumUnitConfig.getCacheMemcachedHost(),
                    PersoniumUnitConfig.getCacheMemcachedPort(),
                    PersoniumUnitConfig.getCacheMemcachedOpTimeout());
        }
        if ("memcached".equals(PersoniumUnitConfig.getLockType())) {
            lockClient = new MemcachedClient(PersoniumUnitConfig.getLockMemcachedHost(),
                    PersoniumUnitConfig.getLockMemcachedPort(),
                    PersoniumUnitConfig.getLockMemcachedOpTimeout());
        }
    }
    /**
     * キャッシュに用いるクライアント.
     */
    static MemcachedClient cacheClient;
    /**
     * ロックに用いるクライアント.
     */
    static MemcachedClient lockClient;

    /**
     * @return キャッシュに用いるクライアント.
     */
    public static final MemcachedClient getCacheClient() {
        return cacheClient;
    }

    /**
     * @return ロックに用いるクライアント.
     */
    public static final MemcachedClient getLockClient() {
        return lockClient;
    }

    /**
     * isReportErrorの値の判定と、ログ出力を行う.
     */
    public static final void reportError() {
        if (isReportError) {
            log.info("Failed to connect memcached.");
        } else {
            log.error("Failed to connect memcached.");
            isReportError = true;
        }
    }

    /**
     * Memcachedクライアント用のExceptionクラス.
     */
    @SuppressWarnings("serial")
    public static class MemcachedClientException extends RuntimeException {
        /**
         * コンストラクタ.
         * @param cause 根本例外
         */
        public MemcachedClientException(Throwable cause) {
            super(cause);
        }
    };

}
