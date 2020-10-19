/**
 * Personium
 * Copyright 2014-2020 Personium Project Authors
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
package io.personium.test.jersey.box.dav.col;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreException;
import io.personium.core.model.Box;
import io.personium.core.model.ctl.Account;
import io.personium.core.model.ctl.Role;
import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.jersey.PersoniumIntegTestRunner;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.utils.AccountUtils;
import io.personium.test.utils.BoxUtils;
import io.personium.test.utils.CellUtils;
import io.personium.test.utils.DavResourceUtils;
import io.personium.test.utils.LinksUtils;
import io.personium.test.utils.ResourceUtils;
import io.personium.test.utils.RoleUtils;
import io.personium.test.utils.TResponse;
import io.personium.test.utils.UrlUtils;

/**
 * ODataコレクションに対する親子関係のアクセス制御のテスト.<br />
 * MOVEメソッドはMoveODataCollectionAclTestクラスでテストしているため、本クラスではテストを省略している。
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ODataCollectionAccessControlTest extends PersoniumTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "collectionacltestcell";
    private static final String BOX_NAME = "box1";
    private static final String PARENT_COL_NAME = "parentCollection";
    private static final String COL_NAME = "testCollection";
    private static final String TARGET_COL_NAME = "targetCollection";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT = "account";
    private static final String ROLE = "role";

    /**
     * コンストラクタ.
     */
    public ODataCollectionAccessControlTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * すべてのテストで最初に実行する処理.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    @Before
    public void before() throws JAXBException {
        createTestCollection();
    }

    /**
     * すべてのテストで最後に実行する処理.
     */
    @After
    public void after() {
        CellUtils.bulkDeletion(AbstractCase.BEARER_MASTER_TOKEN, CELL_NAME);
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでコレクションのDELETEを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでコレクションのDELETEを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, path, token,
                HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにwrite権限があるアカウントでコレクションのDELETEを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにwrite権限があるアカウントでコレクションのDELETEを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, path, token,
                HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwrite権限がある_かつ_対象コレクションに権限がないアカウントでコレクションのDELETEを行い204となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限がある_かつ_対象コレクションに権限がないアカウントでコレクションのDELETEを行い204となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, path, token, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Delete the collection with an account that has _ and _ the target collection with unbind permission on the parent collection, and become 204.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void DELETE_parent_has_unbind_authority_and_current_has_not_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // set ACL
        setAcl(PARENT_COL_NAME, ROLE, "unbind");

        // get token.
        token = getToken(ACCOUNT);

        // execute
        DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, path, token, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 親コレクションにwrite権限がある_かつ_対象コレクションにwrite権限があるアカウントでコレクションのDELETEを行い204となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限がある_かつ_対象コレクションにwrite権限があるアカウントでコレクションのDELETEを行い204となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");
        setAcl(path, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, path, token, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 親コレクションに権限がないアカウントでコレクションのMKCOLを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでコレクションのMKCOLを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.createODataCollection(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME,
                path);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにwrite権限があるアカウントでコレクションのMKCOLを行い201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwrite権限があるアカウントでコレクションのMKCOLを行い201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, path);
    }

    /**
     * Make MKCOL of the collection with an account that has bind authority to the parent collection and become 201.
     * @throws JAXBException Advance preparation
     */
    @Test
    public void MKCOL_has_bind_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, COL_NAME);

        // set ACL
        setAcl(PARENT_COL_NAME, ROLE, "write");

        // get token
        token = getToken(ACCOUNT);

        // execute
        DavResourceUtils.createODataCollection(token, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, path);
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでコレクションのPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでコレクションのPROPFINDを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにreadproperties権限があるアカウントでコレクションのPROPFINDを行いACL情報以外が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにreadproperties権限があるアカウントでコレクションのPROPFINDを行いACL情報以外が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertNotContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにreadacl権限があるアカウントでコレクションのPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにreadacl権限があるアカウントでコレクションのPROPFINDを行い403エラーとなること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでコレクションのPROPFINDを行い全てが表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでコレクションのPROPFINDを行い全てが表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "read-properties", "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadproperties権限がある_かつ_対象コレクションに権限がないアカウントでコレクションのPROPFINDを行いACL以外の情報が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限がある_かつ_対象コレクションに権限がないアカウントでコレクションのPROPFINDを行いACL以外の情報が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertNotContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadproperties権限があるアカウントでコレクションのPROPFINDを行いACL以外の情報が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadproperties権限があるアカウントでコレクションのPROPFINDを行いACL以外の情報が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-properties");
        setAcl(path, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertNotContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadacl権限があるアカウントでコレクションのPROPFINDを行い全ての情報が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadacl権限があるアカウントでコレクションのPROPFINDを行い全ての情報が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-properties");
        setAcl(path, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでPROPFINDを行い全ての情報が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限がある_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでPROPFINDを行い全ての情報が表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-properties");
        setAcl(path, ROLE, "read-properties", "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadacl権限がある_かつ_対象コレクションに権限がないアカウントでPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限がある_かつ_対象コレクションに権限がないアカウントでPROPFINDを行い403エラーとなること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにreadacl権限がある_かつ_対象コレクションにreadproperties権限があるアカウントでPROPFINDを行い全てが表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限がある_かつ_対象コレクションにreadproperties権限があるアカウントでPROPFINDを行い全てが表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-acl");
        setAcl(path, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadacl権限がある_かつ_対象コレクションにreadacl権限があるアカウントでPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限がある_かつ_対象コレクションにreadacl権限があるアカウントでPROPFINDを行い403エラーとなること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-acl");
        setAcl(path, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_FORBIDDEN);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにreadacl権限がある_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでPROPFINDを行い全てが表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限がある_かつ_対象コレクションにreadpropertiesとreadacl権限があるアカウントでPROPFINDを行い全てが表示されること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "read-acl");
        setAcl(path, ROLE, "read-properties", "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでPROPPATCHを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでPROPPATCHを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.setProppatch(token, HttpStatus.SC_FORBIDDEN, CELL_NAME, BOX_NAME, path);
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "write-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.setProppatch(token, HttpStatus.SC_MULTI_STATUS, CELL_NAME, BOX_NAME, path);
    }

    /**
     * 親コレクションにwriteproperties権限がある_かつ_対象コレクションに権限がないアカウントでPROPPATCHを行い207となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwriteproperties権限がある_かつ_対象コレクションに権限がないアカウントでPROPPATCHを行い207となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.setProppatch(token, HttpStatus.SC_MULTI_STATUS, CELL_NAME, BOX_NAME, path);
    }

    /**
     * 親コレクションにwriteproperties権限がある_かつ_対象コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwriteproperties権限がある_かつ_対象コレクションにwriteproperties権限があるアカウントでPROPPATCHを行い207となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write-properties");
        setAcl(path, ROLE, "write-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        DavResourceUtils.setProppatch(token, HttpStatus.SC_MULTI_STATUS, CELL_NAME, BOX_NAME, path);
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでACLを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションに権限がないアカウントでACLを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = setAcl(token, HttpStatus.SC_FORBIDDEN, path, ROLE, "write");
        PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションに権限がない_かつ_対象コレクションにwriteacl権限があるアカウントでACLを行い200となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がない_かつ_対象コレクションにwriteacl権限があるアカウントでACLを行い200となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(path, ROLE, "write-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        setAcl(token, HttpStatus.SC_OK, path, ROLE, "write");
    }

    /**
     * 親コレクションにwriteacl権限がある_かつ_対象コレクションに権限がないアカウントでACLを行い200となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwriteacl権限がある_かつ_対象コレクションに権限がないアカウントでACLを行い200となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        setAcl(token, HttpStatus.SC_OK, path, ROLE, "write");
    }

    /**
     * 親コレクションにwriteacl権限がある_かつ_対象コレクションにwriteacl権限があるアカウントでACLを行い200となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにwriteacl権限がある_かつ_対象コレクションにwriteacl権限があるアカウントでACLを行い200となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(PARENT_COL_NAME, ROLE, "write-acl");
        setAcl(path, ROLE, "write-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        setAcl(token, HttpStatus.SC_OK, path, ROLE, "write");
    }

    /**
     * Accountの自分セルローカルトークンを取得する.
     * @param account Account名
     * @return トークン
     */
    private String getToken(String account) {
        return ResourceUtils.getMyCellLocalToken(CELL_NAME, account, PASSWORD);
    }

    /**
     * テスト用のコレクションを作成し、テストに必要なAccountやACLの設定を作成する.
     * @throws JAXBException リクエストに設定したACLの定義エラー
     */
    private void createTestCollection() throws JAXBException {

        // Collection作成
        CellUtils.create(CELL_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        BoxUtils.create(CELL_NAME, BOX_NAME, MASTER_TOKEN, HttpStatus.SC_CREATED);
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                PARENT_COL_NAME);
        DavResourceUtils.createODataCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                PARENT_COL_NAME + "/" + TARGET_COL_NAME);

        // Role作成
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE, HttpStatus.SC_CREATED);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT, PASSWORD, HttpStatus.SC_CREATED);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT, null,
                Role.EDM_TYPE_NAME, ROLE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたコレクションに対しPrivilegeを設定.
     * @param collection コレクション名
     * @param role ロール名
     * @param privileges 権限(カンマ区切りで複数指定可能)
     * @throws JAXBException ACLのパースに失敗
     */
    private void setAcl(String collection, String role, String... privileges) throws JAXBException {
        setAcl(MASTER_TOKEN, HttpStatus.SC_OK, collection, role, privileges);
    }

    /**
     * 指定されたコレクションに対しPrivilegeを設定.
     * @param token 認証トークン
     * @param code 期待するレスポンスコード
     * @param collection コレクション名
     * @param role ロール名
     * @param privileges 権限(カンマ区切りで複数指定可能)
     * @return レスポンス
     * @throws JAXBException ACLのパースに失敗
     */
    private TResponse setAcl(String token, int code, String collection, String role, String... privileges)
            throws JAXBException {
        Acl acl = new Acl();
        for (String privilege : privileges) {
            acl.getAce().add(DavResourceUtils.createAce(false, role, privilege));
        }
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.MAIN_BOX_NAME));
        return DavResourceUtils.setAcl(token, CELL_NAME, BOX_NAME, collection, acl, code);
    }

}
