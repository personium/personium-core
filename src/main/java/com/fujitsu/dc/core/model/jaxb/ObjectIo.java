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
package com.fujitsu.dc.core.model.jaxb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import com.sun.jersey.api.json.JSONUnmarshaller;

/**
 * JAXBを簡単に使うためのユーティリティクラス.
 */
public final class ObjectIo {
    private static JAXBContext context = null;
    private static JSONJAXBContext jsonContext = null;

    /**
     * コンストラクタ.
     */
    private ObjectIo() {

    }

    static {
        try {
            // com.fujitsu.dc.core.model.jaxb パッケージのcontextを作成
            context = JAXBContext.newInstance(ObjectIo.class.getPackage().getName());
            Map<String, String> ns2json = new HashMap<String, String>();
            ns2json.put("DAV:", "D");

            jsonContext = new JSONJAXBContext(JSONConfiguration.mapped().xml2JsonNs(ns2json).build(),
                    ObjectIo.class.getPackage().getName());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param <T> 戻り値としてほしいクラス
     * @param is XML入力ストリーム
     * @param elementClass 要素クラス
     * @return unmarshalされたクラス
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static <T> T unmarshal(final InputStream is, final Class<T> elementClass) throws IOException, JAXBException {
        return unmarshal(new BufferedReader(new InputStreamReader(is)), elementClass);
    }
    /**
     * @param <T> 戻り値としてほしいクラス
     * @param node Documentノード
     * @param elementClass 要素クラス
     * @return unmarshalされたクラス
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static <T> T unmarshal(final Node node, final Class<T> elementClass) throws IOException, JAXBException {
        return unmarshalNode(node, elementClass);
    }
    /**
     * @param <T> 戻り値としてほしいクラス
     * @param node XMLノード
     * @param elementClass 要素クラス
     * @return unmarshalされたクラス
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshalNode(final Node node, final Class<T> elementClass) throws IOException, JAXBException {
        Unmarshaller u = context.createUnmarshaller();
        Object object = u.unmarshal(node);
        if (object instanceof JAXBElement) {
            object = ((JAXBElement<?>) object).getValue();
        }
        return (T) object;
    }
    /**
     * @param <T> 戻り値としてほしいクラス
     * @param reader XML入力ストリーム
     * @param elementClass 要素クラス
     * @return unmarshalされたクラス
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(final Reader reader, final Class<T> elementClass) throws IOException, JAXBException {
        Unmarshaller u = context.createUnmarshaller();
        Object object = u.unmarshal(reader);
        if (object instanceof JAXBElement) {
            object = ((JAXBElement<?>) object).getValue();
        }
        return (T) object;
    }

    /**
     * @param instance オブジェクト
     * @param writer XML出力ストリーム
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static void marshal(
            final Object instance, final Writer writer) throws IOException, JAXBException {
        Marshaller m = context.createMarshaller();
        m.marshal(instance, writer);
    }

    /**
     * @param instance オブジェクト
     * @param outputStream XML出力ストリーム
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static void marshal(
            final Object instance,
            final OutputStream outputStream) throws IOException, JAXBException {
        Marshaller m = context.createMarshaller();
        m.marshal(instance, outputStream);
    }
    /**
     * @param instance オブジェクト
     * @param doc XMLドキュメント
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static void marshal(
            final Object instance, final Document doc) throws IOException, JAXBException {
        Marshaller m = context.createMarshaller();
        m.marshal(instance, doc);
    }
    /**
     * @param instance オブジェクト
     * @param writer JSON出力ストリーム
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static void toJson(
            final Object instance, final Writer writer) throws IOException, JAXBException {
        JSONMarshaller m = jsonContext.createJSONMarshaller();
        m.marshallToJSON(instance, writer);
    }
    /**
     * @param <T> 戻り値としてほしいクラス
     * @param reader XML入力ストリーム
     * @param elementClass 要素クラス
     * @return unmarshalされたクラス
     * @throws IOException IO上の問題があったとき投げられる例外
     * @throws JAXBException JAXB上の問題があったとき投げられる例外
     */
    public static <T> T fromJson(final Reader reader, final Class<T> elementClass) throws IOException, JAXBException {
        JSONUnmarshaller u = jsonContext.createJSONUnmarshaller();
        JAXBElement<T> object = u.unmarshalJAXBElementFromJSON(reader, elementClass);
        return object.getValue();
    }
}
