/**
 * personium.io
 * Modifications copyright 2014 FUJITSU LIMITED
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
 * --------------------------------------------------
 * This code is based on OptionsQueryParser.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 * --------------------------------------------------
 * The copyright and the license text of the original code is as follows:
 */
/****************************************************************************
 * Copyright (c) 2010 odata4j
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
package io.personium.core.odata;

import java.util.List;

import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.MethodCallExpression;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.producer.resources.OptionsQueryParser;

import io.personium.core.PersoniumCoreException;

/**
 *PersoniumOptionsQueryParser class.
 *Copy from OData4j library source
 */
public final class PersoniumOptionsQueryParser extends OptionsQueryParser {

    /**
     *Parsing the Orderby query.
     *@ param orderBy value of the orderby query
     *@return Perth result
     */
    public static List<OrderByExpression> parseOrderBy(String orderBy) {
        if (orderBy == null) {
            return null;
        }
        return PersoniumExpressionParser.parseOrderBy(orderBy);
    }

    /**
     *Parse the Filter query.
     *@ param filter filter query value
     *@return Perth result
     */
    public static BoolCommonExpression parseFilter(String filter) {
        if (filter == null) {
            return null;
        }
        CommonExpression ce = PersoniumExpressionParser.parse(filter);
        if (ce instanceof BoolCommonExpression) {
            return (BoolCommonExpression) ce;
        }
        if (ce instanceof MethodCallExpression) {
            throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_FUNCTION.params("");
        } else {
            throw PersoniumCoreException.OData.UNSUPPORTED_QUERY_OPERATOR.params("");
        }
    }

    /**
     *Parses the Select query.
     *@ param select Select query value
     *@return Perth result
     */
    public static List<EntitySimpleProperty> parseSelect(String select) {
        if (select == null) {
            return null;
        }
        return PersoniumExpressionParser.parseExpand(select);
    }

    /**
     *Parsing Expand's query.
     *@ param expand expand Query value
     *@return Perth result
     */
    public static List<EntitySimpleProperty> parseExpand(String expand) {
        if (expand == null) {
            return null;
        }
        return PersoniumExpressionParser.parseExpandQuery(expand);
    }
}
