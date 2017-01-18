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
package com.fujitsu.dc.test.unit.core.model.progress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.model.progress.Progress;
import com.fujitsu.dc.core.model.progress.ProgressManager;
import com.fujitsu.dc.test.categories.Unit;

/**
 * MemcachedProgressManager ユニットテストクラス.
 */
@Category({ Unit.class })
public class MemcachedProgressManagerTest {

    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(MemcachedProgressManagerTest.class);

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
        log.info("ProcessType: " + ProgressManager.getStoreType());
    }

    /**
     * すべてのテスト完了時に実行される処理.
     */
    @AfterClass
    public static void afterClass() {
    }


    /**
     * 非同期処理状況データが格納されていない場合にNULLを返すこと.
     */
    @Test
    public void 非同期処理状況データが格納されていない場合にNULLを返すこと() {
        String key = "memcachedProgressTest_" + System.currentTimeMillis();
        Progress progress = ProgressManager.getProgress(key);
        assertNull(progress);
    }

    /**
     * 書き込んだ非同期処理状況データが格納されている場合にオブジェクトを返すこと.
     */
    @Test
    public void 書き込んだ非同期処理状況データが格納されている場合にオブジェクトを返すこと() {
        String key = "memcachedProgressTest_" + System.currentTimeMillis();
        String value = createJsonString("box_uuid");
        try {
            Progress progress = new Progress(key, value);
            ProgressManager.putProgress(key, progress);
            Progress response = ProgressManager.getProgress(key);
            assertEquals(progress.getKey(), response.getKey());
            assertEquals(progress.getValue(), response.getValue());
        } finally {
            ProgressManager.deleteProgress(key);
        }
    }

    /**
     * 更新した非同期処理状況データが格納されている場合に更新されたオブジェクトを返すこと.
     */
    @Test
    public void 更新した非同期処理状況データが格納されている場合に更新されたオブジェクトを返すこと() {
        String key = "memcachedProgressTest_" + System.currentTimeMillis();
        String value1 = createJsonString("box_uuid1");
        String value2 = createJsonString("box_uuid2");
        try {
            Progress progress = new Progress(key, value1);
            // 登録
            ProgressManager.putProgress(key, progress);
            Progress response = ProgressManager.getProgress(key);
            assertEquals(progress.getKey(), response.getKey());
            assertEquals(progress.getValue(), response.getValue());
            // 更新
            progress.setValue(value2);
            ProgressManager.putProgress(key, progress);
            response = ProgressManager.getProgress(key);
            assertEquals(progress.getKey(), response.getKey());
            assertEquals(progress.getValue(), response.getValue());
        } finally {
            ProgressManager.deleteProgress(key);
        }
    }

    /**
     * 非同期処理状況データが格納されている場合に削除できること.
     */
    @Test
    public void 非同期処理状況データが格納されている場合に削除できること() {
        String key = "memcachedProgressTest_" + System.currentTimeMillis();
        String value = createJsonString("box_uuid");
        try {
            Progress progress = new Progress(key, value);
            ProgressManager.putProgress(key, progress);
            Progress response = ProgressManager.getProgress(key);
            assertEquals(progress.getKey(), response.getKey());
            assertEquals(progress.getValue(), response.getValue());
            ProgressManager.deleteProgress(key);
            response = ProgressManager.getProgress(key);
            assertNull(response);
        } finally {
            ProgressManager.deleteProgress(key);
        }
    }

    @SuppressWarnings("unchecked")
    private String createJsonString(String boxId) {
        JSONObject json = new JSONObject();
        json.put("process", "barInstall");
        JSONObject barInfo = new JSONObject();
        json.put("barInfo", barInfo);
        barInfo.put("cell_id", "cell_id_value");
        barInfo.put("box_id", boxId);
        barInfo.put("start_time", "2013-10-18T10:10:10.000Z");
        barInfo.put("end_time", "2013-10-18T10:10:20.000Z");
        barInfo.put("status", "FAILED");
        barInfo.put("progress", "30%");
        JSONObject message = new JSONObject();
        barInfo.put("message", message);
        message.put("code", "PR409-OD-0003");
        message.put("value", "The entity already exists.");
        return json.toJSONString();
    }
}
