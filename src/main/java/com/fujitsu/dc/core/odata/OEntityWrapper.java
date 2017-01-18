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
package com.fujitsu.dc.core.odata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OExtension;
import org.odata4j.core.OLink;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmType;

/**
 * OData4J の OEntity に Etag情報と独自メタデータを扱う力を持たせたクラス.
 */
public final class OEntityWrapper implements OEntity {
    String uuid;
    OEntity core;
    String etag; // Opaque Etagの中身 ー W/"(中身)"
    Map<String, Object> metadata = new HashMap<String, Object>();
    Map<String, Object> manyToOneLinks = new HashMap<String, Object>();

    /**
     * コンストラクタ.
     * @param uuid UUID
     * @param oEntity OEntityオブジェクト
     * @param etag ETag
     */
    public OEntityWrapper(final String uuid, final OEntity oEntity, final String etag) {
        this.uuid = uuid;
        this.core = oEntity;
        this.etag = etag;
    }
    /**
     * キーを指定して独自メタデータを取得します.
     * @param key メタデータのキー
     * @return メタデータの値
     */
    public Object get(final String key) {
        return this.metadata.get(key);
    }
    /**
     * 独自メタデータを設定します.
     * @param key メタデータのキー
     * @param val メタデータの値
     */
    public void put(final String key, final Object val) {
        this.metadata.put(key, val);
    }
    /**
     * 独自メタデータをすべて取得します.
     * @return 独自メタデータ
     */
    public Map<String, Object> getMetadata() {
        return this.metadata;
    }

    @Override
    public String getEntitySetName() {
        return this.core.getEntitySetName();
    }

    @Override
    public OEntityKey getEntityKey() {
        return this.core.getEntityKey();
    }

    @Override
    public List<OProperty<?>> getProperties() {
        return this.core.getProperties();
    }


    @Override
    public OProperty<?> getProperty(final String propName) {
        try {
            return this.core.getProperty(propName);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> OProperty<T> getProperty(final String propName, final Class<T> propClass) {
        return this.core.getProperty(propName, propClass);
    }

    @Override
    public EdmType getType() {
        return this.core.getType();
    }

    @Override
    public EdmEntitySet getEntitySet() {
        return this.core.getEntitySet();
    }

    @Override
    public List<OLink> getLinks() {
        return this.core.getLinks();
    }

    @Override
    public <T extends OLink> T getLink(final String title, final Class<T> linkClass) {
        return this.core.getLink(title, linkClass);
    }
    @Override
    public <TExtension extends OExtension<OEntity>> TExtension findExtension(final Class<TExtension> arg0) {
        return this.core.findExtension(arg0);
    }
    @Override
    public EdmEntityType getEntityType() {
        return this.core.getEntityType();
    }

    /**
     * @return 内部ID
     */
    public String getUuid() {
        return uuid;
    }
    /**
     * @param uuid the uuid to set
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }
    /**
     * @param etag the etag to set
     */
    public void setEtag(final String etag) {
        this.etag = etag;
    }
    /**
     * Etagを返します.
     * @return Etag
     */
    public String getEtag() {
        return this.etag;
    }

    /**
     * linksを設定します.
     * @param paramManyToOneLinks リンク情報
     */
    public void setManyToOneLinks(final Map<String, Object> paramManyToOneLinks) {
        this.manyToOneLinks = paramManyToOneLinks;
    }

    /**
     * 指定されたキーのlinkのUUIDを返します.
     * @param key リンクキー
     * @return linkUUID
     */
    public String getLinkUuid(String key) {
        return (String) this.manyToOneLinks.get(key);
    }
}
