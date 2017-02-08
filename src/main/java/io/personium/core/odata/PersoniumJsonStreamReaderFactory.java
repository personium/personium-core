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
 * This code is based on JsonStreamReaderFactory.java of odata4j-core, and some modifications
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

import java.io.IOException;
import java.io.Reader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Stack;

import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonParseException;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader.JsonEndPropertyEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader.JsonEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader.JsonStartPropertyEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamReader.JsonValueEvent;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamTokenizer;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamTokenizer.JsonToken;
import io.personium.core.odata.PersoniumJsonStreamReaderFactory.JsonStreamTokenizer.JsonTokenType;

/**
 * PersoniumJsonStreamReaderFactory.
 * JSONをストリームで読み込むReaderのファクトリー
 */
public class PersoniumJsonStreamReaderFactory {
    /**
     * コンストラクタ.
     */
    private PersoniumJsonStreamReaderFactory() {
    }

    /**
     * JsonParseException.
     * パースエラー時の例外
     */
    public static class JsonParseException extends RuntimeException {

        private static final long serialVersionUID = 2362481232045271688L;

        /**
         * コンストラクタ.
         */
        public JsonParseException() {
            super();
        }

        /**
         * コンストラクタ.
         * @param message メッセージ
         * @param cause Throwable
         */
        public JsonParseException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * コンストラクタ.
         * @param message メッセージ
         */
        public JsonParseException(String message) {
            super(message);
        }

        /**
         * コンストラクタ.
         * @param cause Throwable
         */
        public JsonParseException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * JsonStreamReader.
     */
    public interface JsonStreamReader {

        /**
         * JsonEventインターフェースクラス.
         */
        public interface JsonEvent {

            /**
             * @return boolean
             */
            boolean isStartObject();

            /**
             * @return boolean
             */
            boolean isEndObject();

            /**
             * @return boolean
             */
            boolean isStartProperty();

            /**
             * @return boolean
             */
            boolean isEndProperty();

            /**
             * @return boolean
             */
            boolean isStartArray();

            /**
             * @return boolean
             */
            boolean isEndArray();

            /**
             * @return boolean
             */
            boolean isValue();

            /**
             * @return JsonStartPropertyEvent
             */
            JsonStartPropertyEvent asStartProperty();

            /**
             * @return JsonEndPropertyEvent
             */
            JsonEndPropertyEvent asEndProperty();

            /**
             * @return JsonValueEvent.
             */
            JsonValueEvent asValue();
        }

        /**
         * JsonStartPropertyEvent.
         */
        public interface JsonStartPropertyEvent extends JsonEvent {
            /**
             * @return キー名
             */
            String getName();
        }

        /**
         * JsonEndPropertyEvent.
         */
        public interface JsonEndPropertyEvent extends JsonEvent {
            /**
             * JSONの値を文字列で返す.
             * @return JSON値
             */
            String getValue();

            /**
             * JSONの値をオブジェクトで返す.
             * @return オブジェクト
             */
            Object getObject();
        }

        /**
         * JsonValueEvent.
         */
        public interface JsonValueEvent extends JsonEvent {
            /**
             * JSONの値を文字列で返す.
             * @return JSON値
             */
            String getValue();
        }

        /**
         * 次の値を持っているか.
         * @return boolean
         */
        boolean hasNext();

        /**
         * 次のイベントを返却.
         * @return JSONイベント
         */
        JsonEvent nextEvent();

        /**
         * returns the JsonEvent that the last call to nextEvent() returned.
         * @return the last JsonEvent returned by nextEvent()
         */
        JsonEvent previousEvent();

        /**
         * クローズ.
         */
        void close();
    }

    /**
     * JsonStreamTokenizer.
     */
    public interface JsonStreamTokenizer {

        /**
         * JsonTokenType.
         */
        enum JsonTokenType {
            LEFT_CURLY_BRACKET,
            RIGHT_CURLY_BRACKET,
            LEFT_BRACKET,
            RIGHT_BRACKET,
            COMMA,
            COLON,
            TRUE,
            FALSE,
            NULL,
            NUMBER,
            STRING;
        }

        /**
         * JsonToken.
         */
        class JsonToken {
            /**
             * JSONトークンのタイプ.
             */
            private final JsonTokenType type;
            /**
             * JSONトークンの値.
             */
            private final String value;

            /**
             * コンストラクタ.
             * @param type JSONトークンのタイプ
             */
            public JsonToken(JsonTokenType type) {
                this(type, null);
            }

