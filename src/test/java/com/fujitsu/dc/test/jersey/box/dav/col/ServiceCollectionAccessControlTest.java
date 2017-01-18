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
package com.fujitsu.dc.test.jersey.box.dav.col;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Acl;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.RoleUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

/**
 * Serviceコレクションに対する親子関係のアクセス制御のテスト.<br />
 * MOVEメソッドはMoveServiceCollectionAclTestクラスでテストしているため、本クラスではテストを省略している。
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ServiceCollectionAccessControlTest extends JerseyTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "CollectionAclTestCell";
    private static final String BOX_NAME = "box1";
    private static final String PARENT_COL_NAME = "parentCollection";
    private static final String COL_NAME = "testCollection";
    private static final String TARGET_COL_NAME = "targetCollection";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT = "account";
    private static final String ROLE = "role";

    private static final Map<String, String> INIT_PARAMS = new HashMap<String, String>();
    static {
        INIT_PARAMS.put("com.sun.jersey.config.property.packages",
                "com.fujitsu.dc.core.rs");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerRequestFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
        INIT_PARAMS.put("com.sun.jersey.spi.container.ContainerResponseFilters",
                "com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter");
    }

    /**
     * コンストラクタ.
     */
    public ServiceCollectionAccessControlTest() {
        super(new WebAppDescriptor.Builder(INIT_PARAMS).build());
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
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
     * 親コレクションに権限がないアカウントでsrcコレクションのPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションに権限がないアカウントでsrcコレクションのPROPFINDを行い403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s/__src", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
    }

    /**
     * 親コレクションにreadproperties権限があるアカウントでsrcコレクションのPROPFINDを行いACL情報以外が表示されること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadproperties権限があるアカウントでsrcコレクションのPROPFINDを行いACL情報以外が表示されること()
            throws JAXBException {
        String token;
        String parentColPath = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String path = String.format("%s/%s/__src", PARENT_COL_NAME, TARGET_COL_NAME);
        String pathForPropfind = String.format("%s/%s/%s/__src", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(parentColPath, ROLE, "read-properties");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, pathForPropfind, "1", HttpStatus.SC_MULTI_STATUS);
        String expectedUrl = UrlUtils.box(CELL_NAME, BOX_NAME, path);
        DavResourceUtils.assertContainsHrefUrl(expectedUrl, res);
        DavResourceUtils.assertNotContainsNodeInResXml(res, "ace");
    }

    /**
     * 親コレクションにreadacl権限があるアカウントでsrcコレクションのPROPFINDを行い403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 親コレクションにreadacl権限があるアカウントでsrcコレクションのPROPFINDを行い403エラーとなること() throws JAXBException {
        String token;
        String parentColPath = String.format("%s/%s", PARENT_COL_NAME, TARGET_COL_NAME);
        String path = String.format("%s/%s/%s/__src", BOX_NAME, PARENT_COL_NAME, TARGET_COL_NAME);

        // ACL設定
        setAcl(parentColPath, ROLE, "read-acl");

        // アクセストークン取得
        token = getToken(ACCOUNT);

        // リクエスト実行
        TResponse res = DavResourceUtils.propfind(token, CELL_NAME, path, "1", HttpStatus.SC_FORBIDDEN);
        DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
        ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
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
        DavResourceUtils.createServiceCollection(AbstractCase.BEARER_MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME,
                BOX_NAME,
                PARENT_COL_NAME + "/" + TARGET_COL_NAME);

        // Role作成
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE, HttpStatus.SC_CREATED);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT, ROLE, HttpStatus.SC_NO_CONTENT);
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
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.DEFAULT_BOX_NAME));
        return DavResourceUtils.setAcl(token, CELL_NAME, BOX_NAME, collection, acl, code);
    }

}
