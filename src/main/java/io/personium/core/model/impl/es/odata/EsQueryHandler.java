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
package io.personium.core.model.impl.es.odata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.lang.StringEscapeUtils;
import org.joda.time.DateTimeZone;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.expression.AddExpression;
import org.odata4j.expression.AggregateAllFunction;
import org.odata4j.expression.AggregateAnyFunction;
import org.odata4j.expression.AndExpression;
import org.odata4j.expression.BinaryLiteral;
import org.odata4j.expression.BoolParenExpression;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.ByteLiteral;
import org.odata4j.expression.CastExpression;
import org.odata4j.expression.CeilingMethodCallExpression;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.ConcatMethodCallExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DayMethodCallExpression;
import org.odata4j.expression.DecimalLiteral;
import org.odata4j.expression.DivExpression;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.EndsWithMethodCallExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.EqExpression;
import org.odata4j.expression.ExpressionVisitor;
import org.odata4j.expression.FloorMethodCallExpression;
import org.odata4j.expression.GeExpression;
import org.odata4j.expression.GtExpression;
import org.odata4j.expression.GuidLiteral;
import org.odata4j.expression.HourMethodCallExpression;
import org.odata4j.expression.IndexOfMethodCallExpression;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.IsofExpression;
import org.odata4j.expression.LeExpression;
import org.odata4j.expression.LengthMethodCallExpression;
import org.odata4j.expression.LtExpression;
import org.odata4j.expression.MinuteMethodCallExpression;
import org.odata4j.expression.ModExpression;
import org.odata4j.expression.MonthMethodCallExpression;
import org.odata4j.expression.MulExpression;
import org.odata4j.expression.NeExpression;
import org.odata4j.expression.NegateExpression;
import org.odata4j.expression.NotExpression;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.OrExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.expression.ParenExpression;
import org.odata4j.expression.ReplaceMethodCallExpression;
import org.odata4j.expression.RoundMethodCallExpression;
import org.odata4j.expression.SByteLiteral;
import org.odata4j.expression.SecondMethodCallExpression;
import org.odata4j.expression.SingleLiteral;
import org.odata4j.expression.StartsWithMethodCallExpression;
import org.odata4j.expression.StringLiteral;
import org.odata4j.expression.SubExpression;
import org.odata4j.expression.SubstringMethodCallExpression;
import org.odata4j.expression.SubstringOfMethodCallExpression;
import org.odata4j.expression.TimeLiteral;
import org.odata4j.expression.ToLowerMethodCallExpression;
import org.odata4j.expression.ToUpperMethodCallExpression;
import org.odata4j.expression.TrimMethodCallExpression;
import org.odata4j.expression.YearMethodCallExpression;
import org.odata4j.producer.QueryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.impl.es.QueryMapFactory;
import io.personium.core.model.impl.es.doc.OEntityDocHandler;

/**
 * Converts the query including $ filter of OData to JSON based QueryDSL of ES.
 * $ filter is converted to BoolCommonExpression in Oata4J.
 * OData 4J adopts Visitor pattern for evaluation of Expression,
 * This class implements ExpressionVisitor to act as Visitor here.
 * After getting Visit, if you getSource () this object,
 * You can get the JSON that should be passed to the SearchRequest of the ES.
 * For queries not supported by Personium.io, throw an exception.
 */
public class EsQueryHandler implements ExpressionVisitor, ODataQueryHandler {
    private static final int DEFAULT_TOP_VALUE = PersoniumUnitConfig.getTopQueryDefaultSize();
    EdmEntityType entityType;
    Map<String, Object> source;
    Map<String, Object> current;
    Stack<Map<String, Object>> stack = new Stack<Map<String, Object>>();
    Map<String, Object> orderBy;
    /**
     * SORT_ASC Ascending order.
     */
    public static final String SORT_ASC = "asc";
    /**
     * SORT_DESC Descending order.
     */
    public static final String SORT_DESC = "desc";

