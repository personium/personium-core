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
package com.fujitsu.dc.core.model;

import com.fujitsu.dc.core.auth.AccessContext;


/**
 * JaxRS Resource オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class BoxRsCmp extends DavRsCmp {

    DavCmp davCmp;
    Cell cell;
    AccessContext accessContext;
    Box box;

    /**
     * コンストラクタ.
     * @param davCmp DavCmp
     * @param cell Cell
     * @param accessContext AccessContext
     * @param box ボックス
     */
    public BoxRsCmp(final DavCmp davCmp, final Cell cell, final AccessContext accessContext, final Box box) {
        super(null, davCmp);
        this.cell = cell;
        this.accessContext = accessContext;
        this.box = box;
        this.davCmp = davCmp;
    }
    /**
     * このリソースのURLを返します.
     * @return URL文字列
     */
    public String getUrl() {
        // 再帰的に最上位のBoxResourceまでいって、BoxResourceではここをオーバーライドしてルートURLを与えている。
        //return this.parent.getUrl() + "/" + this.pathName;
        return this.cell.getUrl() + this.box.getName();
    }

    /**
     * リソースが所属するCellを返す.
     * @return Cellオブジェクト
     */
    public Cell getCell() {
        // 再帰的に最上位のBoxResourceまでいって、そこからCellにたどりつくため、BoxResourceではここをオーバーライドしている。
        return this.cell;
    }
    /**
     * リソースが所属するBoxを返す.
     * @return Boxオブジェクト
     */
    public Box getBox() {
        // 再帰的に最上位のBoxResourceまでいって、そこからCellにたどりつくため、BoxResourceではここをオーバーライドしている。
        return this.box;
    }
    /**
     * このリソースのdavCmpを返します.
     * @return davCmp
     */
    public DavCmp getDavCmp() {
        return this.davCmp;
    }

    /**
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }
}
