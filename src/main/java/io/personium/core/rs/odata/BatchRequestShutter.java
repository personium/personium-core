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
package io.personium.core.rs.odata;

import javax.ws.rs.HttpMethod;

import io.personium.core.PersoniumCoreException;

/**
 * Batchリクエスト中にToo Many Concurrentが発生後の実行/スキップを制御するクラス.
 */
public class BatchRequestShutter {

    private boolean shuttered = false;

    /**
     * Too Many Concurrent発生後の場合true, それ以外はfalse.
     * @return Too Many Concurrent発生後の場合true, それ以外はfalse
     */
    public boolean isShuttered() {
        return shuttered;
    }

    /**
     * Batchリクエスト中にToo Many Concurrentが発生したかどうかのステータスを更新する.
     * @param e 発生した例外
     */
    public void updateStatus(Exception e) {
        if (PersoniumCoreException.Misc.TOO_MANY_CONCURRENT_REQUESTS.equals(e)) {
            shuttered = true;
        }
    }

    /**
     * Batch内の個々のリクエスを実行してよいかを判定する. <br />
     * Batchリクエスト中ですでにToo Many Concurrentが発生し、かつ、更新系メソッドの場合は実行不可とする.
     * @param httpMethod メソッド名
     * @return true: 実行可能, false: 実行不可
     */
    public boolean accept(String httpMethod) {
        if (!isShuttered()) {
            return true;
        }
        return HttpMethod.GET.equals(httpMethod);
    }

}
