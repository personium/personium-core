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
package com.fujitsu.dc.test.jersey.cell.auth;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;

import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcResponse;
import com.fujitsu.dc.test.jersey.box.odatacol.UserDataListFilterTest;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.AccountUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.ResourceUtils;
import com.fujitsu.dc.test.utils.TResponse;
import com.fujitsu.dc.test.utils.UserDataUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * 認証のテスト.
 */
public class AuthTestCommon extends JerseyTest {

    static final String TEST_CELL1 = Setup.TEST_CELL1;
    static final String TEST_CELL2 = Setup.TEST_CELL2;
    static final String TEST_APP_CELL1 = "schema1";
    static final String TEST_BOX = Setup.TEST_BOX1;
    static final String DAV_COLLECTION = "setdavcol/";
    static final String ODATA_COLLECTION = "setodata/";
    static final String DAV_RESOURCE = "dav.txt";
    static final String ACL_DEFAULT_SETTING_FILE = "box/acl-default.txt";
    static final String ACL_VARIABLE_SETTING_FILE = "box/acl-setting.txt";
    static final String ACL_AUTH_TEST_FILE = "box/acl-authtest.txt";
    static final String ALL_PROP_FILE = "box/propfind-col-allprop.txt";
    static final String DEL_COL_FILE = "box/delete-col.txt";
    static final String MASTER_TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final int SLEEP_MILLES = 1000;

    /**
     * 認証トークン配列番号.
     */
    static final int NO_PRIVILEGE = 0;
    static final int READ = 1;
    static final int WRITE = 2;
    static final int READ_WRITE = 3;
    static final int READ_ACL = 4;
    static final int WRITE_ACL = 5;
    static final int WRITE_PROP = 6;
    static final int READ_PROP = 7;

    static final int TOKEN_KINKD_NUM = 9;

