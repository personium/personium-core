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

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRequest;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * ROLEの登録のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class RoleCreateTest extends ODataCommon {

    private static String cellName = "testcell1";
    private static String testRoleName = "testrole";
    private static final String ROLE_TYPE = "CellCtl.Role";

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public RoleCreateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ROLE新規登録のテスト Name, _Box.Name指定あり.
     */
    @Test
    public void ROLE登録の正常系ボックス指定ありのテスト() {
        String boxname = "box1";
        try {
            createRole(boxname);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName, boxname);
        }
    }

    /**
     * ROLE新規登録のテスト _Box.Nameにnull指定.
     */
    @Test
    public void ROLE登録の正常系ボックス指定なしのテスト() {
        try {
            createRoleAndCheckResponse(false);
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * ROLE新規登録のテスト _Box.Name指定なし.
     */
    @Test
    public void ROLE登録の正常系ボックス名キー指定なしのテスト() {
        try {
            createRoleAndCheckResponse(true);
            // TODO スキーマチェック実装後は400とする
        } finally {
            CellCtlUtils.deleteRole(cellName, testRoleName);
        }
    }

    /**
     * ROLE新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認.
     */
    @Test
    public void ROLE新規登録のテスト_リクエストボディに管理情報__publishedを指定した場合400エラーとなる確認() {
        errCreateRole("box1", PUBLISHED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ROLE新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認.
     */
    @Test
    public void ROLE新規登録のテスト_リクエストボディに管理情報__updatedを指定した場合400エラーとなる確認() {
        errCreateRole("box1", UPDATED, "/Date(0)/", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ROLE新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認.
     */
    @Test
    public void ROLE新規登録のテスト_リクエストボディに管理情報__metadataを指定した場合400エラーとなる確認() {
        errCreateRole("box1", METADATA, null, HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * ROLE新規登録時にNameに空文字を指定して400になること.
     */
    @Test
    public final void ROLE新規登録時にNameに空文字を指定した場合400になること() {
        String roleName = "";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameにアンダーバー始まりの文字列を指定した場合400になること() {
        String roleName = "_testRole";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameにハイフン始まりの文字列を指定した場合400になること() {
        String roleName = "-testRole";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameにスラッシュを含む文字列を指定した場合400になること() {
        String roleName = "test/Role";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameに__ctlを指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameに__ctlを指定した場合400になること() {
        String roleName = "__ctl";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameに1文字指定した場合201になること.
     */
    @Test
    public final void ROLE新規登録時にNameに1文字指定した場合201になること() {
        String roleName = "1";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameに128文字指定した場合201になること.
     */
    @Test
    public final void ROLE新規登録時にNameに128文字指定した場合201になること() {
        String roleName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameに129文字指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameに129文字指定した場合400になること() {
        String roleName = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時にNameに日本語を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にNameに日本語を指定した場合400になること() {
        String roleName = "日本語";
        String boxname = "box1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに空文字を指定して400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に空文字を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameにアンダーバー始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名にアンダーバー始まりの文字列を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "_box";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameにハイフン始まりの文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名にハイフン始まりの文字列を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "-box";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameにスラッシュを含む文字列を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名にスラッシュを含む文字列を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "box/1";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに__ctlを指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に__ctlを指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "__ctl";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに1文字指定した場合201になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に1文字指定した場合201になること() {
        String roleName = testRoleName;
        String boxname = "1";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {

            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);

        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
            if (locationHeaderBox != null) {
                CellCtlUtils.deleteOdataResource(locationHeaderBox);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに128文字指定した場合201になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に128文字指定した場合201になること() {
        String roleName = testRoleName;
        String boxname = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "123456789012345678901234567890123456789012345678901234567x";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {
            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_CREATED);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
            if (locationHeaderBox != null) {
                CellCtlUtils.deleteOdataResource(locationHeaderBox);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに129文字指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に129文字指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "1234567890123456789012345678901234567890123456789012345678901234567890"
                + "1234567890123456789012345678901234567890123456789012345678x";
        String locationHeader = null;
        String locationHeaderBox = null;

        try {
            // Box作成
            DcResponse resBox = createBox(boxname);
            locationHeaderBox = resBox.getFirstHeader(HttpHeaders.LOCATION);

            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
                if (locationHeaderBox != null) {
                    CellCtlUtils.deleteOdataResource(locationHeaderBox);
                }
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに日本語を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時にBox名に日本語を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "日本語";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * ROLE新規登録時に_Box.Nameに存在しないBox名を指定した場合400になること.
     */
    @Test
    public final void ROLE新規登録時に存在しないBox名を指定した場合400になること() {
        String roleName = testRoleName;
        String boxname = "dummy";
        String locationHeader = null;

        try {
            TResponse res = createRole(roleName, boxname);
            locationHeader = res.getLocationHeader();
            res.statusCode(HttpStatus.SC_BAD_REQUEST);

            // メッセージ確認
            ODataCommon.checkErrorResponseBody(res,
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.getCode(),
                    DcCoreException.OData.BODY_NTKP_NOT_FOUND_ERROR.params(boxname).getMessage());
        } finally {
            if (locationHeader != null) {
                CellCtlUtils.deleteOdataResource(locationHeader);
            }
        }
    }

    /**
     * BoxからNP経由でROLEを作成した場合201になること.
     */
    @Test
    public final void BoxからNP経由でROLEを作成した場合201になること() {
        String roleName = "npRole";

        try {
            RoleUtils.createViaNP(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, "Box", "'" + Setup.TEST_BOX1 + "'",
                    roleName, HttpStatus.SC_CREATED);
        } finally {
            RoleUtils.delete(Setup.TEST_CELL1, Setup.MASTER_TOKEN_NAME, Setup.TEST_BOX1, roleName, -1);
        }
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する.
     * @param boxname
     */
    @SuppressWarnings("unchecked")
    private void createRole(String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", boxname);
        TResponse response = Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Role",
                "Name='" + testRoleName + "',_Box.Name='" + boxname + "'");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRoleName);
        additional.put("_Box.Name", boxname);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する(エラー).
     * @param boxname
     * @param errKey エラーキー
     * @param errValue エラー値
     * @param 期待するエラーステータスコード
     */
    @SuppressWarnings("unchecked")
    private void errCreateRole(String boxname, String errKey, String errValue, int errSC) {
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        body.put("_Box.Name", boxname);
        body.put(errKey, errValue);

        Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(errSC);
    }

    /**
     * 指定されたボックス名にリンクされたロール情報を作成する(エラー).
     * @param roleName
     * @param boxname
     * @param 期待するエラーステータスコード
     * @return ロケーションヘッダ
     */
    @SuppressWarnings("unchecked")
    private TResponse createRole(String roleName, String boxname) {
        JSONObject body = new JSONObject();
        body.put("Name", roleName);
        body.put("_Box.Name", boxname);

        return Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .debug();
    }

    /**
     * ボックス名にリンクされていないロール情報を作成する.
     * @param boxNameEmpty _Box.Nameを指定しない
     */
    @SuppressWarnings("unchecked")
    private void createRoleAndCheckResponse(boolean boxNameEmpty) {
        JSONObject body = new JSONObject();
        body.put("Name", testRoleName);
        if (!boxNameEmpty) {
            body.put("_Box.Name", null);
        }

        TResponse response = Http.request("role-create.txt")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", cellName)
                .with("body", body.toString())
                .returns()
                .statusCode(HttpStatus.SC_CREATED);

        // レスポンスヘッダーのチェック
        String location = UrlUtils.cellCtlWithoutSingleQuote("testcell1", "Role",
                "Name='" + testRoleName + "',_Box.Name=null");
        ODataCommon.checkCommonResponseHeader(response, location);

        // レスポンスボディーのチェック
        Map<String, Object> additional = new HashMap<String, Object>();
        additional.put("Name", testRoleName);
        additional.put("_Box.Name", null);
        ODataCommon.checkResponseBody(response.bodyAsJson(), location, ROLE_TYPE, additional);

    }

    private DcResponse createBox(String boxname) {
        DcRequest req = DcRequest.post(UrlUtils.cellCtl(cellName, Box.EDM_TYPE_NAME));
        req.header(HttpHeaders.AUTHORIZATION, BEARER_MASTER_TOKEN).addJsonBody("Name", boxname);
        return request(req);
    }
}
