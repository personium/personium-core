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
package com.fujitsu.dc.test.unit.core.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.Marker;

import com.fujitsu.dc.core.event.DcEvent;
import com.fujitsu.dc.core.event.EventLogger;
import com.fujitsu.dc.core.model.impl.es.CellEsImpl;
import com.fujitsu.dc.test.categories.Unit;

/**
 * EventLogger ユニットテストクラス.
 */
@Category({ Unit.class })
public class EventLoggerTest {

    /**
     * テスト用Loggerクラス.
     *
     */
    class LoggerForEvent implements Logger {
        int loggedLevel = 0;
        @Override
        public String getName() {
            return null;
        }
        @Override
        public boolean isTraceEnabled() {
            return false;
        }
        @Override
        public void trace(String msg) {
        }
        @Override
        public void trace(String format, Object arg) {
        }
        @Override
        public void trace(String format, Object arg1, Object arg2) {
        }
        @Override
        public void trace(String format, Object[] argArray) {
        }
        @Override
        public void trace(String msg, Throwable t) {
        }
        @Override
        public boolean isTraceEnabled(Marker marker) {
            return false;
        }
        @Override
        public void trace(Marker marker, String msg) {
        }
        @Override
        public void trace(Marker marker, String format, Object arg) {
        }
        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
        }
        @Override
        public void trace(Marker marker, String format, Object[] argArray) {
        }
        @Override
        public void trace(Marker marker, String msg, Throwable t) {
        }
        @Override
        public boolean isDebugEnabled() {
            return false;
        }
        @Override
        public void debug(String msg) {
        }
        @Override
        public void debug(String format, Object arg) {
        }
        @Override
        public void debug(String format, Object arg1, Object arg2) {
        }
        @Override
        public void debug(String format, Object[] argArray) {
        }
        @Override
        public void debug(String msg, Throwable t) {
        }
        @Override
        public boolean isDebugEnabled(Marker marker) {
            return false;
        }
        @Override
        public void debug(Marker marker, String msg) {
        }
        @Override
        public void debug(Marker marker, String format, Object arg) {
        }
        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {
        }
        @Override
        public void debug(Marker marker, String format, Object[] argArray) {
        }
        @Override
        public void debug(Marker marker, String msg, Throwable t) {
        }
        @Override
        public boolean isInfoEnabled() {
            return false;
        }
        @Override
        public void info(String msg) {
            this.loggedLevel = DcEvent.Level.INFO;
        }
        @Override
        public void info(String format, Object arg) {
        }
        @Override
        public void info(String format, Object arg1, Object arg2) {
        }
        @Override
        public void info(String format, Object[] argArray) {
        }
        @Override
        public void info(String msg, Throwable t) {
        }
        @Override
        public boolean isInfoEnabled(Marker marker) {
            return false;
        }
        @Override
        public void info(Marker marker, String msg) {
        }
        @Override
        public void info(Marker marker, String format, Object arg) {
        }
        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {
        }
        @Override
        public void info(Marker marker, String format, Object[] argArray) {
        }
        @Override
        public void info(Marker marker, String msg, Throwable t) {
        }
        @Override
        public boolean isWarnEnabled() {
            return false;
        }
        @Override
        public void warn(String msg) {
            this.loggedLevel = DcEvent.Level.WARN;
        }
        @Override
        public void warn(String format, Object arg) {
        }
        @Override
        public void warn(String format, Object[] argArray) {
        }
        @Override
        public void warn(String format, Object arg1, Object arg2) {
        }
        @Override
        public void warn(String msg, Throwable t) {
        }
        @Override
        public boolean isWarnEnabled(Marker marker) {
            return false;
        }
        @Override
        public void warn(Marker marker, String msg) {
        }
        @Override
        public void warn(Marker marker, String format, Object arg) {
        }
        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
        }
        @Override
        public void warn(Marker marker, String format, Object[] argArray) {
        }
        @Override
        public void warn(Marker marker, String msg, Throwable t) {
        }
        @Override
        public boolean isErrorEnabled() {
            return false;
        }
        @Override
        public void error(String msg) {
            this.loggedLevel = DcEvent.Level.ERROR;
        }
        @Override
        public void error(String format, Object arg) {
        }
        @Override
        public void error(String format, Object arg1, Object arg2) {
        }
        @Override
        public void error(String format, Object[] argArray) {
        }
        @Override
        public void error(String msg, Throwable t) {
        }
        @Override
        public boolean isErrorEnabled(Marker marker) {
            return false;
        }
        @Override
        public void error(Marker marker, String msg) {
        }
        @Override
        public void error(Marker marker, String format, Object arg) {
        }
        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {
        }
        @Override
        public void error(Marker marker, String format, Object[] argArray) {
        }
        @Override
        public void error(Marker marker, String msg, Throwable t) {
        }
        public int getLoggedLevel() {
            return this.loggedLevel;
        }
    }

    /**
     * イベントロガーに正しくログ出力ができること.
     */
    @Test
    public void イベントロガーに正しくログ出力ができること() {
        Logger defaultLogger = null;
        Class<?> clazz = EventLogger.class;
        Field baseDir = null;
        try {
            baseDir = clazz.getDeclaredField("log");
            baseDir.setAccessible(true);
            defaultLogger = (Logger) baseDir.get(null);
            LoggerForEvent testLogger = new LoggerForEvent();
            baseDir.set(null, testLogger);
            EventLogger evLogger = new EventLogger(new CellEsImpl(), DcEvent.Level.INFO);
            assertEquals(DcEvent.Level.INFO, evLogger.getLogLevel());
            evLogger.log(new DcEvent("", "", DcEvent.Level.ERROR, "", "", "", ""));
            assertEquals(DcEvent.Level.ERROR, testLogger.getLoggedLevel());
            evLogger.log(new DcEvent("", "", DcEvent.Level.WARN, "", "", "", ""));
            assertEquals(DcEvent.Level.WARN, testLogger.getLoggedLevel());
            evLogger.log(new DcEvent("", "", DcEvent.Level.INFO, "", "", "", ""));
            assertEquals(DcEvent.Level.INFO, testLogger.getLoggedLevel());
            evLogger.setLogLevel(DcEvent.Level.ERROR);
            assertEquals(DcEvent.Level.ERROR, evLogger.getLogLevel());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        } finally {
            if (baseDir != null) {
                try {
                    baseDir.set(null, defaultLogger);
                } catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }
            }
        }

    }


    /**
     * イベントロガーのログ出力可能レベルが変更できること.
     */
    @Test
    public void イベントロガーのログ出力可能レベルが変更できること() {
            EventLogger evLogger = new EventLogger(new CellEsImpl(), DcEvent.Level.INFO);
            assertEquals(DcEvent.Level.INFO, evLogger.getLogLevel());
            evLogger.setLogLevel(DcEvent.Level.ERROR);
            assertEquals(DcEvent.Level.ERROR, evLogger.getLogLevel());
    }
}
