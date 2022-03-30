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
package io.personium.core.model.impl.es.odata;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTimeZone;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.expression.BooleanLiteral;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.DateTimeLiteral;
import org.odata4j.expression.DateTimeOffsetLiteral;
import org.odata4j.expression.DoubleLiteral;
import org.odata4j.expression.Int64Literal;
import org.odata4j.expression.IntegralLiteral;
import org.odata4j.expression.NullLiteral;
import org.odata4j.expression.StringLiteral;

import io.personium.core.PersoniumCoreException;
import io.personium.core.utils.ODataUtils;


/**
 * $ filter Class that validates the consistency of the data type of the property specified in the search condition of the query and the data type specified as the value of the search condition.
 */
public class FilterConditionValidator {

    private static Map<EdmSimpleType<?>, AbstractValidator> validatorMap;

    /**
     * Initialization of data type verification class.
     */
    static {
        validatorMap = new HashMap<EdmSimpleType<?>, AbstractValidator>();
        validatorMap.put(EdmSimpleType.STRING, new StringValidator());
        validatorMap.put(EdmSimpleType.BOOLEAN, new BooleanValidator());
        validatorMap.put(EdmSimpleType.INT32, new Int32Validator());
        validatorMap.put(EdmSimpleType.SINGLE, new SingleValidator());
        validatorMap.put(EdmSimpleType.DOUBLE, new DoubleValidator());
        validatorMap.put(EdmSimpleType.DATETIME, new DateTimeValidator());
    }

    private FilterConditionValidator() {
    }

