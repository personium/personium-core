/**
 * Personium
 * Copyright 2014 - 2019 Personium Project Authors
 *  - FUJITSU LIMITED
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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.auth.OAuth2Helper;
import io.personium.core.auth.OAuth2Helper.Key;
import io.personium.core.auth.OAuth2Helper.Scheme;
import io.personium.core.auth.ScopeArbitrator;
import io.personium.core.model.Cell;
import io.personium.core.model.CellRsCmp;
import io.personium.core.model.ctl.Account;
import io.personium.core.utils.TestUtils;
import io.personium.test.categories.Unit;
import io.personium.test.utils.UrlUtils;

/**
 * Unit Test class for AuthzEndPointResource.
 */
@Category({ Unit.class })
public class AuthzEndPointResourceTest {
    public final String MOCK_FORM_HTML = "<html>test</html>";

    private Cell mockCell() {
        Cell cell = (Cell) mock(Cell.class);
        when(cell.authenticateAccount((Account)any(), anyString())).thenReturn(true);
        when(cell.getOwnerNormalized()).thenReturn("cellowner");
        when(cell.getUrl()).thenReturn(TestUtils.URL_TEST_CELL);
        when(cell.getUnitUrl()).thenReturn(UrlUtils.getBaseUrl());
        when(cell.getId()).thenReturn(TestUtils.TEST_UUID64);
        when(cell.authenticateAccount(any(), anyString())).thenReturn(true);
        when(cell.getScopeArbitrator(anyString(), anyString())).thenReturn(new ScopeArbitrator(cell, null, OAuth2Helper.GrantType.AUTHORIZATION_CODE));
        return cell;
    }
    private CellRsCmp mockCellRsCmp() {
        HttpResponse res = mock(HttpResponse.class);
        HttpEntity ent = null;
        try {
            ent = new StringEntity(MOCK_FORM_HTML);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        when(res.getEntity()).thenReturn(ent);
        CellRsCmp crc = mock(CellRsCmp.class);
        when(crc.requestGetAuthorizationHtml()).thenReturn(res);
        Cell cell = mockCell();
        when(crc.getCell()).thenReturn(cell, cell);
        return crc;
    }

    @Test
    public void authGet_normal() {
        String clientId = TestUtils.URL_TEST_SCHEMA;
        String redirectUri = clientId + "__/redirect.html";
        String state = "123";
        String scopeStr = "root";
        CellRsCmp crc = mockCellRsCmp();
        AuthzEndPointResource authz = new AuthzEndPointResource(crc.getCell(), crc);
        Response res = authz.authGet(OAuth2Helper.ResponseType.CODE, clientId, redirectUri,
                null, state, scopeStr, "false",
                "false", "3600", null,
                null, TestUtils.X_FORWARDED_FOR);
        assertEquals(200, res.getStatus());
        // luckily res.getEntity() here returns a String;
        assertEquals(MOCK_FORM_HTML, res.getEntity());
    }
    @Test
    public void authPost_() {
        String clientId = TestUtils.URL_TEST_SCHEMA;
        String redirectUri = clientId + "__/redirect.html";
        String state = "123";
        String scopeStr = "root";
        CellRsCmp crc = mockCellRsCmp();
        AuthzEndPointResource authz = new AuthzEndPointResource(crc.getCell(), crc);
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.addFirst(Key.RESPONSE_TYPE, OAuth2Helper.ResponseType.CODE);
        form.addFirst(Key.CLIENT_ID, clientId);
        form.addFirst(Key.STATE, state);
        form.addFirst(Key.REDIRECT_URI, redirectUri);
        form.addFirst(Key.USERNAME, "user");
        form.addFirst(Key.PASSWORD, "psw");
        Response res = authz.authPost(null, form,
                 TestUtils.X_FORWARDED_FOR);
        assertEquals(303, res.getStatus());
        System.out.println(res.getLocation().toString());
    }
    @Test
    public void createForm_normal() {
        String clientId = TestUtils.URL_TEST_SCHEMA;
        CellRsCmp crc = mockCellRsCmp();
        AuthzEndPointResource authz = new AuthzEndPointResource(crc.getCell(), crc);
        String html = authz.createForm(clientId);
    }
    /**
     * 認証に成功している場合チェックがtrueを返すこと.
     */
    @Test
    public void 認証に成功している場合チェックがtrueを返すこと()  {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, UrlUtils.cellRoot("authz") + "#"
                + Key.ACCESS_TOKEN + "=tokenstr&"
                + Key.TOKEN_TYPE + "="
                + Scheme.BEARER
                + "&" + Key.EXPIRES_IN + "=9999&"
                + Key.STATE + "=State");
        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertTrue(authz.isSuccessAuthorization(res));
    }

