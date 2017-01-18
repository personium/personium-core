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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.impl.es.CellEsImpl;
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.core.utils.MemcachedClient.MemcachedClientException;
import com.fujitsu.dc.test.categories.Unit;

/**
 * BoxCache ユニットテストクラス.
 */
@Category({Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({BoxCache.class, DcCoreConfig.class })
public class BoxCacheTest {

    /**
     * Memcachedへの接続に失敗した場合NULLを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Memcachedへの接続に失敗した場合NULLを返すこと() throws Exception {
        String boxName = "boxCacheTestBox";

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doThrow(new MemcachedClientException(null)).when(mockMemcachedClient)
                .get(Mockito.anyString(), Mockito.any(Class.class));

        // BoxCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(BoxCache.class);
        PowerMockito.when(BoxCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        CellEsImpl cell = new CellEsImpl();
        cell.setId("CellId");
        Box cache = BoxCache.get(boxName, cell);
        assertNull(cache);
    }

    /**
     * Memcachedからキャッシュを取得できた場合オブジェクトを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void Memcachedからキャッシュを取得できた場合オブジェクトを返すこと() throws Exception {
        String boxName = "boxCacheTestBox";
        Map<String, Object> box = new HashMap<String, Object>();
        box.put("name", boxName);
        box.put("schema", null);
        box.put("id", "boxId");
        box.put("published", 1380604545159L);

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doReturn(box).when(mockMemcachedClient).get(Mockito.anyString(), Mockito.any(Class.class));

        // BoxCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(BoxCache.class);
        PowerMockito.when(BoxCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        CellEsImpl cell = new CellEsImpl();
        cell.setId("CellId");
        Box cache = BoxCache.get(boxName, cell);
        assertNotNull(cache);
    }

    /**
     * Boxのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void Boxのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと() throws Exception {
        String cellId = "boxCacheTestCell";
        String boxName = "boxCacheTestBox";
        String boxId = "boxCacheTestBox001";
        String boxSchema = "boxSchema";
        long boxPublished = new Date().getTime();

        CellEsImpl cell = new CellEsImpl();
        cell.setId(cellId);
        Box boxToCache = new Box(cell, boxName, boxSchema, boxId, boxPublished);

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "box:" + cellId + ":" + boxName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(BoxCache.class);
        PowerMockito.when(BoxCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(false);

        // キャッシュ登録されないこと
        BoxCache.cache(boxToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();

        // キャッシュを取得できないこと
        Box boxFromCache = BoxCache.get(boxName, cell);
        assertThat(boxFromCache).isNull();
    }

    /**
     * Boxのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void Boxのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと() throws Exception {
        String cellId = "boxCacheTestCell";
        String boxName = "boxCacheTestBox";
        String boxId = "boxCacheTestBox001";
        String boxSchema = "boxSchema";
        long boxPublished = new Date().getTime();

        CellEsImpl cell = new CellEsImpl();
        cell.setId(cellId);
        Box boxToCache = new Box(cell, boxName, boxSchema, boxId, boxPublished);

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "box:" + cellId + ":" + boxName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(BoxCache.class);
        PowerMockito.when(BoxCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録
        BoxCache.cache(boxToCache);
        isEqualTo(boxToCache, mockMemcachedClient.get(cacheKeyForMock, Map.class));
        isEqualTo(boxToCache, BoxCache.get(boxName, cell));

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(false);

        // キャッシュを取得できないこと
        Box boxFromCache = BoxCache.get(boxName, cell);
        assertThat(boxFromCache).isNull();

        // キャッシュを削除できないこと
        BoxCache.clear(boxName, cell);
        isEqualTo(boxToCache, mockMemcachedClient.get(cacheKeyForMock, Map.class));
    }

    /**
     * Boxのキャッシュを有効にしている場合キャッシュの登録取得削除ができること.
     * @throws Exception 実行エラー
     */
    @Test
    public void Boxのキャッシュを有効にしている場合キャッシュの登録取得削除ができること() throws Exception {
        String cellId = "boxCacheTestCell";
        String boxName = "boxCacheTestBox";
        String boxId = "boxCacheTestBox001";
        String boxSchema = "boxSchema";
        long boxPublished = new Date().getTime();

        CellEsImpl cell = new CellEsImpl();
        cell.setId(cellId);
        Box boxToCache = new Box(cell, boxName, boxSchema, boxId, boxPublished);

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "box:" + cellId + ":" + boxName;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(BoxCache.class);
        PowerMockito.when(BoxCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isBoxCacheEnabled").thenReturn(true);

        // キャッシュに登録できること
        BoxCache.cache(boxToCache);
        isEqualTo(boxToCache, mockMemcachedClient.get(cacheKeyForMock, Map.class));

        // キャッシュを取得できること
        isEqualTo(boxToCache, BoxCache.get(boxName, cell));

        // キャッシュを削除できること
        BoxCache.clear(boxName, cell);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();
    }

    void isEqualTo(Box expected, @SuppressWarnings("rawtypes") Map actual) {
        assertThat(actual.get("id")).isEqualTo(expected.getId());
        assertThat(actual.get("name")).isEqualTo(expected.getName());
        assertThat(actual.get("schema")).isEqualTo(expected.getSchema());
        assertThat(actual.get("published")).isEqualTo(expected.getPublished());
    }

    void isEqualTo(Box expected, Box actual) {
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getSchema()).isEqualTo(expected.getSchema());
        assertThat(actual.getPublished()).isEqualTo(expected.getPublished());
    }
}
