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
import static org.junit.Assert.assertFalse;
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
import com.fujitsu.dc.core.utils.MemcachedClient;
import com.fujitsu.dc.core.utils.MemcachedClient.MemcachedClientException;
import com.fujitsu.dc.test.categories.Unit;

/**
 * UserDataSchemaCache ユニットテストクラス.
 */
@Category({Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({UserDataSchemaCache.class, DcCoreConfig.class })
public class UserDataSchemaCacheTest {

    /**
     * getメソッドでMemcachedへの接続に失敗した場合NULLを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getメソッドでMemcachedへの接続に失敗した場合NULLを返すこと() throws Exception {
        String nodeId = "nodeId";

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doThrow(new MemcachedClientException(null)).when(mockMemcachedClient)
                .get(Mockito.anyString(), Mockito.any(Class.class));

        // UserDataSchemaCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        Map<String, Object> cache = UserDataSchemaCache.get(nodeId);
        assertNull(cache);
    }

    /**
     * getメソッドでMemcachedからキャッシュを取得できた場合オブジェクトを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void getメソッドでMemcachedからキャッシュを取得できた場合オブジェクトを返すこと() throws Exception {
        String nodeId = "nodeId";
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("testKey", "testValue");

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doReturn(data).when(mockMemcachedClient).get(Mockito.anyString(), Mockito.any(Class.class));

        // UserDataSchemaCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // getメソッドのテスト
        Map<String, Object> cache = UserDataSchemaCache.get(nodeId);
        // キャッシュが有効の場合、キャッシュに設定したオブジェクトが返却される
        assertEquals(data, cache);
    }

    /**
     * isChangedメソッドでMemcachedへの接続に失敗した場合Trueを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void isChangedメソッドでMemcachedへの接続に失敗した場合Trueを返すこと() throws Exception {
        String nodeId = "nodeId";
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("testKey", "testValue");

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doThrow(new MemcachedClientException(null)).when(mockMemcachedClient)
                .get(Mockito.anyString(), Mockito.any(Class.class));

        // UserDataSchemaCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // isChangedメソッドのテスト
        boolean isChanged = UserDataSchemaCache.isChanged(nodeId, data);
        // キャッシュが有効の場合、trueが返却される
        assertEquals(true, isChanged);
    }

    /**
     * isChangedメソッドでMemcachedからキャッシュを取得できた場合falseを返すこと.
     * @throws Exception テスト中の例外
     */
    @SuppressWarnings("unchecked")
    @Test
    public void isChangedメソッドでMemcachedからキャッシュを取得できた場合falseを返すこと() throws Exception {
        String nodeId = "nodeId";
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("testKey", "testValue");

        // MemcachedClientのモックを作成(getしたときにMemchachedClientExceptionをthrowするモック)
        Constructor<MemcachedClient> c = MemcachedClient.class.getDeclaredConstructor();
        c.setAccessible(true);
        MemcachedClient mockMemcachedClient = Mockito.spy(c.newInstance());
        Mockito.doReturn(data).when(mockMemcachedClient).get(Mockito.anyString(), Mockito.any(Class.class));

        // UserDataSchemaCacheのモックを作成(MemcachedClientのモックを使用するモック)
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // isChangedメソッドのテスト
        boolean isChanged = UserDataSchemaCache.isChanged(nodeId, data);
        assertFalse(isChanged);
    }

    /**
     * UserDataSchemaのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void UserDataSchemaのキャッシュを無効にしている場合キャッシュの登録を実行しても何も行わないこと() throws Exception {
        String nodeId = "node_XXXXXXXXXXX1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("schema001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "userodata:" + nodeId;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(false);

        // キャッシュ登録されないこと
        UserDataSchemaCache.cache(nodeId, schemaToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();

        // キャッシュを取得できないこと
        Map<String, Object> schemaFromCache = UserDataSchemaCache.get(nodeId);
        assertThat(schemaFromCache).isNull();
    }

    /**
     * UserDataSchemaのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void UserDataSchemaのキャッシュを無効にしている場合キャッシュの取得削除を実行しても何も行わないこと() throws Exception {

        String nodeId = "node_YYYYYYYYYY1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "userodata:" + nodeId;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録
        UserDataSchemaCache.cache(nodeId, schemaToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(schemaToCache);
        Map<String, Object> schemaFromCache = UserDataSchemaCache.get(nodeId);
        assertThat(schemaFromCache).isEqualTo(schemaFromCache);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(false);

        // キャッシュを取得できないこと
        schemaFromCache = UserDataSchemaCache.get(nodeId);
        assertThat(schemaFromCache).isNull();

        // キャッシュを削除できないこと
        UserDataSchemaCache.clear(nodeId);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(schemaToCache);
    }

    /**
     * UserDataSchemaのキャッシュを無効にしている場合isChangedメソッドがfalseを返却すること.
     * @throws Exception 実行エラー
     */
    @Test
    public void UserDataSchemaのキャッシュを無効にしている場合isChangedメソッドがfalseを返却すること() throws Exception {
        String nodeId = "node_VVVVVVVVV1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");
        Long now = new Date().getTime();
        schemaToCache.put("disabledTime", now);

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を無効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(false);

        // isChanged?
        Map<String, Object> schemaToCacheNew = new HashMap<String, Object>();
        schemaToCacheNew.put("SchemaCacheTestKey002", "testValue");
        schemaToCacheNew.put("disabledTime", now + 1);
        assertThat(UserDataSchemaCache.isChanged(nodeId, schemaToCacheNew)).isFalse();
    }

    /**
     * isChangedメソッドですでに登録済みのキャッシュと変更が無い場合falseを返すこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void isChangedメソッドですでに登録済みのキャッシュと変更が無い場合falseを返すこと() throws Exception {
        String nodeId = "node_VVVVVVVVV1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");
        Long now = new Date().getTime();
        schemaToCache.put("disabledTime", now);

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録する
        UserDataSchemaCache.cache(nodeId, schemaToCache);

        // isChanged?
        Map<String, Object> schemaToCacheNew = new HashMap<String, Object>();
        schemaToCacheNew.put("SchemaCacheTestKey001", "testValue");
        schemaToCacheNew.put("disabledTime", now);
        assertThat(UserDataSchemaCache.isChanged(nodeId, schemaToCacheNew)).isFalse();
    }

    /**
     * isChangedメソッドで登録済みのキャッシュが存在しない場合trueを返すこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void isChangedメソッドで登録済みのキャッシュが存在しない場合trueを返すこと() throws Exception {
        String nodeId = "node_VVVVVVVVV1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");
        Long now = new Date().getTime();
        schemaToCache.put("disabledTime", now);

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // isChanged?
        Map<String, Object> schemaToCacheNew = new HashMap<String, Object>();
        schemaToCacheNew.put("SchemaCacheTestKey002", "testValue");
        schemaToCacheNew.put("disabledTime", now + 1);
        assertThat(UserDataSchemaCache.isChanged(nodeId, schemaToCacheNew)).isTrue();
    }

    /**
     * isChangedメソッドでキャッシュ無効化時間が登録済みのキャッシュと異なる場合trueを返すこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void isChangedメソッドでキャッシュ無効化時間が登録済みのキャッシュと異なる場合trueを返すこと() throws Exception {
        String nodeId = "node_VVVVVVVVV1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");
        Long now = new Date().getTime();
        schemaToCache.put("disabledTime", now);

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録する
        UserDataSchemaCache.cache(nodeId, schemaToCache);

        // isChanged?
        Map<String, Object> schemaToCacheNew = new HashMap<String, Object>();
        schemaToCacheNew.put("SchemaCacheTestKey002", "testValue");
        schemaToCacheNew.put("disabledTime", now + 1);
        assertThat(UserDataSchemaCache.isChanged(nodeId, schemaToCacheNew)).isTrue();
    }

    /**
     * isChangedメソッドでキャッシュ無効化時間が登録済みのキャッシュと同じ場合falseを返すこと.
     * @throws Exception 実行エラー
     */
    @Test
    public void isChangedメソッドでキャッシュ無効化時間が登録済みのキャッシュと同じ場合falseを返すこと() throws Exception {
        String nodeId = "node_VVVVVVVVV1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");
        Long now = new Date().getTime();
        schemaToCache.put("disabledTime", now);

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // 事前にキャッシュを登録する
        UserDataSchemaCache.cache(nodeId, schemaToCache);

        // isChanged?
        Map<String, Object> schemaToCacheNew = new HashMap<String, Object>();
        schemaToCacheNew.put("SchemaCacheTestKey002", "testValue");
        schemaToCacheNew.put("disabledTime", now);
        assertThat(UserDataSchemaCache.isChanged(nodeId, schemaToCacheNew)).isFalse();
    }

    /**
     * UserDataSchemaのキャッシュを有効にしている場合キャッシュの登録取得削除ができること.
     * @throws Exception 実行エラー
     */
    @Test
    public void UserDataSchemaのキャッシュを有効にしている場合キャッシュの登録取得削除ができること() throws Exception {

        String nodeId = "node_ZZZZZZZZZZ1";
        Map<String, Object> schemaToCache = new HashMap<String, Object>();
        schemaToCache.put("SchemaCacheTestKey001", "testValue");

        // Mock用キャッシュクライアントを直接操作するためのキー
        String cacheKeyForMock = "userodata:" + nodeId;

        // テスト用のキャッシュクラスに接続するよう設定を変更
        MockMemcachedClient mockMemcachedClient = new MockMemcachedClient();
        PowerMockito.spy(UserDataSchemaCache.class);
        PowerMockito.when(UserDataSchemaCache.class, "getMcdClient").thenReturn(mockMemcachedClient);

        // キャッシュの設定を有効にする
        PowerMockito.spy(DcCoreConfig.class);
        PowerMockito.when(DcCoreConfig.class, "isSchemaCacheEnabled").thenReturn(true);

        // キャッシュに登録できること
        UserDataSchemaCache.cache(nodeId, schemaToCache);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isEqualTo(schemaToCache);

        // キャッシュから取得できること
        Map<String, Object> schemaFromCache = UserDataSchemaCache.get(nodeId);
        assertThat(schemaFromCache).isEqualTo(schemaFromCache);

        // キャッシュを削除できること
        UserDataSchemaCache.clear(nodeId);
        assertThat(mockMemcachedClient.get(cacheKeyForMock, Map.class)).isNull();
    }
}
