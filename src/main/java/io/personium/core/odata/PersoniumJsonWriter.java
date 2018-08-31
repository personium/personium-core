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
 * This code is based on JsonWriter.java of odata4j-core, and some modifications
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
import java.io.Writer;
import java.text.DecimalFormat;

import org.odata4j.core.Throwables;
import org.odata4j.format.json.JsonWriter;

/**
 *JsonWriter wrapper class.
 */
public class PersoniumJsonWriter extends JsonWriter {

    private static final int MAX_INTEGER_DIGITS = 15;
    private static final int MAX_FRACTION_DIGITS = 14;

    private final Writer writer;

    /**
     *constructor.
     *@ param writer writer
     */
    public PersoniumJsonWriter(Writer writer) {
        super(writer);
        this.writer = writer;
    }

    /**
     *Double type field output.
     *@ param value field value
     */
    public void writeNumber(double value) {
        try {
            this.writer.write(formatDoubleValue(value));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     *Format Double value in default format.
     *Convert to fixed-point representation in a range where information does not occur
     *@ param value Value to be formatted
     *@return shaping result
     */
    private String formatDoubleValue(double value) {
        //Generate character string converted to fixed decimal representation
        DecimalFormat format = new DecimalFormat("#.#");
        format.setMaximumIntegerDigits(MAX_INTEGER_DIGITS);
        format.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
        String fomattedValue = format.format(value);

        //Convert the character string converted to fixed decimal representation to Double type once
        //If there is information loss, return the original value
        String result = fomattedValue;
        if (value != Double.parseDouble(fomattedValue)) {
            result = Double.toString(value);
        }
        return result;
    }

    @Override
    public void startObject() {
        try {
            writer.write("{");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void endObject() {
        try {
            writer.write("}");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void writeName(String name) {
        try {
            writer.write("\"" + encode(name) + "\":");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void startArray() {
        try {
            writer.write("[");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void endArray() {
        try {
            writer.write("]");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void writeSeparator() {
        try {
            writer.write(",");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void writeString(String value) {
        try {
            writer.write("\"" + encode(value) + "\"");
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private String encode(String unencoded) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unencoded.length(); i++) {
            char c = unencoded.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\f') {
                sb.append("\\f");
            } else if (c == '\b') {
                sb.append("\\b");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '/') {
                sb.append("\\/");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