            /**
             * コンストラクタ.
             * @param type JSONトークンのタイプ
             * @param value JSONトークンの値
             */
            public JsonToken(JsonTokenType type, String value) {
                this.type = type;
                this.value = value;
            }

            @Override
            public String toString() {
                StringBuilder bld = new StringBuilder();

                bld.append(getType());
                if (getValue() != null) {
                    bld.append("(").append(getValue()).append(")");
                }

                return bld.toString();
            }

            /**
             * タイプを取得する.
             * @return type type
             */
            public JsonTokenType getType() {
                return type;
            }

            /**
             * 値を取得する.
             * @return value value
             */
            public String getValue() {
                return value;
            }
        }

        /**
         * 次の値を持っているか.
         * @return boolean
         */
        boolean hasNext();

        /**
         * 次のJSONトークンを返却.
         * @return JSONトークン
         */
        JsonToken nextToken();

        /**
         * クローズ.
         */
        void close();
    }

    /**
     * JsonStreamReaderを作成する.
     * @param reader reader
     * @return JsonStreamReader
     */
    public static JsonStreamReader createJsonStreamReader(Reader reader) {
        return new JsonStreamReaderImpl(reader);
    }

    /**
     * JsonStreamTokenizerを作成する.
     * @param reader reader
     * @return JsonStreamTokenizer
     */
    public static JsonStreamTokenizer createJsonStreamTokenizer(Reader reader) {
        return new JsonStreamTokenizerImpl(reader);
    }

}

/**
 * JsonEventImpl.
 */
class JsonEventImpl implements JsonEvent {

    @Override
    public boolean isStartObject() {
        return false;
    }

    @Override
    public boolean isEndObject() {
        return false;
    }

    @Override
    public boolean isStartProperty() {
        return false;
    }

    @Override
    public boolean isEndProperty() {
        return false;
    }

    @Override
    public boolean isStartArray() {
        return false;
    }

    @Override
    public boolean isEndArray() {
        return false;
    }

    @Override
    public boolean isValue() {
        return false;
    }

    @Override
    public JsonStartPropertyEvent asStartProperty() {
        return (JsonStartPropertyEvent) this;
    }

    @Override
    public JsonEndPropertyEvent asEndProperty() {
        return (JsonEndPropertyEvent) this;
    }

    @Override
    public JsonValueEvent asValue() {
        return (JsonValueEvent) this;
    }

    public String toString() {
        StringBuilder bld = new StringBuilder();

        if (isStartObject()) {
            bld.append("StartObject('{')");
        } else if (isEndObject()) {
            bld.append("EndObject('}')");
        } else if (isStartArray()) {
            bld.append("StartArray('[')");
        } else if (isEndArray()) {
            bld.append("EndArray(']')");
        } else if (isStartProperty()) {
            bld.append("StartProperty(").append(asStartProperty().getName()).append(")");
        } else if (isEndProperty()) {
            if (asEndProperty().getValue() == null) {
                bld.append("EndProperty(").append("<null>").append(")");
            } else {
                bld.append("EndProperty(").append(asEndProperty().getValue()).append(")");
            }
        } else if (isValue()) {
            if (asValue().getValue() == null) {
                bld.append("Value(").append("<null>").append(")");
            } else {
                bld.append("Value(").append(asValue().getValue()).append(")");
            }
        }

        return bld.toString();
    }

}

/**
 * JsonStartPropertyEventImpl.
 */
class JsonStartPropertyEventImpl extends JsonEventImpl implements JsonStartPropertyEvent {

    @Override
    public boolean isStartProperty() {
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

}

/**
 * JsonEndPropertyEventImpl.
 */
class JsonEndPropertyEventImpl extends JsonEventImpl implements JsonEndPropertyEvent {

    @Override
    public boolean isEndProperty() {
        return true;
    }

    @Override
    public String getValue() {
        return null;
    }

    public Object getObject() {
        return null;
    }

}

/**
 * JsonValueEventImpl.
 */
class JsonValueEventImpl extends JsonEventImpl implements JsonValueEvent {

    @Override
    public boolean isValue() {
        return true;
    }

    @Override
    public String getValue() {
        return null;
    }

}

/**
 * JsonStreamTokenizerImpl.
 */
class JsonStreamTokenizerImpl implements JsonStreamTokenizer {
    private Reader reader;
    private JsonToken token;
    private NumberFormat nf = DecimalFormat.getNumberInstance(Locale.US);
    private int pushedBack = -1;

