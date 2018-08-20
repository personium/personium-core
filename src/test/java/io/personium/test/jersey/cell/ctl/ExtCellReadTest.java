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
package io.personium.test.jersey.cell.ctl;

import javax.ws.rs.HttpMethod;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.rs.PersoniumCoreApplication;
import io.personium.test.categories.Integration;
import io.personium.test.categories.Regression;
import io.personium.test.categories.Unit;
import io.personium.test.jersey.AbstractCase;
import io.personium.test.jersey.ODataCommon;
import io.personium.test.unit.core.UrlUtils;
import io.personium.test.utils.ExtCellUtils;

/**
 * ExtCell取得のテスト.
 */
@Category({Unit.class, Integration.class, Regression.class })
public class ExtCellReadTest extends ODataCommon {

    private static String cellName = "testcell1";
    private String extCellUrl = UrlUtils.cellRoot("cellHoge");
    private final String token = AbstractCase.MASTER_TOKEN_NAME;

    /**
     * コンストラクタ. テスト対象のパッケージをsuperに渡す必要がある
     */
    public ExtCellReadTest() {
        super(new PersoniumCoreApplication());
    }

    /**
     * ExtCell取得の正常系のテスト.
     */
    @Test
    public final void ExtCell更新の正常系のテスト() {
        try {
            ExtCellUtils.create(token, cellName, extCellUrl, HttpStatus.SC_CREATED);
            ExtCellUtils.get(token, cellName, extCellUrl, HttpStatus.SC_OK);
        } finally {
            ExtCellUtils.delete(token, cellName, extCellUrl, -1);
        }
    }

    /**
     * Urlが数字の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが数字の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.GET, cellName, "123", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlが真偽値の場合400エラーを返却すること.
     */
    @Test
    public final void Urlが真偽値の場合400エラーを返却すること() {
        ExtCellUtils.extCellAccess(HttpMethod.GET, cellName, "false", token, "", HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Urlがhttpの場合正常に動作すること.
     */
    @Test
    public final void Urlがhttpの場合正常に動作すること() {
        String extCellHttpUrl = "http://localhost:9998/testcell2/";
        ExtCellUtils.get(token, "testcell1", extCellHttpUrl, HttpStatus.SC_OK);
    }

    /**
     * returns_404_on_GET_with_matching_localunit_url_key_even_if_http_scheme_entity_exists.
     */
    @Test
    public final void returns_404_on_GET_with_matching_localunit_url_key_even_if_http_scheme_entity_exists() {
        String extCellUnitUrl = "personium-localunit:/testcell1/";
        ExtCellUtils.get(token, "testcell2", extCellUnitUrl, HttpStatus.SC_NOT_FOUND);
    }
}
