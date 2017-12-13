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
package io.personium.core.model.impl.es.odata;

import java.util.HashMap;
import java.util.Map;

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
 * $filterクエリの検索条件に指定するプロパティのデータ型と検索条件の値として指定されたデータ型の整合性を検証するクラス.
 */
public class FilterConditionValidator {

    private static Map<EdmSimpleType<?>, AbstractValidator> validatorMap;

    /**
     * データ型検証用クラスの初期化.
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
     * $filterクエリの検索条件に指定するプロパティのデータ型と検索条件の値として指定されたデータ型の整合性を検証する.
     * <ul>
     * <li>StringLiteral</li>
     * <li>IntegralLiteral、Int64Literal</li>
     * <li>DoubleLiteral</li>
     * </ul>
     * なお、"1.0f" や "1.0m" などの表記（それぞれSingleLiteral、DecimalLiteral）はパースエラーとする。
     * @param edmProperty $filterの検索条件に指定されたプロパティ
     * @param searchValue $filterの検索条件の値
     */
    static void validateFilterOpCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        // 比較演算子（lt/le/ge/gt）共通で許容するデータ： 文字列／整数値／実数値
        // 真偽値やNULLは大小比較ができないため、許容しない。
        if (searchValue instanceof BooleanLiteral
                || searchValue instanceof NullLiteral) {
            throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
        }

        // スキーマ定義されているプロパティのデータ型として検索条件の値が評価できることを検証する。
        // ただし、スキーマ定義されていない場合は、検証できないので除外する。
        if (edmProperty != null) {
            AbstractValidator validator = validatorMap.get(edmProperty.getType());
            if (null == validator) {
                throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
            }
            validator.validate(searchValue, edmProperty.getName());
        }
    }

    /**
     * $filterクエリのEq演算子における検索条件に指定するプロパティのデータ型と検索条件の値として指定されたデータ型の整合性を検証する.
     * <ul>
     * <li>StringLiteral</li>
     * <li>IntegralLiteral、Int64Literal</li>
     * <li>DoubleLiteral</li>
     * <li>BooleanLiteral</li>
     * <li>NullLiteral</li>
     * </ul>
     * なお、"1.0f" や "1.0m" などの表記（それぞれSingleLiteral、DecimalLiteral）はパースエラーとする。
     * @param edmProperty $filterの検索条件に指定されたプロパティ
     * @param searchValue $filterの検索条件の値
     */
    static void validateFilterEqCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        // スキーマ定義されているプロパティのデータ型として検索条件の値が評価できることを検証する。
        // ただし、スキーマ定義されていない場合は、検証できないので除外する。
        if (edmProperty != null) {
            AbstractValidator validator = validatorMap.get(edmProperty.getType());
            if (null == validator) {
                throw PersoniumCoreException.OData.FILTER_PARSE_ERROR;
            }
            validator.validate(searchValue, edmProperty.getName());
        }
    }

    /**
     * $filterクエリの関数に指定するプロパティのデータ型と値として指定されたデータ型の整合性を検証する.
     * <ul>
     * <li>StringLiteral</li>
     * </ul>
     * @param edmProperty $filterの関数に指定されたプロパティ
     * @param searchValue $filterの関数に指定された値
     */
    static void validateFilterFuncCondition(EdmProperty edmProperty, CommonExpression searchValue) {
        // 関数（substringof/startswith）共通で許容するデータ： 文字列
        if (!(searchValue instanceof StringLiteral)) {
            throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(edmProperty.getName());
        }

        // スキーマ定義されているプロパティのデータ型として検索条件の値が評価できることを検証する。
        // ただし、スキーマ定義されていない場合は、検証できないので除外する。
        if (edmProperty != null
                && !EdmSimpleType.STRING.getFullyQualifiedTypeName().equals(
                        edmProperty.getType().getFullyQualifiedTypeName())) {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(edmProperty.getName());
        }
    }


    /**
     * 検索条件に指定された各データ型の型検証クラスととりまとめる抽象クラス.
     */
    interface AbstractValidator {
        /**
         * 検索条件に指定されたプロパティのデータ型と検索条件値のデータとの不整合を検証する.
         * @param searchValue 検索条件値
         * @param propertyName 検索対象のプロパティ名
         */
        void validate(CommonExpression searchValue, String propertyName);
    }

    /**
     * 検索条件に指定されたEdm.String型の型検証クラス.
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
     * 検索条件に指定されたEdm.Boolean型の型検証クラス.
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
     * 検索条件に指定されたEdm.Int32型の型検証クラス.
     */
    static class Int32Validator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            long value = 0L; // odata4jのInt64Literal#gerValueがlong型の値を返すためvalueはlong型とした。
            if (searchValue instanceof IntegralLiteral) {
                value = ((IntegralLiteral) searchValue).getValue();
            } else if (searchValue instanceof Int64Literal) {
                value = ((Int64Literal) searchValue).getValue();
            } else if (searchValue instanceof NullLiteral) {
                value = 0;
            } else {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
            }

            // 値の範囲チェック
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }

    /**
     * 検索条件に指定されたEdm.Double型の型検証クラス.
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
     * 検索条件に指定されたEdm.Single型の型検証クラス.
     */
    static class SingleValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            double value = 0D; // odata4jのDoubleLiteral#gerValueがdouble型の値を返すためvalueはdouble型とした。
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

            // 値の範囲チェック
            if (!ODataUtils.validateSingle(String.valueOf(value))) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }

    /**
     * 検索条件に指定されたEdm.DateTime型の型検証クラス.
     */
    static class DateTimeValidator implements AbstractValidator {
        @Override
        public void validate(CommonExpression searchValue, String propertyName) {
            long value = 0L; // odata4jのInt64Literal#gerValueがlong型の値を返すためvalueはlong型とした。
            if (searchValue instanceof IntegralLiteral) {
                value = ((IntegralLiteral) searchValue).getValue();
            } else if (searchValue instanceof Int64Literal) {
                value = ((Int64Literal) searchValue).getValue();
            } else if (searchValue instanceof DateTimeLiteral) {
                value = ((DateTimeLiteral) searchValue).getValue().toDateTime().getMillis();
            } else if (searchValue instanceof DateTimeOffsetLiteral) {
                value = ((DateTimeOffsetLiteral) searchValue).getValue().getMillis();
            } else if (searchValue instanceof NullLiteral) {
                value = 0;
            } else {
                throw PersoniumCoreException.OData.OPERATOR_AND_OPERAND_TYPE_MISMATCHED.params(propertyName);
            }

            // 値の範囲チェック
            if (value > ODataUtils.DATETIME_MAX || value < ODataUtils.DATETIME_MIN) {
                throw PersoniumCoreException.OData.UNSUPPORTED_OPERAND_FORMAT.params(propertyName);
            }
        }
    }
}
