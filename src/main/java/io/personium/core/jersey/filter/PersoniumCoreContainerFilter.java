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
package io.personium.core.jersey.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.common.utils.PersoniumCoreUtils;
import io.personium.common.utils.PersoniumCoreUtils.HttpHeaders;
import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumReadDeleteModeManager;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.lock.CellLockManager;
import io.personium.core.utils.ResourceUtils;

/**
 * 本アプリのリクエスト及びレスポンスに対してかけるフィルター.
 */
@Provider
@PreMatching
public final class PersoniumCoreContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static Logger log = LoggerFactory.getLogger(PersoniumCoreContainerFilter.class);

    /** 1day. */
    private static final int ONE_DAY_SECONDS = 86400;

    // Acceptヘッダーが取り得る値の正規表現
    static Pattern acceptHeaderValueRegex = Pattern.compile("\\A\\p{ASCII}*\\z");

    @Context
    private HttpServletRequest httpServletRequest;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        MultivaluedMap<String, String> headers = requestContext.getHeaders();

        // Info log.
        requestLog(method, requestContext.getUriInfo().getRequestUri().toString());
        // Debug log.
        log.debug("== Reqeust Headers");
        if (headers != null) {
            for (String key : headers.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(key);
                sb.append(":");
                sb.append(headers.getFirst(key));
                log.debug(sb.toString());
            }
        }

        // リクエストの時間をセッションに保存する
        long requestTime = System.currentTimeMillis();
        requestContext.setProperty("requestTime", requestTime);

        overrideMethod(requestContext);
        overrideHeaders(requestContext);
        overrideUri(requestContext);

        checkOptionsMethod(method, headers);
        checkAcceptHeader(headers);
        replaceAcceptHeader(headers);

        // PCSの動作モードがReadDeleteOnlyモードの場合は、参照系リクエストのみ許可する
        // 許可されていない場合は例外を発生させてExceptionMapperにて処理する
        PersoniumReadDeleteModeManager.checkReadDeleteOnlyMode(
                requestContext.getMethod(), requestContext.getUriInfo().getPathSegments());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String cellId = (String) requestContext.getProperty("cellId");
        if (cellId != null) {
            CellLockManager.decrementReferenceCount(cellId);
        }

        // 全てのレスポンスに共通するヘッダを追加する
        addResponseHeaders(requestContext.getHeaders(), responseContext);
        // レスポンスログを出力
        Long requestTime = (Long) requestContext.getProperty("requestTime");
        responseLog(requestTime, responseContext.getStatus());
    }

    private void overrideMethod(ContainerRequestContext requestContext) {
        if (HttpMethod.POST.equalsIgnoreCase(requestContext.getMethod())) {
            String overrideMethod = requestContext.getHeaders().getFirst(
                    PersoniumCoreUtils.HttpHeaders.X_HTTP_METHOD_OVERRIDE);
            if (overrideMethod != null && !overrideMethod.isEmpty()) {
                requestContext.setMethod(overrideMethod);
            }
        }
    }

    private void overrideHeaders(ContainerRequestContext requestContext) {
        List<String> overrideHeaderList = requestContext.getHeaders().get(PersoniumCoreUtils.HttpHeaders.X_OVERRIDE);
        if (overrideHeaderList == null) {
            return;
        }

        for (String overrideHeader : overrideHeaderList) {
            int idx = overrideHeader.indexOf(":");
            // :がなかったり先頭にある場合は不正ヘッダなので無視
            if (idx < 1) {
                continue;
            }
            String overrideKey = overrideHeader.substring(0, idx).trim();
            String overrideValue = overrideHeader.substring(idx + 1).trim();

            List<String> overrideValueList = new ArrayList<String>();
            overrideValueList.add(overrideValue);
            requestContext.getHeaders().merge(overrideKey, overrideValueList,
                    (value, newValue) -> {
                        value.clear();
                        value.addAll(newValue);
                        return value;
                    });
        }
    }

    private void overrideUri(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> headers = requestContext.getHeaders();
        String xForwardedProto = headers.getFirst(PersoniumCoreUtils.HttpHeaders.X_FORWARDED_PROTO);
        String xForwardedHost = headers.getFirst(PersoniumCoreUtils.HttpHeaders.X_FORWARDED_HOST);
        String xForwardedPath = headers.getFirst(PersoniumCoreUtils.HttpHeaders.X_FORWARDED_PATH);

        UriInfo uriInfo = requestContext.getUriInfo();
        UriBuilder baseUriBuilder = uriInfo.getBaseUriBuilder();
        UriBuilder requestUriBuilder = uriInfo.getRequestUriBuilder();

        if (xForwardedProto != null) {
            baseUriBuilder.scheme(xForwardedProto);
            requestUriBuilder.scheme(xForwardedProto);
        }
        if (xForwardedHost != null) {
            baseUriBuilder.host(xForwardedHost);
            requestUriBuilder.host(xForwardedHost);
        }
        if (xForwardedPath != null) {
            baseUriBuilder.replacePath("/");
            // クエリを含んでいる場合は、クエリを削除してリクエストパスに設定する
            if (xForwardedPath.contains("?")) {
                xForwardedPath = xForwardedPath.substring(0, xForwardedPath.indexOf("?"));
            }
            requestUriBuilder.replacePath(xForwardedPath);
        }
        requestContext.setRequestUri(baseUriBuilder.build(), requestUriBuilder.build());
    }

    /**
     * 認証なしOPTIONメソッドのチェック.
     * @param request フィルタ前リクエスト
     */
    private void checkOptionsMethod(String method, MultivaluedMap<String, String> headers) {
        String authValue = headers.getFirst(org.apache.http.HttpHeaders.AUTHORIZATION);
        if (authValue == null && HttpMethod.OPTIONS.equals(method)) {
            Response res = ResourceUtils.responseBuilderForOptions(
                    HttpMethod.GET,
                    HttpMethod.POST,
                    HttpMethod.PUT,
                    HttpMethod.DELETE,
                    HttpMethod.HEAD,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MERGE,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MKCOL,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.MOVE,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPPATCH,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.ACL
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
    private void checkAcceptHeader(MultivaluedMap<String, String> headers) {
        // Jersey1.10では、Acceptヘッダーのキー名と値にUS-ASCII文字以外が含まれる場合に異常終了するため以下を対処
        // －値に含まれる場合は、400エラーとする。
        String acceptValue = headers.getFirst(org.apache.http.HttpHeaders.ACCEPT);
        if (acceptValue != null && !acceptHeaderValueRegex.matcher(acceptValue).matches()) {
            PersoniumCoreException exception = PersoniumCoreException.OData.BAD_REQUEST_HEADER_VALUE.params(
                    org.apache.http.HttpHeaders.ACCEPT, acceptValue);
            throw exception;
        }
    }

    private void replaceAcceptHeader(MultivaluedMap<String, String> headers) {
        // Jersey1.10では、Acceptヘッダーのキー名と値にUS-ASCII文字以外が含まれる場合に異常終了するため以下を対処
        // －キー名に含まれる場合は、その指定を無効（Accept:*/*)とする（Jerseryで組み込み済み）。
        for (String key : headers.keySet()) {
            if (key.contains(org.apache.http.HttpHeaders.ACCEPT)
                    && !acceptHeaderValueRegex.matcher(key).matches()) {
                headers.remove(key);
            }
        }
    }

    /**
     * 全てのレスポンスに共通するレスポンスヘッダーを追加する.
     * Access-Control-Allow-Origin, Access-Control-Allow-Headers<br/>
     * X-Personium-Version<br/>
     * @param requestHeaders
     * @param response
     */
    private void addResponseHeaders(
            MultivaluedMap<String, String> requestHeaders, ContainerResponseContext responseContext) {
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();

        // X-Personium-Version
        responseHeaders.putSingle(HttpHeaders.X_PERSONIUM_VERSION, PersoniumUnitConfig.getCoreVersion());

        // CORS
        if (requestHeaders.getFirst(HttpHeaders.ORIGIN) != null) {
            // Access-Control-Allow-Origin
            responseHeaders.putSingle(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                    requestHeaders.getFirst(HttpHeaders.ORIGIN));
            responseHeaders.putSingle(HttpHeaders.ACCESS_CONTROLE_ALLOW_CREDENTIALS, true);
            responseHeaders.putSingle(HttpHeaders.ACCESS_CONTROL_MAX_AGE, ONE_DAY_SECONDS);

            // Access-Control-Allow-Headers
            String acrh = requestHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
            if (acrh != null) {
                responseHeaders.putSingle(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, acrh);
            }

            // Access-Control-Expose-Headers
            String exposeValue = HttpHeaders.X_PERSONIUM_VERSION;
            if (responseHeaders.containsKey(HttpHeaders.ACCESS_CONTROLE_EXPOSE_HEADERS)) {
                StringBuilder builder = new StringBuilder();
                builder.append(exposeValue).append(",").append(
                        responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROLE_EXPOSE_HEADERS));
                exposeValue = builder.toString();
            }
            responseHeaders.putSingle(HttpHeaders.ACCESS_CONTROLE_EXPOSE_HEADERS, exposeValue);
        } else {
            responseHeaders.remove(HttpHeaders.ACCESS_CONTROLE_EXPOSE_HEADERS);
        }
    }

    /**
     * リクエストログ出力.
     * @param request
     * @param response
     */
    private void requestLog(String method, String requestUri) {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + PersoniumUnitConfig.getCoreVersion() + "] " + "Started. ");
        sb.append(method);
        sb.append(" ");
        sb.append(requestUri);
        sb.append(" ");
        if (httpServletRequest != null) {
            sb.append(httpServletRequest.getRemoteAddr());
        }
        log.info(sb.toString());
    }

    /**
     * レスポンスログ出力.
     * @param response
     */
    private void responseLog(Long requestTime, int responseStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("[" + PersoniumUnitConfig.getCoreVersion() + "] " + "Completed. ");
        sb.append(responseStatus);
        sb.append(" ");

        // レスポンスの時間を記録する
        long responseTime = System.currentTimeMillis();
        // レスポンスとリクエストの時間差を出力する
        sb.append((responseTime - requestTime) + "ms");
        log.info(sb.toString());
    }

}
