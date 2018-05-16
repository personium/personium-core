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
package io.personium.core.bar;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import io.personium.common.es.util.IndexNameEncoder;
import io.personium.core.PersoniumCoreException;
import io.personium.core.auth.AccessContext;
import io.personium.core.event.EventBus;
import io.personium.core.event.PersoniumEvent;
import io.personium.core.model.ctl.ExtRole;
import io.personium.core.model.ctl.Relation;
import io.personium.core.model.ctl.Role;
import io.personium.core.model.progress.Progress;
import io.personium.core.model.progress.ProgressManager;

/**
 * Httpリクエストボディからbarファイルを読み込むためのクラス.
 */
public class BarFileUtils {

    static Logger log = LoggerFactory.getLogger(BarFileUtils.class);

    static final String CODE_BAR_INSTALL_COMPLETED = "PL-BI-0000";
    static final String CODE_BAR_INSTALL_FAILED = "PL-BI-0001";
    static final String CODE_INSTALL_STARTED = "PL-BI-1001";
    static final String CODE_INSTALL_PROCESSING = "PL-BI-1002";
    static final String CODE_INSTALL_COMPLETED = "PL-BI-1003";

    /** xml:baseのチェック用. 現状ほとんどの値を内部で置き換えてしまうのでチェックは緩くしている. */
    private static final String PATTERN_XML_BASE = "^(https?://.+)/([^/]{1,128})/__role/([^/]{1,128})/?$";

    private BarFileUtils() {
    }

    /**
     * 70_$links.jsonのFromName/ToNameに関する複合キーを取得する.
     * @param type FromType/ToNameに指定されたEntitySet名
     * @param names FromName/ToNameに指定されたEntityKey名
     * @param boxName Box名
     * @return Entity作成時に使用するEntityKey名
     */
    static String getComplexKeyName(final String type, final Map<String, String> names, final String boxName) {
        String keyname = null;
        // 複合キー
        if (type.equals(Role.EDM_TYPE_NAME) || type.equals(Relation.EDM_TYPE_NAME)) {
            keyname = String.format("(Name='%s',_Box.Name='%s')", names.get("Name"), boxName);
            // URI(ExtRole)
        } else if (type.equals(ExtRole.EDM_TYPE_NAME)) {
            keyname = String.format("(ExtRole='%s',_Relation.Name='%s',_Relation._Box.Name='%s')",
                    names.get("ExtRole"), names.get("_Relation.Name"), boxName);
            // その他
        } else {
            keyname = String.format("(Name='%s')", names.get("Name"));
        }

        return keyname;
    }

    /**
     * ACLの名前空間をバリデートする.
     * この際、ついでにロールインスタンスURLへの変換、BaseURLの変換を行う。
     * @param element element
     * @param baseUrl baseUrl
     * @param cellName cellName
     * @param boxName boxName
     * @return 生成したElementノード
     */
    static Element convertToRoleInstanceUrl(
            final Element element, final String baseUrl, final String cellName, final String boxName) {
        String namespaceUri = element.getAttribute("xml:base");
        if (StringUtils.isEmpty(namespaceUri)) {
            return null;
        }

        Pattern pattern = Pattern.compile(PATTERN_XML_BASE);
        Matcher m = pattern.matcher(namespaceUri);
        if (!m.matches()) {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(BarFileReadRunner.ROOTPROPS_XML);
        }

        String converted = getLocalUrl(baseUrl, cellName, boxName);
        Element retElement = (Element) element.cloneNode(true);
        retElement.setAttribute("xml:base", converted);

        return retElement;
    }

    /**
     * barファイル内に記載されたURLのホスト情報（scheme://hostname/)を処理中サーバの情報へ置換する.
     * @param baseUrl baseUrl
     * @param cellName cellName
     * @param boxName boxName
     * @return 生成したURL
     */
    private static String getLocalUrl(String baseUrl, String cellName, String boxName) {
        StringBuilder builder = new StringBuilder();
        builder.append(baseUrl);
        if (!baseUrl.endsWith("/")) {
            builder.append("/");
        }
        builder.append(cellName).append("/__role/").append(boxName).append("/");
        return builder.toString();
    }

