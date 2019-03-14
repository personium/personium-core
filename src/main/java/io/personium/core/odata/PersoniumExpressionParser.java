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
 * This code is based on ExpressionParser.java of odata4j-core, and some modifications
 * for personium.io are applied by us.
 *  - Add support for query containing '_' in tokenize(), readWord().
 *  - Add support for $expand query in parseExpandQuery().
 *  - If "datetime" or "datetimeoffset" are specified in the $filter query, default time zone is fixed to "UTC".
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.core4j.Enumerable;
import org.core4j.Func1;
import org.core4j.Func2;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.odata4j.core.Guid;
import org.odata4j.core.Throwables;
import org.odata4j.expression.BoolCommonExpression;
import org.odata4j.expression.CommonExpression;
import org.odata4j.expression.EntitySimpleProperty;
import org.odata4j.expression.Expression;
import org.odata4j.expression.ExpressionParser;
import org.odata4j.expression.ExpressionParser.AggregateFunction;
import org.odata4j.expression.ExpressionParser.TokenType;
import org.odata4j.expression.OrderByExpression;
import org.odata4j.expression.OrderByExpression.Direction;
import org.odata4j.expression.StringLiteral;
import org.odata4j.internal.InternalUtil;
import org.odata4j.repack.org.apache.commons.codec.DecoderException;
import org.odata4j.repack.org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PersoniumExpressionParser class.
 * Copy from OData4j library source
 */
public class PersoniumExpressionParser {

    static Logger log = LoggerFactory.getLogger(PersoniumExpressionParser.class);

    private static final int TOKEN_SIZE_3 = 3;
    private static final int TOKEN_SIZE_4 = 4;
    private static final int TOKEN_SIZE_5 = 5;
    private static final int TOKEN_SIZE_6 = 6;
    private static final int BINARY_EXPRESSION_CONSTANT = 3;

    private PersoniumExpressionParser() {

    }

    /**
     * Methods.
     */
    private static class Methods {

        public static final String CAST = "cast";
        public static final String ISOF = "isof";
        public static final String ENDSWITH = "endswith";
        public static final String STARTSWITH = "startswith";
        public static final String SUBSTRINGOF = "substringof";
        public static final String INDEXOF = "indexof";
        public static final String REPLACE = "replace";
        public static final String TOLOWER = "tolower";
        public static final String TOUPPER = "toupper";
        public static final String TRIM = "trim";
        public static final String SUBSTRING = "substring";
        public static final String CONCAT = "concat";
        public static final String LENGTH = "length";
        public static final String YEAR = "year";
        public static final String MONTH = "month";
        public static final String DAY = "day";
        public static final String HOUR = "hour";
        public static final String MINUTE = "minute";
        public static final String SECOND = "second";
        public static final String ROUND = "round";
        public static final String FLOOR = "floor";
        public static final String CEILING = "ceiling";
    }

    private static Set<String> methods = Enumerable.create(
            Methods.CAST, Methods.ISOF, Methods.ENDSWITH, Methods.STARTSWITH, Methods.SUBSTRINGOF, Methods.INDEXOF,
            Methods.REPLACE,
            Methods.TOLOWER, Methods.TOUPPER, Methods.TRIM, Methods.SUBSTRING, Methods.CONCAT, Methods.LENGTH,
            Methods.YEAR, Methods.MONTH, Methods.DAY, Methods.HOUR, Methods.MINUTE, Methods.SECOND, Methods.ROUND,
            Methods.FLOOR, Methods.CEILING).toSet();

    /**
     * tokenizer.
     * OData4j's tokenizer defines its own as it contains errors as including _
     * @param value value
     * @return token
     */
    public static List<Token> tokenize(String value) {
        List<Token> rt = new ArrayList<Token>();
        int current = 0;
        int end = 0;

        while (true) {
            if (current == value.length()) {
                return rt;
            }
            char c = value.charAt(current);
            if (Character.isWhitespace(c)) {
                end = readWhitespace(value, current);
                rt.add(new Token(TokenType.WHITESPACE, value.substring(current, end)));
                current = end;
            } else if (c == '\'') {
                end = readQuotedString(value, current + 1);
                rt.add(new Token(TokenType.QUOTED_STRING, value.substring(current, end)));
                current = end;
            } else if (Character.isLetter(c)) {
                end = readWord(value, current + 1);
                rt.add(new Token(TokenType.WORD, value.substring(current, end)));
                current = end;
            } else if (c == '_') {
                end = readWord(value, current + 1);
                rt.add(new Token(TokenType.WORD, value.substring(current, end)));
                current = end;
            } else if (Character.isDigit(c)) {
                end = readDigits(value, current + 1);
                rt.add(new Token(TokenType.NUMBER, value.substring(current, end)));
                current = end;
            } else if (c == '(') {
                rt.add(new Token(TokenType.OPENPAREN, Character.toString(c)));
                current++;
            } else if (c == ')') {
                rt.add(new Token(TokenType.CLOSEPAREN, Character.toString(c)));
                current++;
            } else if (c == '-') {
                if (Character.isDigit(value.charAt(current + 1))) {
                    end = readDigits(value, current + 1);
                    rt.add(new Token(TokenType.NUMBER, value.substring(current, end)));
                    current = end;
                } else {
                    rt.add(new Token(TokenType.SYMBOL, Character.toString(c)));
                    current++;
                }
            } else if (",.+=:".indexOf(c) > -1) {
                rt.add(new Token(TokenType.SYMBOL, Character.toString(c)));
                current++;
            } else {
                dumpTokens(rt);
                throw new RuntimeException("Unable to tokenize: " + value + " current: " + current + " rem: "
                        + value.substring(current));
            }
        }

    }

