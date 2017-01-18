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
package com.fujitsu.dc.test.unit.core.jersey.filter;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.core.jersey.filter.DcCoreContainerFilter;
import com.fujitsu.dc.test.categories.Unit;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.WebApplication;


/**
 * DcCoreContainerFilterユニットテストクラス.
 */
@Category({ Unit.class })
public class DcCoreContainerFilterTest {
    /**
     * リクエストフィルタとしてメソッド/ヘッダオーバライドを実施していることを確認.
     * X-FORWARDED-PROTO、X-FORWARDED-HOSTヘッダでリクエストUri, Base UriのPROTO, HOST部が書き換わることを確認。
     * @throws URISyntaxException URISyntaxException
     */
    @Test
    public void testFilterContainerRequest() throws URISyntaxException {
        // 被テストオブジェクトを準備
        DcCoreContainerFilter containerFilter = new DcCoreContainerFilter();
        // ContainerRequiestを準備
        WebApplication wa = mock(WebApplication.class);
        InBoundHeaders headers = new InBoundHeaders();
        // メソッドオーバーライド
        headers.add(DcCoreUtils.HttpHeaders.X_HTTP_METHOD_OVERRIDE, HttpMethod.OPTIONS);
        // ヘッダオーバーライド
        String authzValue = "Bearer tokenstring";
        String acceptValue = "text/html";
        String contentTypeValue = "application/xml";
        headers.add(DcCoreUtils.HttpHeaders.X_OVERRIDE, HttpHeaders.AUTHORIZATION + ": " + authzValue);
        headers.add(HttpHeaders.ACCEPT, contentTypeValue);
        headers.add(DcCoreUtils.HttpHeaders.X_OVERRIDE, HttpHeaders.ACCEPT + ": " + acceptValue);
        headers.add(HttpHeaders.CONTENT_TYPE, contentTypeValue);

        // X-FORWARDED-* 系のヘッダ設定
        String scheme = "https";
        String host = "example.org";
        headers.add(DcCoreUtils.HttpHeaders.X_FORWARDED_PROTO, scheme);
        headers.add(DcCoreUtils.HttpHeaders.X_FORWARDED_HOST, host);

        ContainerRequest request = new ContainerRequest(wa, HttpMethod.POST,
                new URI("http://dc1.example.com/hoge"),
                new URI("http://dc1.example.com/hoge/hoho"),
                headers, null);

        // HttpServletRequestのmockを準備
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        when(mockServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://dc1.example.com"));

        ServletContext mockServletContext = mock(ServletContext.class);
        when(mockServletContext.getContextPath()).thenReturn("");
        when(mockServletRequest.getServletContext()).thenReturn(mockServletContext);
        containerFilter.setHttpServletRequest(mockServletRequest);

        // 被テスト処理の実行
        ContainerRequest filteredRequest = containerFilter.filter(request);

        // 結果の検証。
        Assert.assertEquals(HttpMethod.OPTIONS, filteredRequest.getMethod());
        Assert.assertEquals(authzValue, filteredRequest.getHeaderValue(HttpHeaders.AUTHORIZATION));
        Assert.assertEquals(acceptValue, filteredRequest.getHeaderValue(HttpHeaders.ACCEPT));

        Assert.assertEquals(contentTypeValue, filteredRequest.getHeaderValue(HttpHeaders.CONTENT_TYPE));
        Assert.assertEquals(scheme, filteredRequest.getRequestUri().getScheme());
        Assert.assertEquals(host, filteredRequest.getRequestUri().getHost());
    }

    /**
     * filter(ContainerRequest req, ContainerResponse res)： ContainerResponseのテスト.
     * レスポンスフィルタとしてログ出力をしていることを確認。
     */
    @Ignore
    @Test
    public void testFilterContainerRequestContainerResponse() {
        // 被テストオブジェクトを準備
        DcCoreContainerFilter containerFilter = new DcCoreContainerFilter();
        // ContainerRequiestモックを準備
        ContainerRequest mockRequest = mock(ContainerRequest.class);
        HttpServletRequest mockServletRequest = mock(HttpServletRequest.class);
        when(mockServletRequest.getAttribute("requestTime")).thenReturn(System.currentTimeMillis());
        containerFilter.setHttpServletRequest(mockServletRequest);

        // ContainerResponseモックを準備
        ContainerResponse mockResponse = mock(ContainerResponse.class);
        when(mockResponse.getStatus()).thenReturn(HttpStatus.SC_OK);

        // 被テスト処理の実行
        ContainerResponse filteredResponse = containerFilter.filter(mockRequest, mockResponse);

        // 結果の検証。
        // ログ出力するだけなので、非Nullであることのみ検査.
        assertNotNull(filteredResponse);
    }
}
