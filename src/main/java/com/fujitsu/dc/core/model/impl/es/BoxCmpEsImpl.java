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
package com.fujitsu.dc.core.model.impl.es;

import org.apache.wink.webdav.model.ObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.core.model.BoxCmp;
import com.fujitsu.dc.core.model.DavCmp;
import com.fujitsu.dc.core.model.impl.es.accessor.DavNodeAccessor;

/**
 * Boxに対応した処理を扱う部品のESを使った実装.
 */
public class BoxCmpEsImpl extends DavCmpEsImpl implements BoxCmp {
    DavNodeAccessor esCol;

    /**
     * ログ.
     */
    static Logger log = LoggerFactory.getLogger(BoxCmpEsImpl.class);

    /**
     * コンストラクタ.
     * @param box Boxオブジェクト
     */
    public BoxCmpEsImpl(final Box box) {
        this.of = new ObjectFactory();
        this.box = box;
        this.cell = box.getCell();
        this.esCol = EsModel.col(box.getCell());
        this.nodeId = this.box.getId();
        try {
            // nodeIDを元に管理データをDBからロードする
            this.load();
        } catch (Exception e) {
            log.debug("Exception occured. Maybe Dav index is not present so creating..");
        }

        if (this.davNode == null) {
            // 管理データのロードに失敗した場合（col.boxが存在しない場合）はcol.box情報を作成する
            this.createRootDoc();
        }
    }

    @Override
    public final DavNodeAccessor getEsColType() {
        return this.esCol;
    }

    @Override
    public String getUrl() {
        return this.cell.getUrl() + this.box.getName();
    }

    private void createRootDoc() {
        this.davNode = new DavNode(this.cell.getId(), this.box.getId(), DavCmp.TYPE_COL_BOX);
        this.esCol.create(this.box.getId(), this.davNode);
    }

    @Override
    public DcCoreException getNotFoundException() {
        return DcCoreException.Dav.BOX_NOT_FOUND;
    }
}
