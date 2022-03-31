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
package io.personium.core.rs.cell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.core.auth.OAuth2Helper;
import io.personium.test.categories.Unit;
import io.personium.test.utils.UrlUtils;

/**
 * Unit Test class for AuthzEndPointResource.
 */
@Category({ Unit.class })
public class AuthzEndPointResourceTest {

    /**
     * 認証に成功している場合チェックがtrueを返すこと.
     */
    @Test
    public void 認証に成功している場合チェックがtrueを返すこと() {
        ResponseBuilder rb = Response.status(Status.SEE_OTHER)
                .type(MediaType.APPLICATION_JSON_TYPE);
        rb.header(HttpHeaders.LOCATION, UrlUtils.cellRoot("authz") + "#"
                + OAuth2Helper.Key.ACCESS_TOKEN + "=tokenstr&"
                + OAuth2Helper.Key.TOKEN_TYPE + "="
                + OAuth2Helper.Scheme.BEARER
                + "&" + OAuth2Helper.Key.EXPIRES_IN + "=9999&"
                + OAuth2Helper.Key.STATE + "=State");
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
