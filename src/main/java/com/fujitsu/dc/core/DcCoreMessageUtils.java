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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * ログメッセージ作成クラス.
 */
public abstract class DcCoreMessageUtils {
    /**
     * ログレベルの設定を保持する.
     */
    private static final Properties LOG_LEVEL_PROP;
    /**
     * ログメッセージの設定を保持する.
     */
    private static final Properties LOG_MSG_PROP;

    /**
     * ログレベル設定のキー.
     * 後ろにメッセージコードをつけるのでドットまで定義
     * 例）com.fujitsu.dc.core.loglevel.PR400-OD-0001
     */
    public static final String LOG_LEVEL = DcCoreConfig.KEY_ROOT + "loglevel.";

    /**
     * ログメッセージ設定のキー.
     * 後ろにメッセージコードをつけるのでドットまで定義
     */
    public static final String LOG_MESSAGE = DcCoreConfig.KEY_ROOT + "msg.";

    /**
     * ログレベルに対応する例外の深刻さ.
     */
    public enum Severity {
        /**
         * ERRORレベル.
         */
        ERROR,
        /**
         * WARNレベル.
         */
        WARN,
        /**
         * INFOレベル.
         */
        INFO,
        /**
         * DEBUGレベル.
         */
        DEBUG
    }

    static {
        LOG_LEVEL_PROP = doLoad("dc-log-level.properties");
        LOG_MSG_PROP = doLoad("dc-messages.properties");
    }

    private static Properties doLoad(String file) {
        Properties prop = new Properties();
        prop.clear();
        InputStream is = DcCoreConfig.class.getClassLoader().getResourceAsStream(file);
        try {
            prop.load(is);
        } catch (IOException e) {
            throw new RuntimeException("failed to load config!", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("failed to close config stream", e);
            }
        }
        return prop;
    }

    /**
     * コンストラクタ.
     */
    private DcCoreMessageUtils() {
    }

    /**
     * 設定ファイルからログレベルの取得.
     * @param code メッセージコード
     * @return ログレベル
     */
    public static Severity getSeverity(String code) {
        String logLevel = LOG_LEVEL_PROP.getProperty(LOG_LEVEL + code);
        Severity severity = null;
        if (Severity.DEBUG.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.DEBUG;
        } else if (Severity.INFO.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.INFO;
        } else if (Severity.WARN.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.WARN;
        } else if (Severity.ERROR.toString().equalsIgnoreCase(logLevel)) {
            severity = Severity.ERROR;
        }
        return severity;
    }

    /**
     * 設定ファイルからメッセージの取得.
     * @param code メッセージコード
     * @return メッセージ
     */
    public static String getMessage(String code) {
        String msg = LOG_MSG_PROP.getProperty(LOG_MESSAGE + code);
        if (msg == null) {
            // ログが定義されていなかったら例外
            throw new RuntimeException("message undefined for code=[" + code + "].");
        }
        return msg;
    }
}