    /**
     * log.
     */
    static Logger log = LoggerFactory.getLogger(EsQueryHandler.class);

    /**
     * constructor.
     */
    public EsQueryHandler() {
        this.source = new HashMap<String, Object>();
        this.stack.push(this.source);
        this.current = new HashMap<String, Object>();
        this.source.put("filter", this.current);
    }

    /**
     * Constructor 2.
     * Processes $ filter, $ skip, $ top, $ orderby, $ select.
     * $ expand is not supported.
     * @param entityType entity type
     */
    public EsQueryHandler(EdmEntityType entityType) {
        this.source = new HashMap<String, Object>();
        this.entityType = entityType;
    }

    /**
     * Initialization.
     * @param queryInfo QueryInfo of OData 4 j.
     * @param implicitConds Implicit search condition.
     */
    public void initialize(QueryInfo queryInfo, List<Map<String, Object>> implicitConds) {
        List<Map<String, Object>> filters = new ArrayList<Map<String, Object>>();
        if (queryInfo != null) {
            if (queryInfo.filter != null) {
                this.stack.push(this.source);
                this.current = new HashMap<String, Object>();
                filters.add(this.current);
                queryInfo.filter.visit(this);
            }

            if (queryInfo.customOptions != null && !queryInfo.customOptions.isEmpty()) {
                String keywords = queryInfo.customOptions.get("q");
                if (keywords != null && !keywords.equals("")) {
                    //When single-byte space is specified, it is set to AND search
                    for (String keyword : keywords.split(" ")) {
                        if (keyword.isEmpty()) {
                            continue;
                        }
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("query", keyword);
                        map.put("operator", "and");
                        map.put("type", "phrase");
                        Map<String, Object> all = new HashMap<String, Object>();
                        all.put("_all", map);
                        Map<String, Object> match = new HashMap<String, Object>();
                        match.put("match", all);
                        Map<String, Object> query = new HashMap<String, Object>();
                        query.put("query", match);
                        filters.add(query);
                    }
                }
            }

            this.setTop(queryInfo.top);
            this.setSkip(queryInfo.skip);
            this.setOrderBy(queryInfo.orderBy);
            this.setSelect(queryInfo.select);
        }
        Map<String, Object> filter = new HashMap<String, Object>();
        if (!filters.isEmpty()) {
            Map<String, Object> and = new HashMap<String, Object>();
            and.put("filters", filters);
            filter.put("and", and);
        }
        this.source.put("filter", filter);

        //When there is setting of implicit condition
        if (implicitConds != null && implicitConds.size() != 0) {
            Map<String, Object> query = QueryMapFactory.filteredQuery(null, QueryMapFactory.mustQuery(implicitConds));
            this.source.put("query", query);
        }

        //Return _version
        this.source.put("version", true);
    }

    /**
     * @param top $ top value
     */
    public void setTop(Integer top) {
        if (top != null) {
            this.source.put("size", top);
        } else {
            this.source.put("size", DEFAULT_TOP_VALUE);
        }
    }

    /**
     * @param skip The value of $ skip
     */
    public void setSkip(Integer skip) {
        if (skip != null) {
            this.source.put("from", skip);
        }
    }

    /**
     * @param orderBy value of $ orderBy
     */
    public void setOrderBy(List<OrderByExpression> orderBy) {
        if (orderBy != null) {
            List<Map<String, Object>> sort = new ArrayList<Map<String, Object>>();

            for (OrderByExpression order : orderBy) {
                this.orderBy = new HashMap<String, Object>();
                order.visit(this);
                if (!this.orderBy.isEmpty()) {
                    sort.add(this.orderBy);
                }
            }
            this.source.put("sort", sort);
        }
    }

    /**
     * @param selects $ select value
     */
    public void setSelect(List<EntitySimpleProperty> selects) {
        getSelectQuery(this.source, selects);
    }

