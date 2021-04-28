/**
 * Personium
 * Copyright 2019-2021 Personium Project Authors
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
package io.personium.core.model.impl.fs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.personium.core.PersoniumCoreException;
import io.personium.core.PersoniumUnitConfig;

/**
 * Class dealing with cell specific key information.
 */
public class CellKeysFile {
    /** Logger. */
    private static Logger log = LoggerFactory.getLogger(CellKeysFile.class);

    /** Algorithm at key creation. */
    private static final String KEY_ALGORITHM = "RSA";
    /** Key length at key creation. */
    private static final int KEY_SIZE = 2048;
    /** Public key file name. */
    public static final String PUBLIC_KEY_NAME = ".publickey";
    /** Private key file name. */
    public static final String PRIVATE_KEY_NAME = ".privatekey";

    /** Milliseconds to wait of metafile reading retries. */
    private static final long META_LOAD_RETRY_WAIT = 100L;
    /** Maximum number of metafile reading retries. */
    private static final int META_LOAD_RETRY_MAX = 5;

    /** Keys file storage directory path. Directories of keyId is created under this. */
    private Path keysDirPath;
    /** Key ID. */
    private String keyId;
    /** Public key. */
    private byte[] publicKey;
    /**  Private key. */
    private byte[] privateKey;

    /**
     * Constructor.
     * @param keysDirPath Keys file storage directory path
     */
    private CellKeysFile(Path keysDirPath) {
        this.keysDirPath = keysDirPath;
    }

    /**
     * Constructor.
     * @param keysDirPath Keys file storage directory path
     * @param keyId Key ID
     */
    private CellKeysFile(Path keysDirPath, String keyId) {
        this.keysDirPath = keysDirPath;
        this.keyId = keyId;
    }

    /**
     * Create new keys and return it.<br>
     * In this method keys is not saved in the file.<br>
     * If you want to save, please use the save() method.
     * @param keysDirPath Keys file storage directory path.
     *        Directories of keyId is created under this.
     * @return Created new instance.
     */
    public static CellKeysFile newInstance(Path keysDirPath) { // CHECKSTYLE IGNORE
        CellKeysFile cellKeysFile = new CellKeysFile(keysDirPath);
        try {
            cellKeysFile.createKeys();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Usually it is impossible to go through this route.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
        return cellKeysFile;
    }

    /**
     * Read keys from the file and return it.
     * @param keysDirPath Keys file storage directory path.
     * @param keyId Key ID
     * @return Loaded instance
     */
    public static CellKeysFile load(Path keysDirPath, String keyId) { // CHECKSTYLE IGNORE
        CellKeysFile cellKeysFile = new CellKeysFile(keysDirPath, keyId);

        int retryCount = 0;
        while (true) {
            try {
                cellKeysFile.loadKeys();
                break;
            } catch (PersoniumCoreException pe) {
                if (retryCount < META_LOAD_RETRY_MAX) {
                    try {
                        Thread.sleep(META_LOAD_RETRY_WAIT);
                    } catch (InterruptedException ie) {
                        // If sleep fails, Error
                        throw new RuntimeException(ie);
                    }
                    retryCount++;
                    log.info("Keys file load retry. RetryCount:" + retryCount);
                } else {
                    // IO failure.
                    throw pe;
                }
            }
        }

        return cellKeysFile;
    }

    /**
     * Save keys to the file.
     */
    public void save() {
        Path keyIdDirPath = keysDirPath.resolve(keyId);
        if (!Files.exists(keyIdDirPath)) {
            try {
                Files.createDirectories(keyIdDirPath);
            } catch (IOException e) {
                throw PersoniumCoreException.Common.FILE_IO_ERROR.params("create KeyIdDir").reason(e);
            }
        }
        Path publicKeyPath = keyIdDirPath.resolve(PUBLIC_KEY_NAME);
        Path privateKeyPath = keyIdDirPath.resolve(PRIVATE_KEY_NAME);
        writeFile(publicKeyPath, publicKey);
        writeFile(privateKeyPath, privateKey);
    }

    /**
     * Get Key ID.
     * @return Key ID
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Get Public key.
     * @return Public key
     */
    public byte[] getPublicKeyBytes() {
        return publicKey;
    }

    /**
     * Get Public key.
     * @return Public key
     */
    public PublicKey getPublicKey() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKey);
            return keyFactory.generatePublic(publicKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Usually it is impossible to go through this route.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
    }

    /**
     * Get Private key.
     * @return Private key
     */
    public byte[] getPrivateKeyBytes() {
        return privateKey;
    }

    /**
     * Get Private key.
     * @return Private key
     */
    public PrivateKey getPrivateKey() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKey);
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Usually it is impossible to go through this route.
            throw PersoniumCoreException.Server.UNKNOWN_ERROR;
        }
    }

    /**
     * Create new keys.
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    private void createKeys() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(KEY_SIZE);
        KeyPair keyPair = generator.generateKeyPair();

        KeyFactory factoty = KeyFactory.getInstance(KEY_ALGORITHM);

        RSAPublicKeySpec publicKeySpec = factoty.getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);
        RSAPrivateKeySpec privateKeySpec = factoty.getKeySpec(keyPair.getPrivate(), RSAPrivateKeySpec.class);

        publicKey = factoty.generatePublic(publicKeySpec).getEncoded();
        privateKey = factoty.generatePrivate(privateKeySpec).getEncoded();
        keyId = UUID.randomUUID().toString();
    }

    /**
     * Load keys.
     */
    private void loadKeys() {
        Path publicKeyPath = keysDirPath.resolve(keyId).resolve(PUBLIC_KEY_NAME);
        Path privateKeyPath = keysDirPath.resolve(keyId).resolve(PRIVATE_KEY_NAME);
        publicKey = readFile(publicKeyPath);
        privateKey = readFile(privateKeyPath);
    }

    /**
     * Read file data.
     * @param filePath Target file path
     * @return file data
     */
    private byte[] readFile(Path filePath) {
        if (!Files.exists(filePath)) {
            throw PersoniumCoreException.Dav.RESOURCE_NOT_FOUND.params(filePath.getFileName());
        }
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            StringBuilder builder = new StringBuilder();
            builder.append("reading ").append(filePath.getFileName());
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params(builder.toString()).reason(e);
        }
    }

    /**
     * Write file data.
     * @param filePath Target file path
     * @param data file data
     */
    private void writeFile(Path filePath, byte[] data) {
        try (OutputStream out = Files.newOutputStream(filePath, getWriteOptions())) {
            out.write(data);
        } catch (IOException e) {
            StringBuilder builder = new StringBuilder();
            builder.append("writing ").append(filePath.getFileName());
            throw PersoniumCoreException.Common.FILE_IO_ERROR.params(builder.toString()).reason(e);
        }
    }

    /**
     * get options for write.
     * @return options specifying how the file is opened
     */
    private OpenOption[] getWriteOptions() {
        if (PersoniumUnitConfig.getFsyncEnabled()) {
            OpenOption[] options = {
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.SYNC};
            return options;
        }
        return new OpenOption[0];
    }

}
