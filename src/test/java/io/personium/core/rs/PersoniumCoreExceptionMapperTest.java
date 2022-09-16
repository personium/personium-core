/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
package io.personium.core.rs;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import io.personium.test.categories.Unit;

/**
 * Unit test for PersoniumCoreExceptionMapper.
 * Testing log output and error response.
 */
@Category({ Unit.class })
public final class PersoniumCoreExceptionMapperTest {
    static Logger log = LoggerFactory.getLogger(PersoniumCoreExceptionMapper.class);
    static Logger shelterdLogger;
    /**
     * BeforeClass.
     */
    @BeforeClass
    public static void beforeClass() {
        shelterdLogger = PersoniumCoreExceptionMapper.log;
    }
    /**
     * AfterClass.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumCoreExceptionMapper.log = shelterdLogger;
    }

    /**
     * Test for toResponse method.
     */
    @Test
    public void toResponse() {
        PersoniumCoreExceptionMapper mapper = new PersoniumCoreExceptionMapper();
        PersoniumCoreExceptionMapper.log = new TestLogger() {
            @Override
            public void error(String msg, Throwable t) {
                log.debug(msg);
                StackTraceElement[] ste = t.getStackTrace();
                // Message should include the getMessage() content of the throwable
                assertTrue(msg.indexOf(t.getMessage()) > 0);
                // Message should include the Class Name of the throwable's StackTrace 1st Element.
                assertTrue(msg.indexOf(ste[0].getClassName()) > 0);
                // Message should include the Method Name of the throwable's StackTrace 1st Element.
                assertTrue(msg.indexOf(ste[0].getMethodName()) > 0);
                // Message should include the Line Number of the throwable's StackTrace 1st Element.
                log.debug(String.valueOf(ste[0].getLineNumber()));
                assertTrue(msg.indexOf(String.valueOf(ste[0].getLineNumber())) > 0);
            }

        };

        try {
            ExceptionGeneratorForTest i = new ExceptionGeneratorForTest();
            i.process();
        } catch (Exception exception) {
            Response res = mapper.toResponse(exception);
            res.getStatus();
        }

    }


    /**
     * Exception generator for testing.
     */
    private static class ExceptionGeneratorForTest {
        public String process() {
            int a = 0;
            return "" + 1 / a;
        }
    }

    /**
     * Mock Logger.
     */
    static class TestLogger implements Logger {
        @Override
        public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
        }
        @Override
        public void warn(Marker arg0, String arg1, Throwable arg2) {
        }
        @Override
        public void warn(Marker arg0, String arg1, Object... arg2) {
        }
        @Override
        public void warn(Marker arg0, String arg1, Object arg2) {
        }
        @Override
        public void warn(String arg0, Object arg1, Object arg2) {
        }
        @Override
        public void warn(Marker arg0, String arg1) {
        }
        @Override
        public void warn(String arg0, Throwable arg1) {
        }
        @Override
        public void warn(String arg0, Object... arg1) {
        }
        @Override
        public void warn(String arg0, Object arg1) {
        }
        @Override
        public void warn(String arg0) {
        }
        @Override
        public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
        }
        @Override
        public void trace(Marker arg0, String arg1, Throwable arg2) {
        }
        @Override
        public void trace(Marker arg0, String arg1, Object... arg2) {
        }
        @Override
        public void trace(Marker arg0, String arg1, Object arg2) {
        }
        @Override
        public void trace(String arg0, Object arg1, Object arg2) {
        }
        @Override
        public void trace(Marker arg0, String arg1) {
        }
        @Override
        public void trace(String arg0, Throwable arg1) {
        }
        @Override
        public void trace(String arg0, Object... arg1) {
        }
        @Override
        public void trace(String arg0, Object arg1) {
        }
        @Override
        public void trace(String arg0) {
        }
        @Override
        public boolean isWarnEnabled(Marker arg0) {
            return true;
        }
        @Override
        public boolean isWarnEnabled() {
            return false;
        }
        @Override
        public boolean isTraceEnabled(Marker arg0) {
            return false;
        }
        @Override
        public boolean isTraceEnabled() {
            return false;
        }
        @Override
        public boolean isInfoEnabled(Marker arg0) {
            return false;
        }
        @Override
        public boolean isInfoEnabled() {
            return false;
        }
        @Override
        public boolean isErrorEnabled(Marker arg0) {
            return false;
        }
        @Override
        public boolean isErrorEnabled() {
            return false;
        }
        @Override
        public boolean isDebugEnabled(Marker arg0) {
            return false;
        }
        @Override
        public boolean isDebugEnabled() {
            return false;
        }
        @Override
        public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
        }
        @Override
        public void info(Marker arg0, String arg1, Throwable arg2) {
        }
        @Override
        public void info(Marker arg0, String arg1, Object... arg2) {
        }
        @Override
        public void info(Marker arg0, String arg1, Object arg2) {
        }
        @Override
        public void info(String arg0, Object arg1, Object arg2) {
        }
        @Override
        public void info(Marker arg0, String arg1) {
        }
        @Override
        public void info(String arg0, Throwable arg1) {
        }
        @Override
        public void info(String arg0, Object... arg1) {
        }
        @Override
        public void info(String arg0, Object arg1) {
        }
        @Override
        public void info(String arg0) {
        }
        @Override
        public String getName() {
            return null;
        }
        @Override
        public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
        }
        @Override
        public void error(Marker arg0, String arg1, Throwable arg2) {
        }
        @Override
        public void error(Marker arg0, String arg1, Object... arg2) {
        }
        @Override
        public void error(Marker arg0, String arg1, Object arg2) {
        }
        @Override
        public void error(String arg0, Object arg1, Object arg2) {
        }
        @Override
        public void error(Marker arg0, String arg1) {
        }
        @Override
        public void error(String msg, Throwable t) {
        }
        @Override
        public void error(String arg0, Object... arg1) {
        }
        @Override
        public void error(String arg0, Object arg1) {
        }
        @Override
        public void error(String arg0) {
        }
        @Override
        public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
        }
        @Override
        public void debug(Marker arg0, String arg1, Throwable arg2) {
        }
        @Override
        public void debug(Marker arg0, String arg1, Object... arg2) {
        }
        @Override
        public void debug(Marker arg0, String arg1, Object arg2) {
        }
        @Override
        public void debug(String arg0, Object arg1, Object arg2) {
        }
        @Override
        public void debug(Marker arg0, String arg1) {
        }
        @Override
        public void debug(String arg0, Throwable arg1) {
        }
        @Override
        public void debug(String arg0, Object... arg1) {
        }
        @Override
        public void debug(String arg0, Object arg1) {
        }
        @Override
        public void debug(String arg0) {
        }
    }
}

