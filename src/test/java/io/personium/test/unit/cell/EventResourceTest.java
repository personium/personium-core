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
package io.personium.test.unit.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Reader;
import java.io.StringReader;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.eventbus.JSONEvent;
import io.personium.core.model.ctl.Common;
import io.personium.core.rs.cell.EventResource;
import io.personium.test.categories.Unit;

/**
 * LogResourceユニットテストクラス.
 */
@Category({Unit.class })
public class EventResourceTest {

    /**
     * テスト用LogResourceクラス.
     */
    private class TestEventResource extends EventResource {
        TestEventResource() {
            super(null, null, null);
        }

        /**
         * リクエストボディを解析してEventオブジェクトを取得する.
         * @param reader Http入力ストリーム
         * @return 解析したEventオブジェクト
         */
        @Override
        protected JSONEvent getRequestBody(final Reader reader) {
            return super.getRequestBody(reader);
        }

        /**
         * Event内の各プロパティ値をバリデートする.
         * @param event Eventオブジェクト
         */
        protected void validateEventProperties(final JSONEvent event) {
            super.validateEventProperties(event);
        }
    }

    /**
     * ヘッダの指定が無い場合デフォルト値が入ること.
     */
    @Test
    public void ヘッダの指定が無い場合デフォルト値が入ること() {
        String result = EventResource.validateXPersoniumRequestKey(null);
        assertTrue(result.startsWith("PCS-"));
        String timeStr = result.substring(4);
        Long.parseLong(timeStr);
    }

    /**
     * ヘッダに空文字を指定した場合空文字が入ること.
     */
    @Test
    public void ヘッダに空文字を指定した場合空文字が入ること() {
        String result = EventResource.validateXPersoniumRequestKey("");
        assertNotNull(result);
        assertEquals(0, result.length());
    }

    /**
     * ヘッダに1文字の文字列を指定した場合正しく扱われること.
     */
    @Test
    public void ヘッダに1文字の文字列を指定した場合正しく扱われること() {
        String result = EventResource.validateXPersoniumRequestKey("a");
        assertEquals("a", result);
    }

    /**
     * ヘッダに最大長の文字列を指定した場合正しく扱われること.
     */
    @Test
    public void ヘッダに最大長の文字列を指定した場合正しく扱われること() {
        String maxHeaderStr128 = "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 40char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 80char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 120char
                "12345678";

        String result = EventResource.validateXPersoniumRequestKey(maxHeaderStr128);
        assertEquals(maxHeaderStr128, result);
    }

    /**
     * ヘッダに最大長を超えた文字列を指定した場合エラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void ヘッダに最大長を超えた文字列を指定した場合エラーとなること() {
        String maxHeaderStr128 = "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 40char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 80char
                "abcdefghij" + "ABCDEFGHIJ" + "1234567890" + "-_-_-_-_-_" + // 120char
                "12345678";

        EventResource.validateXPersoniumRequestKey(maxHeaderStr128 + "X");
    }

    /**
     * ヘッダに不正な文字種を指定した場合エラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void ヘッダに不正な文字種を指定した場合エラーとなること() {
        EventResource.validateXPersoniumRequestKey("abc-012#");
    }

    @SuppressWarnings("unchecked")
    private JSONObject createEventBody() {
        JSONObject body = new JSONObject();
        body.put("level", "INFO");
        body.put("action", "POST");
        body.put("object", "ObjectData");
        body.put("result", "resultData");
        return body;
    }

    /**
     * リクエストボディに正しいJSONを指定した場合にEventオブジェクトが取得できること.
     */
    @Test
    public void リクエストボディに正しいJSONを指定した場合にEventオブジェクトが取得できること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("POST", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディに空データを指定した場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディに空データを指定した場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        StringReader reader = new StringReader("");
        resource.getRequestBody(reader);
    }

