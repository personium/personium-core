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
package com.fujitsu.dc.core.model.impl.es.cache;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.core.utils.MemcachedClient.MemcachedClientException;
import com.fujitsu.dc.test.categories.Unit;

/**
 * CellCache ユニットテストクラス.
 */
@Category({Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({CellCache.class, DcCoreConfig.class })
public class CellCacheTest {

    /**
     * Memcachedへの接続に失敗した場合NULLを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Memcachedへの接続に失敗した場合NULLを返すこと() throws Exception {
        String cellName = "cellCacheTestCell";
        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = spy(c.newInstance());
        doThrow(new MemcachedClientException(null)).when(mockMemcachedClient)
                .get(anyString(), any(Class.class));

        // CellCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(CellCache.class);
        PowerMockito.when(CellCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        Map<String, Object> cache = CellCache.get(cellName);
        assertNull(cache);
    }

    /**
     * Memcachedからキャッシュを取得できた場合オブジェクトを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Memcachedからキャッシュを取得できた場合オブジェクトを返すこと() throws Exception {
        String cellName = "cellCacheTestCell";
        Map<String, Object> cell = new HashMap<String, Object>();
        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = spy(c.newInstance());
        doReturn(cell).when(mockMemcachedClient).get(anyString(), any(Class.class));

        // CellCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(CellCache.class);
        PowerMockito.when(CellCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        Map<String, Object> cache = CellCache.get(cellName);
        assertEquals(cell, cache);
    }

    /**
     * Cellのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void Cellのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと() throws Exception {

        String cellName = "cellCacheTestCell";
        Map<String, Object> cellToCache = new HashMap<String, Object>();
        cellToCache.put("CellCacheTestKey001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "cell:" + cellName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(CellCache.class);
        PowerMockito.when(CellCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(false);

        // キャッシュ登録されないこと
        CellCache.cache(cellName, cellToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();

        // キャッシュを取得できないこと
        Map<String, Object> cellFromCache = CellCache.get(cellName);
        assertThat(cellFromCache).isNull();
    }

    /**
     * Cellのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void Cellのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと() throws Exception {

        String cellName = "cellCacheTestCell";
        Map<String, Object> cellToCache = new HashMap<String, Object>();
        cellToCache.put("CellCacheTestKey001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "cell:" + cellName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(CellCache.class);
        PowerMockito.when(CellCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録
        CellCache.cache(cellName, cellToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(cellToCache);
        Map<String, Object> cellFromCache = CellCache.get(cellName);
        assertThat(cellFromCache).isEqualTo(cellFromCache);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(false);

        // キャッシュを取得できないこと
        cellFromCache = CellCache.get(cellName);
        assertThat(cellFromCache).isNull();

        // キャッシュを削除できないこと
        CellCache.clear(cellName);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(cellToCache);
    }

    /**
     * Cellのキャッシュを有効にしている場合キャッシュの登録取得削除ができること.
     * @throws Exception 実行エラー
     */
    @Test
    public void Cellのキャッシュを有効にしている場合キャッシュの登録取得削除ができること() throws Exception {

        String cellName = "cellCacheTestCell";
        Map<String, Object> cellToCache = new HashMap<String, Object>();
        cellToCache.put("CellCacheTestKey001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "cell:" + cellName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(CellCache.class);
        PowerMockito.when(CellCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isCellCacheEnabled").thenReturn(true);

        // キャッシュに登録できること
        CellCache.cache(cellName, cellToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(cellToCache);

        // キャッシュから取得できること
        Map<String, Object> cellFromCache = CellCache.get(cellName);
        assertThat(cellFromCache).isEqualTo(cellFromCache);

        // キャッシュを削除できること
        CellCache.clear(cellName);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();
    }

}