    /**
     * Perth in orderby.
     * @param value orderby value
     * @return Perth result
     */
    public static List<OrderByExpression> parseOrderBy(String value) {
        List<Token> tokens = tokenize(value);
        // dump(value,tokens,null);

        List<CommonExpression> expressions = readExpressions(tokens);
        if (ExpressionParser.DUMP_EXPRESSION_INFO) {
            dump(value, tokens, Enumerable.create(expressions).toArray(CommonExpression.class));
        }

        return Enumerable.create(expressions).select(new Func1<CommonExpression, OrderByExpression>() {
            public OrderByExpression apply(CommonExpression input) {
                if (input instanceof OrderByExpression) {
                    return (OrderByExpression) input;
                }
                return Expression.orderBy(input, Direction.ASCENDING); // default to asc
            }
        }).toList();
    }

    /**
     * Parse of filter.
     * @param value Value of filter
     * @return Perth result
     */
    public static CommonExpression parse(String value) {
        List<Token> tokens = tokenize(value);
        // dump(value,tokens,null);

        CommonExpression rt = readExpression(tokens);
        if (ExpressionParser.DUMP_EXPRESSION_INFO) {
            dump(value, tokens, rt);
        }

        return rt;
    }

    /**
     * A parse of select.
     * @param value Value of select
     * @return Perth result
     */
    public static List<EntitySimpleProperty> parseExpand(String value) {
        List<Token> tokens = tokenize(value);
        // dump(value,tokens,null);

        List<CommonExpression> expressions = readExpressions(tokens);

        // since we support currently simple properties only we have to
        // confine ourselves to EntitySimpleProperties.
        return Enumerable.create(expressions).select(new Func1<CommonExpression, EntitySimpleProperty>() {
            public EntitySimpleProperty apply(CommonExpression input) {
                if (input instanceof EntitySimpleProperty) {
                    return (EntitySimpleProperty) input;
                }
                return null;
            }
        }).toList();
    }

    /**
     * Parsing expand.
     * @param value The value of expand
     * @return Perth result
     */
    public static List<EntitySimpleProperty> parseExpandQuery(String value) {
        List<Token> tokens = tokenize(value);
        for (Token token : tokens) {
            if (!token.value.equals(",") && !token.value.startsWith("_")) {
                throw new RuntimeException("Invalid navigationProperty name.(" + token.toString() + ")");
            }
        }

        List<CommonExpression> expressions = readExpressions(tokens);

        // since we support currently simple properties only we have to
        // confine ourselves to EntitySimpleProperties.
        return Enumerable.create(expressions).select(new Func1<CommonExpression, EntitySimpleProperty>() {
            public EntitySimpleProperty apply(CommonExpression input) {
                if (input instanceof EntitySimpleProperty) {
                    return (EntitySimpleProperty) input;
                }
                return null;
            }
        }).toList();
    }

    /**
     * dumpTokens.
     * @param tokens token list
     */
    public static void dumpTokens(List<Token> tokens) {
        for (Token t : tokens) {
            if (t.type != null) {
                log.debug(t.type.toString() + t.toString());
            }
        }
    }

