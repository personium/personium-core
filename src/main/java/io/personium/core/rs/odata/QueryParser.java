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
package io.personium.core.rs.odata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.producer.InlineCount;
import org.odata4j.producer.QueryInfo;
import org.odata4j.producer.resources.OptionsQueryParser;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.odata.PersoniumOptionsQueryParser;
import com.sun.jersey.api.uri.UriComponent;

/**
 * クエリをパースして値を返却するクラス.
 */
public class QueryParser {

    private QueryParser() {
    }

    /**
     * skipクエリをパースして値を返却する.
     * @param query クエリ文字列("$skip=VALUE")
     * @return クエリで指定された値
     */
    public static Integer parseSkipQuery(String query) {
        Integer skip = null;
        try {
            skip = OptionsQueryParser.parseTop(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$skip").reason(e);
        }
        if (skip != null && (0 > skip || skip > PersoniumUnitConfig.getSkipQueryMaxSize())) {
            // Integerでそのまま値を返却すると、カンマが付くため、文字列でエラーメッセージを返却する
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$skip", skip.toString());
        }
        return skip;
    }

    /**
     * topクエリをパースして値を返却する.
     * @param query クエリ文字列("$top=VALUE")
     * @return クエリで指定された値
     */
    public static Integer parseTopQuery(String query) {
        Integer top = null;
        try {
            top = OptionsQueryParser.parseTop(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.QUERY_PARSE_ERROR_WITH_PARAM.params("$top").reason(e);
        }
        if (top != null && (0 > top || top > PersoniumUnitConfig.getTopQueryMaxSize())) {
            // Integerでそのまま値を返却すると、カンマが付くため、文字列でエラーメッセージを返却する
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", top.toString());
        }
        return top;
    }

    /**
     * orderbyクエリをパースして値を返却する.
     * @param query クエリ文字列("$orderby=VALUE")
     * @return クエリで指定された値
     */
    public static List<OrderByExpression> parseOderByQuery(String query) {
        List<OrderByExpression> orderBy = null;
        try {
            if (query != null && query.equals("")) {
                throw PersoniumCoreException.OData.ORDERBY_PARSE_ERROR;
            }
            orderBy = PersoniumOptionsQueryParser.parseOrderBy(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.ORDERBY_PARSE_ERROR;
        }
        return orderBy;
    }

    /**
     * skiptokenクエリをパースして値を返却する.
     * @param query クエリ文字列("$skiptoken=VALUE")
     * @return クエリで指定された値
     */
    public static String parseSkipTokenQuery(String query) {
        String skipToken = null;
        try {
            skipToken = OptionsQueryParser.parseSkipToken(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        return skipToken;
    }

    /**
     * inlinecountクエリをパースして値を返却する.
     * @param query クエリ文字列("$inlinecount=VALUE")
     * @return クエリで指定された値
     */
    public static InlineCount parseInlinecountQuery(String query) {
        InlineCount inlineCount = null;
        if (query == null) {
            // デフォルト値の設定（__countなし）
            inlineCount = InlineCount.NONE;
        } else {
            // パースをして有効値以外が返却された場合はパースエラーとする
            inlineCount = OptionsQueryParser.parseInlineCount(query);
            if (inlineCount == null) {
                throw PersoniumCoreException.OData.INLINECOUNT_PARSE_ERROR.params(query);
            }
        }
        return inlineCount;
    }

    /**
     * expandクエリをパースして値を返却する.
     * @param query クエリ文字列("$expand=VALUE")
     * @return クエリで指定された値
     */
    public static List<EntitySimpleProperty> parseExpandQuery(String query) {
        List<EntitySimpleProperty> expand = null;
        try {
            expand = PersoniumOptionsQueryParser.parseExpand(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.EXPAND_PARSE_ERROR;
        }
        // $expandに指定されたプロパティ数の上限チェック
        if (expand != null && expand.size() > PersoniumUnitConfig.getExpandPropertyMaxSizeForList()) {
            throw PersoniumCoreException.OData.EXPAND_COUNT_LIMITATION_EXCEEDED;
        }
        return expand;
    }

    /**
     * selectクエリをパースして値を返却する.
     * @param query クエリ文字列("$select=VALUE")
     * @return クエリで指定された値
     */
    public static List<EntitySimpleProperty> parseSelectQuery(String query) {
        List<EntitySimpleProperty> select = null;
        if ("".equals(query)) {
            throw PersoniumCoreException.OData.SELECT_PARSE_ERROR;
        }
        if (!"*".equals(query)) {
            try {
                select = PersoniumOptionsQueryParser.parseSelect(query);
            } catch (Exception e) {
                throw PersoniumCoreException.OData.SELECT_PARSE_ERROR.reason(e);
            }
        }
        return select;
    }

    /**
     * filterクエリをパースして値を返却する.
     * @param query クエリ文字列("$filter=VALUE")
     * @return クエリで指定された値
     */
    public static BoolCommonExpression parseFilterQuery(String query) {
        BoolCommonExpression filter = null;
        try {
            filter = PersoniumOptionsQueryParser.parseFilter(query);
        } catch (Exception e) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR.reason(e);
        }
        return filter;
    }

    /**
     * qクエリをパースする.
     * @param fullTextSearchKeyword クエリ文字列("q=VALUE")
     */
    public static void parseFullTextSearchQuery(String fullTextSearchKeyword) {
        // 全文検索クエリqのバリデート
        if (fullTextSearchKeyword != null && (fullTextSearchKeyword.getBytes().length < 1
                || fullTextSearchKeyword.getBytes().length > Common.MAX_Q_VALUE_LENGTH)) {
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("q", fullTextSearchKeyword);
        }
    }

    /**
     * QueryInfoを生成.
     * @param requestQuery requestQuery
     * @return requestQuery
     */
    public static QueryInfo createQueryInfo(String requestQuery) {
        MultivaluedMap<String, String> queryParams = UriComponent.decodeQuery(requestQuery, true);
        Integer top = QueryParser.parseTopQuery(queryParams.getFirst("$top"));
        Integer skip = QueryParser.parseSkipQuery(queryParams.getFirst("$skip"));
        BoolCommonExpression filter = QueryParser.parseFilterQuery(queryParams.getFirst("$filter"));
        List<EntitySimpleProperty> select = QueryParser.parseSelectQuery(queryParams.getFirst("$select"));
        List<EntitySimpleProperty> expand = QueryParser.parseExpandQuery(queryParams.getFirst("$expand"));
        InlineCount inlineCount = QueryParser.parseInlinecountQuery(queryParams.getFirst("$inlinecount"));
        String skipToken = QueryParser.parseSkipTokenQuery(queryParams.getFirst("$skiptoken"));
        List<OrderByExpression> orderBy = QueryParser.parseOderByQuery(queryParams.getFirst("$orderby"));
        parseFullTextSearchQuery(queryParams.getFirst("q"));

        // $expand指定時は$topの最大値が変わるためチェックする
        if (expand != null && top != null && top > PersoniumUnitConfig.getTopQueryMaxSizeWithExpand()) {
            // Integerでそのまま値を返却すると、カンマが付くため、文字列でエラーメッセージを返却する
            throw PersoniumCoreException.OData.QUERY_INVALID_ERROR.params("$top", top.toString());
        }

        Map<String, String> customOptions = new HashMap<String, String>();
        customOptions.put("q", queryParams.getFirst("q"));
        QueryInfo queryInfo = new QueryInfo(
                inlineCount,
                top,
                skip,
                filter,
                orderBy,
                skipToken,
                customOptions,
                expand,
                select);
        return queryInfo;
    }
}
