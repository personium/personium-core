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
package com.fujitsu.dc.test.unit.core.model.impl.es.odata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.core4j.Enumerable;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.ExpressionParser;
import org.odata4j.producer.QueryInfo;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.model.impl.es.odata.EsQueryHandler;
import com.fujitsu.dc.core.odata.DcOptionsQueryParser;
import com.fujitsu.dc.test.categories.Unit;

/**
 * EsQueryHandlerユニットテストクラス.
 */
@Category({Unit.class })
public class EsQueryHandlerTest {
    private static final EdmProperty.Builder ITEM_PROP = EdmProperty.newBuilder("item")
            .setType(EdmSimpleType.STRING);
    private static final EdmProperty.Builder ITEM_KEY_PROP = EdmProperty.newBuilder("itemKey")
            .setType(EdmSimpleType.STRING);
    private static final EdmProperty.Builder ITEM_KEY2_PROP = EdmProperty.newBuilder("itemKey2")
            .setType(EdmSimpleType.STRING);
    private static final EdmProperty.Builder ITEM_KEY3_PROP = EdmProperty.newBuilder("itemKey3")
            .setType(EdmSimpleType.STRING);

    private EdmEntityType entityType = EdmEntityType.newBuilder()
            .setNamespace("namespace")
            .setName("sample")
            .addProperties(Enumerable.create(ITEM_PROP, ITEM_KEY_PROP, ITEM_KEY2_PROP, ITEM_KEY3_PROP).toList())
            .addKeys("ukKey").build();

