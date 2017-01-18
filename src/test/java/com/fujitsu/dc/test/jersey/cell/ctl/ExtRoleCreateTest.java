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

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtRoleUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ExtRoleの登録のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtRoleCreateTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static final String EXTROLE_PATH_DEFAULT = UrlUtils.roleResource(cellName, "__", "testrole");
    private static final String EXT_ROLE_TYPE = "CellCtl.ExtRole";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtRoleCreateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ExtRole新規登録のテスト ExtRole, _Relation.Name, _Relation._Box.Name指定あり.
     */
    @Test
    public void ExtRole登録でボックスありのリレーションを指定して正常に登録できること() {
        String relationName = "testrelation";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            createExtRole(relationName, relationBoxName);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, EXTROLE_PATH_DEFAULT, relationName, relationBoxName);
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRole新規登録のテスト _Relation.Name, _Relation._Box.Nameにnull指定.
     */
    @Test
    public void ExtRole登録でボックスなしのリレーションを指定して正常に登録できること() {
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            createExtRole(relationName, null);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, EXTROLE_PATH_DEFAULT, relationName);
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRole登録でリレーションにnullを指定した場合400エラーを返却すること.
     */
    @Test
    public void ExtRole登録でリレーションにnullを指定した場合400エラーを返却すること() {
        createExtRole(false, false);
    }

    /**
     * ExtRole登録でリレーションを指定しない場合400エラーを返却すること.
     */
    @Test
    public void ExtRole登録でリレーションを指定しない場合400エラーを返却すること() {
        createExtRole(true, true);
    }

    /**
     * ExtRole登録でリレーションに空文字を指定した場合400エラーを返却すること.
     */
    @Test
    public void ExtRole登録でリレーションに空文字を指定した場合400エラーを返却すること() {
        TResponse res = createExtRole(EXTROLE_PATH_DEFAULT, "", null);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ExtRole登録で存在しないリレーションを指定した場合400エラーを返却すること.
     */
    @Test
    public void ExtRole登録で存在しないリレーションを指定した場合400エラーを返却すること() {
        TResponse res = createExtRole(EXTROLE_PATH_DEFAULT, "", null);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ExtRole登録で存在するリレーションを指定した場合正常に登録できること.
     */
    @Test
    public void ExtRole登録で存在するリレーションを指定した場合正常に登録できること() {
        TResponse res = createExtRole(EXTROLE_PATH_DEFAULT, "", null);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ExtRoleが空文字の場合400エラーを返却すること.
     */
    @Test
    public final void ExtRoleが空文字の場合400エラーを返却すること() {
        String extRolePath = "";
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            TResponse res = createExtRole(extRolePath, relationName, null);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRoleの指定がない場合400エラーを返却すること.
     */
    @Test
    @SuppressWarnings("unchecked")
    public final void ExtRoleの指定がない場合400エラーを返却すること() {
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);

            JSONObject body = new JSONObject();
            body.put("_Relation.Name", relationName);
            body.put("_Relation._Box.Name", null);
            TResponse res = Http.request("cell/extRole/extRole-create.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", cellName)
                    .with("body", body.toString())
                    .returns()
                    .debug();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRoleがnullの場合400エラーを返却すること.
     */
    @Test
    public final void ExtRoleがnullの場合400エラーを返却すること() {
        String extRolePath = null;
        TResponse res = createExtRole(extRolePath, null, null);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ExtRoleがURL形式でない場合400エラーを返却すること.
     */
    @Test
    public final void ExtRoleがURL形式でない場合400エラーを返却すること() {
        String extRolePath = "testExtRole";
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            TResponse res = createExtRole(extRolePath, relationName, null);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRoleが1024文字の場合正常に作成されること.
     */
    @Test
    public final void ExtRoleが1024文字の場合正常に作成されること() {
        String extRolePath = "http://localhost:8080/dc1-core/testextRole"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaax";
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            TResponse res = createExtRole(extRolePath, relationName, null);
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, extRolePath, relationName);
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRoleが1025文字の場合正常に作成されること.
     */
    @Test
    public final void ExtRoleが1025文字の場合正常に作成されること() {
        String extRolePath = "http://localhost:8080/dc1-core/testextRole"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaax";
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            TResponse res = createExtRole(extRolePath, relationName, null);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRoleが1025文字の場合正常に作成されること.
     */
    @Test
    public final void ExtRoleのschemeが不正の場合400エラーを返却すること() {
        String extRolePath = "ftp://localhost:21/dc1-core/testextRole";
        String relationName = "testrelation";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            TResponse res = createExtRole(extRolePath, relationName, null);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }

    /**
     * ExtRole新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認.
     */
    @Test
    public void ExtRole新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認() {
        String relationName = "testrelation";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            errCreateExtRole(relationName, relationBoxName, PUBLISHED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRole新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認.
     */
    @Test
    public void ExtRole新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認() {
        String relationName = "testrelation";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            errCreateExtRole(relationName, relationBoxName, UPDATED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * ExtRole新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認.
     */
    @Test
    public void ExtRole新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認() {
        String relationName = "testrelation";
        String relationBoxName = "box1";
        try {
            CellCtlUtils.createRelation(cellName, relationName, relationBoxName);
            errCreateExtRole(relationName, relationBoxName, METADATA, null, HttpStatus.SC_BAD_REQUEST);
        } finally {
            CellCtlUtils.deleteRelation(cellName, relationName, relationBoxName);
        }
    }

    /**
     * RelationからNP経由でExtRoleを作成した場合201になること.
     */
    @Test
    public void RelationからNP経由でExtRoleを作成した場合201になること() {
        String relationName = "testrelation";
        String extRoleUrl = "http://fqdn/cell/__role/__/testrole";
        try {
            CellCtlUtils.createRelation(cellName, relationName);
            ExtRoleUtils.createViaNP(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME,
                    relationName, null, extRoleUrl, HttpStatus.SC_CREATED);
        } finally {
            CellCtlUtils.deleteExtRole(cellName, extRoleUrl, relationName);
            CellCtlUtils.deleteRelation(cellName, relationName);
        }
    }


    /**
     * 指定されたリレーション名にリンクされたExtRole情報を作成する.
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    private void createExtRole(String relationName, String relationBoxName) {
        TResponse response = createExtRole(EXTROLE_PATH_DEFAULT, relationName, relationBoxName);

        response.statusCode(HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote(
                "testcell1",
                "ExtRole",
                "ExtRole=" + CellCtlUtils.addSingleQuarto(EXTROLE_PATH_DEFAULT)
                        + ",_Relation.Name=" + CellCtlUtils.addSingleQuarto(relationName)
                        + ",_Relation._Box.Name=" + CellCtlUtils.addSingleQuarto(relationBoxName));

        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("ExtRole", EXTROLE_PATH_DEFAULT);
        additional.put("_Relation.Name", relationName);
        additional.put("_Relation._Box.Name", relationBoxName);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, EXT_ROLE_TYPE, additional);
    }

    /**
     * 指定されたリレーション名にリンクされたExtRole情報を作成する.
     * @param extRolePath ExtRoleのパス
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     */
    @SuppressWarnings("unchecked")
    private TResponse createExtRole(String extRolePath, String relationName, String relationBoxName) {
        JSONObject body = new JSONObject();
        body.put("ExtRole", extRolePath);
        body.put("_Relation.Name", relationName);
        body.put("_Relation._Box.Name", relationBoxName);
        return Http.request("cell/extRole/extRole-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .debug();
    }

    /**
     * 指定されたリレーション名にリンクされたExtRole情報を作成する(エラー).
     * @param relationName リレーション名
     * @param relationBoxName リレーションボックス名
     * @param errKey エラーキー
     * @param errValue エラー値
     * @param 期待するエラーステータスコード
     */
    @SuppressWarnings("unchecked")
    private void errCreateExtRole(String relationName,
            String relationBoxName,
            String errKey,
            String errValue,
            int errSC) {
        JSONObject body = new JSONObject();
        body.put("ExtRole", EXTROLE_PATH_DEFAULT);
        body.put("_Relation.Name", relationName);
        body.put("_Relation._Box.Name", relationBoxName);
        body.put(errKey, errValue);

        Http.request("cell/extRole/extRole-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(errSC);
    }

    /**
     * リレーション名にリンクされていないExtRole情報を作成する.
     * @param relationNameEmpty _Relation.Nameを指定しない
     * @param relationBoxNameEmpty _Relation._Box.Nameを指定しない
     */
    @SuppressWarnings("unchecked")
    private void createExtRole(boolean relationNameEmpty, boolean relationBoxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("ExtRole", EXTROLE_PATH_DEFAULT);
        if (!relationNameEmpty) {
            body.put("_Relation.Name", null);
        }
        if (!relationBoxNameEmpty) {
            body.put("_Relation._Box.Name", null);
        }
        Http.request("cell/extRole/extRole-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_BAD_REQUEST);
    }

}
