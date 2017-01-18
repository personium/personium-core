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
package com.fujitsu.dc.test.unit.core.odata;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.odata4j.expression.ExpressionParser.TokenType;

import com.fujitsu.dc.core.odata.DcExpressionParser;
import com.fujitsu.dc.core.odata.DcExpressionParser.Token;
import com.fujitsu.dc.test.categories.Unit;

/**
 * DcExpressionParser ユニットテストクラス.
 */
@Category({ Unit.class })
public class DcExpressionParserTest {


    /**
     * パース対象に括弧とじがない場合.
     */
    @Test
    public void パース対象の括弧始まりが1つ多い場合に括弧始まりが除去されること() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.OPENPAREN, "(");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        Token t4 = new Token(TokenType.CLOSEPAREN, ")");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        List<Token> result = DcExpressionParser.processParentheses(tokenList);
        assertEquals("[[(][test][)]]", result.toString());
    }

    /**
     * パース対象に括弧とじがない場合に括弧始まりが除去されること_boolean.
     */
    @Test
    public void パース対象に括弧とじがない場合に括弧始まりが除去されること_boolean() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.OPENPAREN, "(");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "true");
        Token t4 = new Token(TokenType.CLOSEPAREN, ")");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        List<Token> result = DcExpressionParser.processParentheses(tokenList);
        assertEquals("[[(][true][)]]", result.toString());
    }

    /**
     * メソッドanyの指定があるが次のTokenがSYMBOLではない場合エラーとなること.
     */
    @Test
    public void メソッドanyの指定があるが次のTokenがSYMBOLではない場合エラーとなること() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        Token t4 = new Token(TokenType.CLOSEPAREN, ")");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        try {
            DcExpressionParser.processParentheses(tokenList);
        } catch (Exception e) {
            assertEquals("expected ':', found: [)]", e.getMessage());
        }
    }

    /**
     * メソッドanyの指定があるがSYMBOLの指定がない場合エラーとなること.
     */
    @Test
    public void メソッドanyの指定があるがSYMBOLの指定がない場合エラーとなること() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        try {
            DcExpressionParser.processParentheses(tokenList);
        } catch (Exception e) {
            assertEquals("expected ':', found: eof", e.getMessage());
        }
    }

    /**
     * メソッドの指定があるがWORDの指定がない場合エラーとなること.
     */
    @Test
    public void メソッドの指定があるがWORDの指定がない場合エラーとなること() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        tokenList.add(t1);
        tokenList.add(t2);
        try {
            DcExpressionParser.processParentheses(tokenList);
        } catch (Exception e) {
            assertEquals("unexpected token: eof", e.getMessage());
        }
    }


    /**
     * メソッドanyの次がWORDその次がSYMBOLのときエラーにならないこと.
     */
    @Test
    public void メソッドanyの次がWORDその次がSYMBOLのときエラーにならないこと() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        Token t4 = new Token(TokenType.SYMBOL, ":");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        List<Token> result = DcExpressionParser.processParentheses(tokenList);
        assertEquals("[[/any], [test], [:]]", result.toString());
    }

    /**
     * anyとascを同時に指定した場合にエラーとなること.
     */
    @Test
    public void anyとascを同時に指定した場合にエラーとなること() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        Token t4 = new Token(TokenType.SYMBOL, ":");
        Token t5 = new Token(TokenType.WORD, "test");
        Token t6 = new Token(TokenType.WORD, "asc");
        Token t7 = new Token(TokenType.CLOSEPAREN, ")");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        tokenList.add(t5);
        tokenList.add(t6);
        tokenList.add(t7);

        try {
            DcExpressionParser.processParentheses(tokenList);
        } catch (Exception e) {
            assertEquals("illegal any predicate", e.getMessage());
        }
    }

    /**
     * メソッドanyの括弧の中身が空のときにエラーとならないこと.
     */
    @Test
    public void メソッドanyの括弧の中身が空のときにエラーとならないこと() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/any");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.CLOSEPAREN, ")");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        List<Token> result = DcExpressionParser.processParentheses(tokenList);
        assertEquals("[[/any][(][)]]", result.toString());
    }

    /**
     * メソッドallの次がWORDその次がSYMBOLのときエラーにならないこと.
     */
    @Test
    public void メソッドallの次がWORDその次がSYMBOLのときエラーにならないこと() {
        List<Token> tokenList = new ArrayList<Token>();
        Token t1 = new Token(TokenType.WORD, "/all");
        Token t2 = new Token(TokenType.OPENPAREN, "(");
        Token t3 = new Token(TokenType.WORD, "test");
        Token t4 = new Token(TokenType.SYMBOL, ":");
        tokenList.add(t1);
        tokenList.add(t2);
        tokenList.add(t3);
        tokenList.add(t4);
        List<Token> result = DcExpressionParser.processParentheses(tokenList);
        assertEquals("[[/all], [test], [:]]", result.toString());
    }

}
