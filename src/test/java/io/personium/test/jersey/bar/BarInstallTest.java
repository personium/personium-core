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
package io.personium.test.jersey.bar;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.OAuth2Helper;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.AssociationEnd;
import io.personium.core.model.ctl.ComplexType;
import io.personium.core.model.ctl.ComplexTypeProperty;
import io.personium.core.model.ctl.EntityType;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.progress.ProgressInfo;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.cell.ctl.CellCtlUtils;
import io.personium.test.setup.Setup;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.AssociationEndUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.EntityTypeUtils;
import io.personium.test.utils.ExtCellUtils;
import io.personium.test.utils.Http;
import io.personium.test.utils.RelationUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.TestMethodUtils;
import io.personium.test.utils.UserDataUtils;

/**
 * barファイルインストール用テスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class BarInstallTest extends JerseyTest {
    /**
     * ログ用オブジェクト.
     */
    private static Logger log = LoggerFactory.getLogger(BarInstallTest.class);

    private static final String INSTALL_TARGET = "installBox";
    private static final String REQ_CONTENT_TYPE = "application/zip";
    private static final String REQUEST_NORM_FILE = "bar-install.txt";
    private static final String REQUEST_NOTYPE_FILE = "bar-install-without-type.txt";
    private static final String REQUEST_METHOD_OVERRIDE_FILE = "bar-install-method-override.txt";
    private static final String RESOURCE_PATH = "requestData/barInstall";
    private static final String BAR_FILE_MINIMUM = "/V1_1_2_bar_minimum.bar";
    private static final String BAR_FILE_ROOTPROP = "/V1_1_2_bar_90rootprops.bar";
    private static final String BAR_FILE_EMPTY = "/V1_1_2_bar_empty.bar";
    private static final String BAR_FILE_WRONGDIR = "/V1_1_2_bar_wrongdir.bar";
    private static final String BAR_FILE_WRONGFILE = "/V1_1_2_bar_wrongfile.bar";
    private static final String BAR_FILE_00META_NOTEXIST = "/V1_1_2_bar_00meta_notexsist.bar";
    private static final String BAR_FILE_00MANIFEST_NOTEXIST = "/V1_1_2_bar_00manifest_notexsist.bar";
    private static final String BAR_FILE_90ROOTPROPS_NOTEXIST = "/V1_1_2_bar_90rootprops_notexsist.bar";
    private static final String BAR_FILE_ROOTDIR_ORDER = "/V1_1_2_bar_rootdir_order.bar";
    private static final String BAR_FILE_00META_ORDER = "/V1_1_2_bar_00meta_order.bar";
    private static final String BAR_FILE_RELATION_NONAME = "/V1_1_2_bar_relation_noname.bar";
    private static final String BAR_FILE_RELATION_NOFIELDNAME = "/V1_1_2_bar_relation_nofield.bar";
    private static final String BAR_FILE_ROLE_NOFIELDNAME = "/V1_1_2_bar_role_nofield.bar";
    private static final String BAR_FILE_EXROLE_NOFIELDNAME = "/V1_1_2_bar_extrole_nofield.bar";
    private static final String BAR_FILE_RELATION_FILE_NAME = "/V1_1_2_bar_relation_bad_name.bar";
    private static final String BAR_FILE_ROLE_FILE_NAME = "/V1_1_2_bar_role_bad_name.bar";
    private static final String BAR_FILE_EXROLE_FILE_NAME = "/V1_1_2_bar_extrole_bad_name.bar";
    private static final String BAR_FILE_NO_JSON_FORM = "/V1_1_2_bar_no_json_form.bar";
    private static final String BAR_FILE_NGROLE = "/V1_1_2_bar_role_ng.bar";
    private static final String BAR_FILE_NGRELATION = "/V1_1_2_bar_relation_ng.bar";
    private static final String BAR_FILE_RELATION_CONFLICT = "/V1_1_2_bar_relation_conflict.bar";
    private static final String BAR_FILE_EXTROLE_CONFLICT = "/V1_1_2_bar_extrole_conflict.bar";
    private static final String BAR_FILE_LINK_ENTITYNOEXIST = "/V1_1_2_bar_links_entitynoexist.bar";
    private static final String BAR_FILE_ODATA_CONT_NOTEXIST = "/V1_1_2_bar_odata_contents_notexist.bar";
    private static final String BAR_FILE_ODATA_SPEC_NOTEXIST = "/V1_1_2_bar_odata_spcifiy_notexist.bar";
    private static final String BAR_FILE_ODATA_CONT_EMPTY = "/V1_1_2_bar_odata_contents_empty.bar";
    private static final String BAR_FILE_00METADATA_NOTEXIST = "/V1_1_2_bar_00metadata_notexist.bar";
    private static final String BAR_FILE_INVALID_CONT_ORDER = "/V1_1_2_bar_invalid_contents_order.bar";
    private static final String BAR_FILE_WEBDAV_ODATA = "/V1_1_2_bar_webdav_odata.bar";

    private static final String SCHEMA_URL = "https://fqdn/testcell1/";

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();

    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "io.personium.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "io.personium.core.jersey.filter.PersoniumCoreContainerFilter");
    }

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public BarInstallTest() {
        super(new WebAppDescriptor.Builder(BarInstallTest.INIT_PARAMS).build());
    }

    /**
     * すべてのテストで必ず１度実行される処理.
     */
    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * すべてのテスト完了時に実行される処理.
     */
    @AfterClass
    public static void afterClass() {
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     * @throws InterruptedException InterruptedException
     */
    @Before
    public void before() throws InterruptedException {
        cleanup();
    }

    /**
     * すべてのテスト毎に１度実行される処理.
     */
    @After
    public void after() {
        cleanup();
    }

    private static void cleanup() {
        String reqCell = Setup.TEST_CELL1;

        try {
            // コレクションの削除
            Http.request("box/delete-box-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col1/col11")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-box-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col1")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-box-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col3")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // コレクションの削除
            Http.request("box/delete-box-col.txt")
                    .with("cellPath", reqCell)
                    .with("box", "installBox")
                    .with("path", "col2")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // Delete link.
            String extRole = PersoniumCoreUtils.encodeUrlComp("https://fqdn/cellName/__role/__/role2");
            Http.request("links-request.txt")
                    .with("method", "DELETE")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("entitySet", Role.EDM_TYPE_NAME)
                    .with("key", "Name='role1',_Box.Name='" + INSTALL_TARGET + "'")
                    .with("navProp", "_" + ExtRole.EDM_TYPE_NAME)
                    .with("navKey", "ExtRole='" + extRole + "'"
                            + ",_Relation.Name='relation1',_Relation._Box.Name='" + INSTALL_TARGET + "'")
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // Delete link.
            Http.request("links-request.txt")
                    .with("method", "DELETE")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("entitySet", Relation.EDM_TYPE_NAME)
                    .with("key", "Name='relation1',_Box.Name='" + INSTALL_TARGET + "'")
                    .with("navProp", "_" + Role.EDM_TYPE_NAME)
                    .with("navKey", "Name='role1',_Box.Name='" + INSTALL_TARGET + "'")
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // Delete ExtRole.
            String extRole = PersoniumCoreUtils.encodeUrlComp("https://fqdn/cellName/__role/__/role2");
            Http.request("cell/extRole/extRole-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("extRoleName", extRole)
                    .with("relationName", "'" + "relation1" + "'")
                    .with("relationBoxName", "'" + INSTALL_TARGET + "'")
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // Delete Role.
            Http.request("role-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("rolename", "role1")
                    .with("boxname", "'" + INSTALL_TARGET + "'")
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            // Delete Relation.
            Http.request("relation-delete.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("cellPath", Setup.TEST_CELL1)
                    .with("relationname", "relation1")
                    .with("boxname", "'" + INSTALL_TARGET + "'")
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }

        try {
            Http.request("cell/box-delete.txt")
                    .with("cellPath", reqCell)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("boxPath", INSTALL_TARGET)
                    .returns()
                    .debug();
        } catch (Exception ex) {
            log.debug(ex.getMessage());
        }
    }

    /**
     * 存在しないセルに対してbarインストールを実行し404エラーとなること.
     */
    @Test
    public final void 存在しないセルに対してbarインストールを実行し404エラーとなること() {
        String reqCell = "dummyCell";
        String reqPath = "box";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_NOT_FOUND);
        String code = PersoniumCoreException.Dav.CELL_NOT_FOUND.getCode();
        String message = PersoniumCoreException.Dav.CELL_NOT_FOUND.getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * Owner情報にシャープが含まれないセルに対してbarインストールを実行し正しくインストールできること.
     */
    @Test
    public final void Owner情報にシャープが含まれないセルに対してbarインストールを実行し正しくインストールできること() {

        String cellName = "barInstallTest_ownercell";
        String ownerName = "http://xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        String reqPath = INSTALL_TARGET;

        try {
            // Cell作成
            CellUtils.create(cellName, AbstractCase.MASTER_TOKEN_NAME, ownerName, HttpStatus.SC_CREATED);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cellName, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cellName) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

        } finally {
            // Box削除
            Http.request("cell/box-delete.txt")
                    .with("cellPath", cellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("boxPath", INSTALL_TARGET)
                    .returns()
                    .debug();

            // Cell削除
            CellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, cellName, -1);
        }
    }

    /**
     * 存在するスキーマありBoxに対してbarインストールを実行し400エラーとなること.
     */
    @Test
    public final void 存在するスキーマありBoxに対してbarインストールを実行し400エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            // テスト用Box作成（Schema付）
            BoxUtils.createWithSchema(reqCell, "boxInstallTestBox", AbstractCase.MASTER_TOKEN_NAME, SCHEMA_URL);
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            String code = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.getCode();
            String message = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.
                    params(SCHEMA_URL).getMessage();
            res.checkErrorResponse(code, message);
        } finally {
            BoxUtils.delete(reqCell, AbstractCase.MASTER_TOKEN_NAME, "boxInstallTestBox", -1);
            BoxUtils.delete(reqCell, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, -1);
        }
    }

    /**
     * 存在するスキーマなしBoxに対してbarインストールを実行し405エラーとなること.
     */
    @Test
    public final void 存在するスキーマなしBoxに対してbarインストールを実行し405エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = Setup.TEST_BOX2;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.
                params(reqPath).getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * メインボックスに対してbarインストールすると405エラーとなること.
     */
    @Test
    public final void メインボックスに対してbarインストールすると405エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = Box.DEFAULT_BOX_NAME;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
        String code = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED.getCode();
        String message = PersoniumCoreException.Misc.METHOD_NOT_ALLOWED.
                params("'" + reqPath + "'").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * ContentTypeを指定しない場合は400エラーとなること.
     */
    @Test
    public final void ContentTypeを指定しない場合は400エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NOTYPE_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.getCode();
        String message = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.
                params(HttpHeaders.CONTENT_TYPE).getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * ContentTypeが値なしの場合は400エラーとなること. personium-coreで処理される前に"Bad Content-Type header value: ''"が返却されるためテスト無効化
     */
    @Test
    @Ignore
    public final void ContentTypeが値なしの場合は400エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String contType = "";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, contType);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.getCode();
        String message = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.
                params(HttpHeaders.CONTENT_TYPE).getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * ContentTypeに不正な値を指定した場合は400エラーとなること.
     */
    @Test
    public final void ContentTypeに不正な値を指定した場合は400エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String contType = "application/xml";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, contType);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.getCode();
        String message = PersoniumCoreException.BarInstall.REQUEST_HEADER_FORMAT_ERROR.
                params(HttpHeaders.CONTENT_TYPE).getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * リクエストボディがない状態でbarインストールを実行し400エラーとなること.
     */
    @Test
    public final void リクエストボディがない状態でbarインストールを実行し400エラーとなること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        byte[] body = new byte[0];
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_CANNOT_OPEN.
                params("archive is not a ZIP archive").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルのルートディレクトリがbarではない場合に異常終了すること.
     */
    @Test
    public final void barファイルのルートディレクトリがbarではない場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_WRONGDIR);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/ bar/00_meta/ bar/00_meta/00_manifest.json bar/00_meta/90_rootprops.xml").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * 全てのOdata情報をもつbarファイルをインストールして正常終了すること.
     */
    @Test
    public final void 全てのOdata情報をもつbarファイルをインストールして正常終了すること() {
        final String barFilename = "/V1_3_12_bar_maximum.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String user = "testuser";
        final String password = "password";
        final String testCell = "dummyCell00";
        final String odataColName = "odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    user, password, HttpStatus.SC_CREATED);
            ExtCellUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot(testCell), HttpStatus.SC_CREATED);

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);
            checkRegistData();
        } finally {
            deleteAllData(reqCell, reqPath, user, testCell, odataColName);
        }
    }

    /**
     * 全てのOdata情報をもつbarファイルをMethodOverrideのMKCOL指定でもインストールが正常終了すること.
     */
    @Test
    public final void 全てのOdata情報をもつbarファイルをMethodOverrideのMKCOL指定でもインストールが正常終了すること() {
        final String barFilename = "/V1_3_12_bar_maximum.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String user = "testuser";
        final String password = "password";
        final String testCell = "dummyCell00";
        final String odataColName = "odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));
        headers.put("X-HTTP-Method-Override", "MKCOL");

        try {
            AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    user, password, HttpStatus.SC_CREATED);
            ExtCellUtils.create(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                    UrlUtils.cellRoot(testCell), HttpStatus.SC_CREATED);

            res = BarInstallTestUtils.request(REQUEST_METHOD_OVERRIDE_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);
            checkRegistData();
        } finally {
            deleteAllData(reqCell, reqPath, user, testCell, odataColName);
        }
    }

    /**
     * 全てのOdata情報をもつbarファイルをMethodOverrideのPOST指定で異常終了すること.
     */
    @Test
    public final void 全てのOdata情報をもつbarファイルをMethodOverrideのPUT指定で異常終了すること() {
        final String barFilename = "/V1_3_12_bar_maximum.bar";
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String user = "testuser";
        final String testCell = "dummyCell00";
        final String odataColName = "odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + barFilename);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));
        headers.put("X-HTTP-Method-Override", "PUT");

        try {
            res = BarInstallTestUtils.request(REQUEST_METHOD_OVERRIDE_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_NOT_FOUND);
            String code = PersoniumCoreException.Dav.BOX_NOT_FOUND.getCode();
            String message = PersoniumCoreException.Dav.BOX_NOT_FOUND
                    .params(UrlUtils.boxRoot(reqCell, INSTALL_TARGET)).getMessage();
            res.checkErrorResponse(code, message);
        } finally {
            deleteAllData(reqCell, reqPath, user, testCell, odataColName);
        }
    }

    private void deleteAllData(final String reqCell,
            final String reqPath,
            final String user,
            final String testCell,
            final String odataColName) {
        // ユーザデータの削除
        String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity3", "barInstallTest");
        ODataCommon.deleteOdataResource(resourceUrl);

        // ComplexTypeProperty
        resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                odataColName, "compProp1", "complex1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                odataColName, "compProp2", "complex1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                odataColName, "compProp3", "complex2");
        ODataCommon.deleteOdataResource(resourceUrl);
        // Property
        resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property3", "entity2");
        ODataCommon.deleteOdataResource(resourceUrl);
        // AssociationEnd
        String key = "Name='entity1-entity2',_EntityType.Name='entity1'";
        String navKey = "Name='entity2-entity1',_EntityType.Name='entity2'";
        AssociationEndUtils.deleteLink(reqCell, odataColName, INSTALL_TARGET, key, navKey, -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity1",
                INSTALL_TARGET, "entity1-entity2", -1);
        AssociationEndUtils.delete(AbstractCase.MASTER_TOKEN_NAME, reqCell, odataColName, "entity2",
                INSTALL_TARGET, "entity2-entity1", -1);

        // ComplexType
        resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex2");
        ODataCommon.deleteOdataResource(resourceUrl);

        // EntityType
        Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
        Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);
        Setup.entityTypeDelete(odataColName, "entity3", Setup.TEST_CELL1, INSTALL_TARGET);
        BarInstallTestUtils.deleteCollection(odataColName);

        String role1 = "Name='role1',_Box.Name='" + INSTALL_TARGET + "'";
        // Role <--> Relation
        deleteLink(Relation.EDM_TYPE_NAME, "Name='relation1',_Box.Name='" + INSTALL_TARGET + "'",
                "_" + Role.EDM_TYPE_NAME, role1);
        // Role <--> ExtRole
        String extRole = PersoniumCoreUtils.encodeUrlComp("https://fqdn/cellName/__role/__/role2");
        deleteLink(Role.EDM_TYPE_NAME, role1, "_" + ExtRole.EDM_TYPE_NAME,
                "ExtRole='" + extRole + "'"
                        + ",_Relation.Name='relation1',_Relation._Box.Name='" + INSTALL_TARGET + "'");

        // ExtRole
        resourceUrl = UrlUtils.extRoleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "relation1", extRole);
        ODataCommon.deleteOdataResource(resourceUrl);

        // Relation
        resourceUrl = UrlUtils.relationUrl(Setup.TEST_CELL1, INSTALL_TARGET, "relation1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.relationUrl(Setup.TEST_CELL1, INSTALL_TARGET, "relation2");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.relationUrl(Setup.TEST_CELL1, INSTALL_TARGET, "relation3");
        ODataCommon.deleteOdataResource(resourceUrl);

        // Role
        resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "role1");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "role2");
        ODataCommon.deleteOdataResource(resourceUrl);
        resourceUrl = UrlUtils.roleUrl(Setup.TEST_CELL1, INSTALL_TARGET, "role3");
        ODataCommon.deleteOdataResource(resourceUrl);

        ExtCellUtils.delete(AbstractCase.MASTER_TOKEN_NAME, Setup.TEST_CELL1,
                UrlUtils.cellRoot(testCell));
        AccountUtils.delete(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, user, -1);

        // ファイルの削除
        DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET,
                "webdavcol1/testdavfile.txt");
        DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET,
                "svccol1/__src/test.js");

        // コレクションの削除
        DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, "webdavcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, "svccol1", AbstractCase.MASTER_TOKEN_NAME, -1);
    }

    /**
     * barファイルインストールで登録された各データの存在および内容チェックを行う.
     */
    private void checkRegistData() {
        final String token = AbstractCase.MASTER_TOKEN_NAME;
        final String colName = "odatacol1";
        final String roleName = "../__/role1";
        final String nameSpace = "http://www.w3.com/standards/z39.50/";
        List<String> rolList = new ArrayList<String>();
        rolList.add("read");
        rolList.add("write");

        String resorce = UrlUtils.box(Setup.TEST_CELL1, INSTALL_TARGET);
        StringBuffer sb = new StringBuffer(resorce);
        // UrlUtilで作成されるURLの最後のスラッシュを削除する
        String boxUrl = sb.deleteCharAt(resorce.length() - 1).toString();

        // セル制御オブジェクト存在チェック
        BoxUtils.get(Setup.TEST_CELL1, token, INSTALL_TARGET, HttpStatus.SC_OK);
        RelationUtils.get(Setup.TEST_CELL1, token, "relation1", INSTALL_TARGET, HttpStatus.SC_OK);
        RelationUtils.get(Setup.TEST_CELL1, token, "relation2", INSTALL_TARGET, HttpStatus.SC_OK);
        RelationUtils.get(Setup.TEST_CELL1, token, "relation3", INSTALL_TARGET, HttpStatus.SC_OK);
        RoleUtils.get(Setup.TEST_CELL1, token, "role1", INSTALL_TARGET, HttpStatus.SC_OK);
        RoleUtils.get(Setup.TEST_CELL1, token, "role2", INSTALL_TARGET, HttpStatus.SC_OK);
        RoleUtils.get(Setup.TEST_CELL1, token, "role3", INSTALL_TARGET, HttpStatus.SC_OK);
        // ComplexType内容チェック
        checkComplexType(Setup.TEST_CELL1, INSTALL_TARGET, colName, "complex1");
        checkComplexType(Setup.TEST_CELL1, INSTALL_TARGET, colName, "complex2");
        // EntityType内容チェック
        checkEntityType(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity1");
        checkEntityType(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity2");
        // AssociationEnd内容チェック
        checkAssocEnd(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity1", "entity1-entity2");
        checkAssocEnd(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity2", "entity2-entity1");
        // AssociationEnd間の$linksチェック
        checkAssocEndLinks(colName);
        // Propertyの内容チェック
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity1", null, "property1", true);
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity1", null, "property2", true);
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity2", null, "property3", false);
        // ComplexTypePropertyの内容チェック
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, null, "complex1", "compProp1", true);
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, null, "complex1", "compProp2", true);
        checkProperty(Setup.TEST_CELL1, INSTALL_TARGET, colName, null, "complex2", "compProp3", false);

        // ユーザデータの内容チェック
        checkUserData(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity3", "barInstallTest");
        // OdataCollectionのACLチェック
        TResponse res = BarInstallTestUtils.propfind(Setup.TEST_CELL1, INSTALL_TARGET, colName,
                AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_MULTI_STATUS);
        Element root = res.bodyAsXml().getDocumentElement();
        // DavCollectionのACLチェック
        checkAcl(root, rolList, roleName, boxUrl + "/" + colName);

        // DavCollection
        DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                INSTALL_TARGET, "webdavcol1", HttpStatus.SC_OK);
        // DavCollectionのPROPFIND
        res = BarInstallTestUtils.propfind(Setup.TEST_CELL1, INSTALL_TARGET, "webdavcol1", "0",
                AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_MULTI_STATUS);
        root = res.bodyAsXml().getDocumentElement();
        // DavCollectionのACLチェック
        checkAcl(root, rolList, roleName, boxUrl + "/webdavcol1");
        // DavCollectionのAuthorチェック
        String author = root.getElementsByTagNameNS(nameSpace, "Author").item(0).getFirstChild().getNodeValue();
        assertEquals("Test User2", author);

        // DavFile
        DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                INSTALL_TARGET, "webdavcol1/testdavfile.txt", HttpStatus.SC_OK);

        // ServiceCollection
        BarInstallTestUtils.propfind(Setup.TEST_CELL1, INSTALL_TARGET, "svccol1", AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_MULTI_STATUS);

        // ServiceFile
        DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                INSTALL_TARGET, "svccol1/__src/test.js", HttpStatus.SC_OK);

        // Boxの属性チェック
        res = DavResourceUtils.propfind("box/propfind-box-allprop.txt",
                token, HttpStatus.SC_MULTI_STATUS, INSTALL_TARGET);
        root = res.bodyAsXml().getDocumentElement();
        // BOXのACLチェック
        checkAcl(root, rolList, roleName, boxUrl);
        // Box Authorのチェック
        author = root.getElementsByTagNameNS(nameSpace, "Author").item(0).getFirstChild().getNodeValue();
        assertEquals("Test User1", author);
    }

    // BOX ACLのチェック
    private void checkAcl(Element root, List<String> rolList, String roleName, String resourceUrl) {
        List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        list.add(map);
        map.put(roleName, rolList);
        TestMethodUtils.aclResponseTest(root, resourceUrl, list, 1,
                UrlUtils.roleResource(Setup.TEST_CELL1, INSTALL_TARGET, ""), null);
    }

    /**
     * EntityTypeの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType名
     */
    private void checkEntityType(String cellName, String boxName, String colName, String entityName) {
        TResponse res = EntityTypeUtils.get(cellName, AbstractCase.MASTER_TOKEN_NAME,
                boxName, colName, entityName, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get(EntityType.P_ENTITYTYPE_NAME.getName()).toString();
        assertEquals(entityName, value);
    }

    /**
     * ComplexTypeの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param complexName ComplexType名
     */
    private void checkComplexType(String cellName, String boxName, String colName, String complexName) {
        String complex1Url = UrlUtils.complexType(cellName, boxName, colName, complexName);
        PersoniumResponse dres = ODataCommon.getOdataResource(complex1Url);
        JSONObject json = dres.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get(ComplexType.P_COMPLEXTYPE_NAME.getName()).toString();
        assertEquals(complexName, value);
    }

    /**
     * associationEndの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType名
     * @param assocName associationEnd名
     */
    private void checkAssocEnd(String cellName, String boxName, String colName, String entityName, String assocName) {
        String aeUrl = UrlUtils.associationEnd(cellName, boxName, colName, assocName, entityName);
        PersoniumResponse res = ODataCommon.getOdataResource(aeUrl);
        JSONObject json = res.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get(AssociationEnd.P_ASSOCIATION_NAME.getName()).toString();
        assertEquals(assocName, value);
    }

    /**
     * AssociationEnd間のリンク内容チェック.
     * @param colName Collection名
     */
    private void checkAssocEndLinks(final String colName) {
        String path = UrlUtils.associationEnd(Setup.TEST_CELL1,
                INSTALL_TARGET, colName, "entity1-entity2", "entity1") + "/$links/_AssociationEnd";
        PersoniumResponse dres = ODataCommon.getOdataResource(path);
        JSONObject json = dres.bodyAsJson();
        JSONArray reses = (JSONArray) ((JSONObject) json.get("d")).get("results");
        assertEquals(1, reses.size());
        JSONObject jsonResult = (JSONObject) reses.get(0);
        String value = jsonResult.get("uri").toString();
        String aeUrl =
                UrlUtils.associationEnd(Setup.TEST_CELL1, INSTALL_TARGET, colName, "entity2-entity1", "entity2");
        assertEquals(aeUrl, value);
    }

    /**
     * Propertyの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType
     * @param complexName ComplexType名
     * @param propName Property名
     */
    private void checkProperty(String cellName, String boxName, String colName,
            String entityName, String complexName, String propName, boolean master) {
        String refTypeName = Property.P_ENTITYTYPE_NAME.getName();
        String typeName = entityName;
        String aeUrl = UrlUtils.property(cellName, boxName, colName, propName, entityName);
        if (complexName != null) {
            refTypeName = ComplexTypeProperty.P_COMPLEXTYPE_NAME.getName();
            typeName = complexName;
            aeUrl = UrlUtils.complexTypeProperty(cellName, boxName, colName, propName, complexName);
        }
        PersoniumResponse res = ODataCommon.getOdataResource(aeUrl);
        JSONObject json = res.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get(Property.P_NAME.getName()).toString();
        assertEquals(propName, value);
        value = results.get(refTypeName).toString();
        assertEquals(typeName, value);
        // Nullableチェック（全て"true"として確認）
        value = results.get(Property.P_NULLABLE.getName()).toString();
        assertEquals(String.valueOf(master), value);
    }

    /**
     * ユーザデータの内容チェック.
     * @param cellName Cell名
     * @param boxName Box名
     * @param colName Collection名
     * @param entityName EntityType名
     * @param userId ユーザID
     */
    private void checkUserData(String cellName, String boxName, String colName, String entityName, String userId) {
        TResponse res = UserDataUtils.get(cellName, AbstractCase.MASTER_TOKEN_NAME,
                boxName, colName, entityName, userId, HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        JSONObject results = (JSONObject) ((JSONObject) json.get("d")).get("results");
        String value = results.get("__id").toString();
        assertEquals(userId, value);
    }

    /**
     * $linksの削除リクエスト.
     * @param entitySet EntitySet名
     * @param key EntitySetのキー名
     * @param navProp NavigationProperty名
     * @param navKey NavigationPropertyのキー名
     */
    protected void deleteLink(String entitySet, String key, String navProp, String navKey) {
        Http.request("links-request.txt")
                .with("method", "DELETE")
                .with("token", AbstractCase.MASTER_TOKEN_NAME)
                .with("cellPath", Setup.TEST_CELL1)
                .with("entitySet", entitySet)
                .with("key", key)
                .with("navProp", navProp)
                .with("navKey", navKey)
                .returns()
                .statusCode(-1)
                .debug();
    }

    /**
     * 最小構成のbarファイルをインストールして正常終了すること.
     */
    @Test
    public final void 最小構成のbarファイルをインストールして正常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);
    }

    /**
     * 90_rootprops.xmlファイルをインストールして正常終了すること.
     */
    @Test
    public final void rootprops_xmlファイルをインストールして正常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_ROOTPROP);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // PROPFINDでACLの確認_adminに対してall権限が付加されていること
            TResponse tresponse = CellUtils.propfind(reqCell + "/" + reqPath,
                    AbstractCase.MASTER_TOKEN_NAME, "1", HttpStatus.SC_MULTI_STATUS);
            List<Map<String, List<String>>> list = new ArrayList<Map<String, List<String>>>();
            Map<String, List<String>> map = new HashMap<String, List<String>>();
            List<String> rolList = new ArrayList<String>();
            rolList.add("all");
            list.add(map);
            map.put("admin", rolList);
            Element root = tresponse.bodyAsXml().getDocumentElement();
            String resorce = UrlUtils.box(reqCell, reqPath);
            // UrlUtilで作成されるURLの最後のスラッシュを削除するため
            StringBuffer sb = new StringBuffer(resorce);
            sb.deleteCharAt(resorce.length() - 1);
            TestMethodUtils.aclResponseTest(root, sb.toString(), list, 4,
                    UrlUtils.roleResource(reqCell, reqPath, ""), null);
        } finally {
            CellCtlUtils.deleteRole(Setup.TEST_CELL1, "admin", INSTALL_TARGET);
            CellCtlUtils.deleteRole(Setup.TEST_CELL1, "user", INSTALL_TARGET);
        }
    }

    /**
     * 不正な名前のRole情報をもつbarファイルをインストールできないこと.
     */
    @Test
    public final void 不正な名前のRole情報をもつbarファイルをインストールできないこと() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_NGROLE);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * 不正な名前のRelation情報をもつbarファイルをインストールできないこと.
     */
    @Test
    public final void 不正な名前のRelation情報をもつbarファイルをインストールできないこと() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_NGRELATION);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * Relation情報が重複しているbarファイルをインストールできないこと.
     */
    @Test
    public final void Relation情報が重複しているbarファイルをインストールできないこと() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_RELATION_CONFLICT);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * ExtRole情報が重複しているbarファイルをインストールできないこと.
     */
    @Test
    public final void ExtRole情報が重複しているbarファイルをインストールできないこと() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_EXTROLE_CONFLICT);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * $link先が存在しないbarファイルをインストールできないこと.
     */
    @Test
    public final void $link先が存在しないbarファイルをインストールできないこと() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_LINK_ENTITYNOEXIST);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * barファイルではないファイルをインストールして異常終了すること.
     */
    @Test
    public final void barファイルではないファイルをインストールして異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_EMPTY);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/ bar/00_meta/ bar/00_meta/00_manifest.json bar/00_meta/90_rootprops.xml").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルのルートディレクトリ配下に不正なファイルがある場合異常終了すること.
     */
    @Test
    public final void barファイルのルートディレクトリ配下に不正なファイルがある場合異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_WRONGFILE);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * barファイルに00_metaディレクトリが存在しない場合に異常終了すること.
     */
    @Test
    public final void barファイルに00_metaディレクトリが存在しない場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_00META_NOTEXIST);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/00_meta/ bar/00_meta/00_manifest.json bar/00_meta/90_rootprops.xml").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルに00_manifestファイルが存在しない場合に異常終了すること.
     */
    @Test
    public final void barファイルに00_manifestファイルが存在しない場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_00MANIFEST_NOTEXIST);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/00_meta/00_manifest.json").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルに90_rootpropsファイルが存在しない場合に異常終了すること.
     */
    @Test
    public final void barファイルに90_rootpropsファイルが存在しない場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_90ROOTPROPS_NOTEXIST);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/00_meta/90_rootprops.xml").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルのルートディレクトリ配下のファイルの並び順が誤っている場合に異常終了すること.
     */
    @Test
    public final void barファイルのルートディレクトリ配下のファイルの並び順が誤っている場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_ROOTDIR_ORDER);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_INVALID_STRUCTURES.
                params("bar/00_meta/").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * barファイルの00_metadata配下のファイルの並び順が誤っている場合に異常終了すること.
     */
    @Test
    public final void barファイルの00_metadata配下のファイルの並び順が誤っている場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_00META_ORDER);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * relation_jsonの必須項目がない場合に異常終了すること.
     */
    @Test
    public final void relation_jsonの必須項目がない場合に異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_RELATION_NONAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * relation_jsonのFIELDNAME定義が不正な場合に異常終了すること.
     */
    @Test
    public final void relation_jsonのFIELDNAME定義が不正な場合に異常終了すること() {
        // JSONファイルのバリデートエラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_RELATION_NOFIELDNAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * role_jsonのFIELDNAME定義が不正な場合に異常終了すること.
     */
    @Test
    public final void role_jsonのFIELDNAME定義が不正な場合に異常終了すること() {
        // JSONファイルのバリデートエラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_ROLE_NOFIELDNAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);

    }

    /**
     * extrole_jsonのFIELDNAME定義が不正な場合に異常終了すること.
     */
    @Test
    public final void extrole_jsonのFIELDNAME定義が不正な場合に異常終了すること() {
        // JSONファイルのバリデートエラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_EXROLE_NOFIELDNAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * relation_jsonがJSON形式でない場合に異常終了すること.
     */
    @Test
    public final void relation_jsonがJSON形式でない場合に異常終了すること() {
        // JSONファイルの解析エラーー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_NO_JSON_FORM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * relation_jsonの項目名が不正な場合に異常終了すること.
     */
    @Test
    public final void relation_jsonの項目名が不正な場合に異常終了すること() {
        // JSONファイルのデータ定義エラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_RELATION_FILE_NAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * role_jsonの項目名が不正な場合に異常終了すること.
     */
    @Test
    public final void role_jsonの項目名が不正な場合に異常終了すること() {
        // JSONファイルのデータ定義エラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_ROLE_FILE_NAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * extrole_jsonの項目名が不正な場合に異常終了すること.
     */
    @Test
    public final void extrole_jsonの項目名が不正な場合に異常終了すること() {
        // JSONファイルのデータ定義エラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_EXROLE_FILE_NAME);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);

        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * 不正なXMLを含んでいる場合に異常終了する.
     */
    @Test
    public final void 不正なXMLを含んでいる場合に異常終了する() {
        // JSONファイルのデータ定義エラー
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + "/V1_1_2_bar_invalid_xml.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
    }

    /**
     * rootprops_xmlで未定義のODataCol用contentsがある場合に異常終了する.
     */
    @Test
    public final void rootprops_xmlで未定義のODataCol用contentsがある場合に異常終了する() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_ODATA_CONT_NOTEXIST);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * rootprops_xmlで定義済みのODataCol用contentsがない場合に異常終了する.
     */
    @Test
    public final void rootprops_xmlで定義済みのODataCol用contentsがない場合に異常終了する() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_ODATA_SPEC_NOTEXIST);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用のコンテンツが空ディレクトリの場合に異常終了する.
     */
    @Test
    public final void ODataCol用のコンテンツが空ディレクトリの場合に異常終了する() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        try {
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_ODATA_CONT_EMPTY);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用の00_metadata_xmlがない場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xmlがない場合に異常終了する() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_00METADATA_NOTEXIST);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * ODataCol用の00_metadata_xmlと10_odatarelations_jsonの順序が不正の場合に異常終了する.
     */
    @Test
    public final void ODataCol用の00_metadata_xmlと10_odatarelations_jsonの順序が不正の場合に異常終了する() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;
        String odataColName = "odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_INVALID_CONT_ORDER);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        } finally {
            // Delete entity
            Setup.entityTypeDelete(odataColName, "entity", reqCell, reqPath);

            // Delete collection
            BarInstallTestUtils.deleteCollection("odatacol1");
        }
    }

    /**
     * WebDAV(複数ファイル）_ODataの順のbarファイルをインストールして正常終了すること.
     */
    @Test
    public final void WebDAV複数ファイル_ODataの順のbarファイルをインストールして正常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqPath = INSTALL_TARGET;
        final String odataColName = "odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_WEBDAV_ODATA);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqPath;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータの内容チェック
            checkUserData(Setup.TEST_CELL1, INSTALL_TARGET, odataColName, "entity1", "barInstallTest");

            // Fileのチェック
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/testdavfile.txt", HttpStatus.SC_OK);
            DavResourceUtils.getWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-get.txt",
                    INSTALL_TARGET, "davcol1/testdavfile2.txt", HttpStatus.SC_OK);
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqPath, odataColName, "entity1", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqPath, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);

            // ファイルの削除
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET,
                    "davcol1/testdavfile.txt");
            DavResourceUtils.deleteWebDavFile(Setup.TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET,
                    "davcol1/testdavfile2.txt");

            // コレクションの削除
            DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * ODataCol用の90_data配下のentityTypeの順序が昇順で無い場合も正常終了すること.
     */
    @Test
    public final void ODataCol用の90_data配下のentityTypeの順序が昇順で無い場合も正常終了すること() {
        final String reqCell = Setup.TEST_CELL1;
        final String reqBox = INSTALL_TARGET;
        final String odataColName = "davcol1/odatacol1";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + "/V1_1_2_bar_90data_order.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqBox, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(reqCell) + reqBox;
            assertEquals(expected, location);

            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

            // ユーザデータの登録確認
            res = UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqBox, odataColName, "entity1",
                    "barInstallTest", HttpStatus.SC_OK);
            String locationHeader = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest");
            Map<String, Object> additional = new HashMap<String, Object>();
            additional.put("__id", "barInstallTest");
            additional.put("name", "barInstall");
            additional.put("property1", "property");
            Map<String, String> compProp = new HashMap<String, String>();
            compProp.put("compProp1", "compProp1");
            additional.put("property2", compProp);
            ODataCommon.checkResponseBody(res.bodyAsJson(), locationHeader, "UserData.entity1", additional);

            res = UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqBox, odataColName, "entity1",
                    "barInstallTest2", HttpStatus.SC_OK);
            locationHeader = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest2");
            additional = new HashMap<String, Object>();
            additional.put("__id", "barInstallTest2");
            additional.put("name", "barInstall2");
            additional.put("property1", "property");
            compProp = new HashMap<String, String>();
            compProp.put("compProp1", "compProp1");
            additional.put("property2", compProp);
            ODataCommon.checkResponseBody(res.bodyAsJson(), locationHeader, "UserData.entity1", additional);

            res = UserDataUtils.get(reqCell, AbstractCase.MASTER_TOKEN_NAME, reqBox, odataColName, "entity2",
                    "barInstallTest", HttpStatus.SC_OK);
            locationHeader = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity2", "barInstallTest");
            additional = new HashMap<String, Object>();
            additional.put("__id", "barInstallTest");
            additional.put("name", "barInstall");
            additional.put("property1", "property");
            ODataCommon.checkResponseBody(res.bodyAsJson(), locationHeader, "UserData.entity2", additional);
        } finally {
            // ユーザデータの削除
            String resourceUrl = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity1", "barInstallTest2");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.userdata(reqCell, reqBox, odataColName, "entity2", "barInstallTest");
            ODataCommon.deleteOdataResource(resourceUrl);

            // ComplexTypePropertyの削除
            resourceUrl = UrlUtils.complexTypeProperty(reqCell, INSTALL_TARGET,
                    odataColName, "compProp1", "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // Propertyの削除
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property1", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            resourceUrl = UrlUtils.property(reqCell, INSTALL_TARGET, odataColName, "property2", "entity1");
            ODataCommon.deleteOdataResource(resourceUrl);
            // ComplexTypeの削除
            resourceUrl = UrlUtils.complexType(reqCell, reqBox, odataColName, "complex1");
            ODataCommon.deleteOdataResource(resourceUrl);

            // EntityTypeの削除
            Setup.entityTypeDelete(odataColName, "entity1", Setup.TEST_CELL1, INSTALL_TARGET);
            Setup.entityTypeDelete(odataColName, "entity2", Setup.TEST_CELL1, INSTALL_TARGET);

            // コレクションの削除
            DavResourceUtils
                    .deleteCollection(reqCell, INSTALL_TARGET, odataColName, AbstractCase.MASTER_TOKEN_NAME, -1);
            DavResourceUtils.deleteCollection(reqCell, INSTALL_TARGET, "davcol1", AbstractCase.MASTER_TOKEN_NAME, -1);
        }
    }

    /**
     * 存在するBoxに対して権限なしの自分セルローカルトークンでbarInstarllを実施して403が返却されること.
     */
    @Test
    public final void 存在するBoxに対して権限なしの自分セルローカルトークンでbarInstarllを実施して403が返却されること() {
        String reqCellName = "boxInstallTestCell";
        String reqBoxName = INSTALL_TARGET;
        String userName = "boxInstallTestAccount";
        String password = "password27";

        try {
            createInstallTarget(reqCellName, userName, password, true);

            // テスト用Box作成
            BoxUtils.create(reqCellName, reqBoxName, AbstractCase.MASTER_TOKEN_NAME);

            // パスワード認証
            String token = getAccessToken(reqCellName, userName, password);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(token, REQUEST_NORM_FILE, reqCellName, reqBoxName, headers, body);
            res.statusCode(HttpStatus.SC_FORBIDDEN);
            String code = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getCode();
            String message = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING.getMessage();
            res.checkErrorResponse(code, message);
        } finally {
            Setup.cellBulkDeletion(reqCellName);
        }
    }

    /**
     * 存在するスキーマ付Boxに対して権限ありの自分セルローカルトークンでbarInstarllを実施して400が返却されること.
     */
    @Test
    public final void 存在するスキーマ付Boxに対して権限ありの自分セルローカルトークンでbarInstarllを実施して400が返却されること() {
        String reqCellName = "boxInstallTestCell";
        String reqBoxName = INSTALL_TARGET;
        String userName = "boxInstallTestAccount";
        String password = "password27";

        try {
            createInstallTarget(reqCellName, userName, password, false);

            // テスト用Box作成（Schema付）
            BoxUtils.createWithSchema(reqCellName, reqBoxName, AbstractCase.MASTER_TOKEN_NAME, SCHEMA_URL);

            // パスワード認証
            String token = getAccessToken(reqCellName, userName, password);

            // リクエスト実行
            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(token, REQUEST_NORM_FILE, reqCellName, reqBoxName, headers, body);
            res.statusCode(HttpStatus.SC_BAD_REQUEST);
            String code = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.getCode();
            String message = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.
                    params(SCHEMA_URL).getMessage();
            res.checkErrorResponse(code, message);
        } finally {
            Setup.cellBulkDeletion(reqCellName);
        }
    }

    /**
     * 存在するスキーマなしBoxに対して権限ありの自分セルローカルトークンでbarInstarllを実施して405が返却されること.
     */
    @Test
    public final void 存在するスキーマなしBoxに対して権限ありの自分セルローカルトークンでbarInstarllを実施して405が返却されること() {
        String reqCellName = "boxInstallTestCell";
        String reqBoxName = INSTALL_TARGET;
        String userName = "boxInstallTestAccount";
        String password = "password27";

        try {
            createInstallTarget(reqCellName, userName, password, false);

            // テスト用Box作成
            BoxUtils.create(reqCellName, reqBoxName, AbstractCase.MASTER_TOKEN_NAME);

            // パスワード認証
            String token = getAccessToken(reqCellName, userName, password);

            TResponse res = null;
            File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
            byte[] body = BarInstallTestUtils.readBarFile(barFile);
            Map<String, String> headers = new LinkedHashMap<String, String>();
            headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
            headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

            res = BarInstallTestUtils.request(token, REQUEST_NORM_FILE, reqCellName, reqBoxName, headers, body);
            res.statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            String code = PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.getCode();
            String message = PersoniumCoreException.BarInstall.BAR_FILE_BOX_ALREADY_EXISTS.
                    params(reqBoxName).getMessage();
            res.checkErrorResponse(code, message);
        } finally {
            Setup.cellBulkDeletion(reqCellName);
        }
    }

    /**
     * サービスコレクションの定義で__srcが未定義の場合にBoxインストールが異常終了すること.
     */
    @Test
    public final void サービスコレクションの定義で__srcが未定義の場合にBoxインストールが異常終了すること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + "/V1_1_2_bar_90rootprops_invalid_src.bar");
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);

        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.FAILED);
        // エラーとなったサービスコレクションが存在しないこと
        BarInstallTestUtils.propfind(reqCell, reqPath, "engine", AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NOT_FOUND);
        BarInstallTestUtils.propfind(reqCell, reqPath, "svccol1", AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NOT_FOUND);
    }

    /**
     * 登録済みスキーマURLを指定してBoxインストールすると400エラーになること.
     */
    @Test
    public final void 登録済みスキーマURLを指定してBoxインストールすると400エラーになること() {
        String reqCell = Setup.TEST_CELL1;
        String reqPath = INSTALL_TARGET;

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        // Boxインストール１回目（正常登録）
        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, reqPath, headers, body);
        res.statusCode(HttpStatus.SC_ACCEPTED);
        String location = res.getHeader(HttpHeaders.LOCATION);
        String expected = UrlUtils.cellRoot(reqCell) + reqPath;
        assertEquals(expected, location);
        BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);

        // Boxインストール２回目（既に同じスキーマURLを持つBoxが存在するため400エラー）
        res = BarInstallTestUtils.request(REQUEST_NORM_FILE, reqCell, "anotherBox", headers, body);
        res.statusCode(HttpStatus.SC_BAD_REQUEST);
        String code = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.getCode();
        String message = PersoniumCoreException.BarInstall.BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS.
                params("https://fqdn/testcell1/").getMessage();
        res.checkErrorResponse(code, message);
    }

    /**
     * 別のセル配下のBoxで参照しているスキーマURLを指定してBoxインストールしても正常終了すること.
     */
    @Test
    public final void 別のセル配下のBoxで参照しているスキーマURLを指定してBoxインストールしても正常終了すること() {
        String cell1Name = "barInstallTest_ownercell_1";
        String cell2Name = "barInstallTest_ownercell_2";
        String ownerName = "http://xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

        TResponse res = null;
        File barFile = new File(RESOURCE_PATH + BAR_FILE_MINIMUM);
        byte[] body = BarInstallTestUtils.readBarFile(barFile);
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put(HttpHeaders.CONTENT_TYPE, REQ_CONTENT_TYPE);
        headers.put(HttpHeaders.CONTENT_LENGTH, String.valueOf(body.length));

        try {
            // Cell作成
            CellUtils.create(cell1Name, AbstractCase.MASTER_TOKEN_NAME, ownerName, HttpStatus.SC_CREATED);
            CellUtils.create(cell2Name, AbstractCase.MASTER_TOKEN_NAME, ownerName, HttpStatus.SC_CREATED);

            // Cell1へスキーマあり用Boxを作成
            BoxUtils.createWithSchema(cell1Name, INSTALL_TARGET, AbstractCase.MASTER_TOKEN_NAME, SCHEMA_URL);

            // 先に作成したBoxと同じスキーマURLを持つBox（名前も同じ）へBoxインストールして正常登録（Boxの親Cellはそれぞれ異なる）
            res = BarInstallTestUtils.request(REQUEST_NORM_FILE, cell2Name, INSTALL_TARGET, headers, body);
            res.statusCode(HttpStatus.SC_ACCEPTED);
            String location = res.getHeader(HttpHeaders.LOCATION);
            String expected = UrlUtils.cellRoot(cell2Name) + INSTALL_TARGET;
            assertEquals(expected, location);
            BarInstallTestUtils.assertBarInstallStatus(location, SCHEMA_URL, ProgressInfo.STATUS.COMPLETED);
            // インストールしたBoxの存在確認
            BoxUtils.get(cell2Name, AbstractCase.MASTER_TOKEN_NAME, INSTALL_TARGET, HttpStatus.SC_OK);
        } finally {
            Setup.cellBulkDeletion(cell1Name);
            Setup.cellBulkDeletion(cell2Name);
        }
    }

    private void createInstallTarget(String reqCellName,
            String userName,
            String password,
            Boolean isForbiddenUser) {
        // Cell作成
        CellUtils.create(reqCellName, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        // Account作成
        AccountUtils.create(AbstractCase.MASTER_TOKEN_NAME, reqCellName, userName, password,
                HttpStatus.SC_CREATED);
        // Role作成
        RoleUtils.create(reqCellName, AbstractCase.MASTER_TOKEN_NAME, "boxInstallTestRole",
                HttpStatus.SC_CREATED);
        // Account-Role $links作成
        ResourceUtils.linkAccountRole(reqCellName, AbstractCase.MASTER_TOKEN_NAME, userName, null,
                "boxInstallTestRole", HttpStatus.SC_NO_CONTENT);

        if (!isForbiddenUser) {
            // Box-install権限設定
            Http.request("cell/acl-setting-single-request.txt")
                    .with("url", reqCellName)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("roleBaseUrl", UrlUtils.roleResource(reqCellName, null, ""))
                    .with("role", "boxInstallTestRole")
                    .with("privilege", "<D:box-install/>")
                    .returns()
                    .statusCode(HttpStatus.SC_OK);
        }
    }

    /**
     * パスワード認証を行い、アクセストークンを取得する.
     * @param cellName Cell名
     * @param user user
     * @param password password
     * @return アクセストークン
     */
    protected String getAccessToken(String cellName, String user, String password) {
        TResponse res = Http.request("authn/password-cl-c0.txt")
                .with("remoteCell", cellName)
                .with("username", user)
                .with("password", password)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        JSONObject json = res.bodyAsJson();
        String token = (String) json.get(OAuth2Helper.Key.ACCESS_TOKEN);
        return token;
    }

}
