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
package com.fujitsu.dc.test.jersey.box.dav.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.core.DcCoreAuthzException;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.Box;
import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.jersey.box.acl.jaxb.Acl;
import com.fujitsu.dc.test.jersey.cell.auth.AuthTestCommon;
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
 * ファイルのMOVEに対するアクセス制御のテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveFileAccessControlTest extends JerseyTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "MoveFileAclTestCell";
    private static final String BOX_NAME = "box1";
    private static final String SRC_COL_NAME = "srcCollection";
    private static final String DST_COL_NAME = "dstCollection";
    private static final String FILE_NAME = "test-file.txt";
    private static final String FILE_BODY = "Hello World!!";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT_READ = "read-account";
    private static final String ACCOUNT_WRITE = "write-account";
    private static final String ACCOUNT_NO_PRIVILEGE = "no-privilege-account";
    private static final String ACCOUNT_ALL_PRIVILEGE = "all-account";
    private static final String ACCOUNT_COMB_PRIVILEGE = "comb-account";

    private static final String ROLE_READ = "role-read";
    private static final String ROLE_WRITE = "role-write";
    private static final String ROLE_NO_PRIVILEGE = "role-no-privilege";
    private static final String ROLE_ALL_PRIVILEGE = "role-all-privilege";
    private static final String ROLE_COMB_PRIVILEGE = "role-comb-privilege";

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
    public MoveFileAccessControlTest() {
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
     * 正しいトークンを指定した場合ファイルのMOVEができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 正しいトークンを指定した場合ファイルのMOVEができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // write権限
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 不正なトークンを指定した場合ファイルのMOVEで401エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 不正なトークンを指定した場合ファイルのMOVEで401エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 不正なトークン
            token = "invalid_token";
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_UNAUTHORIZED);
            DcCoreException expectedException = DcCoreAuthzException.TOKEN_PARSE_ERROR;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
            AuthTestCommon.waitForAccountLock();
        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元Collectionにread権限を持つアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元Collectionにread権限を持つアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // read権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元Collectionにwrite権限を持つアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元Collectionにwrite権限を持つアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元Collectionにwrite権限を含むアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元Collectionにwrite権限を含むアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // read+write権限→201
            token = getToken(ACCOUNT_COMB_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元Collectionにall権限を持つアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元Collectionにall権限を持つアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // all権限→201
            token = getToken(ACCOUNT_ALL_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動先Collectionにread権限を持つアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにread権限を持つアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // read権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動先Collectionにwrite権限を持つアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにwrite権限を持つアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動先Collectionにwrite権限を含むアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにwrite権限を含むアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // read+write権限→201
            token = getToken(ACCOUNT_COMB_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動先Collectionにall権限を持つアカウントでファイルのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにall権限を持つアカウントでファイルのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // all権限→201
            token = getToken(ACCOUNT_ALL_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動先Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元の親Collectionにwrite権限が設定されている場合ファイルのMOVEのができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにwrite権限が設定されている場合ファイルのMOVEのができること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s/%s", BOX_NAME, SRC_COL_NAME, collection, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + FILE_NAME);
        }
    }

    /**
     * 移動元の親Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s/%s", BOX_NAME, SRC_COL_NAME, collection, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, FILE_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + collection + "/"
                    + FILE_NAME);
        }
    }

    /**
     * 移動先の親Collectionにwrite権限が設定されている場合ファイルのMOVEのができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親Collectionにwrite権限が設定されている場合ファイルのMOVEのができること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, collection, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    DST_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, DST_COL_NAME + "/" + collection + "/"
                    + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先の親Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親Collectionに権限を持たないアカウントでファイルのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, FILE_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, collection, FILE_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    DST_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, path, FILE_BODY, MediaType.TEXT_PLAIN,
                    HttpStatus.SC_CREATED);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            DcCoreException expectedException = DcCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteWebDavFile(CELL_NAME, MASTER_TOKEN, BOX_NAME, SRC_COL_NAME + "/" + FILE_NAME);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
        }
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
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, SRC_COL_NAME);
        DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME, DST_COL_NAME);

        // Role作成
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_READ, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_WRITE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_NO_PRIVILEGE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_ALL_PRIVILEGE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_COMB_PRIVILEGE, HttpStatus.SC_CREATED);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_READ, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_WRITE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_NO_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_ALL_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_COMB_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_READ, ROLE_READ, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_WRITE, ROLE_WRITE, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_NO_PRIVILEGE, ROLE_NO_PRIVILEGE, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_ALL_PRIVILEGE, ROLE_ALL_PRIVILEGE, HttpStatus.SC_NO_CONTENT);
        AccountUtils.createLinkWithRole(
                MASTER_TOKEN, CELL_NAME, null, ACCOUNT_COMB_PRIVILEGE, ROLE_COMB_PRIVILEGE, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたコレクションに対し、Role毎に対応するPrivilegeを設定.
     * @param collection コレクション名
     * @throws JAXBException ACLのパースに失敗
     */
    private void setDefaultAcl(String collection) throws JAXBException {
        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_READ, "read"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_WRITE, "write"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_ALL_PRIVILEGE, "all"));
        List<String> privileges = new ArrayList<String>();
        privileges.add("read");
        privileges.add("write");
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_COMB_PRIVILEGE, privileges));
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.DEFAULT_BOX_NAME));

        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, collection, acl, HttpStatus.SC_OK);
    }

    /**
     * 指定されたコレクションに対しPrincipal ALL Privilege ALLを設定.
     * @param collection コレクション名
     * @throws JAXBException ACLのパースに失敗
     */
    private void setPrincipalAllAcl(String collection) throws JAXBException {
        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(true, null, "all"));

        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, collection, acl, HttpStatus.SC_OK);
    }

}
