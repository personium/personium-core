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
package io.personium.core.eventbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.model.Cell;
import io.personium.core.model.ctl.Event;

/**
 * イベントバス用クラス.
 */
public class PersoniumEventBus {

    private static final int IDX_1ST_START = 0;
    private static final int IDX_1ST_END = 2;
    private static final int IDX_2ND_START = 2;
    private static final int IDX_2ND_END = 4;
    private Logger logger;

    private PersoniumEventBus() {
        this.logger = LoggerFactory.getLogger("io.personium.core.eventbus");
    }

    /**
     * コンストラクタ.
     * @param cell セル
     */
    public PersoniumEventBus(final Cell cell) {
        this();
        String unitUserName = getUnitUserName(cell.getOwner());
        String prefix1 = cell.getId().substring(IDX_1ST_START, IDX_1ST_END);
        String prefix2 = cell.getId().substring(IDX_2ND_START, IDX_2ND_END);
        String path = String.format("%s/%s/%s/%s", unitUserName, prefix1, prefix2, cell.getId());

        // MDCにCell名を設定
        MDC.put("eventlog_path", path);

    }

    /**
     * UnitUser名を取得する.
     * @param owner オーナー
     * @return UnitUser名
     */
    protected String getUnitUserName(final String owner) {
        String unitUserName = null;
        if (owner == null) {
            unitUserName = "anon";
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }
        return unitUserName;
    }

    /**
     * 受け付けたイベントの情報をログファイルへ出力する.
     * @param event Eventオブジェクト
     */
    public void outputEventLog(Event event) {

        if (event.getLevel() == Event.LEVEL.INFO) {
            logger.info(createLogContent(event));
        } else if (event.getLevel() == Event.LEVEL.WARN) {
            logger.warn(createLogContent(event));
        } else if (event.getLevel() == Event.LEVEL.ERROR) {
            logger.error(createLogContent(event));
        }
    }

    /**
     * ログ出力用文字列を作成する.
     * @param event Eventオブジェクト
     * @return ログ出力用文字列
     */
    private String createLogContent(Event event) {
        return String.format("%s,%s,%s,%s,%s,%s,%s",
                makeCsvItem(event.getRequestKey()),
                makeCsvItem(event.getName()),
                makeCsvItem(event.getSchema()),
                makeCsvItem(event.getSubject()),
                makeCsvItem(event.getAction()),
                makeCsvItem(event.getObject()),
                makeCsvItem(event.getResult()));
    }

    /**
     * CSVのitemを作成する.
     * @param item
     * @return CSV形式に変換した文字列
     */
    private String makeCsvItem(String item) {
        if (null == item) {
            return item;
        }
        String replacedItem = item.replaceAll("\"", "\"\"");
        return String.format("\"%s\"", replacedItem);
    }

}