    /**
     * ステータスコードが200の場合チェックがfalseを返すこと.
     */
    @Test
    public void ステータスコードが200の場合チェックがfalseを返すこと() {
        ResponseBuilder rb = Response.ok().type(MediaType.TEXT_HTML);
        rb.header("Content-Type", "text/html; charset=UTF-8").build();
        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertFalse(authz.isSuccessAuthorization(res));
    }

    /**
     * ステータスコードが204の場合チェックがfalseを返すこと.
     */
    @Test
    public void ステータスコードが204の場合チェックがfalseを返すこと() {
        ResponseBuilder rb = Response.noContent().type(MediaType.TEXT_HTML);
        rb.header("Content-Type", "text/html; charset=UTF-8").build();
        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertFalse(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントにエラー情報が全て存在する場合チェックがfalseを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントにエラー情報が全て存在する場合チェックがfalseを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "#" + OAuth2Helper.Key.ERROR + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
        sbuf.append(URLEncoder.encode("0000000111", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
        sbuf.append(URLEncoder.encode("PR503-SV-0002", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertFalse(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントにerrorが存在しない場合チェックがtrueを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントにerrorが存在しない場合チェックがtrueを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "#");
        sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
        sbuf.append(URLEncoder.encode("0000000111", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
        sbuf.append(URLEncoder.encode("PR503-SV-0002", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertTrue(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントにerror_descriptionが存在しない場合チェックがtrueを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントにerror_descriptionが存在しない場合チェックがtrueを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "#" + OAuth2Helper.Key.ERROR + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
        sbuf.append(URLEncoder.encode("0000000111", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
        sbuf.append(URLEncoder.encode("PR503-SV-0002", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertTrue(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントにstateが存在しない場合チェックがtrueを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントにstateが存在しない場合チェックがtrueを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "#" + OAuth2Helper.Key.ERROR + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
        sbuf.append(URLEncoder.encode("PR503-SV-0002", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertTrue(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントにcodeが存在しない場合チェックがtrueを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントにcodeが存在しない場合チェックがtrueを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "#" + OAuth2Helper.Key.ERROR + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
        sbuf.append(URLEncoder.encode("0000000111", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertTrue(authz.isSuccessAuthorization(res));
    }

    /**
     * LocationヘッダのURLのフラグメントが存在しない場合チェックがfalseを返すこと.
     * @throws UnsupportedEncodingException URLのエラー
     */
    @Test
    public void LocationヘッダのURLのフラグメントが存在しない場合チェックがfalseを返すこと() throws UnsupportedEncodingException {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER).type(MediaType.APPLICATION_JSON_TYPE);
        StringBuilder sbuf = new StringBuilder(UrlUtils.cellRoot("authz") + "?" + OAuth2Helper.Key.ERROR + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.ERROR_DESCRIPTION + "=");
        sbuf.append(URLEncoder.encode("Server Connection Error.", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.STATE + "=");
        sbuf.append(URLEncoder.encode("0000000111", "utf-8"));
        sbuf.append("&" + OAuth2Helper.Key.CODE + "=");
        sbuf.append(URLEncoder.encode("PR503-SV-0002", "utf-8"));
        rb.header(HttpHeaders.LOCATION, sbuf.toString());

        Response res = rb.entity("").build();
        AuthzEndPointResource authz = new AuthzEndPointResource(null, null);
        assertFalse(authz.isSuccessAuthorization(res));
    }
}
