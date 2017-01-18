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

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.test.categories.Integration;
import com.fujitsu.dc.test.categories.Regression;
import com.fujitsu.dc.test.categories.Unit;
import com.fujitsu.dc.test.jersey.AbstractCase;
import com.fujitsu.dc.test.jersey.ODataCommon;
import com.fujitsu.dc.test.unit.core.UrlUtils;
import com.fujitsu.dc.test.utils.ExtCellUtils;

/**
 * ExtCell更新のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtCellUpdateTest extends ODataCommon {

    private static String cellName = "testcell1";
    private String extCellUrl = UrlUtils.cellRoot("cellHoge");
    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtCellUpdateTest() {
        super("com.fujitsu.dc.core.rs");
    }

    /**
     * ExtCell更新の正常系のテスト.
     */
    @Test
    public final void ExtCell更新の正常系のテスト() {
        String newCellUrl = UrlUtils.cellRoot("cellXXX");

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            ExtCellUtils.delete(token, cellName, newCellUrl, -1);
        }
    }

    /**
     * Urlが空文字の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが空文字の場合400エラーを返却すること() {
        String newCellUrl = "";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * Urlが1024文字の場合正常に作成されること.
     */
    @Test
    public final void Urlが1024文字の場合正常に作成されること() {
        String newCellUrl = "http://localhost:8080/dc1-core/testcell1" + StringUtils.repeat("a", 983) + "/";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_NO_CONTENT);
        } finally {
            ExtCellUtils.delete(token, cellName, newCellUrl, -1);
        }
    }

    /**
     * Urlが1025文字以上の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが1025文字以上の場合400エラーを返却すること() {
        String newCellUrl = "http://localhost:8080/dc1-core/testcell1" + StringUtils.repeat("a", 984) + "/";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * UrlのschemeがFTPの場合400エラーを返却すること.
     */
    @Test
    public final void UrlのschemeがFTPの場合400エラーを返却すること() {
        String newCellUrl = "ftp://localhost:21/dc1-core/testcell1/";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * Urlが数字の場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Urlが数字の場合400エラーを返却すること() {
        JSONObject body = new JSONObject();
        body.put("Url", extCellUrl);
        ExtCellUtils.extCellAccess(HttpMethod.PUT, cellName, "123", token, body.toJSONString(),
                HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlが真偽値の場合400エラーを返却すること.
     */
    @SuppressWarnings("unchecked")
    @Test
    public final void Urlが真偽値の場合400エラーを返却すること() {
        JSONObject body = new JSONObject();
        body.put("Url", extCellUrl);
        ExtCellUtils.extCellAccess(HttpMethod.PUT, cellName, "false", token, body.toJSONString(),
                HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlが正規化されていない場合400エラーを返却すること.
     */
    @Test
    public final void Urlが正規化されていない場合400エラーを返却すること() {
        String newCellUrl = "https://localhost:8080/dc1-core/test/../cell/./box/../";

        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.update(token, cellName, extCellUrl, newCellUrl, HttpStatus.SC_BAD_REQUEST);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }
}
