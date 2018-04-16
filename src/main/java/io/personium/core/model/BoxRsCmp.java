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
package io.personium.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.wink.webdav.model.Multistatus;

import io.personium.core.auth.AccessContext;
import io.personium.core.utils.UriUtils;


/**
 * JaxRS Resource オブジェクトから処理の委譲を受けてDav関連の永続化を除く処理を行うクラス.
 */
public class BoxRsCmp extends DavRsCmp {

    Cell cell;
    AccessContext accessContext;
    Box box;

    /**
     * コンストラクタ.
     * @param cellRsCmp CellRsCmp
     * @param davCmp DavCmp
     * @param accessContext AccessContext
     * @param box ボックス
     */
    public BoxRsCmp(final CellRsCmp cellRsCmp, final DavCmp davCmp, final AccessContext accessContext, final Box box) {
        super(cellRsCmp, davCmp);
        this.cell = cellRsCmp.getCell();
        this.accessContext = accessContext;
        this.box = box;
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
     * @return AccessContext
     */
    public AccessContext getAccessContext() {
        return this.accessContext;
    }

    /**
     * Get rootprops xml object.
     * @return Multistatus xml object
     */
    public Multistatus getRootProps() {
        String url = getEsacapingUrl();
        String localBoxUrl = UriUtils.convertSchemeFromHttpToLocalBox(url, url);
        // The actural processing
        final Multistatus ms = this.of.createMultistatus();
        List<org.apache.wink.webdav.model.Response> responseList = ms.getResponse();
        responseList.addAll(createPropfindResponseList(pathName, localBoxUrl, this.davCmp));
        return ms;
    }

    /**
     * Create propfind response list.<p>
     * Called recursively, including children.<p>
     * It is the same as calling propfind method with depth=Infinity.
     * @param pathName WebDav object name
     * @param href href of propfind response
     * @param dCmp DavCmp
     * @return Propfind response list
     */
    private List<org.apache.wink.webdav.model.Response> createPropfindResponseList(
            String pathName, String href, DavCmp dCmp) {
        List<org.apache.wink.webdav.model.Response> resList = new ArrayList<org.apache.wink.webdav.model.Response>();
        resList.add(createDavResponse(pathName, href, dCmp, null, true));
        Map<String, DavCmp> childrenMap = dCmp.getChildren();
        for (String childName : childrenMap.keySet()) {
            DavCmp child = childrenMap.get(childName);
            if (!href.endsWith("/")) {
                href += "/";
            }
            resList.addAll(createPropfindResponseList(childName, href + childName, child));
        }
        return resList;
    }
}