    JsonStreamTokenizerImpl(Reader reader) {
        if (reader == null) {
            throw new NullPointerException();
        }

        this.reader = reader;
        move();
    }

    public boolean hasNext() {
        return token != null;
    }

    public JsonToken nextToken() {
        JsonToken cur = token;
        move();
        return cur;
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException ioe) {
            throw new JsonParseException(ioe);
        }
    }

    /**
     * TokenizerState.
     */
    enum TokenizerState {
        DEFAULT,
        STRING,
        NUMBER
    }

    static final int LENGTH_FOUR = 4;
    static final int LENGTH_SIXTEEN = 16;

    private void move() {
        token = null;

        StringBuilder buffer = new StringBuilder();
        boolean quote = false;
        TokenizerState state = TokenizerState.DEFAULT;
        int i = 0;
        while (token == null && i != -1) {
            i = next();
            char c = (char) i;
            if (state == TokenizerState.DEFAULT) {
                if ('{' == c) {
                    token = new JsonToken(JsonTokenType.LEFT_CURLY_BRACKET);
                } else if ('}' == c) {
                    if (hasConstantParsed(buffer)) {
                        pushBack(i);
                    } else {
                        token = new JsonToken(JsonTokenType.RIGHT_CURLY_BRACKET);
                    }
                } else if ('[' == c) {
                    token = new JsonToken(JsonTokenType.LEFT_BRACKET);
                } else if (']' == c) {
                    if (hasConstantParsed(buffer)) {
                        pushBack(i);
                    } else {
                        token = new JsonToken(JsonTokenType.RIGHT_BRACKET);
                    }
                } else if (':' == c) {
                    token = new JsonToken(JsonTokenType.COLON);
                } else if (',' == c) {
                    if (hasConstantParsed(buffer)) {
                        pushBack(i);
                    } else {
                        token = new JsonToken(JsonTokenType.COMMA);
                    }
                } else if ('"' == c) {
                    if (buffer.length() > 0) {
                        throw new JsonParseException("no JSON format");
                    }
                    state = TokenizerState.STRING;
                } else if ('-' == c || Character.isDigit(c)) {
                    buffer.append(c);
                    state = TokenizerState.NUMBER;
                } else if (Character.isWhitespace(c)) {
                    hasConstantParsed(buffer);
                } else {
                    buffer.append(c);
                }
            } else if (state == TokenizerState.STRING) {
                if (quote) {
                    if ('b' == c) {
                        buffer.append('\b');
                    } else if ('f' == c) {
                        buffer.append('\f');
                    } else if ('n' == c) {
                        buffer.append('\n');
                    } else if ('r' == c) {
                        buffer.append('\r');
                    } else if ('t' == c) {
                        buffer.append('\t');
                    } else if ('/' == c) {
                        buffer.append('/');
                    } else if ('\\' == c) {
                        buffer.append('\\');
                    } else if ('"' == c) {
                        buffer.append('"');
                    } else if ('u' == c) {
                        buffer.append((char) Integer.parseInt(new String(next(LENGTH_FOUR)), LENGTH_SIXTEEN));
                    } else {
                        throw new JsonParseException("illegal escaped character " + c);
                    }
                    quote = false;
                } else if ('\\' == c) {
                    quote = true;
                } else if ('"' == c) {
                    token = new JsonToken(JsonTokenType.STRING, buffer.toString());
                } else {
                    buffer.append(c);
                    quote = false;
                }
            } else {
                if ('-' == c || Character.isDigit(c)
                        || 'E' == c || 'e' == c
                        || '+' == c || '.' == c) {
                    buffer.append(c); // a valid character in a number
                } else {
                    // must be done with the number.
                    pushBack(i);
                    checkNumberFormat(buffer);
                    token = new JsonToken(JsonTokenType.NUMBER, buffer.toString());
                }
            }
        }

        if (state == TokenizerState.NUMBER) {
            checkNumberFormat(buffer);
            token = new JsonToken(JsonTokenType.NUMBER, buffer.toString());
        }
    }

    private Number checkNumberFormat(StringBuilder memory) {
        try {
            return nf.parse(memory.toString());
        } catch (Exception nfe) {
            throw new JsonParseException("", nfe);
        }
    }

    private boolean hasConstantParsed(StringBuilder memory) {
        if (isTrue(memory)) {
            token = new JsonToken(JsonTokenType.TRUE, "true");
        } else if (isFalse(memory)) {
            token = new JsonToken(JsonTokenType.FALSE, "false");
        } else if (isNull(memory)) {
            token = new JsonToken(JsonTokenType.NULL, "null");
        }
        return token != null;
    }

