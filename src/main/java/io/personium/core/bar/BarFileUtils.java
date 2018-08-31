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
 * Http Class for reading bar files from the request body.
 */
public class BarFileUtils {

    static Logger log = LoggerFactory.getLogger(BarFileUtils.class);

    static final String CODE_BAR_INSTALL_COMPLETED = "PL-BI-0000";
    static final String CODE_BAR_INSTALL_FAILED = "PL-BI-0001";
    static final String CODE_INSTALL_STARTED = "PL-BI-1001";
    static final String CODE_INSTALL_PROCESSING = "PL-BI-1002";
    static final String CODE_INSTALL_COMPLETED = "PL-BI-1003";

    /** For checking xml: base Current status Most of the values ​​are replaced internally, so checking is made loose.*/
    private static final String PATTERN_XML_BASE = "^(https?://.+)/([^/]{1,128})/__role/([^/]{1,128})/?$";

    private BarFileUtils() {
    }

    /**
     * Get a composite key for FromName / ToName of 70 _ $ links.json.
     * @ param type FromType / EntitySet name specified in ToName
     * @ param names EntityKey name specified in FromName / ToName
     * @ param boxName Box name
     * @return EntityKey name to use when creating Entity
     */
    static String getComplexKeyName(final String type, final Map<String, String> names, final String boxName) {
        String keyname = null;
        //Compound key
        if (type.equals(Role.EDM_TYPE_NAME) || type.equals(Relation.EDM_TYPE_NAME)) {
            keyname = String.format("(Name='%s',_Box.Name='%s')", names.get("Name"), boxName);
            // URI(ExtRole)
        } else if (type.equals(ExtRole.EDM_TYPE_NAME)) {
            keyname = String.format("(ExtRole='%s',_Relation.Name='%s',_Relation._Box.Name='%s')",
                    names.get("ExtRole"), names.get("_Relation.Name"), boxName);
            //Other
        } else {
            keyname = String.format("(Name='%s')", names.get("Name"));
        }

        return keyname;
    }

    /**
     * Validate the namespace of the ACL.
     * At the same time, it converts to the role instance URL and converts the Base URL.
     * @param element element
     * @param baseUrl baseUrl
     * @param cellName cellName
     * @param boxName boxName
     * @return Generated Element node
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
     * Replace the host information (scheme: // hostname /) of the URL described in the bar file with the information of the server being processed.
     * @param baseUrl baseUrl
     * @param cellName cellName
     * @param boxName boxName
     * @return Generated URL
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
     * Get UnitUser name from Cell owner information.
     * @ param owner owner information (URL)
     * @return UnitUser name
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
     * bar Read JSON file from file entry.
     * @param <T> JSONMappedObject
     * @ param inStream bar File entry's InputStream
     * @param entryName entryName
     * @param clazz clazz
     * @return Object read from JSON file
     * @ throws IOException Error loading JSON file
     */
    static <T> T readJsonEntry(
            InputStream inStream, String entryName, Class<T> clazz) throws IOException {
        JsonParser jp = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory f = new JsonFactory();
        jp = f.createParser(inStream);
        JsonToken token = jp.nextToken(); //JSON root element ("{")
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
     * Output the installation processing status to EventBus as an internal event.
     * @param event Personium event object
     * @param eventBus Personium event bus for sending event
     * @ param code processing code (ex. PL - BI - 0000)
     * @ param path bar Entry path in the file (in the case of Edmx, the path of OData)
     * @ param message Output message
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
     * Output bar installation status to ProgressInfo.
     * @ param isError Specify true on error, false otherwise.
     * @param progressInfo bar install progress
     * @ param code processing code (ex. PL - BI - 0000)
     * @ param message Output message
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
     * Output bar installation status to cache.
     * @ param forceOutput Specify true to forcibly output, false otherwise
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