    /**
     * Assemble a query for ES search from the value of $ select.
     * @param baseSource Map containing input values
     * @param selects $select
     */
    public void getSelectQuery(Map<String, Object> baseSource,
            List<EntitySimpleProperty> selects) {
        if (selects != null && selects.size() > 0) {
            //Assembling the fields query
            List<String> fields = new ArrayList<String>();
            fields.add(OEntityDocHandler.KEY_STATIC_FIELDS + "."
                    + Common.P_ID.getName());
            fields.add(OEntityDocHandler.KEY_PUBLISHED);
            fields.add(OEntityDocHandler.KEY_UPDATED);

            fields.add(OEntityDocHandler.KEY_CELL_ID);
            fields.add(OEntityDocHandler.KEY_BOX_ID);
            fields.add(OEntityDocHandler.KEY_NODE_ID);
            fields.add(OEntityDocHandler.KEY_ENTITY_ID);

            for (EntitySimpleProperty select : selects) {
                if (select == null) {
                    //When the value specified by $ select is not a property name
                    throw PersoniumCoreException.OData.SELECT_PARSE_ERROR;
                }
                String prop = select.getPropertyName();
                if (!Common.P_ID.getName().equals(prop)
                        && !Common.P_PUBLISHED.getName().equals(prop)
                        && !Common.P_UPDATED.getName().equals(prop)
                        && !"__metadata".equals(prop)) {
                    String fieldName = getFieldName(prop);
                    fields.add(fieldName);
                }
            }

            //Select field designation method differs between Es0.19 and Es1.X systems
            //The difference of this part corresponds to Helper
            EsQueryHandlerHelper.composeSourceFilter(baseSource, fields);
        }
    }

    /**
     * Get the field name.
     * @param prop Property name
     * @return field name
     */
    protected String getFieldName(String prop) {
        String fieldName = OEntityDocHandler.KEY_STATIC_FIELDS + "." + prop;
        return fieldName;
    }

    /**
     * Get search query.
     * @return search query.
     */
    public Map<String, Object> getSource() {
        log.debug(this.source.toString());
        return this.source;
    }

    /**
     * Common processing before left side processing.
     */
    @Override
    public void beforeDescend() {
    }

    /**
     * Common processing after processing on left side and right side.
     */
    @Override
    public void afterDescend() {
    }

    /**
     * Common processing before left side processing, right side processing after processing.
     */
    @Override
    public void betweenDescend() {
    }

    @Override
    public void visit(String type) {
        log.debug("visit(String type)");
    }

