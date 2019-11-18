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

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.utils.ExtRoleUtils;
import io.personium.test.utils.UrlUtils;

/**
 * ExtCell更新のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleUpdateTest extends ODataCommon {

    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    private static String cellName = "testcell1";
    private static String testExtRoleName = UrlUtils.roleResource(cellName, "__", "testextrole");

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleUpdateTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ExtRole更新の正常系のテスト.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void ExtRole更新の正常系のテスト() {
        String relationName = "testrelation01";
        String relationName2 = "testrelation02";
        String relationBoxName = "box1";
        String relationBoxName2 = "box2";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.createRelation(cellName, relationName2, relationBoxName2);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);
            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    UrlUtils.roleResource(cellName, "__", "newextRoleName"),
                    "\"" + relationName2 + "\"",
                    "\"" + relationBoxName2 + "\"", HttpStatus.SC_NO_CONTENT);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, UrlUtils.roleResource(cellName, "__", "newextRoleName"),
                    relationName2, relationBoxName2);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName2, relationBoxName2);
        }
    }

    /**
     * Reration名とBox名指定有の複合キーのExtRoleに対して同名で更新すると204を返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Reration名とBox名指定有の複合キーのExtRoleに対して同名で更新すると204を返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);
            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    testExtRoleName,
                    "\"" + relationName + "\"",
                    "\"" + relationBoxName + "\"", HttpStatus.SC_NO_CONTENT);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * Reration名指定有の複合キーのExtRoleに対して同名で更新すると204を返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Reration名指定有の複合キーのExtRoleに対して同名で更新すると204を返却すること() {
        String relationName = "testrelation01";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);
            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "null",
                    testExtRoleName,
                    "\"" + relationName + "\"",
                    "null", HttpStatus.SC_NO_CONTENT);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName);
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * リクエストボディにExtRoleの指定がない場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディにExtRoleの指定がない場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("_Relation.Name", relationName);
            body2.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * リクエストボディのExtRoleがnullの場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディのExtRoleがnullの場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", null);
            body2.put("_Relation.Name", relationName);
            body2.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * リクエストボディにRelationの指定がない場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディにRelationの指定がない場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", testExtRoleName);

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * リクエストボディのRelationにnullを指定した場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディのRelationにnullを指定した場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", testExtRoleName);
            body2.put("_Relation.Name", null);

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * リクエストボディのRelationに空を指定した場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディのRelationに空を指定した場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", testExtRoleName);
            body2.put("_Relation.Name", "");

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * リクエストボディに存在しないRelationを指定した場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void リクエストボディに存在しないRelationを指定した場合400エラーを返却すること() {
        String relationName = "testrelation01";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);

            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            JSONObject body2 = new JSONObject();
            body2.put("ExtRole", testExtRoleName);
            body2.put("_Relation.Name", "dummy");

            ExtRoleUtils.update(token, cellName, testExtRoleName, "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body2, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName,
                    relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * merge test.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void merge() {
        String relationName = "testrelation01";
        String relationName2 = "testrelation02";
        String relationName3 = "testrelation03";
        String relationBoxName = "box1";
        String relationBoxName2 = "box2";
        try {
            // Advance preparation.
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.createRelation(cellName, relationName2, relationBoxName2);
            CellCtlUtils.createRelation(cellName, relationName3);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);
            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            // Execute update (MERGE) with all parameters specified
            body = new JSONObject();
            body.put("ExtRole", UrlUtils.roleResource(cellName, "__", "newextRoleName"));
            body.put("_Relation.Name", relationName2);
            body.put("_Relation._Box.Name", relationBoxName2);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    testExtRoleName,
                    "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body,
                    HttpStatus.SC_NO_CONTENT);

            // ExtRole only
            body = new JSONObject();
            body.put("ExtRole", UrlUtils.roleResource(cellName, "__", "newextRoleName2"));
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    UrlUtils.roleResource(cellName, "__", "newextRoleName"),
                    "'" + relationName2 + "'",
                    "'" + relationBoxName2 + "'",
                    body,
                    HttpStatus.SC_NO_CONTENT);

            // Set other than ExtRole
            body = new JSONObject();
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    UrlUtils.roleResource(cellName, "__", "newextRoleName2"),
                    "'" + relationName2 + "'",
                    "'" + relationBoxName2 + "'",
                    body,
                    HttpStatus.SC_NO_CONTENT);

            // Set null to "_Relation._Box.Name"
            body = new JSONObject();
            body.put("ExtRole", UrlUtils.roleResource(cellName, "__", "newextRoleName3"));
            body.put("_Relation.Name", relationName3);
            body.put("_Relation._Box.Name", null);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    UrlUtils.roleResource(cellName, "__", "newextRoleName2"),
                    "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body,
                    HttpStatus.SC_NO_CONTENT);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, UrlUtils.roleResource(cellName, "__", "newextRoleName3"),
                    relationName3);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName2, relationBoxName2);
            CellCtlUtils.deleteRelation(cellName, relationName3);
        }
    }

    /**
     * merge test.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void merge_invalid() {
        String relationName = "testrelation01";
        String relationName2 = "testrelation02";
        String relationName3 = "testrelation03";
        String relationBoxName = "box1";
        String relationBoxName2 = "box2";
        try {
            // Advance preparation.
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.createRelation(cellName, relationName2, relationBoxName2);
            CellCtlUtils.createRelation(cellName, relationName3);
            JSONObject body = new JSONObject();
            body.put("ExtRole", testExtRoleName);
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", relationBoxName);
            ExtRoleUtils.create(token, cellName, body, HttpStatus.SC_CREATED);

            // Required ExtRole
            body = new JSONObject();
            body.put("ExtRole", null);
            body.put("_Relation.Name", relationName2);
            body.put("_Relation._Box.Name", relationBoxName2);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    testExtRoleName,
                    "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body,
                    HttpStatus.SC_BAD_REQUEST);

            // Required _Relation.Name
            body = new JSONObject();
            body.put("ExtRole", "newextRoleNameBad");
            body.put("_Relation.Name", null);
            body.put("_Relation._Box.Name", relationBoxName2);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    testExtRoleName,
                    "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body,
                    HttpStatus.SC_BAD_REQUEST);

            // Nonexistent Relation.(MERGE:testrelation03 - box1)
            body = new JSONObject();
            body.put("ExtRole", "newextRoleNameBad");
            body.put("_Relation.Name", relationName3);
            ExtRoleUtils.updateMerge(
                    token,
                    cellName,
                    testExtRoleName,
                    "'" + relationName + "'",
                    "'" + relationBoxName + "'",
                    body,
                    HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, testExtRoleName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName2, relationBoxName2);
            CellCtlUtils.deleteRelation(cellName, relationName3);
        }
    }
}