    private void pushBack(int c) {
        if (pushedBack != -1) {
            throw new IllegalStateException("can push back only one character");
        } else {
            pushedBack = c;
        }
    }

    private char[] next(int count) {
        char[] ret = new char[count];
        for (int i = 0; i < count; i++) {
            ret[i] = (char) next();
        }

        return ret;
    }

    private int next() {
        if (pushedBack != -1) {
            int ret = pushedBack;
            pushedBack = -1;
            return ret;
        } else {
            try {
                return reader.read();
            } catch (IOException ioe) {
                throw new JsonParseException(ioe);
            }
        }
    }

    static final int TRUE_LENGTH = 4;
    static final int FALSE_LENGTH = 5;
    static final int NULL_LENGTH = 4;
    static final int INDEX_ZERO = 0;
    static final int INDEX_ONE = 1;
    static final int INDEX_TWO = 2;
    static final int INDEX_THREE = 3;
    static final int INDEX_FOUR = 4;

    private boolean isTrue(StringBuilder str) {
        return str.length() == TRUE_LENGTH
                && str.charAt(INDEX_ZERO) == 't'
                && str.charAt(INDEX_ONE) == 'r'
                && str.charAt(INDEX_TWO) == 'u'
                && str.charAt(INDEX_THREE) == 'e';
    }

    private boolean isFalse(StringBuilder str) {
        return str.length() == FALSE_LENGTH
                && str.charAt(INDEX_ZERO) == 'f'
                && str.charAt(INDEX_ONE) == 'a'
                && str.charAt(INDEX_TWO) == 'l'
                && str.charAt(INDEX_THREE) == 's'
                && str.charAt(INDEX_FOUR) == 'e';
    }

    private boolean isNull(StringBuilder str) {
        return str.length() == NULL_LENGTH
                && str.charAt(INDEX_ZERO) == 'n'
                && str.charAt(INDEX_ONE) == 'u'
                && str.charAt(INDEX_TWO) == 'l'
                && str.charAt(INDEX_THREE) == 'l';
    }
}

/**
 * ReaderState.
 */
enum ReaderState {
    NONE,
    OBJECT,
    ARRAY,
    PROPERTY
}

/**
 * JsonStreamReaderImpl.
 */
class JsonStreamReaderImpl implements JsonStreamReader {

    private JsonStreamTokenizerImpl tokenizer;
    private Stack<ReaderState> state = new Stack<ReaderState>();
    private Stack<Boolean> expectCommaOrEndStack = new Stack<Boolean>();
    private boolean expectCommaOrEnd;
    private boolean fireEndPropertyEvent;
    private JsonEvent previousEvent = null;

    JsonStreamReaderImpl(Reader reader) {
        this.state.push(ReaderState.NONE);
        this.tokenizer = new JsonStreamTokenizerImpl(reader);
    }

    @Override
    public boolean hasNext() {
        return tokenizer.hasNext();
    }

