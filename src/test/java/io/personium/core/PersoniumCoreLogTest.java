/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import io.personium.plugin.base.PluginMessageUtils.Severity;
import io.personium.test.categories.Unit;

/**
 * Unit test for PersoniumCoreLog class.
 */

@Category({ Unit.class })
public class PersoniumCoreLogTest {
    static Logger log = LoggerFactory.getLogger(PersoniumCoreLog.class);
    static Logger shelterdLogger;
    /**
     * BeforeClass.
     */
    @BeforeClass
    public static void beforeClass() {
        shelterdLogger = PersoniumCoreLog.log;
    }
    /**
     * AfterClass.
     */
    @AfterClass
    public static void afterClass() {
        PersoniumCoreLog.log = shelterdLogger;
    }
    /**
     * Normal case testing using params, writeLog methods.
     */
    @Test
    public void normal_check_params_and_writeLog() {
        final String replaceValue = "AAAAABBBBBCCCCCDDDDDD";
        PersoniumCoreLog coreLog = PersoniumCoreLog.Dav.ROLE_NOT_FOUND;
        PersoniumCoreLog.log = new TestLogger() {
            @Override
            public void info(String msg) {
                doLog(msg);
            }
            @Override
            public void warn(String msg) {
                doLog(msg);
            }
            @Override
            public void error(String msg) {
                doLog(msg);
            }
            private void doLog(String msg) {
                log.debug(msg);
                // the messege should include the given replacement text with the params method, .
                assertTrue(msg.indexOf(replaceValue) > 0);

                // テストの呼び出し元からの階層
                int callStackLevel = 3;
                StackTraceElement[] ste = (new Throwable()).getStackTrace();
                // メッセージにログ出力元のクラス名が含まれていることを確認
                log.debug(ste[callStackLevel].getClassName());
                assertTrue(msg.indexOf(ste[callStackLevel].getClassName()) > 0);
                // メッセージにログ出力元のメソッド名が含まれていることを確認
                log.debug(ste[callStackLevel].getMethodName());
                assertTrue(msg.indexOf(ste[callStackLevel].getMethodName()) > 0);
                // メッセージにログ出力元の行数が含まれていることを確認
                log.debug(String.valueOf(ste[callStackLevel].getLineNumber()));
                assertTrue(msg.indexOf(String.valueOf(ste[callStackLevel].getLineNumber())) > 0);
            }
        };
        coreLog.params(replaceValue).writeLog();
    }

    /**
     * Default Log Level should be WARN when log level configuration is omitted.
     */
    @Test
    public void writeLog_DefaultLogLevel_ShouldBe_WARN_WhenConfigIsOmitted() {
        PersoniumCoreLog coreLog = PersoniumCoreLog.Misc.UNREACHABLE_CODE_ERROR;
        PersoniumCoreLog.log = new TestLogger() {
            @Override
            public void info(String msg) {
                fail("Default leve should be WARN.");
            }
            @Override
            public void warn(String msg) {
                doLog(msg);
            }
            @Override
            public void error(String msg) {
                fail("Default leve should be WARN.");
            }
            private void doLog(String msg) {
                log.debug(msg);

                // テストの呼び出し元からの階層
                int callStackLevel = 3;
                StackTraceElement[] ste = (new Throwable()).getStackTrace();
                // メッセージにログ出力元のクラス名が含まれていることを確認
                log.debug(ste[callStackLevel].getClassName());
                assertTrue(msg.indexOf(ste[callStackLevel].getClassName()) > 0);
                // メッセージにログ出力元のメソッド名が含まれていることを確認
                log.debug(ste[callStackLevel].getMethodName());
                assertTrue(msg.indexOf(ste[callStackLevel].getMethodName()) > 0);
                // メッセージにログ出力元の行数が含まれていることを確認
                log.debug(String.valueOf(ste[callStackLevel].getLineNumber()));
                assertTrue(msg.indexOf(String.valueOf(ste[callStackLevel].getLineNumber())) > 0);
            }
        };

        coreLog.params("test").writeLog();
    }

    /**
     * Should throw an RuntimeException if unkonwn key is given.
     * @throws RuntimeException RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void create_ShouldThrow_RutimeException_When_UnknownKey_IsGiven() throws RuntimeException {
        PersoniumCoreLog.create("UNKNOWN");
    }

    /**
     * ログレベル設定を切り替えてPersoniumCoreLogを生成する.
     */
    @Test
    public void ログレベル設定を切り替えてPersoniumCoreLogを生成する() {
        final String errorMsg = "ERROR Message.";
        final String warningMsg = "WARNING Message.";
        final String infoMsg = "INFO Message.";
        PersoniumCoreLog.log = new TestLogger() {
            @Override
            public void info(String msg) {
                assertTrue(msg.indexOf(infoMsg) > 0);
            }
            @Override
            public void warn(String msg) {
                assertTrue(msg.indexOf(warningMsg) > 0);
            }
            @Override
            public void error(String msg) {
                assertTrue(msg.indexOf(errorMsg) > 0);
            }
        };
        PersoniumCoreLog infoLog = new PersoniumCoreLog("TEST", Severity.INFO, infoMsg);
        infoLog.writeLog();
        PersoniumCoreLog warningLog = new PersoniumCoreLog("TEST", Severity.WARN, warningMsg);
        warningLog.writeLog();
        PersoniumCoreLog errorLog = new PersoniumCoreLog("TEST", Severity.ERROR, errorMsg);
        errorLog.writeLog();
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
        public void warn(Marker arg0, String arg1, Object[] arg2) {
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
        public void warn(String arg0, Object[] arg1) {
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
        public void trace(Marker arg0, String arg1, Object[] arg2) {
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
        public void trace(String arg0, Object[] arg1) {
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
        public void info(Marker arg0, String arg1, Object[] arg2) {
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
        public void info(String arg0, Object[] arg1) {
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
        public void error(Marker arg0, String arg1, Object[] arg2) {
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
        public void error(String arg0, Object[] arg1) {
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
        public void debug(Marker arg0, String arg1, Object[] arg2) {
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
        public void debug(String arg0, Object[] arg1) {
        }
        @Override
        public void debug(String arg0, Object arg1) {
        }
        @Override
        public void debug(String arg0) {
        }
    }
}