    /**
     * processParentheses.
     * @param tokens token list
     * @return token list
     */
    public static List<Token> processParentheses(List<Token> tokens) {

        List<Token> rt = new ArrayList<Token>();

        for (int i = 0; i < tokens.size(); i++) {
            Token openToken = tokens.get(i);
            if (openToken.type == TokenType.OPENPAREN) {
                int afterParenIdx = i + 1;
                // is this a method call or any/all aggregate function?
                String methodName = null;
                String aggregateSource = null;
                String aggregateVariable = null;
                AggregateFunction aggregateFunction = AggregateFunction.none;
                int k = i - 1;
                while (k > 0 && tokens.get(k).type == TokenType.WHITESPACE) {
                    k--;
                }
                if (k >= 0) {
                    Token methodNameToken = tokens.get(k);
                    if (methodNameToken.type == TokenType.WORD) {
                        if (methods.contains(methodNameToken.value)) {
                            methodName = methodNameToken.value;
                            // this isn't strictly correct. I think the parser has issues
                            // with sequences of WORD, WHITESPACE, WORD, etc. I'm not sure I've
                            // ever seen a token type of WHITESPACE producer by a lexer..
                        } else if (methodNameToken.value.endsWith("/any") || methodNameToken.value.endsWith("/all")) {
                            aggregateSource = methodNameToken.value.substring(0, methodNameToken.value.length()
                                    - TOKEN_SIZE_3);
                            aggregateFunction = Enum.valueOf(AggregateFunction.class,
                                    methodNameToken.value.substring(methodNameToken.value.length() - TOKEN_SIZE_3));
                            // to get things rolling I'm going to lookahead and require a very strict
                            // sequence of tokens:
                            // i + 1 must be a WORD
                            // i + 2 must be a SYMBOL ':'
                            // or, for any, i + 1 can be CLOSEPAREN
                            int ni = i + 1;
                            Token ntoken = null;
                            if (ni < tokens.size()) {
                                ntoken = tokens.get(ni);
                            }
                            processParenthesesCheckToken(aggregateFunction, ntoken);
                            if (ntoken != null) {
                                if (ntoken.type == TokenType.WORD) {
                                    aggregateVariable = ntoken.value;
                                    ni += 1;
                                    ntoken = null;
                                    if (ni < tokens.size()) {
                                        ntoken = tokens.get(ni);
                                    }
                                    if (ntoken == null || ntoken.type != TokenType.SYMBOL
                                            || !ntoken.value.equals(":")) {
                                        String reason;
                                        if (ntoken == null) {
                                            reason = "eof";
                                        } else {
                                            reason = ntoken.toString();
                                        }
                                        throw new RuntimeException("expected ':', found: " + reason);
                                    }
                                    // now we can parse the predicate, starting after the ':'
                                    afterParenIdx = ni + 1;
                                } else {
                                    // any(), easiest to early out here
                                    List<Token> tokensIncludingParens = tokens.subList(k, ni + 1);
                                    CommonExpression any = Expression.any(
                                            Expression.simpleProperty(aggregateSource));

                                    ExpressionToken et = new ExpressionToken(any, tokensIncludingParens);
                                    rt.subList(rt.size() - (i - k), rt.size()).clear();
                                    rt.add(et);
                                    return rt;
                                }
                            }
                        }
                    }
                }

                // find matching close paren
                int stack = 0;
                int start = i;
                List<CommonExpression> methodArguments = new ArrayList<CommonExpression>();
                for (int j = afterParenIdx; j < tokens.size(); j++) {
                    Token closeToken = tokens.get(j);
                    if (closeToken.type == TokenType.OPENPAREN) {
                        stack++;
                    } else if (methodName != null && stack == 0 && closeToken.type == TokenType.SYMBOL
                            && closeToken.value.equals(",")) {
                        List<Token> tokensInsideComma = tokens.subList(start + 1, j);
                        CommonExpression expressionInsideComma = readExpression(tokensInsideComma);
                        methodArguments.add(expressionInsideComma);
                        start = j;
                    } else if (closeToken.type == TokenType.CLOSEPAREN) {
                        if (stack > 0) {
                            stack--;
                            continue;
                        }
                        if (methodName != null) {
                            methodCall(tokens, rt, i, methodName, k, start, methodArguments, j);

                        } else if (aggregateVariable != null) {
                            List<Token> tokensIncludingParens = tokens.subList(k, j + 1);
                            List<Token> tokensInsideParens = tokens.subList(afterParenIdx, j);
                            CommonExpression expressionInsideParens = readExpression(tokensInsideParens);
                            if (!(expressionInsideParens instanceof BoolCommonExpression)) {
                                throw new RuntimeException("illegal any predicate");
                            }
                            CommonExpression any = Expression.aggregate(
                                    aggregateFunction,
                                    Expression.simpleProperty(aggregateSource),
                                    aggregateVariable,
                                    (BoolCommonExpression) expressionInsideParens);

                            ExpressionToken et = new ExpressionToken(any, tokensIncludingParens);
                            rt.subList(rt.size() - (i - k), rt.size()).clear();
                            rt.add(et);
                        } else {

                            List<Token> tokensIncludingParens = tokens.subList(i, j + 1);
                            List<Token> tokensInsideParens = tokens.subList(i + 1, j);
                            // paren expression: replace t ( t t t ) t with t et t
                            CommonExpression expressionInsideParens = readExpression(tokensInsideParens);
                            CommonExpression exp = null;
                            if (expressionInsideParens instanceof BoolCommonExpression) {
                                exp = Expression.boolParen(expressionInsideParens);
                            } else {
                                exp = Expression.paren(expressionInsideParens);
                            }

                            ExpressionToken et = new ExpressionToken(exp, tokensIncludingParens);
                            rt.add(et);
                        }

                        i = j;
                    }
                }
            } else {
                rt.add(openToken);
            }
        }

        return rt;

    }

