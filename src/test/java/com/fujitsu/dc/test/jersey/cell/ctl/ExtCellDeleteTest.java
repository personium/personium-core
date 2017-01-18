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
package com.fujitsu.dc.test.jersey.cell.ctl;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.model.ctl.ExtCell;
import com.fujitsu.dc.core.model.ctl.Relation;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.RelationUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;

/**
 * ExtRoleの削除のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtCellDeleteTest extends ODataCommon {

    private static String cellName = "testcell1";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtCellDeleteTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * RoleとLinkされているExtCellを削除するとresponseが409であること.
     */
    @Test
    public final void RoleとLinkされているExtCellを削除するとresponseが409であること() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String extCellUrl = UrlUtils.cellRoot("cellHoge");
        String boxName = null;
        String roleName = "role";
        String roleUrl = UrlUtils.roleUrl(cellName, boxName, roleName);

        try {
            // 準備。ExtCell、ロール作ってリンクさせる。
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            RoleUtils.create(cellName, token, boxName, roleName, HttpStatus.SC_CREATED);
            ResourceUtils.linkExtCelltoRole(DcCoreUtils.encodeUrlComp(extCellUrl), cellName,
                    roleUrl, token, HttpStatus.SC_NO_CONTENT);

            // 削除できないことの確認
            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_CONFLICT);

            // リンクを解除し、削除できるようになることの確認
            ResourceUtils.linkExtCellRoleDelete(cellName, token, DcCoreUtils.encodeUrlComp(extCellUrl),
                    boxName, roleName);
            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            ResourceUtils.linkExtCellRoleDelete(cellName, token,
                    DcCoreUtils.encodeUrlComp(extCellUrl), boxName, roleName);
            RoleUtils.delete(cellName, token, boxName, roleName, -1);
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * RelationとLinkされているExtCellを削除するとresponseが409であること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void RelationとLinkされているExtCellを削除するとresponseが409であること() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String extCellUrl = UrlUtils.cellRoot("cellhoge");
        String boxName = null;
        String relationName = "relationhoge";
        JSONObject body = new JSONObject();
        body.put("Name", relationName);
        body.put("_Box.Name", boxName);

        try {
            // 準備。ExtCell、Relation作ってリンクさせる。
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            RelationUtils.create(cellName, token, body, HttpStatus.SC_CREATED);
            ResourceUtils.linksWithBody(cellName, Relation.EDM_TYPE_NAME, relationName, "null",
                    ExtCell.EDM_TYPE_NAME, UrlUtils.extCellResource(cellName, extCellUrl),
                    token, HttpStatus.SC_NO_CONTENT);

            // 削除できないことの確認
            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_CONFLICT);

            // リンクを解除し、削除できるようになることの確認
            ResourceUtils.linksDelete(cellName, Relation.EDM_TYPE_NAME, relationName,
                    "null", ExtCell.EDM_TYPE_NAME,
                    "'" + DcCoreUtils.encodeUrlComp(extCellUrl + "'"), token);
            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            ResourceUtils.linksDelete(cellName, Relation.EDM_TYPE_NAME, relationName,
                    "null", ExtCell.EDM_TYPE_NAME,
                    "'" + DcCoreUtils.encodeUrlComp(extCellUrl + "'"), token);
            RelationUtils.delete(cellName, token, relationName, boxName, -1);
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }

    }
}