    @Override
    public void visit(OrderByExpression expr) {
        log.debug("visit(OrderByExpression expr)");
        if (!(expr.getExpression() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }

        //Set up sort queries
        String key = getSearchKey(expr.getExpression(), true);

        Map<String, Object> sortOption = new HashMap<String, Object>();
        sortOption.put("order", getOrderOption(expr.getDirection()));
        sortOption.put("ignore_unmapped", true);
        this.orderBy.put(key, sortOption);
    }

    /**
     * Get $ orderby option.
     * @param option odata 4 j options
     * @return optionValue Obtained options
     */
    public String getOrderOption(Direction option) {
        String optionValue;
        //The default value is ascending (ASCENDING)
        if (option == null || option.equals(Direction.ASCENDING)) {
            optionValue = SORT_ASC;
        } else {
            optionValue = SORT_DESC;
        }
        return optionValue;
    }

    @Override
    public void visit(Direction direction) {
        log.debug("visit(Direction direction)");
    }

    @Override
    public void visit(AddExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(AndExpression expr) {
        log.debug("visit(AndExpression expr)");
        List<Object> andList = new ArrayList<Object>();
        Map<String, Object> lhs = new HashMap<String, Object>();
        Map<String, Object> rhs = new HashMap<String, Object>();
        andList.add(lhs);
        andList.add(rhs);
        this.current.put("and", andList);
        this.current = lhs;
        stack.push(rhs);
    }

    @Override
    public void visit(OrExpression expr) {
        log.debug("visit(OrExpression expr)");
        List<Object> orList = new ArrayList<Object>();
        Map<String, Object> lhs = new HashMap<String, Object>();
        Map<String, Object> rhs = new HashMap<String, Object>();
        orList.add(lhs);
        orList.add(rhs);
        this.current.put("or", orList);
        this.current = lhs;
        stack.push(rhs);
    }

    /**
     * Visit at exact match search.
     * @param expr EqExpression
     */
    @Override
    public void visit(EqExpression expr) {
        log.debug("visit(EqExpression expr)");

        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterEqCondition(edmProperty, searchValue);

        //Set up search queries
        //If the search target is null, create {"missing": {"field": "xxx"}}
        if (expr.getRHS() instanceof NullLiteral) {
            Map<String, Object> missing = new HashMap<String, Object>();
            missing.put("field", getSearchKey(expr.getLHS(), true));
            this.current.put("missing", missing);
            this.current = stack.pop();
        } else {
            //If the search target is not null, create a term query
            Map<String, Object> term = new HashMap<String, Object>();
            term.put(getSearchKey(expr.getLHS(), true), getSearchValue(expr.getRHS()));
            this.current.put("term", term);
            this.current = stack.pop();
        }
    }

    /**
     * Gets the schema definition of the key specified in the search condition.
     * An error occurs in the following cases
     * <ul>
     * <li> When __ metadata is specified </ li>
     * <li> When an undefined Property is specified </ li>
     * <li> If a name that does not follow Proprety's naming convention is specified <br />
     * (Since the above can not be registered as Property, it is regarded as a format error because the schema definition can not be acquired.) </ Li>
     * </ul>
     * @param searchKey
     * @return EdmProperty
     */
    private EdmProperty getEdmProprety(EntitySimpleProperty searchKey) {
        String propertyName = searchKey.getPropertyName();
        EdmProperty edmProperty = this.entityType.findProperty(propertyName);
        if (null == edmProperty) {
            throw PersoniumCoreException.OData.UNKNOWN_QUERY_KEY.params(propertyName);
        }
        return edmProperty;
    }

    /**
     * Return search string of elasticsearch.
     * @param expr CommonExpression
     * @return search string of elasticsearch
     */
    private Object getSearchValue(CommonExpression expr) {
        if (expr instanceof IntegralLiteral) {
            return ((IntegralLiteral) expr).getValue();
        } else if (expr instanceof Int64Literal) {
            return ((Int64Literal) expr).getValue();
        } else if (expr instanceof DoubleLiteral) {
            return ((DoubleLiteral) expr).getValue();
        } else if (expr instanceof BooleanLiteral) {
            return ((BooleanLiteral) expr).getValue();
        } else if (expr instanceof DateTimeLiteral) {
            return ((DateTimeLiteral) expr).getValue().toDateTime(DateTimeZone.UTC).getMillis();
        } else if (expr instanceof DateTimeOffsetLiteral) {
            return ((DateTimeOffsetLiteral) expr).getValue().getMillis();
        } else {
            String value;
            try {
                value = StringEscapeUtils.unescapeJavaScript(((StringLiteral) expr).getValue());
            } catch (Exception e) {
                log.info("Failed to unescape searchValue.", e);
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_UNABLE_TO_UNESCAPE.params(((StringLiteral) expr)
                        .getValue());
            }
            return value;
        }
    }

    /**
     * Return the search key of elasticsearch.
     * @param expr CommonExpression
     * Search key for @return elasticsearch
     */
    private String getSearchKey(CommonExpression expr) {
        return getSearchKey(expr, false);
    }

    /**
     * Return the search key of elasticsearch.
     * @param expr CommonExpression
     * @param isUntouched isUntouched
     * @return Search key for elasticsearch
     */
    protected String getSearchKey(CommonExpression expr, Boolean isUntouched) {
        //Set as search key
        String keyName = ((EntitySimpleProperty) expr).getPropertyName();

        // published, updated
        if (Common.P_PUBLISHED.getName().equals(keyName)) {
            return OEntityDocHandler.KEY_PUBLISHED;
        } else if (Common.P_UPDATED.getName().equals(keyName)) {
            return OEntityDocHandler.KEY_UPDATED;
        }

        //If it is a schema definition item, it is s field, if it is an undefined item d field is searched
        String fieldPrefix = OEntityDocHandler.KEY_STATIC_FIELDS + ".";

        //If untouched field is specified, untouched is returned
        if (isUntouched) {
            return fieldPrefix + keyName + ".untouched";
        } else {
            return fieldPrefix + keyName;
        }
    }

    @Override
    public void visit(BooleanLiteral expr) {
    }

    @Override
    public void visit(CastExpression expr) {
    }

    @Override
    public void visit(ConcatMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(DateTimeLiteral expr) {
    }

    @Override
    public void visit(DateTimeOffsetLiteral expr) {
    }

    @Override
    public void visit(DecimalLiteral expr) {
    }

    @Override
    public void visit(DivExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(EndsWithMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    /**
     * Visit of EntitySimpleProperty.
     * @param expr EntitySimpleProperty
     */
    @Override
    public void visit(EntitySimpleProperty expr) {
    }

    @Override
    public void visit(GeExpression expr) {
        log.debug("visit(GeExpression expr)");

        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterOpCondition(edmProperty, searchValue);

        //Set the ES Range filter
        Map<String, Object> ge = new HashMap<String, Object>();
        Map<String, Object> property = new HashMap<String, Object>();
        ge.put("gte", getSearchValue(expr.getRHS()));
        property.put(getSearchKey(expr.getLHS(), true), ge);
        this.current.put("range", property);
        this.current = stack.pop();
    }

    @Override
    public void visit(GtExpression expr) {
        log.debug("visit(GtExpression expr)");

        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterOpCondition(edmProperty, searchValue);

        Map<String, Object> gt = new HashMap<String, Object>();
        Map<String, Object> property = new HashMap<String, Object>();
        gt.put("gt", getSearchValue(expr.getRHS()));
        property.put(getSearchKey(expr.getLHS(), true), gt);
        this.current.put("range", property);
        this.current = stack.pop();
    }

    @Override
    public void visit(GuidLiteral expr) {
    }

    @Override
    public void visit(BinaryLiteral expr) {
    }

    @Override
    public void visit(ByteLiteral expr) {
    }

    @Override
    public void visit(SByteLiteral expr) {
    }

    @Override
    public void visit(IndexOfMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(SingleLiteral expr) {
        log.debug("visit(SingleLiteral expr)");
    }

    @Override
    public void visit(DoubleLiteral expr) {
    }

    @Override
    public void visit(IntegralLiteral expr) {
    }

    @Override
    public void visit(Int64Literal expr) {
    }

    @Override
    public void visit(IsofExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(LeExpression expr) {
        log.debug("visit(LeExpression expr)");

        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterOpCondition(edmProperty, searchValue);

        //Set the ES Range filter
        Map<String, Object> le = new HashMap<String, Object>();
        Map<String, Object> property = new HashMap<String, Object>();
        le.put("lte", getSearchValue(expr.getRHS()));
        property.put(getSearchKey(expr.getLHS(), true), le);
        this.current.put("range", property);
        this.current = stack.pop();
    }

    @Override
    public void visit(LengthMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(LtExpression expr) {
        log.debug("visit(LtExpression expr)");

        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterOpCondition(edmProperty, searchValue);

        //Set the ES Range filter
        Map<String, Object> lt = new HashMap<String, Object>();
        Map<String, Object> property = new HashMap<String, Object>();
        lt.put("lt", getSearchValue(expr.getRHS()));
        property.put(getSearchKey(expr.getLHS(), true), lt);
        this.current.put("range", property);
        this.current = stack.pop();
    }

    @Override
    public void visit(ModExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(MulExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(NeExpression expr) {
        //If the property of the search condition specified in $ filter is not a simple type, it is a parse error.
        if (!(expr.getLHS() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getLHS());

        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        CommonExpression searchValue = expr.getRHS();
        FilterConditionValidator.validateFilterEqCondition(edmProperty, searchValue);

        //Set search queries (not filter)
        //If the search target is null, create {"missing": {"field": "xxx"}}
        if (expr.getRHS() instanceof NullLiteral) {
            Map<String, Object> field = new HashMap<String, Object>();
            Map<String, Object> missing = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            field.put("field", getSearchKey(expr.getLHS(), true));
            missing.put("missing", field);
            filter.put("filter", missing);
            this.current.put("not", filter);
            this.current = stack.pop();
        } else {
            //If the search target is not null, create a term query
            Map<String, Object> field = new HashMap<String, Object>();
            Map<String, Object> term = new HashMap<String, Object>();
            Map<String, Object> filter = new HashMap<String, Object>();
            field.put(getSearchKey(expr.getLHS(), true), getSearchValue(expr.getRHS()));
            term.put("term", field);
            filter.put("filter", term);
            this.current.put("not", filter);
            this.current = stack.pop();
        }

    }

    @Override
    public void visit(NegateExpression expr) {
    }

    @Override
    public void visit(NotExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(NullLiteral expr) {
    }

    @Override
    public void visit(ParenExpression expr) {
        log.debug("visit(ParenExpression expr)");
    }

    @Override
    public void visit(BoolParenExpression expr) {
    }

    @Override
    public void visit(ReplaceMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(StartsWithMethodCallExpression expr) {
        log.debug("visit(StartsWithMethodCallExpression expr)");

        //If the left side is a property and the right side is not a character string, it is assumed to be a parse error
        if (!(expr.getTarget() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getTarget());
        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        FilterConditionValidator.validateFilterFuncCondition(edmProperty, expr.getValue());

        //Set up search queries
        Map<String, Object> prefix = new HashMap<String, Object>();

        prefix.put(getSearchKey(expr.getTarget(), true), getSearchValue(expr.getValue()));

        this.current.put("prefix", prefix);
        this.current = stack.pop();
    }

    /**
     * String visit.
     * @param expr StringLiteral
     */
    @Override
    public void visit(StringLiteral expr) {
    }

    @Override
    public void visit(SubExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR;
    }

    @Override
    public void visit(SubstringMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(SubstringOfMethodCallExpression expr) {
        log.debug("visit(SubstringOfMethodCallExpression expr)");

        //If the left-hand side is a character string and the right-hand side is not a property, it will be a parse error
        if (!(expr.getTarget() instanceof EntitySimpleProperty)) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }
        EdmProperty edmProperty = getEdmProprety((EntitySimpleProperty) expr.getTarget());
        //Validate the type of property specified in $ filter and the data type specified as the value of the search condition
        FilterConditionValidator.validateFilterFuncCondition(edmProperty, expr.getValue());

        //Set up search queries
        Map<String, Object> searchKey = new HashMap<String, Object>();
        Map<String, Object> query = new HashMap<String, Object>();
        Map<String, Object> text = new HashMap<String, Object>();

        searchKey.put("query", getSearchValue(expr.getValue()));
        searchKey.put("type", "phrase");
        text.put(getSearchKey(expr.getTarget()), searchKey);
        query.put("match", text);
        this.current.put("query", query);
        this.current = stack.pop();

    }

    @Override
    public void visit(TimeLiteral expr) {
    }

    @Override
    public void visit(ToLowerMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(ToUpperMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(TrimMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(YearMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(MonthMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(DayMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(HourMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(MinuteMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(SecondMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(RoundMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(FloorMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(CeilingMethodCallExpression expr) {
        throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION;
    }

    @Override
    public void visit(AggregateAnyFunction expr) {
    }

    @Override
    public void visit(AggregateAllFunction expr) {
    }
}