    private static void methodCall(List<Token> tokens,
            List<Token> rt,
            int i,
            String methodName,
            int k,
            int start,
            List<CommonExpression> methodArguments,
            int j) {
        List<Token> tokensIncludingParens = tokens.subList(k, j + 1);
        List<Token> tokensInsideParens = tokens.subList(start + 1, j);
        CommonExpression expressionInsideParens = readExpression(tokensInsideParens);
        methodArguments.add(expressionInsideParens);

        // method call expression: replace t mn ( t t , t t ) t with t et t
        CommonExpression methodCall = methodCall(methodName, methodArguments);

        ExpressionToken et = new ExpressionToken(methodCall, tokensIncludingParens);
        rt.subList(rt.size() - (i - k), rt.size()).clear();
        rt.add(et);
    }

    private static void processParenthesesCheckToken(AggregateFunction aggregateFunction, Token ntoken) {
        if (ntoken == null
                || (aggregateFunction == AggregateFunction.all //NOPMD -To maintain readability
                && ntoken.type != TokenType.WORD)
                || (aggregateFunction == AggregateFunction.any //NOPMD
                && ntoken.type != TokenType.WORD
                && ntoken.type != TokenType.CLOSEPAREN)) {
            String reason;
            if (ntoken == null) {
                reason = "eof";
            } else {
                reason = ntoken.toString();
            }
            throw new RuntimeException("unexpected token: " + reason);
        }
    }

