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
 * This code is based on JsonFormatWriter.java of odata4j-core, and some modifications
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
package com.fujitsu.dc.core.odata;

import java.io.Writer;
import java.util.Iterator;

import javax.ws.rs.core.UriInfo;

import org.odata4j.core.OCollection;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperty;
import org.odata4j.core.ORelatedEntitiesLinkInline;
import org.odata4j.core.ORelatedEntityLinkInline;
import org.odata4j.core.OSimpleObject;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.format.json.JsonFormatWriter;
import org.odata4j.format.json.JsonWriter;

import com.fujitsu.dc.core.rs.odata.AbstractODataResource;
import com.fujitsu.dc.core.rs.odata.ODataResource;

/**
 * DcJsonFormatWriterクラス.
 * @param <T>
 */
public abstract class DcJsonFormatWriter<T> extends JsonFormatWriter<T> {

    /**
     * "results".
     */
    protected static final String RESULTS_PROPERTY = "results";

    /**
     * コンストラクタ.
     * @param jsonpCallback コールバック
     */
    public DcJsonFormatWriter(String jsonpCallback) {
        super(jsonpCallback);
    }

    @Override
    public void write(UriInfo uriInfo, Writer w, T target) {
        JsonWriter jw = new DcJsonWriter(w);
        if (getJsonpCallback() != null) {
            jw.startCallback(getJsonpCallback());
        }

        jw.startObject();
        jw.writeName("d");
        writeContent(uriInfo, jw, target);
        jw.endObject();

        if (getJsonpCallback() != null) {
            jw.endCallback();
        }
    }

    @Override
    protected void writeOEntity(UriInfo uriInfo, JsonWriter jw, OEntity oe, EdmEntitySet ees, boolean isResponse) {

        jw.startObject();
        String baseUri = null;

        if (isResponse && null != oe.getEntityType()) {
            baseUri = uriInfo.getBaseUri().toString();

            jw.writeName("__metadata");

            jw.startObject();
            String absId = baseUri + getEntityRelId(oe);
            OEntityWrapper oew = (OEntityWrapper) oe;
            String etag = ODataResource.renderEtagHeader(oew.getEtag());

            jw.writeName("uri");
            jw.writeString(absId);
            // etagを返却する
            jw.writeSeparator();
            jw.writeName("etag");
            jw.writeString(etag);
            jw.writeSeparator();
            jw.writeName("type");
            jw.writeString(oe.getEntityType().getFullyQualifiedTypeName());
            jw.endObject();

            jw.writeSeparator();
        }

        writeOProperties(jw, oe.getProperties());
        writeLinks(jw, oe, uriInfo, isResponse);
        jw.endObject();
    }

    @Override
    protected void writeResponseLink(JsonWriter jw, OLink link, OEntity oe, UriInfo uriInfo) {
        jw.writeSeparator();
        jw.writeName(link.getTitle());
        if (link.isInline()) {
            if (link.isCollection()) {

                jw.startArray();
                if (((ORelatedEntitiesLinkInline) link).getRelatedEntities() != null) {
                    boolean isFirstInlinedEntity = true;
                    for (OEntity re : ((ORelatedEntitiesLinkInline) link).getRelatedEntities()) {

                        if (isFirstInlinedEntity) {
                            isFirstInlinedEntity = false;
                        } else {
                            jw.writeSeparator();
                        }

                        writeOEntity(uriInfo, jw, re, re.getEntitySet(), true);
                    }

                }
                jw.endArray();

            } else {
                OEntity re = ((ORelatedEntityLinkInline) link).getRelatedEntity();
                if (re == null) {
                    jw.writeNull();
                } else {
                    writeOEntity(uriInfo, jw, re, re.getEntitySet(), true);
                }
            }
        } else {
            // deferred
            jw.startObject();
            jw.writeName("__deferred");

            jw.startObject();
            String absId = uriInfo.getBaseUri().toString() + getEntityRelId(oe);
            jw.writeName("uri");
            //
            jw.writeString(absId + "/" + link.getTitle());
            jw.endObject();

            jw.endObject();
        }
    }

    @Override
    protected void writeProperty(JsonWriter jw, OProperty<?> prop) {
        jw.writeName(prop.getName());
        if (prop.getValue() != null && AbstractODataResource.isDummy(prop.getValue())) {
            writeValue(jw, prop.getType(), null);
        } else {
            writeValue(jw, prop.getType(), prop.getValue());
        }
    }

    /**
     * Collectionに対する応答データ作成.
     * @param jw JsonWriter
     * @param type コレクションタイプ
     * @param coll OCollection
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected void writeCollection(JsonWriter jw, EdmCollectionType type, OCollection<? extends OObject> coll) {
        // Ocollectionの応答データ作成時に[results]が追加されてしまうためオーバーライド
        jw.startArray();
        boolean isFirst = true;
        Iterator<? extends OObject> iter = coll.iterator();
        while (iter.hasNext()) {
            OObject obj = iter.next();
            if (isFirst) {
                isFirst = false;
            } else {
                jw.writeSeparator();
            }
            if (obj instanceof OComplexObject) {
                writeComplexObject(jw, obj.getType().getFullyQualifiedTypeName(),
                        ((OComplexObject) obj).getProperties());
            } else if (obj instanceof OSimpleObject) {
                writeValue(jw, obj.getType(), ((OSimpleObject) obj).getValue());
            }
        }
        jw.endArray();
    }

    /**
     * EntityRelIdを返却する.
     * @param oe OEntity
     * @return EntityRelId
     */
    public static String getEntityRelId(OEntity oe) {
        return getEntityRelId(oe.getEntitySet(), oe.getEntityKey());
    }

    /**
     * EntityRelIdを返却する.
     * @param entitySet エンティティセット
     * @param entityKey エンティティキー
     * @return EntityRelId
     */
    public static String getEntityRelId(EdmEntitySet entitySet, OEntityKey entityKey) {
        String key = AbstractODataResource.replaceDummyKeyToNull(entityKey.toKeyString());
        return entitySet.getName() + key;
    }

    /**
     * JSONのフィールド値をデータ型に合わせて出力する.
     * @param jw 出力先ライター
     * @param type フィールドのデータ型
     * @param pvalue フィールド値
     */
    @Override
    protected void writeValue(JsonWriter jw, EdmType type, Object pvalue) {
        if (pvalue != null && type.equals(EdmSimpleType.DOUBLE)) {
            ((DcJsonWriter) jw).writeNumber((Double) pvalue);
        } else {
            super.writeValue(jw, type, pvalue);
        }
    }
}
