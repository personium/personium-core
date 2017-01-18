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
package com.fujitsu.dc.core.jersey.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.utils.DcCoreUtils;
import com.fujitsu.dc.common.utils.DcCoreUtils.HttpHeaders;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.DcReadDeleteModeManager;
import com.fujitsu.dc.core.model.lock.CellLockManager;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 * 本アプリのリクエスト及びレスポンスに対してかけるフィルター.
 */
public final class DcCoreContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static Logger log = LoggerFactory.getLogger(DcCoreContainerFilter.class);

    // Acceptヘッダーが取り得る値の正規表現
    static Pattern acceptHeaderValueRegex = Pattern.compile("\\A\\p{ASCII}*\\z");

    @Context
    private HttpServletRequest httpServletRequest;

    /**
     * @param httpServletRequest HttpServletRequest
     */
    public void setHttpServletRequest(final HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * リクエスト全体に対してかけるフィルター.
     * @param request フィルタ前リクエスト
     * @return フィルタ後リクエスト
     */
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        requestLog(request);

        // リクエストの時間を記録する
        long requestTime = System.currentTimeMillis();
        // リクエストの時間をセッションに保存する
        this.httpServletRequest.setAttribute("requestTime", requestTime);

        methodOverride(request);
        headerOverride(request);
        uriOverride(request);
        responseOptionsMethod(request);

        // リクエストヘッダーの不正値をチェックする
        checkRequestHeader(request);

        // DcCoreConfig.setUnitRootIfNotSet(this.httpServletRequest);

        // PCSの動作モードがReadDeleteOnlyモードの場合は、参照系リクエストのみ許可する
        // 許可されていない場合は例外を発生させてExceptionMapperにて処理する
        DcReadDeleteModeManager.checkReadDeleteOnlyMode(request.getMethod(), request.getPathSegments());

        return request;
    }

    /**
     * レスポンス全体に対してかけるフィルター.
     * @param request リクエスト
     * @param response フィルタ前レスポンス
     * @return フィルタ後レスポンス
     */
    @Override
    public ContainerResponse filter(final ContainerRequest request, final ContainerResponse response) {
        String cellId = (String) httpServletRequest.getAttribute("cellId");
        if (cellId != null) {
            CellLockManager.decrementReferenceCount(cellId);
        }

        // 全てのレスポンスに共通するヘッダを追加する
        addResponseHeaders(request, response);
        // レスポンスログを出力
        responseLog(response);
        return response;
    }

    /**
     * メソッドオーバーライド処理.
     * @param request 加工するリクエスト
     */
    private void methodOverride(final ContainerRequest request) {
        if (request.getMethod().equalsIgnoreCase(HttpMethod.POST)) {
            // メソッドオーバーライド
            String method = request.getRequestHeaders().getFirst(DcCoreUtils.HttpHeaders.X_HTTP_METHOD_OVERRIDE);
            if (method != null && !method.isEmpty()) {
                request.setMethod(method);
            }
        }
    }

    /**
     * ヘッダオーバーライド処理.
     * @param request 加工するリクエスト
     */
    private void headerOverride(final ContainerRequest request) {
        // ヘッダオーバーライド
        List<String> overrideHeaderList = request.getRequestHeaders().get(DcCoreUtils.HttpHeaders.X_OVERRIDE);
        if (overrideHeaderList == null) {
            return;
        }
        InBoundHeaders headers = new InBoundHeaders();
        MultivaluedMap<String, String> originalHeaders = request.getRequestHeaders();
        if (originalHeaders instanceof InBoundHeaders) {
            headers = (InBoundHeaders) originalHeaders;
        }

        InBoundHeaders overrideHeaders = new InBoundHeaders();
        for (String overrideHeader : overrideHeaderList) {
            int idx = overrideHeader.indexOf(":");
            // :がなかったり先頭にある場合は不正ヘッダなので無視
            if (idx < 1) {
                continue;
            }
            String key = overrideHeader.substring(0, idx).trim();
            String value = overrideHeader.substring(idx + 1).trim();

            List<String> vl = overrideHeaders.get(key);
            if (vl == null) {
                vl = new ArrayList<String>();
            }
            vl.add(value);
            overrideHeaders.put(key, vl);
        }
        for (Entry<String, List<String>> entry : overrideHeaders.entrySet()) {
            headers.put(entry.getKey(), entry.getValue());
        }
        request.setHeaders(headers);
    }

    /**
     * Uriのオーバーライド処理.
     * @param request 加工するリクエスト
     */
    private void uriOverride(final ContainerRequest request) {
        String xForwardedProto = request.getHeaderValue(DcCoreUtils.HttpHeaders.X_FORWARDED_PROTO);
        String xForwardedHost = request.getHeaderValue(DcCoreUtils.HttpHeaders.X_FORWARDED_HOST);
        String xForwardedPath = request.getHeaderValue(DcCoreUtils.HttpHeaders.X_FORWARDED_PATH);

        UriBuilder bub = request.getBaseUriBuilder();
        UriBuilder rub = request.getRequestUriBuilder();

        if (xForwardedProto != null) {
            bub.scheme(xForwardedProto);
            rub.scheme(xForwardedProto);
        }
        if (xForwardedHost != null) {
            bub.host(xForwardedHost);
            rub.host(xForwardedHost);
        }
        if (xForwardedPath != null) {
            bub.replacePath("/");
            // クエリを含んでいる場合は、クエリを削除してリクエストパスに設定する
            if (xForwardedPath.contains("?")) {
                xForwardedPath = xForwardedPath.substring(0, xForwardedPath.indexOf("?"));
            }
            rub.replacePath(xForwardedPath);
        }
        request.setUris(bub.build(), rub.build());
    }

    /**
     * 全てのレスポンスに共通するレスポンスヘッダーを追加する.
     * Access-Control-Allow-Origin, Access-Control-Allow-Headers<br/>
     * X-Dc-Version<br/>
     * @param request
     * @param response
     */
    private void addResponseHeaders(final ContainerRequest request, final ContainerResponse response) {
        MultivaluedMap<String, Object> mm = response.getHttpHeaders();
        String acrh = request.getHeaderValue(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (acrh != null) {
            mm.putSingle(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, acrh);
        } else {
            mm.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
        }
        mm.putSingle(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, HttpHeaders.Value.ASTERISK);
        // X-Dc-Version
        mm.putSingle(HttpHeaders.X_DC_VERSION, DcCoreConfig.getCoreVersion());
    }

    /**
     * リクエストログ出力.
     * @param request
     * @param response
     */
    private void requestLog(final ContainerRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + DcCoreConfig.getCoreVersion() + "] " + "Started. ");
        sb.append(request.getMethod());
        sb.append(" ");
        sb.append(request.getRequestUri().toString());
        sb.append(" ");
        sb.append(this.httpServletRequest.getRemoteAddr());
        log.info(sb.toString());
    }

    /**
     * レスポンスログ出力.
     * @param response
     */
    private void responseLog(final ContainerResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + DcCoreConfig.getCoreVersion() + "] " + "Completed. ");
        sb.append(response.getStatus());
        sb.append(" ");

        // レスポンスの時間を記録する
        long responseTime = System.currentTimeMillis();
        // セッションからリクエストの時間を取り出す
        long requestTime = (Long) this.httpServletRequest.getAttribute("requestTime");
        // レスポンスとリクエストの時間差を出力する
        sb.append((responseTime - requestTime) + "ms");
        log.info(sb.toString());
    }

    /**
     * 認証なしOPTIONメソッドのレスポンスを返却する.
     * @param request フィルタ前リクエスト
     */
    private void responseOptionsMethod(ContainerRequest request) {
        String authValue = request.getHeaderValue(org.apache.http.HttpHeaders.AUTHORIZATION);
        String methodName = request.getMethod();
        if (authValue == null && HttpMethod.OPTIONS.equals(methodName)) {
            Response res = DcCoreUtils.responseBuilderForOptions(
                    HttpMethod.GET,
                    HttpMethod.POST,
                    HttpMethod.PUT,
                    HttpMethod.DELETE,
                    HttpMethod.HEAD,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MERGE,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MKCOL,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.MOVE,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPFIND,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.PROPPATCH,
                    com.fujitsu.dc.common.utils.DcCoreUtils.HttpMethod.ACL
                    ).build();

            // 例外を発行することでServletへ制御を渡さない
            throw new WebApplicationException(res);
        }
    }

    /**
     * リクエストヘッダーの値をチェックする.
     * 現在は、Acceptヘッダーのみ(US-ASCII文字以外かどうか)をチェックする
     * @param request フィルター前リクエスト
     */
    private void checkRequestHeader(ContainerRequest request) {
        // ヘッダーのキー名に全角文字が含まれる場合は、その文字を含めたキー名となるため、実際にはこの指定は無視される。
        // Jersey1.10では、Acceptヘッダーのキー名と値にUS-ASCII文字以外が含まれる場合に異常終了するため以下を対処
        // (Acceptを含む他のヘッダーにも同様の処理が行われるが、上記理由により動作上は問題ないと判断）
        // －キー名に含まれる場合は、その指定を無効（Accept:*/*)とする（Jerseryで組み込み済み）。
        // －値に含まれる場合は、400エラーとする。
        InBoundHeaders newHeaders = new InBoundHeaders();
        MultivaluedMap<String, String> headers = request.getRequestHeaders();
        for (String header : headers.keySet()) {
            if (header.contains(org.apache.http.HttpHeaders.ACCEPT)
                    && !acceptHeaderValueRegex.matcher(header).matches()) {
                continue;
            } else {
                newHeaders.put(header, request.getRequestHeader(header));
            }
        }
        request.setHeaders(newHeaders);
        String acceptValue = request.getHeaderValue(org.apache.http.HttpHeaders.ACCEPT);
        if (acceptValue != null && !acceptHeaderValueRegex.matcher(acceptValue).matches()) {
            DcCoreException exception = DcCoreException.OData.BAD_REQUEST_HEADER_VALUE.params(
                    org.apache.http.HttpHeaders.ACCEPT, acceptValue);
            throw exception;
        }
    }
}
