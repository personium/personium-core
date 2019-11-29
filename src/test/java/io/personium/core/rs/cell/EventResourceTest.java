/**
 * personium.io
 * Copyright 2014-2018 FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.DavRsCmp;
import io.personium.test.categories.Unit;

/**
 * EventResourceユニットテストクラス.
 */
@Category({Unit.class })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ EventResource.class, AccessContext.class })
@SuppressWarnings("unchecked")
public class EventResourceTest {

    private JSONObject createEventBody() {
        JSONObject body = new JSONObject();
        body.put("Type", "TypeData");
        body.put("Object", "ObjectData");
        body.put("Info", "InfoData");
        return body;
    }

    /**
     * リクエストボディに正しいJSONを指定した場合にEventオブジェクトが取得できること.
     * @throws Exception exception occurred in some errors
     */
    @Test
    public void validateEventProperties_Normal() throws Exception {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        EventResource resource = new EventResource(null, accessContext, davRsCmp);
        Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
        method1.setAccessible(true);
        Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
        method2.setAccessible(true);
        PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        method2.invoke(resource, event);

        // --------------------
        // Confirm result
        // --------------------
        assertEquals("TypeData", event.getType().get());
        assertEquals("ObjectData", event.getObject().get());
        assertEquals("InfoData", event.getInfo().get());
    }

