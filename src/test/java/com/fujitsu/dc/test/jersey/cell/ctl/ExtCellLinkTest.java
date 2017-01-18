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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpStatus;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.DcRunner;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;
import com.fujitsu.dc.test.utils.Http;
import com.fujitsu.dc.test.utils.TResponse;

/**
 * BoxとRelationの$linksのテスト.
 */
@RunWith(DcRunner.class)
@Category({Unit.class, Integration.class, Regression.class })
public class ExtCellLinkTest extends ODataCommon {

    private static final String CELL_NAME = "testcell1";
    private static final String EXTCELL_NAME = "cellHoge";
    private static final String ENTITY_SET_EXTCELL = "ExtCell";
    private static final String ENTITY_SET_ROLE = "Role";
    private static final String NAV_PROP_EXTCELL = "_ExtCell";
    private static final String NAV_PROP_ROLE = "_Role";
    private static final String TOKEN = AbstractCase.MASTER_TOKEN_NAME;
    private static String roleUri;
    private static String roleKey;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtCellLinkTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * 各テストを実行する前の処理.
     */
    @Before
    public final void before() {
        if (roleUri == null) {
            TResponse response = Http.request("role-list.txt")
                    .with("token", AbstractCase.MASTER_TOKEN_NAME).with("cellPath", CELL_NAME)
                    .returns().statusCode(HttpStatus.SC_OK);
            JSONObject d = (JSONObject) response.bodyAsJson().get("d");
            JSONArray results = (JSONArray) d.get("results");
            String name = (String) ((JSONObject) results.get(0)).get("Name");
            String boxName = (String) ((JSONObject) results.get(0)).get("_Box.Name");
            if (boxName == null) {
                roleKey = "Name='" + name + "',_Box.Name=null";
            } else {
                roleKey = "Name='" + name + "',_Box.Name='" + boxName + "'";
            }
            roleUri = UrlUtils.cellCtlWithoutSingleQuote(CELL_NAME, ENTITY_SET_ROLE, roleKey);
        }
    }

    /**
     * ExtCellとIdなしRoleのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void ExtCellとIdなしRoleのlinkを更新するとresponseが400であること() {
        String extCellUrl = UrlUtils.cellRoot(EXTCELL_NAME);
        String extCellUrlEncoded = null;
        try {
            extCellUrlEncoded = URLEncoder.encode(UrlUtils.cellRoot(EXTCELL_NAME), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            extCellUrlEncoded = extCellUrl;
        }
        String linkPath = "__ctl/" + ENTITY_SET_EXTCELL + "\\('" + extCellUrlEncoded + "'\\)/\\$links/" + NAV_PROP_ROLE;

        try {
            ExtCellUtils.create(TOKEN, CELL_NAME, extCellUrl, -1);

            // IdなしRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(TOKEN, CELL_NAME, extCellUrl, -1);
        }
    }

    /**
     * ExtCellとIdありRoleのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void ExtCellとIdありRoleのlinkを更新するとresponseが400であること() {
        String extCellUrl = UrlUtils.cellRoot(EXTCELL_NAME);
        String extCellUrlEncoded = null;
        try {
            extCellUrlEncoded = URLEncoder.encode(UrlUtils.cellRoot(EXTCELL_NAME), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            extCellUrlEncoded = extCellUrl;
        }
        String linkPath = "__ctl/" + ENTITY_SET_EXTCELL + "\\('" + extCellUrlEncoded + "'\\)/\\$links/"
                + NAV_PROP_ROLE + "\\(" + roleKey + "\\)";

        try {
            ExtCellUtils.create(TOKEN, CELL_NAME, extCellUrl, -1);

            // IdなしRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + roleUri + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } finally {
            ExtCellUtils.delete(TOKEN, CELL_NAME, extCellUrl, -1);
        }
    }

    /**
     * RoleとIdなしExtCellのlinkを更新するとresponseが400であること.
     */
    @Test
    public final void RoleとIdなしExtCellのlinkを更新するとresponseが400であること() {
        String extCellUrl = UrlUtils.cellRoot(EXTCELL_NAME);
        String linkPath = "__ctl/" + ENTITY_SET_ROLE + "\\('" + roleKey + "'\\)/\\$links/" + NAV_PROP_EXTCELL;

        try {
            ExtCellUtils.create(TOKEN, CELL_NAME, extCellUrl, -1);

            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + extCellUrl + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(TOKEN, CELL_NAME, extCellUrl, -1);
        }
    }

    /**
     * RoleとIdありExtCellのlinkを更新するとresponseが501であること.
     */
    @Test
    public final void RoleとIdありExtCellのlinkを更新するとresponseが501であること() {
        String extCellUrl = UrlUtils.cellRoot(EXTCELL_NAME);
        String extCellUrlEncoded = null;
        try {
            extCellUrlEncoded = URLEncoder.encode(UrlUtils.cellRoot(EXTCELL_NAME), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            extCellUrlEncoded = extCellUrl;
        }
        String linkPath = "__ctl/" + ENTITY_SET_ROLE + "\\(" + roleKey + "\\)/\\$links/"
                + NAV_PROP_EXTCELL + "\\('" + extCellUrlEncoded + "'\\)";

        try {
            ExtCellUtils.create(TOKEN, CELL_NAME, extCellUrl, -1);
            ExtCellUtils.get(TOKEN, CELL_NAME, extCellUrl, HttpStatus.SC_OK);

            // IdなしRoleへの$link更新
            Http.request("link-update-with-body.txt")
                    .with("cellPath", CELL_NAME)
                    .with("linkPath", linkPath)
                    .with("token", AbstractCase.MASTER_TOKEN_NAME)
                    .with("body", "\\{\\\"uri\\\":\\\"" + extCellUrl + "\\\"")
                    .returns()
                    .statusCode(HttpStatus.SC_NOT_IMPLEMENTED);
        } finally {
            ExtCellUtils.delete(TOKEN, CELL_NAME, extCellUrl, -1);
        }
    }

}
