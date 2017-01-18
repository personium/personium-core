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
package com.fujitsu.dc.core.model.progress;

import org.json.simple.JSONObject;

/**
 * 非同期処理状況のためのインターフェース定義.
 */
public interface ProgressInfo {

    /**
     * 処理状況（ステータス）.
     */
    enum STATUS {
        /** 処理中. */
        PROCESSING("installation in progress"),
        /** 処理完了（正常終了）. **/
        COMPLETED("ready"),
        /** 処理完了（異常終了）. */
        FAILED("installation failed"),
        /** キャンセル完了. */
        CANCELLED("installation cancelled"); // 未使用

        private String message;

        /**
         * コンストラクタ.
         * @param message メッセージ
         */
        STATUS(String message) {
            this.message = message;
        }

        /**
         * 各Enum値に対応したメッセージを取得する.
         * @return メッセージ
         */
        public String value() {
            return message;
        }
    }

    /**
     * 保存されているデータの内容をJSON形式で取得する.
     * @return JSONオブジェクト.
     */
    JSONObject getJsonObject();
}

