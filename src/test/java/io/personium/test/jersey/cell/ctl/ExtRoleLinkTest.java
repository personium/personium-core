/**
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.common.utils.CommonUtils;
import io.personium.core.model.ctl.Relation;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.Http;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * ExtRoleとRelationの$linksのテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleLinkTest extends ODataCommon {

    private static String testCellName = "testcell1";
    private static String testExtRoleName = UrlUtils.roleResource(testCellName, "__", "testrole");

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleLinkTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ExtRoleと_Relationのlinkを作成すると400エラーを返却すること.
     */
    @Test
    public final void ExtRoleと_Relationのlinkを作成すると400エラーを返却すること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            TResponse tResponse = CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            String testExtRoleUrl = extRoleUrl(testCellName,
                    relationBoxName, relationName, CommonUtils.encodeUrlComp(testExtRoleName));

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "POST")
                .with("path", testExtRoleUrl)
                .with("naviPro", "_Relation")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("url", tResponse.getLocationHeader())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(testCellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(testCellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRoleと_Relationのlinkを削除すると400エラーを返却すること.
     */
    @Test
    public final void ExtRoleと_Relationのlinkを削除すると400エラーを返却すること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            LinksUtils.deleteLinksExtRole(testCellName, CommonUtils.encodeUrlComp(testExtRoleName),
                    relationName, relationBoxName, Relation.EDM_TYPE_NAME, relationName, relationBoxName,
                    AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(testCellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(testCellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRoleとlinkしているRelationを取得できること.
     */
    @Test
    public final void ExtRoleとlinkしているRelationを取得できること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            String testExtRoleUrl = extRoleUrl(testCellName,
                    relationBoxName, relationName, CommonUtils.encodeUrlComp(testExtRoleName));

            // $links取得
            Http.request("cell/link-list.txt")
                .with("path", testExtRoleUrl)
                .with("naviPro", "_Relation")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_OK);

            // TODO 取得した値のチェック（取得した値がなんかおかしい）
        } finally {
            CellCtlUtils.deleteExtRole(testCellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(testCellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRoleのURL取得.
     * @param cellName セル名
     * @param boxName ボックス名
     * @param relationName リレーション名
     * @param extRoleName ExtRole名
     * @return ExtRoleリソースURL
     */
    private static String extRoleUrl(final String cellName,
            final String boxName,
            final String relationName,
            final String extRoleName) {
        String box = null;
        String relation = null;
        String extRole = "'" + extRoleName + "'";
        if (boxName == null) {
            box = "null";
        } else {
            box = "'" + boxName + "'";
        }
        if (relationName == null) {
            relation = "null";
        } else {
            relation = "'" + relationName + "'";
        }

        return String.format("%s/__ctl/ExtRole(ExtRole=%s,_Relation.Name=%s,_Relation._Box.Name=%s)",
                cellName, extRole, relation, box);
    }
}