    /**
     * リクエストボディに空JSONを指定した場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディに空JSONを指定した場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = new JSONObject();
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのlevelがない場合に場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディのlevelがない場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.remove("level");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのlevelが正しい値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのlevelが正しい値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        // INFO
        body.put("level", "INFO");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        // info
        body.put("level", "info");
        reader = new StringReader(body.toJSONString());
        event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        // WARN
        body.put("level", "WARN");
        reader = new StringReader(body.toJSONString());
        event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("WARN", event.getLevel().toString());
        // warn
        body.put("level", "warn");
        reader = new StringReader(body.toJSONString());
        event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("WARN", event.getLevel().toString());
        // ERROR
        body.put("level", "ERROR");
        reader = new StringReader(body.toJSONString());
        event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("ERROR", event.getLevel().toString());
        // error
        body.put("level", "error");
        reader = new StringReader(body.toJSONString());
        event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("ERROR", event.getLevel().toString());
    }

    /**
     * リクエストボディのlevelが空文字の場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    @SuppressWarnings("unchecked")
    public void リクエストボディのlevelが空文字の場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("level", "");
        StringReader reader = new StringReader(body.toJSONString());
        resource.getRequestBody(reader);
    }

    /**
     * リクエストボディのlevelにFATALを指定した場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    @SuppressWarnings("unchecked")
    public void リクエストボディのlevelにFATALを指定した場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("level", "FATAL");
        StringReader reader = new StringReader(body.toJSONString());
        resource.getRequestBody(reader);
    }

    /**
     * リクエストボディのactionがない場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディのactionがない場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.remove("action");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのactionが文字列型上限値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが文字列型上限値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("action", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals(newValue.toString(), event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディのactionが文字列型上限値超えの場合に正常終了すること.
     */
    @Test(expected = PersoniumCoreException.class)
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが文字列型上限値超えの場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("action", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのactionが数値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが数値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("action", 1);
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("1", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディのactionが実数値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが実数値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("action", 100.100);
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("100.1", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディのactionが真偽値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが真偽値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("action", false);
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("false", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディのactionが日時の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのactionが日時の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("action", "\\/Date(1350451322147)\\/");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("\\/Date(1350451322147)\\/", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("resultData", event.getResult());
    }


    /**
     * リクエストボディのobjectがない場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディのobjectがない場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.remove("object");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのobjectが文字列型上限値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのobjectが文字列型上限値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("object", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("POST", event.getAction());
        assertEquals(newValue.toString(), event.getObject());
        assertEquals("resultData", event.getResult());
    }

    /**
     * リクエストボディのobjectが文字列型上限値超えの場合に正常終了すること.
     */
    @Test(expected = PersoniumCoreException.class)
    @SuppressWarnings("unchecked")
    public void リクエストボディのobjectが文字列型上限値超えの場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("object", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのobjectが数値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのobjectが数値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("object", 1);
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("POST", event.getAction());
        assertEquals("1", event.getObject());
        assertEquals("resultData", event.getResult());
    }


    /**
     * リクエストボディのresultがない場合にエラーとなること.
     */
    @Test(expected = PersoniumCoreException.class)
    public void リクエストボディのresultがない場合にエラーとなること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.remove("result");
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのresultが文字列型上限値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのresultが文字列型上限値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("result", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("POST", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals(newValue.toString(), event.getResult());
    }

    /**
     * リクエストボディのresultが文字列型上限値超えの場合に正常終了すること.
     */
    @Test(expected = PersoniumCoreException.class)
    @SuppressWarnings("unchecked")
    public void リクエストボディのresultが文字列型上限値超えの場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < Common.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("result", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
    }

    /**
     * リクエストボディのresultが数値の場合に正常終了すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void リクエストボディのresultが数値の場合に正常終了すること() {
        TestEventResource resource = new TestEventResource();
        JSONObject body = createEventBody();
        body.put("result", 1);
        StringReader reader = new StringReader(body.toJSONString());
        JSONEvent event = resource.getRequestBody(reader);
        resource.validateEventProperties(event);
        assertEquals("INFO", event.getLevel().toString());
        assertEquals("POST", event.getAction());
        assertEquals("ObjectData", event.getObject());
        assertEquals("1", event.getResult());
    }
}
