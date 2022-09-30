/**
 * Personium
 * Copyright 2019-2022 Personium Project Authors
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
package io.personium.core.auth;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;

/**
 * a class for handling file storing the last authentication history.
 */
public class AuthHistoryLastFile {
    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(AuthHistoryLastFile.class);

    /** auth history files directory. */
    public static final String AUTH_HISTORY_DIRECTORY = ".pauthhistory";

    /** Metadata file name. */
    public static final String AUTH_HISTORY_LAST_FILE_NAME = "auth_history_last.json";

    /** Milliseconds to wait of metafile reading retries. */
    private static final long AUTH_HISTORY_LOAD_RETRY_WAIT = 100L;
    /** Maximum number of metafile reading retries. */
    private static final int AUTH_HISTORY_LOAD_RETRY_MAX = 5;

    /** data file. */
    File file;
    /** json object. */
    JSONObject json = new JSONObject();
    /** account ID. */
    String accountId;

    /**
     * constructor.
     */
    private AuthHistoryLastFile(String accountId, File file) {
        this.accountId = accountId;
        this.file = file;
    }

    /**
     * Factory method.
     * @param fsPath fs path
     * @param accountId account id
     * @return AuthHistoryLastFile
     */
    public static AuthHistoryLastFile newInstance(String fsPath, String accountId) {
        File file = new File(getFilePath(fsPath, accountId));
        AuthHistoryLastFile instanse = new AuthHistoryLastFile(accountId, file);
        return instanse;
    }

    /**
     * get filepath.
     * @param fsPath fs path
     * @param accountId account id
     * @return filepath
     */
    private static String getFilePath(String fsPath, String accountId) {
        StringBuilder path = new StringBuilder(fsPath);
        path.append(File.separatorChar);
        path.append(AUTH_HISTORY_DIRECTORY);
        path.append(File.separatorChar);
        path.append(accountId);
        path.append(File.separatorChar);
        path.append(AUTH_HISTORY_LAST_FILE_NAME);
        return path.toString();
    }

    /**
     * check exists file.
     * @return true if the file exists.
     */
    public boolean exists() {
        return this.file.exists();
    }

    /**
     * set default params.
     */
    public void setDefault() {
        this.setLastAuthenticated(null);
        this.setFailedCount(0L);
    }

    /**
     * load from the file.
     */
    public void load() {
        log.debug("load started. accountID:" + accountId);
        int retryCount = 0;
        while (true) {
            try {
                doLoad();
                break;
            } catch (PersoniumCoreException pe) {
                if (retryCount < AUTH_HISTORY_LOAD_RETRY_MAX) {
                    try {
                        Thread.sleep(AUTH_HISTORY_LOAD_RETRY_WAIT);
                    } catch (InterruptedException ie) {
                        // If sleep fails, Error
                        throw new RuntimeException(ie);
                    }
                    retryCount++;
                    log.info("file load retry. accountID:" + accountId + " RetryCount:" + retryCount);
                } else {
                    // IO failure or JSON is broken
                    throw pe;
                }
            }
        }
        log.debug("load ended.");
    }

    /**
     * load from the file.
     */
    private void doLoad() throws PersoniumCoreException {
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JSONParser parser = new JSONParser();
            this.json = (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            // IO failure or JSON is broken
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params("read auth history last file").reason(e);
        }
    }

    /**
     * save to the file.
     */
    public void save() {
        log.debug("save started. accountID:" + accountId);
        if (!this.file.getParentFile().exists() && !this.file.getParentFile().mkdirs()) {
            String message = "unable create directory: " + this.file.getParentFile().getAbsolutePath();
            throw PersoniumCoreException.Server.FILE_SYSTEM_ERROR.params(message);
        }
        String jsonStr = JSONObject.toJSONString(this.getJSON());
        try {
            if (PersoniumUnitConfig.getFsyncEnabled()) {
                Files.write(this.file.toPath(), jsonStr.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
            } else {
                Files.write(this.file.toPath(), jsonStr.getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.debug("save ended.");
    }

    /**
     * returns JSONObject representation of the file content.
     * @return JSONObject
     */
    public JSONObject getJSON() {
        return this.json;
    }

    /**
     * Returns the content of metadata as JSONString.
     * @return JSONString
     */
    public String toJSONString() {
        return this.json.toJSONString();
    }

    /**
     * Return the last authenticated.
     * @return the last authenticated
     */
    public Long getLastAuthenticated() {
        return (Long) this.json.get(OAuth2Helper.Key.LAST_AUTHENTICATED);
    }

    /**
     * Set up a last authenticated.
     * @param lastAuthenticated the last authenticated to set
     */
    @SuppressWarnings("unchecked")
    public void setLastAuthenticated(Long lastAuthenticated) {
        this.json.put(OAuth2Helper.Key.LAST_AUTHENTICATED, lastAuthenticated);
    }

    /**
     * Return the failed count.
     * @return the failed count
     */
    public Long getFailedCount() {
        return (Long) this.json.get(OAuth2Helper.Key.FAILED_COUNT);
    }

    /**
     * Set up a failed count.
     * @param failedCount the failed count to set
     */
    @SuppressWarnings("unchecked")
    public void setFailedCount(long failedCount) {
        this.json.put(OAuth2Helper.Key.FAILED_COUNT, failedCount);
    }

}