    /**
     * スキーマ定義上の項目を完全一致クエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void スキーマ定義上の項目を完全一致クエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "item eq 'itemValue'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> termElement = new HashMap<String, Object>();
        Map<String, Object> term = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        termElement.put("s.item.untouched", "itemValue");
        term.put("term", termElement);
        filtersElement.add(term);
        filters.put("filters", filtersElement);
        and.put("and", filters);
        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * スキーマ定義外の項目を完全一致クエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void スキーマ定義外の完全一致クエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> termElement = new HashMap<String, Object>();
        Map<String, Object> term = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        termElement.put("s.itemKey.untouched", "itemValue");
        term.put("term", termElement);
        filtersElement.add(term);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * 完全一致クエリの検索語をシングルクオート無しで指定してDcCoreExceptionが発生すること.
     */
    @Test
    public void 完全一致クエリの検索語をシングルクオート無しで指定してDcCoreExceptionが発生すること() {
        String filterStr = "itemKey eq itemValue";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        try {
            filterExp.visit(esQueryHandler);
            fail("Not Throw Exception.");
        } catch (DcCoreException e) {
            assertEquals(DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.getCode(), e.getCode());
            String message = DcCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params("itemKey").getMessage();
            assertEquals(message, e.getMessage());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * andクエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void andクエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue' and itemKey2 eq 'itemValue2'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters=[{and=[{term={s.itemKey.untouched=itemValue}},
        // {term={s.itemKey2.untouched=itemValue2}}]}]}}, version=true, size=25}>

        Map<String, Object> expected = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        Map<String, Object> leftTermValue = new HashMap<String, Object>();
        Map<String, Object> rightTermValue = new HashMap<String, Object>();
        Map<String, Object> leftTerm = new HashMap<String, Object>();
        Map<String, Object> rightTerm = new HashMap<String, Object>();
        Map<String, Object> innerAnd = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        leftTermValue.put("s.itemKey.untouched", "itemValue");
        rightTermValue.put("s.itemKey2.untouched", "itemValue2");
        leftTerm.put("term", leftTermValue);
        rightTerm.put("term", rightTermValue);
        terms.add(leftTerm);
        terms.add(rightTerm);

        innerAnd.put("and", terms);
        filtersElement.add(innerAnd);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * andクエリを複数指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void andクエリを複数指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue' and itemKey2 eq 'itemValue2' and itemKey3 eq 'itemValue3'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters=[{or=[{term={s.itemKey.untouched=itemValue}},
        // {or=[{term={s.itemKey2.untouched=itemValue2}}, {term={s.itemKey3.untouched=itemValue3}}]}]}]}},
        // version=true, size=25}>

        Map<String, Object> expected = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> terms = new ArrayList<Map<String, Object>>();
        ArrayList<Map<String, Object>> rightAnd = new ArrayList<Map<String, Object>>();
        Map<String, Object> mapRightAnd = new HashMap<String, Object>();
        Map<String, Object> leftTerm = new HashMap<String, Object>();
        Map<String, Object> middleTerm = new HashMap<String, Object>();
        Map<String, Object> rightTerm = new HashMap<String, Object>();
        Map<String, Object> leftTermValue = new HashMap<String, Object>();
        Map<String, Object> middleTermValue = new HashMap<String, Object>();
        Map<String, Object> rightTermValue = new HashMap<String, Object>();
        Map<String, Object> innerAnd = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        leftTermValue.put("s.itemKey.untouched", "itemValue");
        middleTermValue.put("s.itemKey2.untouched", "itemValue2");
        rightTermValue.put("s.itemKey3.untouched", "itemValue3");
        leftTerm.put("term", leftTermValue);
        middleTerm.put("term", middleTermValue);
        rightTerm.put("term", rightTermValue);

        rightAnd.add(middleTerm);
        rightAnd.add(rightTerm);
        mapRightAnd.put("and", rightAnd);

        terms.add(leftTerm);
        terms.add(mapRightAnd);

        innerAnd.put("and", terms);
        filtersElement.add(innerAnd);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * orクエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void orクエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue' or itemKey2 eq 'itemValue2'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters=[{or=[{term={s.itemKey.untouched=itemValue}},
        // {term={s.itemKey2.untouched=itemValue2}}]}]}}, version=true, size=25}>

        Map<String, Object> expected = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> orElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> leftTermValue = new HashMap<String, Object>();
        Map<String, Object> rightTermValue = new HashMap<String, Object>();
        Map<String, Object> leftTerm = new HashMap<String, Object>();
        Map<String, Object> rightTerm = new HashMap<String, Object>();
        Map<String, Object> or = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        leftTermValue.put("s.itemKey.untouched", "itemValue");
        rightTermValue.put("s.itemKey2.untouched", "itemValue2");
        leftTerm.put("term", leftTermValue);
        rightTerm.put("term", rightTermValue);
        orElement.add(leftTerm);
        orElement.add(rightTerm);

        or.put("or", orElement);
        filtersElement.add(or);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * orクエリを複数指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void orクエリを複数指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue' or itemKey2 eq 'itemValue2' or itemKey3 eq 'itemValue3'";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters=[{or=[{term={s.itemKey.untouched=itemValue}},
        // {or=[{term={s.itemKey2.untouched=itemValue2}}, {term={s.itemKey3.untouched=itemValue3}}]}]}]}},
        // version=true, size=25}>

        Map<String, Object> expected = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> orElement = new ArrayList<Map<String, Object>>();
        ArrayList<Map<String, Object>> rightOr = new ArrayList<Map<String, Object>>();
        Map<String, Object> mapRightOr = new HashMap<String, Object>();
        Map<String, Object> leftTerm = new HashMap<String, Object>();
        Map<String, Object> middleTerm = new HashMap<String, Object>();
        Map<String, Object> rightTerm = new HashMap<String, Object>();
        Map<String, Object> leftTermValue = new HashMap<String, Object>();
        Map<String, Object> middleTermValue = new HashMap<String, Object>();
        Map<String, Object> rightTermValue = new HashMap<String, Object>();
        Map<String, Object> or = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        leftTermValue.put("s.itemKey.untouched", "itemValue");
        middleTermValue.put("s.itemKey2.untouched", "itemValue2");
        rightTermValue.put("s.itemKey3.untouched", "itemValue3");
        leftTerm.put("term", leftTermValue);
        middleTerm.put("term", middleTermValue);
        rightTerm.put("term", rightTermValue);

        rightOr.add(middleTerm);
        rightOr.add(rightTerm);
        mapRightOr.put("or", rightOr);

        orElement.add(leftTerm);
        orElement.add(mapRightOr);

        or.put("or", orElement);
        filtersElement.add(or);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * 部分一致クエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void 部分一致クエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "substringof('itemValue', itemKey)";
        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters=[ {query={match={s.itemKey={query=itemValue, type=phrase}}}}]}},
        // version=true, size=25}>
        Map<String, Object> expected = new HashMap<String, Object>();
        Map<String, Object> queryElement = new HashMap<String, Object>();
        Map<String, Object> text = new HashMap<String, Object>();
        Map<String, Object> searchKey = new HashMap<String, Object>();
        Map<String, Object> query = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        searchKey.put("query", "itemValue");
        searchKey.put("type", "phrase");
        text.put("s.itemKey", searchKey);
        queryElement.put("match", text);
        query.put("query", queryElement);
        filtersElement.add(query);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }


    /**
     * 括弧検索クエリを指定してelasitcsearch用のクエリに変換可能であること.
     */
    @Test
    public void 括弧検索クエリを指定してelasitcsearch用のクエリに変換可能であること() {
        String filterStr = "itemKey eq 'itemValue' and (itemKey2 eq 'itemValue2' or itemKey3 eq 'itemValue3')";

        BoolCommonExpression filterExp = DcOptionsQueryParser.parseFilter(filterStr);

        // ESQueryHandlerでVisitする
        QueryInfo queryInfo = new QueryInfo(null, null, null, filterExp, null, null, null, null, null);
        EsQueryHandler esQueryHandler = new EsQueryHandler(entityType);
        esQueryHandler.initialize(queryInfo, null);

        // 期待値
        // <{filter={and={filters= [{and=[{term={s.itemKey.untouched=itemValue}},
        // {or=[{term={s.itemKey2.untouched=itemValue2}}, {term={s.itemKey3.untouched=itemValue3}}]}]}]}},
        // version=true, size=25}>
        Map<String, Object> expected = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> andElement = new ArrayList<Map<String, Object>>();
        ArrayList<Map<String, Object>> rightOr = new ArrayList<Map<String, Object>>();
        Map<String, Object> mapRightOr = new HashMap<String, Object>();
        Map<String, Object> leftTerm = new HashMap<String, Object>();
        Map<String, Object> middleTerm = new HashMap<String, Object>();
        Map<String, Object> rightTerm = new HashMap<String, Object>();
        Map<String, Object> leftTermValue = new HashMap<String, Object>();
        Map<String, Object> middleTermValue = new HashMap<String, Object>();
        Map<String, Object> rightTermValue = new HashMap<String, Object>();
        Map<String, Object> innerAnd = new HashMap<String, Object>();
        List<Map<String, Object>> filtersElement = new ArrayList<Map<String, Object>>();
        Map<String, Object> filters = new HashMap<String, Object>();
        Map<String, Object> and = new HashMap<String, Object>();

        leftTermValue.put("s.itemKey.untouched", "itemValue");
        middleTermValue.put("s.itemKey2.untouched", "itemValue2");
        rightTermValue.put("s.itemKey3.untouched", "itemValue3");
        leftTerm.put("term", leftTermValue);
        middleTerm.put("term", middleTermValue);
        rightTerm.put("term", rightTermValue);

        rightOr.add(middleTerm);
        rightOr.add(rightTerm);
        mapRightOr.put("or", rightOr);

        andElement.add(leftTerm);
        andElement.add(mapRightOr);

        innerAnd.put("and", andElement);
        filtersElement.add(innerAnd);
        filters.put("filters", filtersElement);
        and.put("and", filters);

        expected.put("filter", and);
        expected.put("size", 25);
        expected.put("version", true);

        assertEquals(expected, esQueryHandler.getSource());
    }

    /**
     * filterクエリに未知の演算子を指定した場合にクエリのパースでエラーが発生すること.
     */
    @Test
    public void filterクエリに未知の演算子を指定した場合にクエリのパースでエラーが発生すること() {
        Map<String, CommonExpression> operatorMap = new HashMap<String, CommonExpression>();
        operatorMap.put("not", ExpressionParser.parse("itemKey not 'itemValue'"));
        operatorMap.put("add", ExpressionParser.parse("itemKey add 1"));
        operatorMap.put("sub", ExpressionParser.parse("itemKey sub 1"));
        operatorMap.put("mul", ExpressionParser.parse("itemKey mul 1"));
        operatorMap.put("div", ExpressionParser.parse("itemKey div 1"));

        // OData4jでは、BoolCommonExpression以外を許可しない実装になっているため、
        // 四則演算子はこのレベルでエラーが発生する。
        // EqやGtなどは、BoolCommonExpressionを返却するため、visitメソッドでエラーが発生する。
        EsQueryHandler esQueryHandler = new EsQueryHandler();
        for (Entry<String, CommonExpression> entry : operatorMap.entrySet()) {
            String operator = entry.getKey();
            CommonExpression filterExp = entry.getValue();
            try {
                filterExp.visit(esQueryHandler);
                fail("Not Throw Exception, operator = " + operator);
            } catch (Exception e) {
                String code = DcCoreException.OData.UNSUPPORTED_QUERY_OPERATOR.params(operator).getCode();
                String message = DcCoreException.OData.UNSUPPORTED_QUERY_OPERATOR.params(operator).getMessage();
                assertEquals(code, ((DcCoreException) e).getCode());
                assertEquals(message, ((DcCoreException) e).getMessage());
            }
        }
    }

    /**
     * 四則演算のfilterクエリに未知の演算子を指定した場合にvisitメソッドの呼び出しでエラーが発生すること.
     */
    @Test
    public void 四則演算のfilterクエリに未知の演算子を指定した場合にvisitメソッドの呼び出しでエラーが発生すること() {
        Map<String, String> operatorMap = new HashMap<String, String>();
        operatorMap.put("add", "itemKey add 1");
        operatorMap.put("sub", "itemKey sub 1");
        operatorMap.put("mul", "itemKey mul 1");
        operatorMap.put("div", "itemKey div 1");

        // OData4jでは、BoolCommonExpression以外を許可しない実装になっているため、
        // 四則演算子はこのレベルでエラーが発生する。
        // EqやGtなどは、BoolCommonExpressionを返却するため、visitメソッドでエラーが発生する。
        for (Entry<String, String> entry : operatorMap.entrySet()) {
            String operator = entry.getKey();
            String filterExp = entry.getValue();
            try {
                DcOptionsQueryParser.parseFilter(filterExp);
                fail("Not Throw Exception, operator = " + operator);
            } catch (Exception e) {
                String code = DcCoreException.OData.UNSUPPORTED_QUERY_OPERATOR.getCode();
                String message = DcCoreException.OData.UNSUPPORTED_QUERY_OPERATOR.getMessage();
                assertEquals(code, ((DcCoreException) e).getCode());
                assertEquals(message, ((DcCoreException) e).getMessage());
            }
        }
    }

    /**
     * filterクエリに未知の関数を指定した場合にvisitメソッドの呼び出しでエラーが発生すること.
     */
    @Test
    public void filterクエリに未知の関数を指定した場合にvisitメソッドの呼び出しでエラーが発生すること() {
        Map<String, CommonExpression> operatorMap = new HashMap<String, CommonExpression>();
        operatorMap.put("endswith", ExpressionParser.parse("endswith(itemKey, 'searchValue')"));

        // ESQueryHandlerでVisitする
        EsQueryHandler esQueryHandler = new EsQueryHandler();
        for (Entry<String, CommonExpression> entry : operatorMap.entrySet()) {
            String function = entry.getKey();
            CommonExpression filterExp = entry.getValue();
            try {
                filterExp.visit(esQueryHandler);
                fail("Not Throw Exception, function = " + function);
            } catch (Exception e) {
                String code = DcCoreException.OData.UNSUPPORTED_QUERY_FUNCTION.getCode();
                String message = DcCoreException.OData.UNSUPPORTED_QUERY_FUNCTION.getMessage();
                assertEquals(code, ((DcCoreException) e).getCode());
                assertEquals(message, ((DcCoreException) e).getMessage());
            }
        }
    }

    /**
     * filterクエリに未知の関数を指定した場合にクエリのパースでエラーが発生すること.
     */
    @Test
    public void filterクエリに未知の関数を指定した場合にクエリのパースでエラーが発生すること() {
        Map<String, String> functionMap = new HashMap<String, String>();
        functionMap.put("length", "length('searchValue')");
        functionMap.put("indexof", "indexof(itemKey, 'searchValue')");
        functionMap.put("replace", "replace(itemKey, 'findString', 'replaceString')");
        functionMap.put("substring", "substring(itemKey, 3)");
        functionMap.put("tolower", "tolower('searchValue')");
        functionMap.put("toupper", "toupper('searchValue')");
        functionMap.put("trim", "trim('searchValue')");
        functionMap.put("concat", "concat(itemKey, 'searchValue')");
        functionMap.put("day", "day(1350451322147)");
        functionMap.put("hour", "hour(1350451322147)");
        functionMap.put("minute", "minute(1350451322147)");
        functionMap.put("month", "month(1350451322147)");
        functionMap.put("second", "second(1350451322147)");
        functionMap.put("year", "year(1350451322147)");
        functionMap.put("round", "round(1.23)");
        functionMap.put("floor", "floor(1.23)");
        functionMap.put("ceiling", "ceiling(1.23)");
//        functionMap.put("IsOf", "IsOf('TargetModel.Model')");

        // ESQueryHandlerでVisitする
        for (Entry<String, String> entry : functionMap.entrySet()) {
            String operator = entry.getKey();
            String filterExp = entry.getValue();
            try {
                DcOptionsQueryParser.parseFilter(filterExp);
                fail("Not Throw Exception, function = " + operator);
            } catch (Exception e) {
                String code = DcCoreException.OData.UNSUPPORTED_QUERY_FUNCTION.getCode();
                String message = DcCoreException.OData.UNSUPPORTED_QUERY_FUNCTION.getMessage();
                assertEquals(code, ((DcCoreException) e).getCode());
                assertEquals(message, ((DcCoreException) e).getMessage());
            }
        }
    }
}
