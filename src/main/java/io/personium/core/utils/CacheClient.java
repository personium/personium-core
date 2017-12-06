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

/**
 * Cache操作用のインタフェースクラス.
 */
public interface CacheClient {

    /**
     * 指定キーのキャッシュを取得.
     * @param <T> 取得する型
     * @param key キャッシュキー
     * @param clazz 取得する型、 型に問題があるときはClassCastExcetpion発生
     * @return キャッシュされたオブジェクト / null キャッシュが存在しないとき
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 指定キーでオブジェクトを一定の有効期限のみキャッシュします.
     * @param key キャッシュのキー
     * @param expiresIn 有効期間
     * @param object キャッシュすべきオブジェクト
     * @return 処理成功時はTrue/失敗時はFalseを返す.
     */
    Boolean put(String key, int expiresIn, Object object);

    /**
     * 指定キーのキャッシュを削除.
     * @param key キャッシュキー
     */
    void delete(String key);

}
