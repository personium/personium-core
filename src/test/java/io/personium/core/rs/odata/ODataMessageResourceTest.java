/**
 * personium.io
 * Copyright 2014-2017 FUJITSU LIMITED
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.RequestObject;
import io.personium.core.model.ctl.SentMessage;
import io.personium.test.categories.Unit;

/**
 * ODataMessageResource unit test classs.
 */
@Category({ Unit.class })
public class ODataMessageResourceTest {

    /** Target class of unit test. */
    private ODataMessageResource oDataMessageResource;

    /**
     * Before.
     */
    @Before
    public void befor() {
        oDataMessageResource = spy(new ODataMessageResource(null, null, null, null));
    }

    /**
     * ToがURL形式の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがURL形式の場合にPersoniumCoreExceptionが発生しないこと() {
        oDataMessageResource.validateUriCsv(SentMessage.P_TO.getName(), "http://example.com/test/");
    }

    /**
     * ToがCSV複数URL形式の場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void ToがCSV複数URL形式の場合にPersoniumCoreExceptionが発生しないこと() {
        oDataMessageResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test/,http://example.com/test/");
    }

    /**
     * ToがURL形式でない場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToがURL形式でない場合にPersoniumCoreExceptionが発生すること() {
        oDataMessageResource.validateUriCsv(SentMessage.P_TO.getName(), "ftp://example.com/test");

    }

    /**
     * ToがCSV複数URL形式とURL形式でない場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void ToがCSV複数URL形式とURL形式でない場合にPersoniumCoreExceptionが発生すること() {
        oDataMessageResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test,ftp://example.com/test");
    }

    /**
     * Toが不正なCSV形式の場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Toが不正なCSV形式の場合にPersoniumCoreExceptionが発生すること() {
        oDataMessageResource.validateUriCsv(SentMessage.P_TO.getName(),
                "http://example.com/test/,,http://example.com/test");
    }

    /**
     * Bodyが0byteの場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Bodyが0byteの場合にPersoniumCoreExceptionが発生しないこと() {
        String body = "";
        oDataMessageResource.validateBody(body, SentMessage.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Bodyが64Kbyteの場合にPersoniumCoreExceptionが発生しないこと.
     */
    @Test
    public final void Bodyが64Kbyteの場合にPersoniumCoreExceptionが発生しないこと() {
        char[] buff = new char[65536];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = 0x41;
        }
        String body = String.valueOf(buff);

        oDataMessageResource.validateBody(body, SentMessage.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Bodyが64Kbyteを超える場合にPersoniumCoreExceptionが発生すること.
     */
    @Test(expected = PersoniumCoreException.class)
    public final void Bodyが64Kbyteを超える場合にPersoniumCoreExceptionが発生すること() {
        char[] buff = new char[65537];
        for (int i = 0; i < buff.length; i++) {
            buff[i] = 0x41;
        }
        String body = String.valueOf(buff);

        oDataMessageResource.validateBody(body, SentMessage.MAX_MESSAGE_BODY_LENGTH);
    }

    /**
     * Test validateRequestRelation().
     * Normal test.
     * Name is relation.
     */
    @Test
    public void validateRequestRelation_Normal_name_is_relation() {
        // --------------------
        // Test method args
        // --------------------
        String name = "-relationName_+:";
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateRequestRelation().
     * Normal test.
     * ClassUrl is relation.
     */
    @Test
    public void validateRequestRelation_Normal_classurl_is_relation() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/AppCell/__relation/__/-relationName_+:/";
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateRequestRelation().
     * Error test.
     * Name is null and ClassUrl is null.
     */
    @Test
    public void validateRequestRelation_Error_name_is_null_and_classurl_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_NAME.getName())
                    + "," + oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRelation().
     * Error test.
     * TargetUrl is null.
     */
    @Test
    public void validateRequestRelation_Error_targeturl_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/AppCell/__relation/__/-relationName_+:/";
        String targetUrl = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRelation().
     * Error test.
     * Name is invalid format.
     */
    @Test
    public void validateRequestRelation_Error_name_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String name = "_rerationName-+:";
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRelation().
     * Error test.
     * ClassUrl is invalid format.
     */
    @Test
    public void validateRequestRelation_Error_classurl_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/Cell/relationName";
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRelation(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRole().
     * Normal test.
     * Name is role.
     */
    @Test
    public void validateRequestRole_Normal_name_is_role() {
        // --------------------
        // Test method args
        // --------------------
        String name = "roleName-_";
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateRequestRole().
     * Normal test.
     * ClassUrl is role.
     */
    @Test
    public void validateRequestRole_Normal_classurl_is_role() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/AppCell/__role/__/roleName-_/";
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
        } catch (PersoniumCoreException e) {
            fail("Exception occurred.");
        }
    }

    /**
     * Test validateRequestRole().
     * Error test.
     * Name is null and ClassUrl is null.
     */
    @Test
    public void validateRequestRole_Error_name_is_null_and_classurl_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_NAME.getName())
                    + "," + oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRole().
     * Error test.
     * TargetUrl is null.
     */
    @Test
    public void validateRequestRole_Error_targeturl_is_null() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/AppCell/__role/__/roleName-_/";
        String targetUrl = null;

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_TARGET_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRole().
     * Error test.
     * Name is invalid format.
     */
    @Test
    public void validateRequestRole_Error_name_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String name = "-roleName_";
        String classUrl = null;
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_NAME.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

    /**
     * Test validateRequestRole().
     * Error test.
     * ClassUrl is invalid format.
     */
    @Test
    public void validateRequestRole_Error_classurl_is_invalid_format() {
        // --------------------
        // Test method args
        // --------------------
        String name = null;
        String classUrl = "http://personium/Cell/roleName";
        String targetUrl = "http://personium/ExtCell/";

        // --------------------
        // Mock settings
        // --------------------
        // Nothing.

        // --------------------
        // Expected result
        // --------------------
        // Nothing.

        // --------------------
        // Run method
        // --------------------
        try {
            oDataMessageResource.validateRequestRole(name, classUrl, targetUrl);
            fail("Not exception.");
        } catch (PersoniumCoreException e) {
            // --------------------
            // Confirm result
            // --------------------
            PersoniumCoreException expected = PersoniumCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(
                    oDataMessageResource.concatRequestObjectPropertyName(RequestObject.P_CLASS_URL.getName()));
            assertThat(e.getStatus(), is(expected.getStatus()));
            assertThat(e.getCode(), is(expected.getCode()));
            assertThat(e.getMessage(), is(expected.getMessage()));
        }
    }

}
