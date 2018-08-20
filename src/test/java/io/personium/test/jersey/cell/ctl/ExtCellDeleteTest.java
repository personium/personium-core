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
package io.personium.test.jersey.cell.ctl;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.RoleUtils;

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
        super(new PersoniumCoreApplication());
    }

    /**
     * Normal test.
     * Delete extcell linked with role.
     */
    @Test
    public void normal_delete_extcell_linked_with_role() {
        String token = AbstractCase.MASTER_TOKEN_NAME;
        String extCellUrl = UrlUtils.cellRoot("cellHoge");
        String boxName = null;
        String roleName = "role";

        try {
            // 準備。ExtCell、ロール作ってリンクさせる。
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            RoleUtils.create(cellName, token, roleName, boxName, HttpStatus.SC_CREATED);
            LinksUtils.createLinksExtCell(cellName, PersoniumCoreUtils.encodeUrlComp(extCellUrl),
                    Role.EDM_TYPE_NAME, roleName, boxName, token, HttpStatus.SC_NO_CONTENT);

            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinksExtCell(cellName, PersoniumCoreUtils.encodeUrlComp(extCellUrl),
                    Role.EDM_TYPE_NAME, roleName, boxName, token, -1);
            RoleUtils.delete(cellName, token, roleName, boxName, -1);
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * Normal test.
     * Delete extcell linked with relation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void normal_delete_extcell_linked_with_relation() {
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
            LinksUtils.createLinksExtCell(cellName, PersoniumCoreUtils.encodeUrlComp(extCellUrl),
                    Relation.EDM_TYPE_NAME, relationName, null, token, HttpStatus.SC_NO_CONTENT);

            ExtCellUtils.delete(token, cellName, extCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            LinksUtils.deleteLinksExtCell(cellName, PersoniumCoreUtils.encodeUrlComp(extCellUrl),
                    Relation.EDM_TYPE_NAME, relationName, null, token, -1);
            RelationUtils.delete(cellName, token, relationName, boxName, -1);
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }

    }
}