    /**
     * コンストラクタ.
     */
    public AuthTestCommon() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * BOXのリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void boxAccess(HashMap<Integer, String> tokens) {
        // コレクションのACLワークテスト
        // Boxアクセス制御のテスト testcell1/box1
        // GET
        ResourceUtils.retrieve(tokens.get(NO_PRIVILEGE), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ), "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_WRITE), "", HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_ACL), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE_ACL), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE_PROP), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_PROP), "", HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        // PROPFIND
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(NO_PRIVILEGE),
                HttpStatus.SC_FORBIDDEN, TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(READ), HttpStatus.SC_MULTI_STATUS,
                TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS,
                TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN,
                TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(WRITE_ACL),
                HttpStatus.SC_FORBIDDEN, TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(WRITE_PROP),
                HttpStatus.SC_FORBIDDEN, TEST_BOX);
        DavResourceUtils.propfind("box/propfind-box-allprop.txt", tokens.get(READ_PROP),
                HttpStatus.SC_MULTI_STATUS, TEST_BOX);
        // OPTIONS
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(NO_PRIVILEGE), "", HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ), "", HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE), "", HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_WRITE), "", HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_ACL), "", HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_ACL), "", HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_PROP), "", HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_PROP), "", HttpStatus.SC_FORBIDDEN);
        // WRITE権のテスト
        // PROPATCH
        DavResourceUtils.setProppatch(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "");
        DavResourceUtils.setProppatch(tokens.get(READ), HttpStatus.SC_FORBIDDEN, "");
        DavResourceUtils.setProppatch(tokens.get(WRITE), HttpStatus.SC_MULTI_STATUS, "");
        DavResourceUtils.setProppatch(tokens.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, "");
        DavResourceUtils.setProppatch(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "");
        DavResourceUtils.setProppatch(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "");
        DavResourceUtils.setProppatch(tokens.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, "");
        DavResourceUtils.setProppatch(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "");
        // ACL
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_WRITE), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_ACL), HttpStatus.SC_OK, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");

    }

    /**
     * WebDavのリソースに対するアクセス制御テスト.
     * @param t テストに必要なトークンのリスト
     */
    public static final void davCollectionAccess(HashMap<Integer, String> t) {
        String path = "setdavcol/col";
        String davcolName = "setdavcol";
        String odatacolName = "setodata";
        // DavCollectionアクセス制御のテスト testcell1/box1/setdavcol/
        // GET
        ResourceUtils.retrieve(t.get(NO_PRIVILEGE), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1,
                Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ), DAV_COLLECTION, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_WRITE), DAV_COLLECTION, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_ACL), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE_ACL), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE_PROP), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_PROP), DAV_COLLECTION, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        // PROPFIND
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ), HttpStatus.SC_MULTI_STATUS, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_PROP), HttpStatus.SC_MULTI_STATUS, davcolName);
        // OPTIONS
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(NO_PRIVILEGE), davcolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ), davcolName, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE), davcolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_WRITE), davcolName, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_ACL), davcolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE_ACL), davcolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE_PROP), davcolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_PROP), davcolName, HttpStatus.SC_FORBIDDEN);
        // WRITE権のテスト
        // PROPATCH
        DavResourceUtils.setProppatch(t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.setProppatch(t.get(READ), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.setProppatch(t.get(WRITE), HttpStatus.SC_MULTI_STATUS, davcolName);
        DavResourceUtils.setProppatch(t.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, davcolName);
        DavResourceUtils.setProppatch(t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.setProppatch(t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, davcolName);
        DavResourceUtils.setProppatch(t.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, davcolName);
        DavResourceUtils.setProppatch(t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, davcolName);
        // MKCOL
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(READ), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(WRITE), HttpStatus.SC_CREATED, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(READ_WRITE), HttpStatus.SC_CREATED, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        // DELETE
        DavResourceUtils.createWebDavCollection(path, TEST_CELL1);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE), HttpStatus.SC_NO_CONTENT, path);
        DavResourceUtils.createWebDavCollection(path, TEST_CELL1);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_WRITE), HttpStatus.SC_NO_CONTENT, path);
        DavResourceUtils.createWebDavCollection(path, TEST_CELL1);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
        // ACL
        String file = ACL_AUTH_TEST_FILE;
        DavResourceUtils.createWebDavCollection(path, TEST_CELL1);
        DavResourceUtils.setACL(TEST_CELL1, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_WRITE), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE_ACL), HttpStatus.SC_OK, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path, file, TEST_BOX, "");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT,
                path);
        // GET OdataCollectionアクセス制御のテスト testcell1/box1/setodata
        ResourceUtils.retrieve(t.get(NO_PRIVILEGE), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ), odatacolName, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_WRITE), odatacolName, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_ACL), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE_ACL), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(WRITE_PROP), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(t.get(READ_PROP), odatacolName, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        // PROPFIND
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ), HttpStatus.SC_MULTI_STATUS, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.propfind(ALL_PROP_FILE, t.get(READ_PROP), HttpStatus.SC_MULTI_STATUS, odatacolName);
        // OPTIONS
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(NO_PRIVILEGE), odatacolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ), odatacolName, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE), odatacolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_WRITE), odatacolName, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_ACL), odatacolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE_ACL), odatacolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(WRITE_PROP), odatacolName, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, t.get(READ_PROP), odatacolName, HttpStatus.SC_FORBIDDEN);
        // WRITE権のテスト
        // PROPATCH
        DavResourceUtils.setProppatch(t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.setProppatch(t.get(READ), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.setProppatch(t.get(WRITE), HttpStatus.SC_MULTI_STATUS, odatacolName);
        DavResourceUtils.setProppatch(t.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, odatacolName);
        DavResourceUtils.setProppatch(t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.setProppatch(t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, odatacolName);
        DavResourceUtils.setProppatch(t.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, odatacolName);
        DavResourceUtils.setProppatch(t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, odatacolName);
        // DELETE
        createOdataCollection("authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE), HttpStatus.SC_NO_CONTENT, "authtestOdata");
        createOdataCollection("authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_WRITE), HttpStatus.SC_NO_CONTENT, "authtestOdata");
        createOdataCollection("authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "authtestOdata");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT,
                "authtestOdata");
        // ACL
        file = ACL_AUTH_TEST_FILE;
        createOdataCollection("authtestOdata");
        DavResourceUtils.setACL(TEST_CELL1, t.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "authtestOdata", file,
                TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ), HttpStatus.SC_FORBIDDEN, "authtestOdata", file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE), HttpStatus.SC_FORBIDDEN, "authtestOdata", file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_WRITE), HttpStatus.SC_FORBIDDEN, "authtestOdata", file,
                TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "authtestOdata", file, TEST_BOX,
                "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE_ACL), HttpStatus.SC_OK, "authtestOdata", file, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "authtestOdata", file,
                TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, t.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "authtestOdata", file,
                TEST_BOX, "");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT,
                "authtestOdata");
    }

    /**
     * ODATAのリschemaソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void odataSchemaAccess(HashMap<Integer, String> tokens) {
        // Odataアクセス制御のテスト testcell1/box1/setodata/$metadata GET
        ResourceUtils.getMetadata(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ), HttpStatus.SC_OK, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_WRITE), HttpStatus.SC_OK, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "\\$metadata");
        // OPTIONS
        ResourceUtils.optionsMetadata(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsMetadata(tokens.get(READ), HttpStatus.SC_OK);
        ResourceUtils.optionsMetadata(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsMetadata(tokens.get(READ_WRITE), HttpStatus.SC_OK);
        ResourceUtils.optionsMetadata(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsMetadata(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsMetadata(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsMetadata(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        // GET testcell1/box1/setodata/$metadata/$metadata
        ResourceUtils.getMetadata(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ), HttpStatus.SC_OK, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_WRITE), HttpStatus.SC_OK, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
        ResourceUtils.getMetadata(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "\\$metadata/\\$metadata");
    }

    /**
     * ODATAのentityリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void odataEntityAccess(HashMap<Integer, String> tokens) {
        String path;

        // @POST 【ODataEntitiesResource】testcell1/box1/setodata/entset
        UserDataUtils.createWithDelete(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createWithDelete(tokens.get(READ), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createWithDelete(tokens.get(WRITE), HttpStatus.SC_CREATED);
        UserDataUtils.createWithDelete(tokens.get(READ_WRITE), HttpStatus.SC_CREATED);
        UserDataUtils.createWithDelete(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createWithDelete(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createWithDelete(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createWithDelete(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        // @GET
        UserDataUtils.list(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.list(tokens.get(READ), HttpStatus.SC_OK);
        UserDataUtils.list(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.list(tokens.get(READ_WRITE), HttpStatus.SC_OK);
        UserDataUtils.list(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.list(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.list(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.list(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        // @OPTIONS
        path = "/testcell1/box1/setodata/Category";
        UserDataUtils.options(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_WRITE), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // @GET 【ODataEntityResource】testcell1/box1/setodata/entset('key')
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.get(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.get(tokens.get(READ), HttpStatus.SC_OK);
        UserDataUtils.get(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.get(tokens.get(READ_WRITE), HttpStatus.SC_OK);
        UserDataUtils.get(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.get(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.get(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.get(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, "Price", "auth_test", "setodata");
        // @PUT
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.update(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.update(tokens.get(READ), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.update(tokens.get(WRITE), HttpStatus.SC_NO_CONTENT);
        UserDataUtils.update(tokens.get(READ_WRITE), HttpStatus.SC_NO_CONTENT);
        UserDataUtils.update(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.update(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.update(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.update(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, "Price", "auth_test", "setodata");
        // @MERGE
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.merge(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.merge(tokens.get(READ), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.merge(tokens.get(WRITE), HttpStatus.SC_NO_CONTENT);
        UserDataUtils.merge(tokens.get(READ_WRITE), HttpStatus.SC_NO_CONTENT);
        UserDataUtils.merge(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.merge(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.merge(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.merge(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, "Price", "auth_test", "setodata");
        // @DELETE
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.delete(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "Price", "auth_test",
                "setodata");
        UserDataUtils.delete(tokens.get(READ), HttpStatus.SC_FORBIDDEN, "Price", "auth_test",
                "setodata");
        UserDataUtils.delete(tokens.get(WRITE), HttpStatus.SC_NO_CONTENT, "Price", "auth_test",
                "setodata");
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.delete(tokens.get(READ_WRITE), HttpStatus.SC_NO_CONTENT, "Price",
                "auth_test", "setodata");
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.delete(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "Price",
                "auth_test", "setodata");
        UserDataUtils.delete(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "Price",
                "auth_test", "setodata");
        UserDataUtils.delete(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "Price",
                "auth_test", "setodata");
        UserDataUtils.delete(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "Price",
                "auth_test", "setodata");
        UserDataUtils.delete(MASTER_TOKEN, HttpStatus.SC_NO_CONTENT, "Price", "auth_test", "setodata");
        // @OPTIONS
        path = "/testcell1/box1/setodata/Category('auth_test')";
        UserDataUtils.options(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_WRITE), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // TODO 【ODataLinksResource】testcell1/box1/setodata/$links/trget @POST @PUT @DELETE @GET @OPTIONS
        // TODO 【ODataLinksResource】testcell1/box1/setodata/$links/trget('id')
        // @POST @PUT @DELETE @GET @OPTIONS
        // 【ODataPropertyResource】testcell1/box1/setodata/entset('id')/_np
        // TODO @PUT
        // @POST
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.createViaNPWithDelete(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createViaNPWithDelete(tokens.get(READ), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createViaNPWithDelete(tokens.get(WRITE), HttpStatus.SC_CREATED);
        // TODO Nav-Propertyが判然対応していないため削除にてlink情報が残ってしまい409となるためおまじないの削除
        UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, "Price",
                "auth_test", "setodata");
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.createViaNPWithDelete(tokens.get(READ_WRITE), HttpStatus.SC_CREATED);
        // TODO Nav-Propertyが判然対応していないため削除にてlink情報が残ってしまい409となるためおまじないの削除
        UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, "Price",
                "auth_test", "setodata");
        UserDataUtils.create(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED);
        UserDataUtils.createViaNPWithDelete(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createViaNPWithDelete(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createViaNPWithDelete(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.createViaNPWithDelete(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN);
        UserDataUtils.delete(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_NO_CONTENT, "Price",
                "auth_test", "setodata");
        // TODO @MERGE @DELETE @GET
        // @OPTIONS
        path = "/testcell1/box1/setodata/Price('auth_test')/_Sales";
        UserDataUtils.options(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_WRITE), HttpStatus.SC_OK, path);
        UserDataUtils.options(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        UserDataUtils.options(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // TODO 【ODataPropertyResource】testcell1/box1/setodata/entset('id')/_np('id')
    }

    /**
     * WebDavファイルのリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void davFileAccess(HashMap<Integer, String> tokens) {
        String path;
        String fileBody = "testFileBody";

        // Davファイルのアクセス制御
        // READ権のテスト
        // GET
        path = "setdavcol/dav.txt";
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(NO_PRIVILEGE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_OK);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(WRITE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ_WRITE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_OK);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ_ACL), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(WRITE_ACL), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(WRITE_PROP), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ_PROP), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        // PROPFIND
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(NO_PRIVILEGE),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ),
                HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(WRITE),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ_WRITE),
                HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ_ACL),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(WRITE_ACL),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(WRITE_PROP),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ_PROP),
                HttpStatus.SC_MULTI_STATUS, path);
        // WRITE権のテスト
        // PROPATCH
        DavResourceUtils.setProppatch(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);

        // PUT
        path = "setdavcol/dav1.txt";
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(WRITE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(NO_PRIVILEGE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(WRITE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_NO_CONTENT);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ_WRITE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_NO_CONTENT);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ_ACL), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(WRITE_ACL), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(WRITE_PROP), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ_PROP), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                path, HttpStatus.SC_NO_CONTENT, TEST_BOX);
        // DELETE
        DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(NO_PRIVILEGE),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE),
                HttpStatus.SC_NO_CONTENT, path);
        DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_WRITE),
                HttpStatus.SC_NO_CONTENT, path);
        DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_ACL),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_ACL),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_PROP),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_PROP),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
        // ACL
        DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_WRITE), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_ACL), HttpStatus.SC_OK,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN,
                path, ACL_AUTH_TEST_FILE, TEST_BOX, "");
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
        // OPTIONS
        DavResourceUtils.createWebDavFile(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_WRITE), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_PROP), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_PROP), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);

    }

    /**
     * サービスコレクションのリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void serviceCollectionAccess(HashMap<Integer, String> tokens) {
        String path;
        String allPath;
        // サービスコレクションアクセス制御のテスト testcell1/box1/service_relay
        path = "service_relay";
        allPath = "testcell1/box1/" + path;
        createSvcCollection(path);
        // PROPFIND
        CellUtils.propfind(allPath, tokens.get(NO_PRIVILEGE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(WRITE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_WRITE), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(READ_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_PROP), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_PROP), "0", HttpStatus.SC_MULTI_STATUS);
        // ACL
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_WRITE), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_ACL), HttpStatus.SC_OK, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        DavResourceUtils.setACL(TEST_CELL1, tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        // PROPPATCH
        DavResourceUtils.setProppatch(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // DELETE
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE), HttpStatus.SC_NO_CONTENT, path);
        createSvcCollection(path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_WRITE), HttpStatus.SC_NO_CONTENT, path);
        createSvcCollection(path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // OPTIONS
        UserDataUtils.options(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        UserDataUtils.options(tokens.get(READ), HttpStatus.SC_OK, "/" + allPath);
        UserDataUtils.options(tokens.get(WRITE), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        UserDataUtils.options(tokens.get(READ_WRITE), HttpStatus.SC_OK, "/" + allPath);
        UserDataUtils.options(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        UserDataUtils.options(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        UserDataUtils.options(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        UserDataUtils.options(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, "/" + allPath);
        // サービスリソースコレクションアクセス制御のテスト testcell1/box1/service_relay/__src
        path = "service_relay/__src";
        allPath = "testcell1/box1/" + path;
        // PROPFIND
        CellUtils.propfind(allPath, tokens.get(NO_PRIVILEGE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(WRITE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_WRITE), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(READ_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_PROP), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_PROP), "0", HttpStatus.SC_MULTI_STATUS);

        // サービスソースコレクション配下のリソースに対するアクセス制御テスト
        serviceSourceCollectionAccess(tokens);
    }

    /**
     * サービスソースコレクション配下のリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    private static void serviceSourceCollectionAccess(HashMap<Integer, String> tokens) {
        String path;
        String allPath;
        // サービスソースコレクションアクセス制御のテスト testcell1/box1/service_relay/__src
        path = "service_relay/__src";
        allPath = "testcell1/box1/" + path;
        // PROPFIND
        CellUtils.propfind(allPath, tokens.get(NO_PRIVILEGE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(WRITE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_WRITE), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(READ_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_PROP), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_PROP), "0", HttpStatus.SC_MULTI_STATUS);
        // OPTIONS
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_WRITE), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_PROP), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_PROP), path, HttpStatus.SC_FORBIDDEN);

        // サービスソースアクセス制御のテスト testcell1/box1/service_relay/__src/file
        path = "service_relay/__src/test.js";
        allPath = "testcell1/box1/" + path;
        // MKCOL
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(WRITE), HttpStatus.SC_METHOD_NOT_ALLOWED, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(READ_WRITE), HttpStatus.SC_METHOD_NOT_ALLOWED,
                path, TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        DavResourceUtils.createWebDavCollectionWithDelete(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path,
                TEST_CELL1);
        // PUT
        ResourceUtils.put(tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(READ), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(WRITE), path, HttpStatus.SC_CREATED, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(READ_WRITE), path, HttpStatus.SC_NO_CONTENT, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(READ_ACL), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(WRITE_ACL), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(WRITE_PROP), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        ResourceUtils.put(tokens.get(READ_PROP), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, TEST_BOX);
        // GET
        ResourceUtils.retrieve(tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ), path, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_WRITE), path, HttpStatus.SC_OK, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_ACL), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE_ACL), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(WRITE_PROP), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        ResourceUtils.retrieve(tokens.get(READ_PROP), path, HttpStatus.SC_FORBIDDEN, TEST_CELL1, Setup.TEST_BOX1);
        // DELETE
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE), HttpStatus.SC_NO_CONTENT, path);
        ResourceUtils.put(AbstractCase.MASTER_TOKEN_NAME, path, HttpStatus.SC_CREATED, TEST_CELL1, TEST_BOX);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_WRITE), HttpStatus.SC_NO_CONTENT, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE_PROP), HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // PROPFIND
        ResourceUtils.put(AbstractCase.MASTER_TOKEN_NAME, path, HttpStatus.SC_CREATED, TEST_CELL1, TEST_BOX);
        CellUtils.propfind(allPath, tokens.get(NO_PRIVILEGE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(WRITE), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_WRITE), "0", HttpStatus.SC_MULTI_STATUS);
        CellUtils.propfind(allPath, tokens.get(READ_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_ACL), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(WRITE_PROP), "0", HttpStatus.SC_FORBIDDEN);
        CellUtils.propfind(allPath, tokens.get(READ_PROP), "0", HttpStatus.SC_MULTI_STATUS);
        // PROPPATCH
        DavResourceUtils.setProppatch(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_WRITE), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_ACL), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE_PROP), HttpStatus.SC_MULTI_STATUS, path);
        DavResourceUtils.setProppatch(tokens.get(READ_PROP), HttpStatus.SC_FORBIDDEN, path);
        // OPTIONS
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_WRITE), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_ACL), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE_PROP), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_PROP), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
    }


    /**
     * Nullリソースに対するアクセス制御テスト.
     * @param tokens テストに必要なトークンのリスト
     */
    public static final void nullResouceAccess(HashMap<Integer, String> tokens) {
        String path;
        String fileBody = "testFileBody";

        DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, "",
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
        // GET
        path = "setdavcol/nothing.txt";
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(NO_PRIVILEGE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_NOT_FOUND);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(WRITE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.getWebDavFile(TEST_CELL1, tokens.get(READ_WRITE), "box/dav-get.txt",
                TEST_BOX, path, HttpStatus.SC_NOT_FOUND);

        // PROPFIND
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(NO_PRIVILEGE),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ),
                HttpStatus.SC_NOT_FOUND, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(WRITE),
                HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.propfind(ALL_PROP_FILE, tokens.get(READ_WRITE),
                HttpStatus.SC_NOT_FOUND, path);

        // PROPATCH
        DavResourceUtils.setProppatch(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.setProppatch(tokens.get(WRITE), HttpStatus.SC_NOT_FOUND, path);
        DavResourceUtils.setProppatch(tokens.get(READ_WRITE), HttpStatus.SC_NOT_FOUND, path);

        // PUT
        path = "setdavcol/nothisng.txt";
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(NO_PRIVILEGE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_FORBIDDEN);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(WRITE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                path, HttpStatus.SC_NO_CONTENT, TEST_BOX);
        DavResourceUtils.createWebDavFile(TEST_CELL1, tokens.get(READ_WRITE), "box/dav-put.txt",
                fileBody, TEST_BOX, path, HttpStatus.SC_CREATED);
        DavResourceUtils.deleteWebDavFile("box/dav-delete.txt", TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                path, HttpStatus.SC_NO_CONTENT, TEST_BOX);

        // DELETE
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(NO_PRIVILEGE),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ),
                HttpStatus.SC_FORBIDDEN, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(WRITE),
                HttpStatus.SC_NOT_FOUND, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, tokens.get(READ_WRITE),
                HttpStatus.SC_NOT_FOUND, path);

        // MKCOL
        path = "nothingcol/";
        DavResourceUtils.createWebDavCollection(tokens.get(NO_PRIVILEGE), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.createWebDavCollection(tokens.get(READ), HttpStatus.SC_FORBIDDEN, path);
        DavResourceUtils.createWebDavCollection(tokens.get(WRITE), HttpStatus.SC_CREATED, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
        DavResourceUtils.createWebDavCollection(tokens.get(READ_WRITE), HttpStatus.SC_CREATED, path);
        ResourceUtils.delete(DEL_COL_FILE, TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME,
                HttpStatus.SC_NO_CONTENT, path);
        // OPTIONS
        path = "nothingcol/nothing.txt";
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ), path, HttpStatus.SC_OK);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.optionsUnderBox1(TEST_CELL1, tokens.get(READ_WRITE), path, HttpStatus.SC_OK);
        // POST
        path = "/testcell1/box1/huge/huga";
        ResourceUtils.requestUtil("POST", tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("POST", tokens.get(READ), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.requestUtil("POST", tokens.get(WRITE), path, HttpStatus.SC_NOT_FOUND);
        ResourceUtils.requestUtil("POST", tokens.get(READ_WRITE), path, HttpStatus.SC_NOT_FOUND);
        // REPORT
        path = "huga/hoge";
        ResourceUtils.report(TEST_CELL1, tokens.get(NO_PRIVILEGE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.report(TEST_CELL1, tokens.get(READ), path, HttpStatus.SC_NOT_FOUND);
        ResourceUtils.report(TEST_CELL1, tokens.get(WRITE), path, HttpStatus.SC_FORBIDDEN);
        ResourceUtils.report(TEST_CELL1, tokens.get(READ_WRITE), path, HttpStatus.SC_NOT_FOUND);
    }

    /**
     * コレクション作成及びACL設定(deleteやACLのテスト用).
     * @param path 対象のコレクションのパス
     */
    private static void createOdataCollection(String path) {
        // コレクション作成
        DavResourceUtils.createODataCollection(AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_CREATED, TEST_CELL1,
                TEST_BOX, path);
        // ACL設定
        DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
    }

    /**
     * コレクション作成及びACL設定(deleteやACLのテスト用).
     * @param path 対象のコレクションのパス
     */
    private static void createSvcCollection(String path) {
        // コレクション作成
        DavResourceUtils.createServiceCollection(AbstractCase.MASTER_TOKEN_NAME, -1, path);
        // ACL設定
        DavResourceUtils.setACL(TEST_CELL1, AbstractCase.MASTER_TOKEN_NAME, HttpStatus.SC_OK, path,
                ACL_AUTH_TEST_FILE, TEST_BOX, "");
    }

    /**
     * 認証テストに必要なトークンを取得する（パスワード認証で自分セルローカル）.
     * @param cell 認証先のセル
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void accountAuth(String cell, HashMap<Integer, String> token, HashMap<Integer, String> refreshToken) {
        JSONObject json;
        Long lastAuthenticatedTime;
        // account1 アクセス権無し
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account1");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account1", "password1", -1);
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account1", lastAuthenticatedTime);

        // account2 読み込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account2");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account2", "password2", -1);
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account2", lastAuthenticatedTime);

        // account3 書き込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account3");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account3", "password3", -1);
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account3", lastAuthenticatedTime);

        // account4 読み書き
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account4");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account4", "password4", -1);
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account4", lastAuthenticatedTime);

        // account6 ACL読み込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account6");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account6", "password6", -1);
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account6", lastAuthenticatedTime);

        // account7 ACL書き込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account7");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account7", "password7", -1);
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account7", lastAuthenticatedTime);

        // account8 PROPPACTH書き込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account8");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account8", "password8", -1);
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account8", lastAuthenticatedTime);

        // account9 PROPFIND読み込みのみ
        lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account9");
        json = ResourceUtils.getLocalTokenByPassAuth(cell, "account9", "password9", -1);
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account9", lastAuthenticatedTime);
    }

    /**
     * アカウントの最終ログイン時刻が更新されたかどうかをチェックする.
     * @param cell Cell名
     * @param account Account名
     * @param time 比較対象とする更新時刻(更新前の最終ログイン時刻等を指定)
     */
    public static void accountLastAuthenticatedCheck(String cell, String account,
            Long time) {
        TResponse response = AccountUtils.get(MASTER_TOKEN, HttpStatus.SC_OK, cell, account);
        JSONObject json = response.bodyAsJson();
        if (null == time) {
            time = 0L;
        }

        String lastAuthenticatedString = (String) ((JSONObject) ((JSONObject) json.get("d")).get("results"))
                .get("LastAuthenticated");
        String updatedString = (String) ((JSONObject) ((JSONObject) json.get("d")).get("results"))
                .get("__updated");
        Long lastAuthenticatedValue = 0L;
        if (null != lastAuthenticatedString) {
            lastAuthenticatedValue = UserDataListFilterTest.parseDateStringToLong(lastAuthenticatedString);
        }
        // 比較対象とする更新時刻よりも新しいことをチェック
        assertTrue(String.format("LastAuthenticatedが更新されていない。 lastAuthenticatedValue: %d 更新前のLastAuthenticaed: %d",
                lastAuthenticatedValue, time), lastAuthenticatedValue > time);
        // 現在時刻よりも古いことをチェック(不当に大きな値となっていないことの確認)
        assertTrue(String.format("LastAuthenticatedが現在時刻よりも新しい。 lastAuthenticatedValue: %d", lastAuthenticatedValue),
                lastAuthenticatedValue < System.currentTimeMillis());
        Long updatedValue = UserDataListFilterTest.parseDateStringToLong(updatedString);
        // __updatedが更新されていないことをチェック
        assertTrue(String.format("__updateが更新されている。 lastAuthenticatedValue: %d __updated: %d", lastAuthenticatedValue,
                updatedValue), lastAuthenticatedValue > updatedValue);
    }

    /**
     * アカウントの最終ログイン時刻が更新されたかどうかをチェックする.
     * @param cell Cell名
     * @param account Account名
     * @param time 比較対象とする更新時刻(更新前の最終ログイン時刻等を指定)
     */
    public static void accountLastAuthenticatedNotUpdatedCheck(String cell, String account,
            Long time) {
        if (null == time) {
            time = 0L;
        }
        // 比較対象とする更新時刻と同じであることをチェック
        assertTrue(getAccountLastAuthenticated(cell, account).equals(time));
    }

    /**
     * アカウントの最終ログイン時刻を取得する.
     * @param cell Cell名
     * @param account Account名
     * @return time アカウントの最終ログイン時刻
     */
    public static Long getAccountLastAuthenticated(String cell, String account) {
        TResponse response = AccountUtils.get(MASTER_TOKEN, HttpStatus.SC_OK, cell, account);
        JSONObject json = response.bodyAsJson();
        String lastAuthenticatedString = (String) ((JSONObject) ((JSONObject) json.get("d")).get("results"))
                .get("LastAuthenticated");
        Long lastAuthenticatedValue = 0L;
        if (null != lastAuthenticatedString) {
            lastAuthenticatedValue = UserDataListFilterTest.parseDateStringToLong(lastAuthenticatedString);
        }
        return lastAuthenticatedValue;
    }

    /**
     * リフレッシュトークン認証でトークンを取り直す.
     * @param cell 認証先のセル
     * @param beforeRefreshToken リフレッシュ認証に利用するトークンのリスト
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void refreshAuthForCellLocal(String cell,
            HashMap<Integer, String> beforeRefreshToken, HashMap<Integer, String> token,
            HashMap<Integer, String> refreshToken) {

        JSONObject json;

        // account1 アクセス権無し
        json = cellLocalRefresh(cell, beforeRefreshToken.get(NO_PRIVILEGE));
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));

        // account2 読み込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(READ));
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));

        // account3 書き込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(WRITE));
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));

        // account4 読み書き
        json = cellLocalRefresh(cell, beforeRefreshToken.get(READ_WRITE));
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));

        // account6 ACL読み込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(READ_ACL));
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));

        // account7 ACL書き込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(WRITE_ACL));
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));

        // account8 PROPPACTH書き込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(WRITE_PROP));
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));

        // account9 PROPFIND読み込みのみ
        json = cellLocalRefresh(cell, beforeRefreshToken.get(READ_PROP));
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));

    }

    /**
     * 認証テストに必要なトークンを取得する（パスワード認証でトランスセルトークン）.
     * @param cell 認証先のセル
     * @param targetCell dc_target
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void accountAuthForTransCell(String cell, String targetCell, HashMap<Integer, String> token,
            HashMap<Integer, String> refreshToken) {
        JSONObject json;
        // account1 アクセス権無し
        Long lastAuthenticatedTime = getAccountLastAuthenticated(cell, "account1");
        json = transCellToken(cell, "account1", "password1", UrlUtils.cellRoot(targetCell));
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));
        accountLastAuthenticatedCheck(cell, "account1", lastAuthenticatedTime);

        // account2 読み込みのみ
        json = transCellToken(cell, "account2", "password2", UrlUtils.cellRoot(targetCell));
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));

        // account3 書き込みのみ
        json = transCellToken(cell, "account3", "password3", UrlUtils.cellRoot(targetCell));
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));

        // account4 読み書き
        json = transCellToken(cell, "account4", "password4", UrlUtils.cellRoot(targetCell));
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));

        // account6 ACL読み込みのみ
        json = transCellToken(cell, "account6", "password6", UrlUtils.cellRoot(targetCell));
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));

        // account7 ACL書き込みのみ
        json = transCellToken(cell, "account7", "password7", UrlUtils.cellRoot(targetCell));
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));

        // account8 PROPPACTH書き込みのみ
        json = transCellToken(cell, "account8", "password8", UrlUtils.cellRoot(targetCell));
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));

        // account9 PROPFIND読み込みのみ
        json = transCellToken(cell, "account9", "password9", UrlUtils.cellRoot(targetCell));
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));
    }

    /**
     * リフレッシュトークン認証でトークンを取り直す.
     * @param cell 認証先のセル
     * @param targetCell dc_target
     * @param beforeRefreshToken リフレッシュ認証に利用するトークンのリスト
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void refreshAuthForTransCell(String cell, String targetCell,
            HashMap<Integer, String> beforeRefreshToken,
            HashMap<Integer, String> token, HashMap<Integer, String> refreshToken) {

        JSONObject json;

        // account1 アクセス権無し
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(NO_PRIVILEGE));
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));

        // account2 読み込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(READ));
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));

        // account3 書き込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(WRITE));
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));

        // account4 読み書き
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(READ_WRITE));
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));

        // account6 ACL読み込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(READ_ACL));
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));

        // account7 ACL書き込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(WRITE_ACL));
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));

        // account8 PROPPACTH書き込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(WRITE_PROP));
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));

        // account9 PROPFIND読み込みのみ
        json = refreshTransCell(cell, targetCell, beforeRefreshToken.get(READ_PROP));
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));
    }

    /**
     * 認証テストに必要なトークンを取得する（トークン認証でトランスセルトークン）.
     * @param cell 認証先のセル
     * @param targetCell dc_target
     * @param beforeRefreshToken トークン認証に利用するトークンのリスト
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void samlAuthForTransCell(String cell, String targetCell,
            HashMap<Integer, String> beforeRefreshToken,
            HashMap<Integer, String> token,
            HashMap<Integer, String> refreshToken) {
        JSONObject json;
        // account1 アクセス権無し
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(NO_PRIVILEGE));
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));

        // account2 読み込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(READ));
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));

        // account3 書き込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(WRITE));
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));

        // account4 読み書き
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(READ_WRITE));
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));

        // account6 ACL読み込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(READ_ACL));
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));

        // account7 ACL書き込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(WRITE_ACL));
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));

        // account8 PROPPACTH書き込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(WRITE_PROP));
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));

        // account9 PROPFIND読み込みのみ
        json = samlTransCell(cell, targetCell, beforeRefreshToken.get(READ_PROP));
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));
    }

    /**
     * 認証テストに必要なトークンを取得する（トークン認証で他人セルトークン）.
     * @param cell 認証先のセル
     * @param beforeToken トークン認証に利用するトークンのリスト
     * @param token トークンを入れるリスト
     * @param refreshToken リフレッシュトークンを入れるリスト
     */
    public static void samlAuthForCellLocal(String cell,
            HashMap<Integer, String> beforeToken,
            HashMap<Integer, String> token,
            HashMap<Integer, String> refreshToken) {
        JSONObject json;
        // account1 アクセス権無し
        json = samlCellLocal(cell, beforeToken.get(NO_PRIVILEGE));
        token.put(NO_PRIVILEGE, (String) json.get("access_token"));
        refreshToken.put(NO_PRIVILEGE, (String) json.get("refresh_token"));

        // account2 読み込みのみ
        json = samlCellLocal(cell, beforeToken.get(READ));
        token.put(READ, (String) json.get("access_token"));
        refreshToken.put(READ, (String) json.get("refresh_token"));

        // account3 書き込みのみ
        json = samlCellLocal(cell, beforeToken.get(WRITE));
        token.put(WRITE, (String) json.get("access_token"));
        refreshToken.put(WRITE, (String) json.get("refresh_token"));

        // account4 読み書き
        json = samlCellLocal(cell, beforeToken.get(READ_WRITE));
        token.put(READ_WRITE, (String) json.get("access_token"));
        refreshToken.put(READ_WRITE, (String) json.get("refresh_token"));

        // account6 ACL読み込みのみ
        json = samlCellLocal(cell, beforeToken.get(READ_ACL));
        token.put(READ_ACL, (String) json.get("access_token"));
        refreshToken.put(READ_ACL, (String) json.get("refresh_token"));

        // account7 ACL書き込みのみ
        json = samlCellLocal(cell, beforeToken.get(WRITE_ACL));
        token.put(WRITE_ACL, (String) json.get("access_token"));
        refreshToken.put(WRITE_ACL, (String) json.get("refresh_token"));

        // account8 PROPPACTH書き込みのみ
        json = samlCellLocal(cell, beforeToken.get(WRITE_PROP));
        token.put(WRITE_PROP, (String) json.get("access_token"));
        refreshToken.put(WRITE_PROP, (String) json.get("refresh_token"));

        // account9 PROPFIND読み込みのみ
        json = samlCellLocal(cell, beforeToken.get(READ_PROP));
        token.put(READ_PROP, (String) json.get("access_token"));
        refreshToken.put(READ_PROP, (String) json.get("refresh_token"));
    }

    private static JSONObject transCellToken(String cell, String account, String pass, String target) {
        TResponse res = Http.request("authn/password-tc-c0.txt")
                .with("remoteCell", cell)
                .with("username", account)
                .with("password", pass)
                .with("dc_target", target)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res.bodyAsJson();
    }

    private static JSONObject cellLocalRefresh(String cell, String refreshToken) {
        // アプリセルに対して認証
        TResponse res = Http.request("authn/refresh-cl.txt")
                .with("remoteCell", cell)
                .with("refresh_token", refreshToken)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res.bodyAsJson();
    }

    private static JSONObject refreshTransCell(String cell, String target, String refreshToken) {
        // アプリセルに対して認証
        TResponse res = Http.request("authn/refresh-tc.txt")
                .with("remoteCell", cell)
                .with("refresh_token", refreshToken)
                .with("dc_target", UrlUtils.cellRoot(target))
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res.bodyAsJson();
    }

    private static JSONObject samlTransCell(String cell, String target, String token) {
        TResponse res = Http.request("authn/saml-tc-c0.txt")
                .with("remoteCell", cell)
                .with("assertion", token)
                .with("dc_target", UrlUtils.cellRoot(target))
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res.bodyAsJson();
    }

    private static JSONObject samlCellLocal(String cell, String token) {
        TResponse res = Http.request("authn/saml-cl-c0.txt")
                .with("remoteCell", cell)
                .with("assertion", token)
                .returns()
                .statusCode(HttpStatus.SC_OK);
        return res.bodyAsJson();
    }

    /**
     * Accountロックが解放されるのを待つ.
     */
    public static void waitForAccountLock() {
        try {
            Thread.sleep(Long.parseLong(DcCoreConfig.getAccountLockLifetime()) * SLEEP_MILLES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * WWW-Authenticateヘッダが返却されないことのチェック.
     * @param res レスポンス
     */
    public static void checkAuthenticateHeaderNotExists(TResponse res) {
        assertThat(res.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
    }

    /**
     * WWW-Authenticateヘッダが返却されないことのチェック.
     * @param dcRes レスポンス
     */
    public static void checkAuthenticateHeaderNotExists(DcResponse dcRes) {
        assertThat(dcRes.getFirstHeader(HttpHeaders.WWW_AUTHENTICATE)).isNull();
    }

    /**
     * WWW-Authenticateヘッダが正しいことのチェック.
     * @param res レスポンス
     * @param expectedAuthScheme WWW-Authenticateヘッダに指定されるべきAuth Scheme("Bearer" or "Basic")
     * @param expectedCellName 期待するrealmに含まれるCell名
     */
    public static void checkAuthenticateHeader(TResponse res, String expectedAuthScheme, String expectedCellName) {
        String expected = String.format("%s realm=\"%s\"", expectedAuthScheme, UrlUtils.cellRoot(expectedCellName));
        List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(1, headers.size());
        assertThat(headers).contains(expected);
    }

    /**
     * WWW-Authenticateヘッダが正しいことのチェック.
     * @param res レスポンス
     * @param expectedAuthScheme WWW-Authenticateヘッダに指定されるべきAuth Scheme("Bearer" or "Basic")
     * @param expectedCellName 期待するrealmに含まれるCell名
     */
    public static void checkAuthenticateHeader(DcResponse res, String expectedAuthScheme, String expectedCellName) {
        String expected = String.format("%s realm=\"%s\"", expectedAuthScheme, UrlUtils.cellRoot(expectedCellName));
        Header[] headers = res.getResponseHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(1, headers.length);
        assertThat(headers[0].getValue()).isEqualTo(expected);
    }

    /**
     * WWW-Authenticateヘッダが正しいことのチェック. <br />
     * WWW-Authenticateヘッダには、BearerとBasicのそれぞれのヘッダが存在することを確認する.
     * @param res レスポンス
     * @param expectedCellName 期待するrealmに含まれるCell名
     */
    public static void checkAuthenticateHeader(TResponse res, String expectedCellName) {
        // WWW-Authenticateヘッダチェック
        String bearer = String.format("Bearer realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        String basic = String.format("Basic realm=\"%s\"", UrlUtils.cellRoot(expectedCellName));
        List<String> headers = res.getHeaders(HttpHeaders.WWW_AUTHENTICATE);
        assertEquals(2, headers.size());
        assertThat(headers).contains(bearer);
        assertThat(headers).contains(basic);
    }

}
