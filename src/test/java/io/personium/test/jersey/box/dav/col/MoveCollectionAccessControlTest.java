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
package io.personium.test.jersey.box.dav.col;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import io.personium.core.PersoniumCoreAuthzException;
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
import io.personium.test.jersey.PersoniumRequest;
import io.personium.test.jersey.PersoniumResponse;
import io.personium.test.jersey.PersoniumTest;
import io.personium.test.jersey.box.acl.jaxb.Acl;
import io.personium.test.jersey.cell.auth.AuthTestCommon;
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
 * ファイルのMOVEに対するアクセス制御のテスト.
 */
@RunWith(PersoniumIntegTestRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class MoveCollectionAccessControlTest extends PersoniumTest {

    private static final String PASSWORD = "password";
    private static final String CELL_NAME = "movecollectionacltestcell";
    private static final String BOX_NAME = "box1";
    private static final String SRC_COL_NAME = "srcCollection";
    private static final String DST_COL_NAME = "dstCollection";
    private static final String COL_NAME = "testCollection";
    private static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;

    private static final String ACCOUNT_READ = "read-account";
    private static final String ACCOUNT_BIND = "bind-account";
    private static final String ACCOUNT_UNBIND = "unbind-account";
    private static final String ACCOUNT_WRITE = "write-account";
    private static final String ACCOUNT_NO_PRIVILEGE = "no-privilege-account";
    private static final String ACCOUNT_ALL_PRIVILEGE = "all-account";
    private static final String ACCOUNT_COMB_PRIVILEGE = "comb-account";
    private static final String ACCOUNT_BIND_AND_UNBIND_PRIVILEGE = "bind-and-unbind-account";

    private static final String ROLE_READ = "role-read";
    private static final String ROLE_BIND = "role-bind";
    private static final String ROLE_UNBIND = "role-unbind";
    private static final String ROLE_WRITE = "role-write";
    private static final String ROLE_NO_PRIVILEGE = "role-no-privilege";
    private static final String ROLE_ALL_PRIVILEGE = "role-all-privilege";
    private static final String ROLE_COMB_PRIVILEGE = "role-comb-privilege";
    private static final String ROLE_BIND_AND_UNBIND_PREVILEGE = "role-bind-and-unbind-privilege";

    /**
     * コンストラクタ.
     */
    public MoveCollectionAccessControlTest() {
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
     * 正しいトークンを指定した場合コレクションのMOVEができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 正しいトークンを指定した場合コレクションのMOVEができること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // write権限
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 不正なトークンを指定した場合コレクションのMOVEで401エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 不正なトークンを指定した場合コレクションのMOVEで401エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // 不正なトークン
            token = "invalid_token";
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_UNAUTHORIZED);
            PersoniumCoreException expectedException = PersoniumCoreAuthzException.TOKEN_PARSE_ERROR;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
            AuthTestCommon.waitForIntervalLock();
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにread権限を持つアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにread権限を持つアカウントでコレクションのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // read権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * If you move a collection with an account that has bind privileges to the parent Collection of the move source, a 403 error occurs..
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MOVE_source_has_bind_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // Advance preparation.
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // bind authority
            token = getToken(ACCOUNT_BIND);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * If you move the collection with an account that has unbind permission to the parent Collection of the move source, it will be 201.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MOVE_source_has_unbind_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // Advance preparation.
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // unbind authority
            token = getToken(ACCOUNT_UNBIND);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにwrite権限を含むアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにwrite権限を含むアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // read+write権限→201
            token = getToken(ACCOUNT_COMB_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにall権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにall権限を持つアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // all権限→201
            token = getToken(ACCOUNT_ALL_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること1() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先Collectionにread権限を持つアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにread権限を持つアカウントでコレクションのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // read権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * It will be 201 if you move the collection with an account that has bind permission to the move destination Collection.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MOVE_destination_has_bind_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // Advance preparation.
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // bind authority.
            token = getToken(ACCOUNT_BIND);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * If you move a collection with an account that has unbind permissions to the destination Collection, it will result in 403 error.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MOVE_destination_has_unbind_authority() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // Advance preparation.
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // unbind authority.
            token = getToken(ACCOUNT_UNBIND);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先Collectionにwrite権限を含むアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにwrite権限を含むアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // read+write権限→201
            token = getToken(ACCOUNT_COMB_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先Collectionにall権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionにall権限を持つアカウントでコレクションのMOVEをした場合201となること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // all権限→201
            token = getToken(ACCOUNT_ALL_PRIVILEGE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親の親Collectionにwrite権限が設定されている場合コレクションのMOVEができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親の親Collectionにwrite権限が設定されている場合コレクションのMOVEができること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s/%s", BOX_NAME, SRC_COL_NAME, collection, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection + "/" + COL_NAME);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s/%s", BOX_NAME, SRC_COL_NAME, collection, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + collection + "/" + COL_NAME);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + collection + "/" + COL_NAME,
                    MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先の親の親Collectionにwrite権限が設定されている場合コレクションのMOVEができること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親の親Collectionにwrite権限が設定されている場合コレクションのMOVEができること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, collection, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    DST_COL_NAME + "/" + collection);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + COL_NAME);

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + collection + "/" + COL_NAME,
                    MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動先の親の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動先の親の親Collectionに権限を持たないアカウントでコレクションのMOVEをした場合403エラーとなること() throws JAXBException {
        String token;
        String collection = "collection";
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, collection, COL_NAME);

        try {
            // 事前準備
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    SRC_COL_NAME + "/" + COL_NAME);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED, CELL_NAME, BOX_NAME,
                    DST_COL_NAME + "/" + collection);

            // 権限なし→403
            token = getToken(ACCOUNT_NO_PRIVILEGE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination,
                    HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + collection, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにwrite権限を持つ_かつ_移動対象のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにwrite権限を持つ_かつ_移動対象のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合201となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            setAcl(SRC_COL_NAME + "/" + COL_NAME, ROLE_WRITE, "read");

            // 親にwrite権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにread権限を持つ_かつ_移動対象のCollectionにwrite権限を持つアカウントでコレクションのMOVEをした場合403となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにread権限を持つ_かつ_移動対象のCollectionにwrite権限を持つアカウントでコレクションのMOVEをした場合403となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            setAcl(SRC_COL_NAME + "/" + COL_NAME, ROLE_READ, "write");

            // 自身にwrite権限があるが、親がread権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動元の親Collectionにread権限を持つ_かつ_移動対象のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合403となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動元の親Collectionにread権限を持つ_かつ_移動対象のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合403となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            setDefaultAcl(SRC_COL_NAME + "/" + COL_NAME);

            // 自身と親にread権限→403
            token = getToken(ACCOUNT_READ);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動対象のCollectionにwrite権限を持つ_かつ_移動対象のCollection配下のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合201となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動対象のCollectionにwrite権限を持つ_かつ_移動対象のCollection配下のCollectionにread権限を持つアカウントでコレクションのMOVEをした場合201となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            setDefaultAcl(SRC_COL_NAME);
            setPrincipalAllAcl(DST_COL_NAME);

            // 移動対象のCollection
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            setDefaultAcl(SRC_COL_NAME + "/" + COL_NAME);
            // 移動対象の配下のCollection
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME + "/" + "collection");
            setAcl(SRC_COL_NAME + "/" + COL_NAME + "/collection", ROLE_WRITE, "read");

            // 自身にread権限→201
            token = getToken(ACCOUNT_WRITE);
            DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_CREATED);
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME + "/collection",
                    MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * 移動対象のCollectionにread権限を持つ_かつ_移動先Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合403となること.
     * @throws JAXBException ACLのパース失敗
     */
    @Test
    public void 移動対象のCollectionにread権限を持つ_かつ_移動先Collectionにwrite権限を持つアカウントでコレクションのMOVEをした場合403となること()
            throws JAXBException {
        String token;
        String path = String.format("%s/%s/%s", BOX_NAME, SRC_COL_NAME, COL_NAME);
        String destination = UrlUtils.box(CELL_NAME, BOX_NAME, DST_COL_NAME, COL_NAME);

        try {
            // 事前準備
            // setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);

            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            setAcl(SRC_COL_NAME + "/" + COL_NAME, ROLE_WRITE, "read");

            // write権限→201
            token = getToken(ACCOUNT_WRITE);
            TResponse res = DavResourceUtils.moveWebDav(token, CELL_NAME, path, destination, HttpStatus.SC_FORBIDDEN);
            PersoniumCoreException expectedException = PersoniumCoreException.Auth.NECESSARY_PRIVILEGE_LACKING;
            ODataCommon.checkErrorResponseBody(res, expectedException.getCode(), expectedException.getMessage());
        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, DST_COL_NAME + "/" + COL_NAME, MASTER_TOKEN, -1);
        }
    }

    /**
     * It becomes 403 when it is overwritten by MOVE of collection with the account which has bind authority to move destination Collection.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MoveOverwrite_destination_has_bind_authority() throws JAXBException {
        String token;
        final String srcColPath = SRC_COL_NAME + "/" + COL_NAME;
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColPath);
        final String destFilePath = DST_COL_NAME + "/destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFilePath);

        try {
            // Advance preparation
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, BOX_NAME + "/" + destFilePath, "testFileBody",
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // bind authority
            token = getToken(ACCOUNT_BIND);
            PersoniumRequest req = PersoniumRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColPath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
        }
    }

    /**
     * It becomes 403 when it is overwritten by MOVE of collection with the account which has unbind authority to move destination Collection.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MoveOverwrite_destination_has_unbind_authority() throws JAXBException {
        String token;
        final String srcColPath = SRC_COL_NAME + "/" + COL_NAME;
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColPath);
        final String destFilePath = DST_COL_NAME + "/destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFilePath);

        try {
            // Advance preparation
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, BOX_NAME + "/" + destFilePath, "testFileBody",
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // unbind authority
            token = getToken(ACCOUNT_UNBIND);
            PersoniumRequest req = PersoniumRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColPath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
        }
    }

    /**
     * It will be 204 if it is overwritten by MOVE of the collection with the account which has both bind and unbind privileges in the move destination Collection.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MoveOverwrite_destination_has_bind_and_unbind_authority() throws JAXBException {
        String token;
        final String srcColPath = SRC_COL_NAME + "/" + COL_NAME;
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColPath);
        final String destFilePath = DST_COL_NAME + "/destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFilePath);

        try {
            // Advance preparation
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, BOX_NAME + "/" + destFilePath, "testFileBody",
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // bind and unbind authority
            token = getToken(ACCOUNT_BIND_AND_UNBIND_PRIVILEGE);
            PersoniumRequest req = PersoniumRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColPath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
        }
    }

    /**
     * It becomes 204 when it overwrites with MOVE of collection with the account which has write authority to move destination Collection.
     * @throws JAXBException ACL parse failure
     */
    @Test
    public void MoveOverwrite_destination_has_write_authority() throws JAXBException {
        String token;
        final String srcColPath = SRC_COL_NAME + "/" + COL_NAME;
        final String srcUrl = UrlUtils.box(CELL_NAME, BOX_NAME, srcColPath);
        final String destFilePath = DST_COL_NAME + "/destFile.txt";
        final String destUrl = UrlUtils.box(CELL_NAME, BOX_NAME, destFilePath);

        try {
            // Advance preparation
            setPrincipalAllAcl(SRC_COL_NAME);
            setDefaultAcl(DST_COL_NAME);
            DavResourceUtils.createWebDavCollection(MASTER_TOKEN, HttpStatus.SC_CREATED,
                    CELL_NAME, BOX_NAME, SRC_COL_NAME + "/" + COL_NAME);
            DavResourceUtils.createWebDavFile(MASTER_TOKEN, CELL_NAME, BOX_NAME + "/" + destFilePath, "testFileBody",
                    MediaType.TEXT_PLAIN, HttpStatus.SC_CREATED);

            // write authority
            token = getToken(ACCOUNT_WRITE);
            PersoniumRequest req = PersoniumRequest.move(srcUrl);
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            req.header(HttpHeaders.DESTINATION, destUrl);
            req.header(HttpHeaders.OVERWRITE, "T");
            PersoniumResponse response = AbstractCase.request(req);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);

        } finally {
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, srcColPath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteCollection(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
            DavResourceUtils.deleteWebDavFile(CELL_NAME, BOX_NAME, destFilePath, MASTER_TOKEN, -1);
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
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_BIND, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_UNBIND, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_WRITE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_NO_PRIVILEGE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_ALL_PRIVILEGE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_COMB_PRIVILEGE, HttpStatus.SC_CREATED);
        RoleUtils.create(CELL_NAME, MASTER_TOKEN, ROLE_BIND_AND_UNBIND_PREVILEGE, HttpStatus.SC_CREATED);

        // Account作成
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_READ, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_BIND, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_UNBIND, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_WRITE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_NO_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_ALL_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_COMB_PRIVILEGE, PASSWORD, HttpStatus.SC_CREATED);
        AccountUtils.create(MASTER_TOKEN, CELL_NAME, ACCOUNT_BIND_AND_UNBIND_PRIVILEGE, PASSWORD,
                HttpStatus.SC_CREATED);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_READ, null,
                Role.EDM_TYPE_NAME, ROLE_READ, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_BIND, null,
                Role.EDM_TYPE_NAME, ROLE_BIND, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_UNBIND, null,
                Role.EDM_TYPE_NAME, ROLE_UNBIND, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_WRITE, null,
                Role.EDM_TYPE_NAME, ROLE_WRITE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_NO_PRIVILEGE, null,
                Role.EDM_TYPE_NAME, ROLE_NO_PRIVILEGE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_ALL_PRIVILEGE, null,
                Role.EDM_TYPE_NAME, ROLE_ALL_PRIVILEGE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_COMB_PRIVILEGE, null,
                Role.EDM_TYPE_NAME, ROLE_COMB_PRIVILEGE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
        LinksUtils.createLinks(CELL_NAME, Account.EDM_TYPE_NAME, ACCOUNT_BIND_AND_UNBIND_PRIVILEGE, null,
                Role.EDM_TYPE_NAME, ROLE_BIND_AND_UNBIND_PREVILEGE, null, MASTER_TOKEN, HttpStatus.SC_NO_CONTENT);
    }

    /**
     * 指定されたコレクションに対し、Role毎に対応するPrivilegeを設定.
     * @param collection コレクション名
     * @throws JAXBException ACLのパースに失敗
     */
    private void setDefaultAcl(String collection) throws JAXBException {
        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_READ, "read"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_BIND, "bind"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_UNBIND, "unbind"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_WRITE, "write"));
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_ALL_PRIVILEGE, "all"));
        List<String> privileges = new ArrayList<String>();
        privileges.add("read");
        privileges.add("write");
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_COMB_PRIVILEGE, privileges));
        privileges = new ArrayList<String>();
        privileges.add("bind");
        privileges.add("unbind");
        acl.getAce().add(DavResourceUtils.createAce(false, ROLE_BIND_AND_UNBIND_PREVILEGE, privileges));
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.MAIN_BOX_NAME));

        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, collection, acl, HttpStatus.SC_OK);
    }

    /**
     * 指定されたコレクションに対しPrivilegeを設定.
     * @param collection コレクション名
     * @param role ロール名
     * @param privilege 権限
     * @throws JAXBException ACLのパースに失敗
     */
    private void setAcl(String collection, String role, String privilege) throws JAXBException {
        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(false, role, privilege));
        acl.setXmlbase(String.format("%s/%s/__role/%s/", UrlUtils.getBaseUrl(), CELL_NAME, Box.MAIN_BOX_NAME));
        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, collection, acl, HttpStatus.SC_OK);
    }

    /**
     * 指定されたコレクションに対しPrincipal ROOT Privilege ALLを設定.
     * @param collection コレクション名
     * @throws JAXBException ACLのパースに失敗
     */
    private void setPrincipalAllAcl(String collection) throws JAXBException {
        Acl acl = new Acl();
        acl.getAce().add(DavResourceUtils.createAce(true, null, "all"));

        DavResourceUtils.setAcl(MASTER_TOKEN, CELL_NAME, BOX_NAME, collection, acl, HttpStatus.SC_OK);
    }


}
