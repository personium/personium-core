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
package io.personium.core.event;

import io.personium.core.model.Cell;
import io.personium.core.utils.UriUtils;

/**
 * Bus for sendig event.
 */
public final class EventBus {
    Cell cell;

    /**
     * Constructor.
     * @param cell Cell
     */
    public EventBus(Cell cell) {
        this.cell = cell;
    }

    /**
     * Post event.
     * @param ev event
     */
    public void post(final PersoniumEvent ev) {
        // set cell id
        ev.setCellId(this.cell.getId());

        // convert url to personium-localcell scheme
        String object = ev.getObject();
        String cellUrl = this.cell.getUrl();
        if (object != null && object.startsWith(cellUrl)) {
            String local = object.replaceFirst(cellUrl, UriUtils.SCHEME_LOCALCELL + ":/");
            ev.setObject(local);
        }

        // send event to JMS
        EventSender.send(ev);
    }

}