    private static CommonExpression methodCall(String methodName, List<CommonExpression> methodArguments) {
        if (methodName.equals(Methods.CAST) && methodArguments.size() == 1) {
            CommonExpression arg = methodArguments.get(0);
            assertType(arg, StringLiteral.class);
            String type = ((StringLiteral) arg).getValue();
            return Expression.cast(type);
        } else if (methodName.equals(Methods.CAST) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            assertType(arg2, StringLiteral.class);
            String type = ((StringLiteral) arg2).getValue();
            return Expression.cast(arg1, type);
        } else if (methodName.equals(Methods.ISOF) && methodArguments.size() == 1) {
            CommonExpression arg = methodArguments.get(0);
            assertType(arg, StringLiteral.class);
            String type = ((StringLiteral) arg).getValue();
            return Expression.isof(type);
        } else if (methodName.equals(Methods.ISOF) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            assertType(arg2, StringLiteral.class);
            String type = ((StringLiteral) arg2).getValue();
            return Expression.isof(arg1, type);
        } else if (methodName.equals(Methods.ENDSWITH) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.endsWith(arg1, arg2);
        } else if (methodName.equals(Methods.STARTSWITH) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.startsWith(arg1, arg2);
        } else if (methodName.equals(Methods.SUBSTRINGOF) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.substringOf(arg1);
        } else if (methodName.equals(Methods.SUBSTRINGOF) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.substringOf(arg1, arg2);
        } else if (methodName.equals(Methods.INDEXOF) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.indexOf(arg1, arg2);
        } else if (methodName.equals(Methods.REPLACE) && methodArguments.size() == TOKEN_SIZE_3) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            CommonExpression arg3 = methodArguments.get(2);
            return Expression.replace(arg1, arg2, arg3);
        } else if (methodName.equals(Methods.TOLOWER) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.toLower(arg1);
        } else if (methodName.equals(Methods.TOUPPER) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.toUpper(arg1);
        } else if (methodName.equals(Methods.TRIM) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.trim(arg1);
        } else if (methodName.equals(Methods.SUBSTRING) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.substring(arg1, arg2);
        } else if (methodName.equals(Methods.SUBSTRING) && methodArguments.size() == TOKEN_SIZE_3) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            CommonExpression arg3 = methodArguments.get(2);
            return Expression.substring(arg1, arg2, arg3);
        } else if (methodName.equals(Methods.CONCAT) && methodArguments.size() == 2) {
            CommonExpression arg1 = methodArguments.get(0);
            CommonExpression arg2 = methodArguments.get(1);
            return Expression.concat(arg1, arg2);
        } else if (methodName.equals(Methods.LENGTH) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.length(arg1);
        } else if (methodName.equals(Methods.YEAR) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.year(arg1);
        } else if (methodName.equals(Methods.MONTH) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.month(arg1);
        } else if (methodName.equals(Methods.DAY) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.day(arg1);
        } else if (methodName.equals(Methods.HOUR) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.hour(arg1);
        } else if (methodName.equals(Methods.MINUTE) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.minute(arg1);
        } else if (methodName.equals(Methods.SECOND) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.second(arg1);
        } else if (methodName.equals(Methods.CEILING) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.ceiling(arg1);
        } else if (methodName.equals(Methods.FLOOR) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.floor(arg1);
        } else if (methodName.equals(Methods.ROUND) && methodArguments.size() == 1) {
            CommonExpression arg1 = methodArguments.get(0);
            return Expression.round(arg1);
        } else {
            throw new RuntimeException("Implement method " + methodName);
        }
    }

    private static void dump(String value, List<Token> tokens, CommonExpression... expressions) {
        String msg = "[" + value + "] -> " + Enumerable.create(tokens).join("");
        if (expressions != null) {
            msg = msg + " -> " + Enumerable.create(expressions).select(new Func1<CommonExpression, String>() {
                public String apply(CommonExpression input) {
                    return Expression.asPrintString(input);
                }
            }).join(",");
        }
        if (msg != null) {
            log.debug(msg);
        }
    }

    private static CommonExpression processBinaryExpression(List<Token> tokens,
            String op,
            Func2<CommonExpression, CommonExpression, CommonExpression> fn) {

        int ts = tokens.size();
        for (int i = 0; i < ts; i++) {
            Token t = tokens.get(i);
            if (i < ts - 2
                    && t.type == TokenType.WHITESPACE && tokens.get(i + 2).type == TokenType.WHITESPACE
                    && tokens.get(i + 1).type == TokenType.WORD && tokens.get(i + 1).value.equals(op)) {
                CommonExpression lhs = readExpression(tokens.subList(0, i));
                CommonExpression rhs = readExpression(tokens.subList(i + BINARY_EXPRESSION_CONSTANT, ts));
                return fn.apply(lhs, rhs);
            }
        }
        return null;
    }

    private static CommonExpression processUnaryExpression(List<Token> tokens,
            String op,
            boolean whitespaceRequired,
            Func1<CommonExpression, CommonExpression> fn) {

        int ts = tokens.size();
        for (int i = 0; i < ts; i++) {
            Token t = tokens.get(i);
            if (i < ts - 1
                    && (t.type == TokenType.WORD || t.type == TokenType.SYMBOL)
                    && (!whitespaceRequired || tokens.get(i + 1).type == TokenType.WHITESPACE)
                    && t.value.equals(op)) {
                int idx = 0;
                if (whitespaceRequired) {
                    idx = 2;
                } else {
                    idx = 1;
                }
                CommonExpression expression = readExpression(tokens.subList(i + idx, ts));
                return fn.apply(expression);
            }
        }
        return null;
    }

    private static <T extends CommonExpression> void assertType(CommonExpression expression, Class<T> type) {
        if (!type.isAssignableFrom(expression.getClass())) {
            throw new RuntimeException("Expected " + type.getSimpleName());
        }
    }

    private static List<CommonExpression> readExpressions(List<Token> tokens) {
        List<CommonExpression> rt = new ArrayList<CommonExpression>();

        int stack = 0;
        int start = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.OPENPAREN) {
                stack++;
            } else if (token.type == TokenType.CLOSEPAREN) {
                stack--;
            } else if (stack == 0 && token.type == TokenType.SYMBOL && token.value.equals(",")) {
                List<Token> tokensInsideComma = tokens.subList(start, i);
                CommonExpression expressionInsideComma = readExpression(tokensInsideComma);
                rt.add(expressionInsideComma);
                start = i + 1;
            } else if (i == tokens.size() - 1) {
                List<Token> tokensInside = tokens.subList(start, i + 1);
                CommonExpression expressionInside = readExpression(tokensInside);
                rt.add(expressionInside);
            }

        }
        return rt;
    }

    private static CommonExpression readExpression(List<Token> tokens) {

        CommonExpression rt = null;

        tokens = trimWhitespace(tokens);

        // OrderBy asc, desc
        Token lastToken = tokens.get(tokens.size() - 1);
        if (lastToken.type == TokenType.WORD && (lastToken.value.equals("asc") || lastToken.value.equals("desc"))) {
            Direction direction;
            if (lastToken.value.equals("asc")) {
                direction = Direction.ASCENDING;
            } else {
                direction = Direction.DESCENDING;
            }
            return Expression.orderBy(
                    readExpression(tokens.subList(0, tokens.size() - 1)), direction);
        }

        // Grouping (highest precedence)
        tokens = processParentheses(tokens);

        // now we have a list of tokens with no explicit parens

        // process literals
        rt = readExpressionLiteralProcess(tokens);
        if (rt != null) {
            return rt;
        }

        // single token expression
        if (tokens.size() == 1) {
            final Token token = tokens.get(0);
            if (token.type == TokenType.QUOTED_STRING) {
                return Expression.string(unquote(token.value));
            } else if (token.type == TokenType.WORD) {
                if (token.value.equals("null")) {
                    return Expression.null_();
                }
                if (token.value.equals("true")) {
                    return Expression.boolean_(true);
                }
                if (token.value.equals("false")) {
                    return Expression.boolean_(false);
                }
                return Expression.simpleProperty(token.value);
            } else if (token.type == TokenType.NUMBER) {
                try {
                    int value = Integer.parseInt(token.value);
                    return Expression.integral(value);
                } catch (NumberFormatException e) {
                    long value = Long.parseLong(token.value);
                    return Expression.int64(value);
                }
            } else if (token.type == TokenType.EXPRESSION) {
                return ((ExpressionToken) token).expression;
            } else {
                throw new RuntimeException("Unexpected");
            }
        }

        // process operators from least to highest precedence
        rt = readExpressionPrecedenceOperator(tokens);
        if (rt != null) {
            return rt;
        }

        throw new RuntimeException("Unable to read expression with tokens: " + tokens);

    }

    private static CommonExpression readExpressionLiteralProcess(List<Token> tokens) {

        // literals with prefixes
        if (tokens.size() == 2 && tokens.get(0).type == TokenType.WORD
                && tokens.get(1).type == TokenType.QUOTED_STRING) {
            String word = tokens.get(0).value;
            String value = unquote(tokens.get(1).value);
            if (word.equals("datetime")) {
                DateTime dt = parseUTCDateTime(value);
                return Expression.dateTime(new LocalDateTime(dt));
            } else if (word.equals("time")) {
                LocalTime t = InternalUtil.parseTime(value);
                return Expression.time(t);
            } else if (word.equals("datetimeoffset")) {
                DateTime dt = parseDateTime(value);
                return Expression.dateTimeOffset(dt);
            } else if (word.equals("guid")) {
                // odata: dddddddd-dddd-dddd-dddddddddddd
                // java: dddddddd-dd-dd-dddd-dddddddddddd
                // value = value.substring(0, 11) + "-" + value.substring(11);
                return Expression.guid(Guid.fromString(value));
            } else if (word.equals("decimal")) {
                return Expression.decimal(new BigDecimal(value));
            } else if (word.equals("X") || word.equals("binary")) {
                try {
                    byte[] bValue = Hex.decodeHex(value.toCharArray());
                    return Expression.binary(bValue);
                } catch (DecoderException e) {
                    throw Throwables.propagate(e);
                }
            }
        }
        // long literal: 1234L
        if (tokens.size() == 2 && tokens.get(0).type == TokenType.NUMBER && tokens.get(1).type == TokenType.WORD
                && tokens.get(1).value.equals("L")) {
            long longValue = Long.parseLong(tokens.get(0).value);
            return Expression.int64(longValue);
        }
        // single literal: 2f
        if (tokens.size() == 2 && tokens.get(0).type == TokenType.NUMBER && tokens.get(1).type == TokenType.WORD
                && tokens.get(1).value.equals("f")) {
            float floatValue = Float.parseFloat(tokens.get(0).value);
            return Expression.single(floatValue);
        }
        // single literal: 2.0f
        if (tokens.size() == TOKEN_SIZE_4 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.SYMBOL
                && tokens.get(1).value.equals(".") && tokens.get(2).type == TokenType.NUMBER
                && tokens.get(TOKEN_SIZE_3).value.equals("f")) {
            float floatValue = Float.parseFloat(tokens.get(0).value + "." + tokens.get(2).value);
            return Expression.single(floatValue);
        }
        // double literal: 2.0
        if (tokens.size() == TOKEN_SIZE_3 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.SYMBOL
                && tokens.get(1).value.equals(".") && tokens.get(2).type == TokenType.NUMBER) {
            double doubleValue = Double.parseDouble(tokens.get(0).value + "." + tokens.get(2).value);
            return Expression.double_(doubleValue);
        }
        // double literal: 1E+10
        if (tokens.size() == TOKEN_SIZE_4 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.WORD
                && tokens.get(1).value.equals("E") && tokens.get(2).type == TokenType.SYMBOL
                && tokens.get(2).value.equals("+") && tokens.get(TOKEN_SIZE_3).type == TokenType.NUMBER) {
            double doubleValue = Double.parseDouble(tokens.get(0).value + "E+" + tokens.get(TOKEN_SIZE_3).value);
            return Expression.double_(doubleValue);
        }
        // double literal: 1E-10
        if (tokens.size() == TOKEN_SIZE_3 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.WORD
                && tokens.get(1).value.equals("E") && tokens.get(2).type == TokenType.NUMBER) {
            int e = Integer.parseInt(tokens.get(2).value);
            if (e < 1) {
                double doubleValue = Double.parseDouble(tokens.get(0).value + "E" + tokens.get(2).value);
                return Expression.double_(doubleValue);
            }
        }
        // double literal: 1.2E+10
        if (tokens.size() == TOKEN_SIZE_6 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.SYMBOL
                && tokens.get(1).value.equals(".") && tokens.get(2).type == TokenType.NUMBER
                && tokens.get(TOKEN_SIZE_3).type == TokenType.WORD && tokens.get(TOKEN_SIZE_3).value.equals("E")
                && tokens.get(TOKEN_SIZE_4).type == TokenType.SYMBOL && tokens.get(TOKEN_SIZE_4).value.equals("+")
                && tokens.get(TOKEN_SIZE_5).type == TokenType.NUMBER) {
            double doubleValue = Double.parseDouble(tokens.get(0).value + "." + tokens.get(2).value + "E+"
                    + tokens.get(TOKEN_SIZE_5).value);
            return Expression.double_(doubleValue);
        }
        // double literal: 1.2E-10
        if (tokens.size() == TOKEN_SIZE_5 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.SYMBOL
                && tokens.get(1).value.equals(".") && tokens.get(2).type == TokenType.NUMBER
                && tokens.get(TOKEN_SIZE_3).type == TokenType.WORD && tokens.get(TOKEN_SIZE_3).value.equals("E")
                && tokens.get(TOKEN_SIZE_4).type == TokenType.NUMBER) {
            int e = Integer.parseInt(tokens.get(TOKEN_SIZE_4).value);
            if (e < 1) {
                double doubleValue = Double.parseDouble(tokens.get(0).value + "." + tokens.get(2).value + "E"
                        + tokens.get(TOKEN_SIZE_4).value);
                return Expression.double_(doubleValue);
            }
        }
        // decimal literal: 1234M or 1234m
        if (tokens.size() == 2 && tokens.get(0).type == TokenType.NUMBER && tokens.get(1).type == TokenType.WORD
                && tokens.get(1).value.equalsIgnoreCase("M")) {
            BigDecimal decimalValue = new BigDecimal(tokens.get(0).value);
            return Expression.decimal(decimalValue);
        }
        // decimal literal: 2.0m
        if (tokens.size() == TOKEN_SIZE_4 && tokens.get(0).type == TokenType.NUMBER
                && tokens.get(1).type == TokenType.SYMBOL
                && tokens.get(1).value.equals(".") && tokens.get(2).type == TokenType.NUMBER
                && tokens.get(TOKEN_SIZE_3).value.equalsIgnoreCase("m")) {
            BigDecimal decimalValue = new BigDecimal(tokens.get(0).value + "." + tokens.get(2).value);
            return Expression.decimal(decimalValue);
        }
        // TODO literals: byteLiteral, sbyteliteral

        return null;

    }

    /**
     * Method that Personium added independently.
     * <p>
     * Parse "ISO-8601 format string without time zone offset" and convert it to DateTime type.
     * Time zone is interpreted as UTC.
     * @param value String to parse
     * @return DateTime
     */
    private static DateTime parseUTCDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .withZone(ZoneId.of("UTC"))
                .withResolverStyle(ResolverStyle.STRICT);
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(value, formatter);
        long epochMilli = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        return new DateTime(epochMilli, DateTimeZone.UTC);
    }

    /**
     * Method that Personium added independently.
     * <p>
     * Parse "ISO-8601 format string" and convert it to DateTime type.
     * If no time zone offset is specified in string, time zone is interpreted as UTC.
     * @param value String to parse
     * @return DateTime
     */
    private static DateTime parseDateTime(String value) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME
                .withZone(ZoneId.of("UTC"))
                .withResolverStyle(ResolverStyle.STRICT);
        TemporalAccessor temporalAccessor = formatter.parse(value);

        long epochMilli = 0L;
        if (temporalAccessor.isSupported(ChronoField.OFFSET_SECONDS)) {
            OffsetDateTime dateTime = OffsetDateTime.from(temporalAccessor);
            epochMilli = dateTime.toInstant().toEpochMilli();
        } else {
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.from(temporalAccessor);
            epochMilli = dateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        }

        return new DateTime(epochMilli);
    }

    private static CommonExpression readExpressionPrecedenceOperator(List<Token> tokens) {
        CommonExpression rt = null;

        // Conditional OR: or
        rt = processBinaryExpression(tokens, "or", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                assertType(lhs, BoolCommonExpression.class);
                assertType(rhs, BoolCommonExpression.class);
                return Expression.or((BoolCommonExpression) lhs, (BoolCommonExpression) rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Conditional AND: and
        rt = processBinaryExpression(tokens, "and", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                assertType(lhs, BoolCommonExpression.class);
                assertType(rhs, BoolCommonExpression.class);
                return Expression.and((BoolCommonExpression) lhs, (BoolCommonExpression) rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Equality: eq ne
        rt = processBinaryExpression(tokens, "eq", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.eq(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "ne", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.ne(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Relational and type testing: lt, gt, le, ge, isof(T) , isof(x,T)
        rt = processBinaryExpression(tokens, "lt", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.lt(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "gt", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.gt(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "le", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.le(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "ge", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.ge(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Additive: add, sub
        rt = processBinaryExpression(tokens, "add", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.add(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "sub", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.sub(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Multiplicative: mul, div, mod
        rt = processBinaryExpression(tokens, "mul", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.mul(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "div", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.div(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processBinaryExpression(tokens, "mod", new Func2<CommonExpression, CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression lhs, CommonExpression rhs) {
                return Expression.mod(lhs, rhs);
            }
        });
        if (rt != null) {
            return rt;
        }

        // Unary: not x, -x, cast(T), cast(x,T)
        rt = processUnaryExpression(tokens, "not", true, new Func1<CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression expression) {
                return Expression.not(expression);
            }
        });
        if (rt != null) {
            return rt;
        }
        rt = processUnaryExpression(tokens, "-", false, new Func1<CommonExpression, CommonExpression>() {
            public CommonExpression apply(CommonExpression expression) {
                return Expression.negate(expression);
            }
        });

        return rt;
    }

    private static String unquote(String singleQuotedValue) {
        return singleQuotedValue.substring(1, singleQuotedValue.length() - 1).replace("''", "'");
    }

    private static List<Token> trimWhitespace(List<Token> tokens) {
        int start = 0;
        while (tokens.get(start).type == TokenType.WHITESPACE) {
            start++;
        }
        int end = tokens.size() - 1;
        while (tokens.get(end).type == TokenType.WHITESPACE) {
            end--;
        }
        return tokens.subList(start, end + 1);

    }

    private static int readDigits(String value, int start) {
        int rt = start;
        while (rt < value.length() && Character.isDigit(value.charAt(rt))) {
            rt++;
        }
        return rt;
    }

    private static int readWord(String value, int start) {
        int rt = start;
        while (rt < value.length()
                && (Character.isLetterOrDigit(value.charAt(rt)) || value.charAt(rt) == '/'
                        || value.charAt(rt) == '_' || value.charAt(rt) == '-')) {
            rt++;
        }
        return rt;
    }

    private static int readQuotedString(String value, int start) {
        int rt = start;
        while (value.charAt(rt) != '\'' //NOPMD -To maintain readability
                || (rt < value.length() - 1 && value.charAt(rt + 1) == '\'')) { //NOPMD
            if (value.charAt(rt) != '\'') {
                rt++;
            } else {
                rt += 2;
            }
        }
        rt++;
        return rt;
    }

    private static int readWhitespace(String value, int start) {
        int rt = start;
        while (rt < value.length() && Character.isWhitespace(value.charAt(rt))) {
            rt++;
        }
        return rt;
    }

    /**
     * Token class.
     */
    public static class Token {

        /** TokenType. **/
        private final TokenType type;
        /** TokenValue. **/
        private final String value;

        /**
         * constructor.
         * @param type type
         * @param value value
         */
        public Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "[" + value + "]";
        }
    }

    /**
     * ExpressionToken class.
     */
    private static class ExpressionToken extends Token {

        private final CommonExpression expression;
        private final List<Token> tokens;

        ExpressionToken(CommonExpression expression, List<Token> tokens) {
            super(TokenType.EXPRESSION, null);
            this.expression = expression;
            this.tokens = tokens;
        }

        @Override
        public String toString() {
            return Enumerable.create(tokens).join("");
        }
    }
}