    /**
     * Cellオーナー情報からUnitUser名を取得する.
     * @param owner オーナー情報(URL)
     * @return UnitUser名
     */
    static String getUnitUserName(final String owner) {
        String unitUserName = null;
        if (owner == null) {
            unitUserName = AccessContext.TYPE_ANONYMOUS;
        } else {
            unitUserName = IndexNameEncoder.encodeEsIndexName(owner);
        }
        return unitUserName;
    }

    /**
     * barファイルエントリからJSONファイルを読み込む.
     * @param <T> JSONMappedObject
     * @param inStream barファイルエントリのInputStream
     * @param entryName entryName
     * @param clazz clazz
     * @return JSONファイルから読み込んだオブジェクト
     * @throws IOException JSONファイル読み込みエラー
     */
    static <T> T readJsonEntry(
            InputStream inStream, String entryName, Class<T> clazz) throws IOException {
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        jp = f.createParser(inStream);
        JsonToken token = jp.nextToken(); // JSONルート要素（"{"）
        Pattern formatPattern = Pattern.compile(".*/+(.*)");
        Matcher formatMatcher = formatPattern.matcher(entryName);
        String jsonName = formatMatcher.replaceAll("$1");
        T json = null;
        if (token == JsonToken.START_OBJECT) {
            try {
                json = mapper.readValue(jp, clazz);
            } catch (UnrecognizedPropertyException ex) {
                throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
            }
        } else {
            throw PersoniumCoreException.BarInstall.JSON_FILE_FORMAT_ERROR.params(jsonName);
        }
        return json;
    }

    /**
     * 内部イベントとしてEventBusへインストール処理状況を出力する.
     * @param event Personium event object
     * @param eventBus Personium event bus for sending event
     * @param code 処理コード（ex. PL-BI-0000）
     * @param path barファイル内のエントリパス（Edmxの場合は、ODataのパス）
     * @param message 出力用メッセージ
     */
    static void outputEventBus(PersoniumEvent.Builder eventBuilder, EventBus eventBus, String code,
            String path, String message) {
        if (eventBuilder != null) {
            PersoniumEvent event = eventBuilder
                    .type(code)
                    .object(path)
                    .info(message)
                    .build();
            eventBus.post(event);
        }
    }

    /**
     * ProgressInfoへbarインストール状況を出力する.
     * @param isError エラー時の場合はtrueを、それ以外はfalseを指定する.
     * @param progressInfo bar install progress
     * @param code 処理コード（ex. PL-BI-0000）
     * @param message 出力用メッセージ
     */
    @SuppressWarnings("unchecked")
    static void writeToProgress(boolean isError, BarInstallProgressInfo progressInfo, String code, String message) {
        if (progressInfo != null && isError) {
            JSONObject messageJson = new JSONObject();
            JSONObject messageDetail = new JSONObject();
            messageJson.put("code", code);
            messageJson.put("message", messageDetail);
            messageDetail.put("lang", "en");
            messageDetail.put("value", message);
            progressInfo.setMessage(messageJson);
            writeToProgressCache(true, progressInfo);
        } else {
            writeToProgressCache(false, progressInfo);
        }
    }

    /**
     * キャッシュへbarインストール状況を出力する.
     * @param forceOutput 強制的に出力する場合はtrueを、それ以外はfalseを指定する
     * @param progressInfo bar install progress
     */
    static void writeToProgressCache(boolean forceOutput, BarInstallProgressInfo progressInfo) {
        if (progressInfo != null && progressInfo.isOutputEventBus() || forceOutput) {
            String key = "box-" + progressInfo.getBoxId();
            Progress progress = new Progress(key, progressInfo.toString());
            ProgressManager.putProgress(key, progress);
            log.info("Progress(" + key + "): " + progressInfo.toString());
        }
    }

}