    /**
     * $ filter Verify the consistency of the data type of the property specified in the search condition of the query and the data type specified as the value of the search condition.
     * <ul>
     * <li>StringLiteral</li>
     * <li> IntegralLiteral, Int64Literal </ li>
     * <li>DoubleLiteral</li>
     * </ul>
     * The notation such as "1.0 f" or "1.0 m" (SingleLiteral, DecimalLiteral respectively) is a parse error.
     * @param edmProperty Property specified in search condition of $ filter
     * @param searchValue Value of search condition for $ filter
     */
    static void validateFilterOpCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        //Comparison operator (lt / le / ge / gt) Commonly allowable data: string / integer value / real number
        //Boolean values ​​and NULL can not be compared because they can not be compared in magnitude.
        if (searchValue instanceof BooleanLiteral
                || searchValue instanceof NullLiteral) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }

        //Verify that the value of the search condition can be evaluated as the data type of the schema defined property.
        //However, if the schema is not defined, it can not be verified and is excluded.
        if (edmProperty != null) {
            AbstractValidator validator = validatorMap.get(edmProperty.getType());
            if (null == validator) {
                throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
            }
            validator.validate(searchValue, edmProperty.getName());
        }
    }

    /**
     * $ filter Verify the consistency of the data type specified in the search condition in the Eq operator of the query and the data type specified as the value of the search condition.
     * <ul>
     * <li>StringLiteral</li>
     * <li> IntegralLiteral, Int64Literal </ li>
     * <li>DoubleLiteral</li>
     * <li>BooleanLiteral</li>
     * <li>NullLiteral</li>
     * </ul>
     * The notation such as "1.0 f" or "1.0 m" (SingleLiteral, DecimalLiteral respectively) is a parse error.
     * @param edmProperty Property specified in search condition of $ filter
     * @param searchValue Value of search condition for $ filter
     */
    static void validateFilterEqCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        //Verify that the value of the search condition can be evaluated as the data type of the schema defined property.
        //However, if the schema is not defined, it can not be verified and is excluded.
        if (edmProperty != null) {
            AbstractValidator validator = validatorMap.get(edmProperty.getType());
            if (null == validator) {
                throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
            }
            validator.validate(searchValue, edmProperty.getName());
        }
    }

    /**
     * $ filter Verify the consistency of the data type specified for the query function and the data type specified as the value.
     * <ul>
     * <li>StringLiteral</li>
     * </ul>
     * @param edmProperty Property specified for the function of $ filter
     * @param searchValue The value specified for the function of $ filter
     */
    static void validateFilterFuncCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        //Function (substringof / startswith) Commonly allowable data: Character string
        if (!(searchValue instanceof StringLiteral)) {
            throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(edmProperty.getName());
        }

        //Verify that the value of the search condition can be evaluated as the data type of the schema defined property.
        //However, if the schema is not defined, it can not be verified and is excluded.
        if (edmProperty != null
                && !EdmSimpleType.STRING.getFullyQualifiedTypeName().equals(
                        edmProperty.getType().getFullyQualifiedTypeName())) {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(edmProperty.getName());
        }
    }


    /**
     * The type verification class of each data type specified in the search condition and the abstract class to be compiled.
     */
    interface AbstractValidator {
        /**
         * Verify the inconsistency between the data type of the property specified in the search condition and the data of the search condition value.
         * @param searchValue Search condition value
         * @param propertyName Property name to search for
         */
        void validate(CommonExpression searchValue, String propertyName);
    }

    /**
     * The type validation class of Edm.String type specified in the search condition.
     */
    static class StringValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            if (searchValue instanceof StringLiteral
                    || searchValue instanceof NullLiteral) {
                return;
            }
            throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
        }
    }

    /**
     * Type validation class of Edm.Boolean type specified in search condition.
     */
    static class BooleanValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            if (searchValue instanceof BooleanLiteral
                    || searchValue instanceof NullLiteral) {
                return;
            }
            throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
        }
    }

    /**
     * A type validation class of Edm.Int32 type specified in the search condition.
     */
    static class Int32Validator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            //Since Int64Literal # gerValue of odata4j returns a long type value, value is of type long.
            long value = 0L;
            if (searchValue instanceof IntegralLiteral) {
                value = ((IntegralLiteral) searchValue).getValue();
            } else if (searchValue instanceof Int64Literal) {
                value = ((Int64Literal) searchValue).getValue();
            } else if (searchValue instanceof NullLiteral) {
                value = 0;
            } else {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
            }

            //Value range check
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }

    /**
     * The type validation class of Edm.Double type specified in the search condition.
     */
    static class DoubleValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            if (searchValue instanceof IntegralLiteral
                    || searchValue instanceof Int64Literal
                    || searchValue instanceof DoubleLiteral
                    || searchValue instanceof NullLiteral) {
                return;
            }
            throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
        }
    }

    /**
     * Type validation class of Edm.Single type specified in search condition.
     */
    static class SingleValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            //Since DoubleLiteral # gerValue of odata4j returns a double type value, value is of type double.
            double value = 0D;
            if (searchValue instanceof IntegralLiteral) {
                value = ((IntegralLiteral) searchValue).getValue();
            } else if (searchValue instanceof Int64Literal) {
                value = ((Int64Literal) searchValue).getValue();
            } else if (searchValue instanceof DoubleLiteral) {
                value = ((DoubleLiteral) searchValue).getValue();
            } else if (searchValue instanceof NullLiteral) {
                value = 0;
            } else {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
            }

            //Value range check
            if (!ODataUtils.validateSingle(String.valueOf(value))) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }

    /**
     * The type validation class of Edm.DateTime type specified in the search condition.
     */
    static class DateTimeValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            //Since Int64Literal # gerValue of odata4j returns a long type value, value is of type long.
            long value = 0L;
            if (searchValue instanceof IntegralLiteral) {
                value = ((IntegralLiteral) searchValue).getValue();
            } else if (searchValue instanceof Int64Literal) {
                value = ((Int64Literal) searchValue).getValue();
            } else if (searchValue instanceof DateTimeLiteral) {
                value = ((DateTimeLiteral) searchValue).getValue().toDateTime(DateTimeZone.UTC).getMillis();
            } else if (searchValue instanceof DateTimeOffsetLiteral) {
                value = ((DateTimeOffsetLiteral) searchValue).getValue().getMillis();
            } else if (searchValue instanceof NullLiteral) {
                value = 0;
            } else {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
            }

            //Value range check
            if (value > ODataUtils.DATETIME_MAX || value < ODataUtils.DATETIME_MIN) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }
}