    /**
     * リクエストボディに空データを指定した場合にエラーとなること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_body_is_empty_string() throws Throwable {
        // --------------------
        // Test method args
        // --------------------
        StringReader reader = new StringReader("");
        String requestKey = "KeyString";

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, null, null);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディに空JSONを指定した場合にエラーとなること.
     * @throws Throwable throwable occurred in test
     */
    @Test
    public void validateEventProperties_Normal_body_is_empty() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = new JSONObject();
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent event;
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // --------------------
        // Confirm result
        // --------------------
        assertEquals(null, event.getType().orElse(null));
        assertEquals(null, event.getObject().orElse(null));
        assertEquals(null, event.getInfo().orElse(null));
    }

    /**
     * リクエストボディのTypeがない場合にエラーとなること.
     * @throws Throwable throwable occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Type_is_null() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.remove("Type");
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent event;
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // --------------------
        // Confirm result
        // --------------------
        assertEquals(null, event.getType().orElse(null));
    }

    /**
     * リクエストボディのTypeが文字列型上限値の場合に正常終了すること.
     * @throws Exception exception occurred in test
     */
    @Test
    public void validateEventPropertes_Normal_Type_is_max_length() throws Exception {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("Type", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        EventResource resource = new EventResource(null, accessContext, davRsCmp);
        Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
        method1.setAccessible(true);
        Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
        method2.setAccessible(true);
        PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        method2.invoke(resource, event);

        // --------------------
        // Confirm result
        // --------------------
        assertEquals(newValue.toString(), event.getType().get());
        assertEquals("ObjectData", event.getObject().get());
        assertEquals("InfoData", event.getInfo().get());
    }

    /**
     * リクエストボディのTypeが文字列型上限値超えの場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void validateEventProperties_Error_Type_is_too_long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("Type", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのTypeが数値の場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_Type_is_Long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Type", 1);
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのTypeが実数値の場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_Type_is_Double() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Type", 100.100);
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのTypeが真偽値の場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_Type_is_boolean() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Type", false);
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのTypeが日時の場合に正常終了すること.
     * @throws Exception exception occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Type_is_Date() throws Exception {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Type", "\\/Date(1350451322147)\\/");
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        EventResource resource = new EventResource(null, accessContext, davRsCmp);
        Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
        method1.setAccessible(true);
        Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
        method2.setAccessible(true);
        PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        method2.invoke(resource, event);

        // --------------------
        // Confirm result
        // --------------------
        assertEquals("\\/Date(1350451322147)\\/", event.getType().get());
        assertEquals("ObjectData", event.getObject().get());
        assertEquals("InfoData", event.getInfo().get());
    }


    /**
     * リクエストボディのObjectがない場合にエラーとなること.
     * @throws Throwable throwable occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Object_is_null() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.remove("Object");
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent event;
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // --------------------
        // Confirm result
        // --------------------
        assertEquals(null, event.getObject().orElse(null));
    }

    /**
     * リクエストボディのObjectが文字列型上限値の場合に正常終了すること.
     * @throws Exception exception occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Object_is_max_length() throws Exception {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("Object", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        EventResource resource = new EventResource(null, accessContext, davRsCmp);
        Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
        method1.setAccessible(true);
        Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
        method2.setAccessible(true);
        PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        method2.invoke(resource, event);

        // --------------------
        // Confirm result
        // --------------------
        assertEquals("TypeData", event.getType().get());
        assertEquals(newValue.toString(), event.getObject().get());
        assertEquals("InfoData", event.getInfo().get());
    }

    /**
     * リクエストボディのObjectが文字列型上限値超えの場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void validateEventProperties_Error_Object_is_too_long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("Object", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのObjectが数値の場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_Object_is_Long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Object", 1);
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }


    /**
     * リクエストボディのInfoがない場合にエラーとなること.
     * @throws Throwable throwable occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Info_is_null() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.remove("Info");
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        PersoniumEvent event;
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

        // --------------------
        // Confirm result
        // --------------------
        assertEquals(null, event.getInfo().orElse(null));
    }

    /**
     * リクエストボディのInfoが文字列型上限値の場合に正常終了すること.
     * @throws Exception exception occurred in test
     */
    @Test
    public void validateEventProperties_Normal_Info_is_max_length() throws Exception {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH; i++) {
            newValue.append("a");
        }
        body.put("Info", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        EventResource resource = new EventResource(null, accessContext, davRsCmp);
        Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
        method1.setAccessible(true);
        Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
        method2.setAccessible(true);
        PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        method2.invoke(resource, event);

        // --------------------
        // Confirm result
        // --------------------
        assertEquals("TypeData", event.getType().get());
        assertEquals("ObjectData", event.getObject().get());
        assertEquals(newValue.toString(), event.getInfo().get());
    }

    /**
     * リクエストボディのInfoが文字列型上限値超えの場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void validateEventProperties_Error_Info_is_too_long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < PersoniumEvent.MAX_EVENT_VALUE_LENGTH + 1; i++) {
            newValue.append("a");
        }
        body.put("Info", newValue.toString());
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            Method method2 = EventResource.class.getDeclaredMethod("validateEventProperties", PersoniumEvent.class);
            method2.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
            method2.invoke(resource, event);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * リクエストボディのInfoが数値の場合に正常終了すること.
     * @throws Throwable throwable occurred in test
     */
    @Test(expected = PersoniumCoreException.class)
    public void getRequestBody_Error_Info_is_Long() throws Throwable {
        AccessContext accessContext = mock(AccessContext.class);
        DavRsCmp davRsCmp = mock(DavRsCmp.class);

        // --------------------
        // Test method args
        // --------------------
        String schema = "https://personium/cell/";
        String subject = "https://personium/cell#account";
        JSONObject body = createEventBody();
        body.put("Info", 1);
        StringReader reader = new StringReader(body.toJSONString());
        String requestKey = "KeyString";

        // --------------------
        // Mock settings
        // --------------------
        doReturn(schema).when(accessContext).getSchema();
        doReturn(subject).when(accessContext).getSubject();
        doReturn(accessContext).when(davRsCmp).getAccessContext();
        doReturn(requestKey).when(davRsCmp).getRequestKey();
        doReturn(null).when(davRsCmp).getEventId();
        doReturn(null).when(davRsCmp).getRuleChain();

        // --------------------
        // Run method
        // --------------------
        try {
            EventResource resource = new EventResource(null, accessContext, davRsCmp);
            Method method1 = EventResource.class.getDeclaredMethod("getRequestBody", Reader.class);
            method1.setAccessible(true);
            PersoniumEvent event = (PersoniumEvent) method1.invoke(resource, reader);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
