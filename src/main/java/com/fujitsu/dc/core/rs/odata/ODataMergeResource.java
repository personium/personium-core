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
package com.fujitsu.dc.core.rs.odata;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;

import com.fujitsu.dc.core.DcCoreException;
import com.fujitsu.dc.core.auth.AccessContext;
import com.fujitsu.dc.core.model.ctl.Common;
import com.fujitsu.dc.core.odata.OEntityWrapper;

/**
 * ODataのEntityリソースのMERGEメソッドを扱うJAX-RS リソース.
 */
public class ODataMergeResource extends ODataEntityResource {

    private final String keyString;
    private final ODataResource odataResource;
    private final AccessContext accessContext;
    private OEntityKey oEntityKey;

    /**
     * コンストラクタ.
     * @param odataResource 親リソースであるODataResource
     * @param entitySetName EntitySet Name
     * @param key キー文字列
     */
    public ODataMergeResource(ODataResource odataResource, String entitySetName, String key) {
        super();

        this.odataResource = odataResource;
        this.accessContext = this.odataResource.accessContext;
        setOdataProducer(this.odataResource.getODataProducer());
        setEntitySetName(entitySetName);

        this.keyString = key;

        try {
            this.oEntityKey = OEntityKey.parse(this.keyString);
        } catch (IllegalArgumentException e) {
            throw DcCoreException.OData.ENTITY_KEY_PARSE_ERROR.reason(e);
        }
    }

    /**
     * MERGE メソッドの処理.
     * @param reader リクエストボディ
     * @param accept Accept ヘッダ
     * @param ifMatch If-Match ヘッダ
     * @return JAX-RSResponse
     */
    public Response merge(Reader reader,
            @HeaderParam(HttpHeaders.ACCEPT) final String accept,
            @HeaderParam(HttpHeaders.IF_MATCH) final String ifMatch) {
        // メソッド実行可否チェック
        checkNotAllowedMethod();

        // アクセス制御
        this.odataResource.checkAccessContext(this.accessContext,
                this.odataResource.getNecessaryWritePrivilege(getEntitySetName()));

        // リクエストからOEntityWrapperを作成する.
        OEntity oe = this.createRequestEntity(reader, this.oEntityKey);
        OEntityWrapper oew = new OEntityWrapper(null, oe, null);

        // 必要ならばメタ情報をつける処理
        this.odataResource.beforeMerge(oew, this.oEntityKey);

        // If-Matchヘッダで入力されたETagをMVCC用での衝突検知用にOEntityWrapperに設定する。
        String etag = ODataResource.parseEtagHeader(ifMatch);
        oew.setEtag(etag);

        // MERGE処理をODataProducerに依頼。
        // こちらでリソースの存在確認もしてもらう。
        getOdataProducer().mergeEntity(getEntitySetName(), this.oEntityKey, oew);

        // 特に例外があがらなければ、レスポンスを返す。
        // oewに新たに登録されたETagを返す
        etag = oew.getEtag();
        return Response.noContent()
                .header(HttpHeaders.ETAG, ODataResource.renderEtagHeader(etag))
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString)
                .build();

    }

    /**
     * スキーマ定義をもとにOPropertyにデフォルト値を設定. <br />
     * MERGEの場合、キー, updated, published以外の項目にデフォルト値は設定しない
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     * @param metadata EdmDataServicesスキーマ定義
     * @return Oproperty
     */
    @Override
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op, EdmDataServices metadata) {

        if (metadata != null) {
            // スキーマ情報の取得
            EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(getEntitySetName());
            EdmEntityType edmEntityType = edmEntitySet.getType();
            // スキーマに定義されたキーリストを取得
            List<String> keysDefined = edmEntityType.getKeys();
            String epName = ep.getName();

            // キー, updated, published以外の項目にデフォルト値は設定しない
            if (!keysDefined.contains(epName) && !Common.P_PUBLISHED.getName().equals(epName)
                    && !Common.P_UPDATED.getName().equals(epName)) {
                return null;
            }
        }

        return super.setDefaultValue(ep, propName, op, metadata);
    }

    /**
     * ComplexTypeスキーマを参照して、必須チェックとデフォルト値の設定を行う.
     * @param metadata スキーマ情報
     * @param edmComplexType ComplexTypeのスキーマ情報
     * @param complexProperties ComplexTypePropertyのList
     * @return デフォルト値を設定したComplexTypeプロパティの一覧
     */
    @Override
    protected List<OProperty<?>> createNewComplexProperties(EdmDataServices metadata,
            EdmComplexType edmComplexType,
            Map<String, OProperty<?>> complexProperties) {
        // ComplexTypeスキーマを参照して、必須チェックとデフォルト値の設定を行う
        List<OProperty<?>> newComplexProperties = new ArrayList<OProperty<?>>();
        for (EdmProperty ctp : edmComplexType.getProperties()) {
            // プロパティ情報を取得する
            String compPropName = ctp.getName();
            OProperty<?> complexProperty = complexProperties.get(compPropName);
            if (ctp.getType().isSimple()) {
                // シンプル型の場合
                // MERGEの場合はデフォルト値を設定しない
                if (complexProperty == null) {
                    continue;
                } else if (complexProperty.getValue() == null) {
                    // Nullableチェック
                    complexProperty = setDefaultValue(ctp, compPropName, complexProperty);
                }
            } else {
                // Complex型の場合
                complexProperty = getComplexProperty(ctp, compPropName, complexProperty, metadata);
            }
            if (complexProperty != null) {
                // MERGEリクエストでは、ComplexTypeのPropertyが指定されていない場合は無視する
                newComplexProperties.add(complexProperty);
            }
        }
        return newComplexProperties;
    }

}
