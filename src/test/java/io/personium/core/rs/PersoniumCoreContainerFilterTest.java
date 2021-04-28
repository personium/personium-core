/**
 * Personium
 * Copyright 2014-2021 Personium Project Authors
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
package io.personium.core.rs;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.net.URI;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import io.personium.common.utils.CommonUtils;
import io.personium.core.jersey.filter.PersoniumCoreContainerFilter;
import io.personium.test.categories.Unit;


/**
 * PersoniumCoreContainerFilterユニットテストクラス.
 */
@Category({ Unit.class })
public class PersoniumCoreContainerFilterTest {
    /**
     * リクエストフィルタとしてメソッド/ヘッダオーバライドを実施していることを確認.
     * X-FORWARDED-PROTO、X-FORWARDED-HOSTヘッダでリクエストUri, Base UriのPROTO, HOST部が書き換わることを確認。
     * @throws Exception Exception
     */
    @Test
    public void testFilterContainerRequest() throws Exception {
        // 被テストオブジェクトを準備
        PersoniumCoreContainerFilter containerFilter = new PersoniumCoreContainerFilter();
        // ContainerRequiestを準備
        PropertiesDelegate mockPD = mock(PropertiesDelegate.class);
        doNothing().when(mockPD).setProperty(anyString(), anyObject());
        ContainerRequest request = new ContainerRequest(
                new URI("http://dc1.example.com/hoge"),
                new URI("http://dc1.example.com/hoge/hoho"),
                HttpMethod.POST,
                null,
                mockPD);
        MultivaluedMap<String, String> headers = request.getHeaders();
        // メソッドオーバーライド
        headers.add(CommonUtils.HttpHeaders.X_HTTP_METHOD_OVERRIDE, HttpMethod.OPTIONS);
        // ヘッダオーバーライド
        String authzValue = "Bearer tokenstring";
        String acceptValue = "text/html";
        String contentTypeValue = "application/xml";
        headers.add(CommonUtils.HttpHeaders.X_OVERRIDE, HttpHeaders.AUTHORIZATION + ": " + authzValue);
        headers.add(HttpHeaders.ACCEPT, contentTypeValue);
        headers.add(CommonUtils.HttpHeaders.X_OVERRIDE, HttpHeaders.ACCEPT + ": " + acceptValue);
        headers.add(HttpHeaders.CONTENT_TYPE, contentTypeValue);
        // X-FORWARDED-* 系のヘッダ設定
        String scheme = "https";
        String host = "example.org";
        headers.add(CommonUtils.HttpHeaders.X_FORWARDED_PROTO, scheme);
        headers.add(CommonUtils.HttpHeaders.X_FORWARDED_HOST, host);

        // 被テスト処理の実行
        containerFilter.filter(request);

        // 結果の検証。
        Assert.assertEquals(HttpMethod.OPTIONS, request.getMethod());
        Assert.assertEquals(authzValue, request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0));
        Assert.assertEquals(acceptValue, request.getHeaders().get(HttpHeaders.ACCEPT).get(0));

        Assert.assertEquals(contentTypeValue, request.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        Assert.assertEquals(scheme, request.getRequestUri().getScheme());
        Assert.assertEquals(host, request.getRequestUri().getHost());
    }

//    /**
//     * filter(ContainerRequest req, ContainerResponse res)： ContainerResponseのテスト.
//     * レスポンスフィルタとしてログ出力をしていることを確認。
//     */
//    @Ignore
//    @Test
//    public void testFilterContainerRequestContainerResponse() {
//        // 被テストオブジェクトを準備
//        PersoniumCoreContainerFilter containerFilter = new PersoniumCoreContainerFilter();
//        // ContainerRequiestモックを準備
//        ContainerRequest mockRequest = mock(ContainerRequest.class);
//        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
//        when(mockServletRequest.getAttribute("requestTime")).thenReturn(System.currentTimeMillis());
//        containerFilter.setHttpServletRequest(mockServletRequest);
//
//        // ContainerResponseモックを準備
//        ContainerResponse mockResponse = mock(ContainerResponse.class);
//        when(mockResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
//
//        // 被テスト処理の実行
//        ContainerResponse filteredResponse = containerFilter.filter(mockRequest, mockResponse);
//
//        // 結果の検証。
//        // ログ出力するだけなので、非Nullであることのみ検査.
//        assertNotNull(filteredResponse);
//    }
}
