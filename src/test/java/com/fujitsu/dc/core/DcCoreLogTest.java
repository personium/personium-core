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
package com.fujitsu.dc.core;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.fujitsu.dc.core.DcCoreMessageUtils.Severity;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.DcRunner;

/**
 * EsModelの単体テストケース.
 */

@RunWith(DcRunner.class)
@Category({ Unit.class })
public class DcCoreLogTest {
    static Logger log = LoggerFactory.getLogger(DcCoreLog.class);
    // ロガー差し替えをするので、ここに避難させておく.
    static Logger shelterdLogger;
    /**
     * BeforeClass.
     */
    @BeforeClass
    public static void beforeClass() {
        shelterdLogger = DcCoreLog.log;
    }
    /**
     * AfterClass.
     */
    @AfterClass
    public static void afterClass() {
        DcCoreLog.log = shelterdLogger;
    }
    /**
     * ログ出力正常系のテスト.
     */
    @Test
    public void ログ出力正常系のテスト() {
        final String replaceValue = "AAAAABBBBBCCCCCDDDDDD";
        DcCoreLog coreLog = DcCoreLog.Dav.ROLE_NOT_FOUND;
        DcCoreLog.log = new TestLogger() {
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
                // 置き換え文字列が置き換えられてメッセージがフォーマット通り出力されていることの確認
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
     * ログレベル設定を省略するとWARNレベルでログが出力されることを確認.
     */
    @Test
    public void ログレベル設定を省略するとWARNレベルでログが出力されることを確認() {
        DcCoreLog coreLog = DcCoreLog.Misc.UNREACHABLE_CODE_ERROR;
        DcCoreLog.log = new TestLogger() {
            @Override
            public void info(String msg) {
                fail("WARNレベル以外でログが出力.");
            }
            @Override
            public void warn(String msg) {
                doLog(msg);
            }
            @Override
            public void error(String msg) {
                fail("WARNレベル以外でログが出力.");
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
     * 存在しないメッセージIDを指定すると実行時例外が発生すること.
     * @throws RuntimeException RuntimeException
     */
    @Test(expected = RuntimeException.class)
    public void 存在しないメッセージIDを指定すると実行時例外が発生すること() throws RuntimeException {
        DcCoreLog.create("UNKNOWN");
    }

    /**
     * ログレベル設定を切り替えてDcCoreLogを生成する.
     */
    @Test
    public void ログレベル設定を切り替えてDcCoreLogを生成する() {
        final String errorMsg = "ERROR Message.";
        final String warningMsg = "WARNING Message.";
        final String infoMsg = "INFO Message.";
        DcCoreLog.log = new TestLogger() {
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
        DcCoreLog infoLog = new DcCoreLog("TEST", Severity.INFO, infoMsg);
        infoLog.writeLog();
        DcCoreLog warningLog = new DcCoreLog("TEST", Severity.WARN, warningMsg);
        warningLog.writeLog();
        DcCoreLog errorLog = new DcCoreLog("TEST", Severity.ERROR, errorMsg);
        errorLog.writeLog();
    }

    /**
     * Mockロガー.
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
