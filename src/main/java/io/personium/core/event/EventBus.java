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
package io.personium.core.event;

import java.util.HashMap;
import java.util.Map;

import io.personium.core.model.Cell;


/**
 * イベントを流すバス.
 * 将来的にはJMS等を使ってマシンをまたがってCell単位でひとつのバスとして動作させるが、
 * 現実装としては以下２点の機能のみを実現したいため、バスがマシン毎に分割されてしまって問題ない。
 * ・ログ出力
 * ・リスナ登録によるロジックの実行
 */
public final class EventBus {
    Map<String, EventLogger> eventLoggers = new HashMap<String, EventLogger>();
    /**
     * コンストラクタ.
     * @param cell Cell
     */
    public EventBus(Cell cell) {
        // TODO 本当は、Cellの設定を読み込んで、それぞれのイベントをどのレベルまでログ取得するべきかきめる。
        this.eventLoggers.put("access", new EventLogger(cell, PersoniumEvent.Level.WARN));
        this.eventLoggers.put("client", new EventLogger(cell, PersoniumEvent.Level.INFO));
    }

    /**
     * イベントを投げる.
     * @param ev イベント
     */
    public void post(final PersoniumEvent ev) {
        // 非同期で Subscriberに通知してゆく処理。
        // JMS ? Netty あたりを使って Engineに通知を出したい。
        // ログの出力。
        EventLogger logger = this.eventLoggers.get(ev.getName());
        logger.log(ev);
    }

}
