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


import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.MemcachedClient.MemcachedClientException;

/**
 * Lockを管理するユーティリティ.
 */
public abstract class AccountLockManager extends LockManager {

    abstract Lock getLock(String fullKey);
    abstract Boolean putLock(String fullKey, Lock lock);

    /**
     * UnitUserごとのデータアクセスを一時的に参照モードにするLockカテゴリ.
     */
    public static final String CATEGORY_ACCOUNT_LOCK = "AccountLock_";

    /**
     * memcachedにAccountLockを書き込む.
     * ロック時間はpropertiesに従う.
     * @param accountId 認証に失敗したアカウントID
     */
    public static void registAccountLockObjct(final String accountId) {
        String key =  CATEGORY_ACCOUNT_LOCK + accountId;
        Boolean success = singleton.doPutAccountLock(key, "", accountLockLifeTime);
        if (success) {
            return;
        }
        throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
    }

    /**
     * AccountLockの状態確認.
     * @param accountId アカウントID
     * @return TRUE：Lock／FALSE：Unlock
     */
    public static boolean hasLockObject(final String accountId) {
        try {
            String key =  CATEGORY_ACCOUNT_LOCK + accountId;
            // 対象アカウントのLock確認
            String lockPublic = singleton.doGetAccountLock(key);
            return lockPublic != null;
        } catch (MemcachedClientException e) {
            throw PersoniumCoreException.Server.SERVER_CONNECTION_ERROR;
        }
    }
}
