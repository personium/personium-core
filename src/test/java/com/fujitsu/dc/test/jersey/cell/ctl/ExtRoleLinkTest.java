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
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

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
        super("com.fujitsu.dc.core.rs");
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
                    relationBoxName, relationName, DcCoreUtils.encodeUrlComp(testExtRoleName));

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

            String testExtRoleUrl = extRoleUrl(testCellName,
                    relationBoxName, relationName, DcCoreUtils.encodeUrlComp(testExtRoleName));

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "DELETE")
                .with("path", testExtRoleUrl)
                .with("naviPro", "_Relation")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
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
                    relationBoxName, relationName, DcCoreUtils.encodeUrlComp(testExtRoleName));

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "GET")
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
     * Relationと_ExtRoleのlinkを作成すると400エラーを返却すること.
     */
    @Test
    public final void Relationと_ExtRoleのlinkを作成すると400エラーを返却すること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            String testRelationUrl = String.format("%s/__ctl/Relation(Name='%s',_Box.Name='%s')",
                    testCellName, relationName, relationBoxName);

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "POST")
                .with("path", testRelationUrl)
                .with("naviPro", "_ExtRole")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("url", testRelationUrl)
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(testCellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(testCellName, relationName, relationBoxName);
        }
    }

    /**
     * Relationと_ExtRoleのlinkを削除すると400エラーを返却すること.
     */
    @Test
    public final void Relationと_ExtRoleのlinkを削除すると400エラーを返却すること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            String testRelationUrl = String.format("%s/__ctl/Relation(Name='%s',_Box.Name='%s')",
                    testCellName, relationName, relationBoxName);

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "DELETE")
                .with("path", testRelationUrl)
                .with("naviPro", "_ExtRole")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("body", "\\{\\\"Name\\\":\\\"" + testExtRoleName + "\\\"\\}")
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(testCellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(testCellName, relationName, relationBoxName);
        }
    }

    /**
     * RelationとlinkしているExtRoleを取得できること.
     */
    @Test
    public final void RelationとlinkしているExtRoleを取得できること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";

        try {
            CellCtlUtils.createRelation(testCellName, relationName, relationBoxName);
            CellCtlUtils.createExtRole(testCellName, testExtRoleName, relationName, relationBoxName);

            String testRelationUrl = String.format("%s/__ctl/Relation(Name='%s',_Box.Name='%s')",
                    testCellName, relationName, relationBoxName);

            // $links作成
            Http.request("cell/link.txt")
                .with("method", "GET")
                .with("path", testRelationUrl)
                .with("naviPro", "_ExtRole")
                 .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("body", "\\{\\\"Name\\\":\\\"" + testExtRoleName + "\\\"\\}")
                .returns()
                .statusCode(HttpStatus.SC_OK);

            // TODO 取得した値のチェック
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