    @Override
    public JsonEvent nextEvent() {

        if (fireEndPropertyEvent) {
            if (state.peek() != ReaderState.PROPERTY) {
                throw new IllegalStateException("State is " + state.peek());
            }
            fireEndPropertyEvent = false;
            return createEndPropertyEvent(null);
        }

        if (hasNext()) {
            JsonToken token = tokenizer.nextToken();

            switch (state.peek()) {
            case NONE:
                if (token.getType() != JsonTokenType.LEFT_CURLY_BRACKET) {
                    throw new JsonParseException("no JSON format must start with {");
                } else {
                    return createStartObjectEvent();
                }

            case OBJECT:
                if (expectCommaOrEnd) {
                    if (token.getType() == JsonTokenType.COMMA) {
                        if (!tokenizer.hasNext()) {
                            throw new JsonParseException("no JSON format premature end");
                        }
                        token = tokenizer.nextToken();
                    } else if (token.getType() != JsonTokenType.RIGHT_CURLY_BRACKET) {
                        throw new JsonParseException("no JSON format expected , or ] got " + token.getType());
                    }
                    expectCommaOrEnd = false;
                }

                switch (token.getType()) {
                case STRING:
                    if (!tokenizer.hasNext() || tokenizer.nextToken().getType() != JsonTokenType.COLON) {
                        throw new JsonParseException("no JSON format : expected afer " + token.getValue());
                    }
                    expectCommaOrEnd = true;
                    return createStartPropertyEvent(token.getValue());
                case RIGHT_CURLY_BRACKET:
                    return createEndObjectEvent();
                default:
                    throw new JsonParseException("no JSON format");
                }

            case PROPERTY:
                switch (token.getType()) {
                case STRING:
                    return createEndPropertyEvent(token.getValue());
                case NUMBER:
                    return createEndPropertyEventNumber(token.getValue());
                case TRUE:
                case FALSE:
                    return createEndPropertyEventBoolean(token.getValue());
                case NULL:
                    return createEndPropertyEvent(null);
                case LEFT_CURLY_BRACKET:
                    return createStartObjectEvent();
                case LEFT_BRACKET:
                    return createStartArrayEvent();
                default:
                    throw new JsonParseException("no JSON format");
                }
            case ARRAY:
                if (expectCommaOrEnd) {
                    if (token.getType() == JsonTokenType.COMMA) {
                        if (!tokenizer.hasNext()) {
                            throw new JsonParseException("no JSON format premature end");
                        }
                        token = tokenizer.nextToken();
                    } else if (token.getType() != JsonTokenType.RIGHT_BRACKET) {
                        throw new JsonParseException("no JSON format expected , or ]");
                    }
                    expectCommaOrEnd = false;
                }

                switch (token.getType()) {
                case STRING:
                case NUMBER:
                case TRUE:
                case FALSE:
                    expectCommaOrEnd = true;
                    return createValueEvent(token.getValue());
                case NULL:
                    expectCommaOrEnd = true;
                    return createValueEvent(null);
                case LEFT_CURLY_BRACKET:
                    expectCommaOrEnd = true;
                    return createStartObjectEvent();
                case LEFT_BRACKET:
                    expectCommaOrEnd = true;
                    return createStartArrayEvent();
                case RIGHT_BRACKET:
                    return createEndArrayEvent();
                default:
                    break;
                }
            default:
                break;
            }
        }
        this.previousEvent = null;
        throw new RuntimeException("no event");
    }

    private JsonEvent createStartPropertyEvent(final String name) {
        state.push(ReaderState.PROPERTY);
        this.previousEvent = new JsonStartPropertyEventImpl() {
            @Override
            public String getName() {
                return name;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createEndPropertyEvent(final String value) {
        state.pop();
        this.previousEvent = new JsonEndPropertyEventImpl() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Object getObject() {
                return value;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createEndPropertyEventNumber(final String value) {
        state.pop();
        this.previousEvent = new JsonEndPropertyEventImpl() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Object getObject() {
                return Double.parseDouble(value);
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createEndPropertyEventBoolean(final String value) {
        state.pop();
        this.previousEvent = new JsonEndPropertyEventImpl() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Object getObject() {
                return Boolean.parseBoolean(value);
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createStartObjectEvent() {
        state.push(ReaderState.OBJECT);
        expectCommaOrEndStack.push(expectCommaOrEnd);
        expectCommaOrEnd = false;
        this.previousEvent = new JsonEventImpl() {
            @Override
            public boolean isStartObject() {
                return true;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createEndObjectEvent() {
        state.pop();
        expectCommaOrEnd = expectCommaOrEndStack.pop();

        // if the end of the object is also the of
        // a property, we need to fire the
        // endPropertyEvent before going forward.
        if (state.peek() == ReaderState.PROPERTY) {
            fireEndPropertyEvent = true;
        }

        this.previousEvent = new JsonEventImpl() {
            @Override
            public boolean isEndObject() {
                return true;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createStartArrayEvent() {
        state.push(ReaderState.ARRAY);
        expectCommaOrEndStack.push(expectCommaOrEnd);
        expectCommaOrEnd = false;
        this.previousEvent = new JsonEventImpl() {
            @Override
            public boolean isStartArray() {
                return true;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createEndArrayEvent() {
        state.pop();
        expectCommaOrEnd = expectCommaOrEndStack.pop();

        // if the end of the array is also the of
        // a property, we need to fire the
        // endPropertyEvent before going forward.
        if (state.peek() == ReaderState.PROPERTY) {
            fireEndPropertyEvent = true;
        }

        this.previousEvent = new JsonEventImpl() {
            @Override
            public boolean isEndArray() {
                return true;
            }
        };
        return this.previousEvent;
    }

    private JsonEvent createValueEvent(final String value) {
        this.previousEvent = new JsonValueEventImpl() {
            @Override
            public String getValue() {
                return value;
            }
        };
        return this.previousEvent;
    }

    @Override
    public void close() {
        tokenizer.close();
    }

    @Override
    public JsonEvent previousEvent() {
        return previousEvent;
    }

}
