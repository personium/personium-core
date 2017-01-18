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
package com.fujitsu.dc.test.jersey.box;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.setup.Setup;
import com.fujitsu.dc.test.utils.BoxUtils;
import com.fujitsu.dc.test.utils.CellUtils;
import com.fujitsu.dc.test.utils.DavResourceUtils;
import com.sun.jersey.test.framework.JerseyTest;

/**
 * PROPFINDのContent-Lengthの有無に関するテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class PropfindContentLengthTest extends JerseyTest {

    static final String DAVCOL_NAME = "setdavcol";
    static final String DAVFILE_NAME = "dav.txt";
    static final String SVCCOL_NAME = "setservice";
    static final String ODATACOL_NAME = Setup.TEST_ODATA;
    static final String TEST_CELL1 = "testcell1";
    static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    static final String BOX_NAME = "box1";
    static final String DEPTH = "0";

    /**
     * コンストラクタ.
     */
    public PropfindContentLengthTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * BoxレベルのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void BoxレベルのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        BoxUtils.propfind(TEST_CELL1, BOX_NAME, DEPTH, TOKEN, HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * BoxレベルのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void BoxレベルのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        BoxUtils.propfind(TEST_CELL1, BOX_NAME, DEPTH, TOKEN, true, HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * BoxレベルのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void BoxレベルのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        BoxUtils.propfind(TEST_CELL1, BOX_NAME, DEPTH, TOKEN, false, HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * WebDavCollectionレベルのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void WebDavCollectionレベルのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME);
    }

    /**
     * WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME);
    }

    /**
     * WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0-non-content-length.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME);
    }

    /**
     * WebDavファイルのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void WebDavファイルのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME + "/" + DAVFILE_NAME);
    }

    /**
     * WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void WebDavファイルのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME + "/" + DAVFILE_NAME);
    }

    /**
     * WebDavCollectionレベルのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void WebDavファイルのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0-non-content-length.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, DAVCOL_NAME + "/" + DAVFILE_NAME);
    }

    /**
     * ServiceコレクションのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void ServiceコレクションのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME);
    }

    /**
     * ServiceコレクションのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void ServiceコレクションのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME);
    }

    /**
     * ServiceコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void ServiceコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0-non-content-length.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME);
    }

    /**
     * ODataコレクションのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void ODataコレクションのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, ODATACOL_NAME);
    }

    /**
     * ODataコレクションのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void ODataコレクションのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, ODATACOL_NAME);
    }

    /**
     * ODataコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void ODataコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0-non-content-length.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, ODATACOL_NAME);
    }

    /**
     * ServiceのソースコレクションのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void ServiceのソースコレクションのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-allprop.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME + "/__src");
    }

    /**
     * ServiceのソースコレクションのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void ServiceのソースコレクションのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME + "/__src");
    }

    /**
     * ServiceのソースコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void ServiceのソースコレクションのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        DavResourceUtils.propfind("box/propfind-col-body-0-non-content-length.txt", TOKEN,
                HttpStatus.SC_MULTI_STATUS, SVCCOL_NAME + "/__src");
    }

    /**
     * CellのPROPFINDでbodyありのテスト.
     */
    @Test
    public final void CellのPROPFINDでbodyありのテスト() {
        // リクエストボディありのPROPFIND
        CellUtils.propfind(TEST_CELL1, "cell/propfind-cell-allprop.txt", TOKEN, "1", HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * CellのPROPFINDでbodyなしかつContentLengthありのテスト.
     */
    @Test
    public final void CellのPROPFINDでbodyなしかつContentLengthありのテスト() {
        // リクエストボディなしかつContentLengthありのPROPFIND
        CellUtils.propfind(TEST_CELL1, "cell/propfind-cell-body-0.txt", TOKEN, "1", HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * CellのPROPFINDでbodyなしかつContentLengthなしのテスト.
     */
    @Test
    public final void CellのPROPFINDでbodyなしかつContentLengthなしのテスト() {
        // リクエストボディなしかつContentLengthなしのPROPFIND
        CellUtils.propfind(TEST_CELL1, "cell/propfind-cell-body-0-non-content-length.txt",
                TOKEN, "1", HttpStatus.SC_MULTI_STATUS);
    }
}
