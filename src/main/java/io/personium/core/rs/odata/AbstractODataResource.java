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
package io.personium.core.rs.odata;

import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.http.HttpStatus;
import org.odata4j.core.NamedValue;
import org.odata4j.core.NamespacedAnnotation;
import org.odata4j.core.OCollection;
import org.odata4j.core.OCollections;
import org.odata4j.core.OComplexObject;
import org.odata4j.core.OComplexObjects;
import org.odata4j.core.ODataConstants;
import org.odata4j.core.ODataVersion;
import org.odata4j.core.OEntities;
import org.odata4j.core.OEntity;
import org.odata4j.core.OEntityKey;
import org.odata4j.core.OLink;
import org.odata4j.core.OObject;
import org.odata4j.core.OProperties;
import org.odata4j.core.OProperty;
import org.odata4j.edm.EdmCollectionType;
import org.odata4j.edm.EdmComplexType;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmEntityType;
import org.odata4j.edm.EdmProperty;
import org.odata4j.edm.EdmProperty.CollectionKind;
import org.odata4j.edm.EdmSimpleType;
import org.odata4j.edm.EdmType;
import org.odata4j.format.Entry;
import org.odata4j.format.FormatParser;
import org.odata4j.format.FormatWriter;
import org.odata4j.format.Settings;
import org.odata4j.producer.EntityResponse;

import io.personium.common.es.util.DcUUID;
import io.personium.core.DcCoreConfig;
import io.personium.core.DcCoreException;
import io.personium.core.model.ctl.Common;
import io.personium.core.model.ctl.Property;
import io.personium.core.model.impl.es.odata.PropertyLimitChecker;
import io.personium.core.model.impl.es.odata.PropertyLimitChecker.CheckError;
import io.personium.core.odata.DcFormatParserFactory;
import io.personium.core.odata.DcFormatWriterFactory;
import io.personium.core.odata.DcODataProducer;
import io.personium.core.odata.OEntityWrapper;
import io.personium.core.utils.EscapeControlCode;
import io.personium.core.utils.ODataUtils;

/**
 * ODataのリソースを扱う抽象クラス.
 */
public abstract class AbstractODataResource {

    /**
     * エンティティセット名.
     */
    private String entitySetName;

    /**
     * ダミーキー名.
     */
    public static final String DUMMY_KEY = "key_dummy@";

    /**
     * ODataProducer.
     */
    private DcODataProducer odataProducer;

    /** $formatのJSON. */
    public static final String FORMAT_JSON = "json";
    /** $formatのatom. */
    public static final String FORMAT_ATOM = "atom";

    /** データ格納時の時刻(リクエストボディに"SYSUTCDATETIME()"が指定された場合はDcJsonFromatParserクラスで設定). */
    private long currentTimeMillis = System.currentTimeMillis();

    /**
     * entitySetNameのセッター.
     * @param entitySetName エンティティセット名
     */
    public void setEntitySetName(String entitySetName) {
        this.entitySetName = entitySetName;
    }

    /**
     * odataProducerのゲッター.
     * @return odataProducer
     */
    public DcODataProducer getOdataProducer() {
        return this.odataProducer;
    }

    /**
     * odataProducerのセッター.
     * @param odataProducer odataProducer
     */
    public void setOdataProducer(DcODataProducer odataProducer) {
        this.odataProducer = odataProducer;
    }

    /**
     * entitySetNameのゲッター.
     * @return エンティティセット名
     */
    public String getEntitySetName() {
        return this.entitySetName;
    }

    /**
     * 返すべきContentTypeを決定します.
     * @param accept Accept ヘッダの内容
     * @param format $format パラメタ
     * @return 返すべきContent-Type
     */
    public final MediaType decideOutputFormat(final String accept, final String format) {
        MediaType mediaType = null;
        if (format != null) {
            mediaType = decideOutputFormatFromQueryValue(format);
        } else if (accept != null) {
            mediaType = decideOutputFormatFromHeaderValues(accept);
        }
        if (mediaType == null) {
            // set default.
            mediaType = MediaType.APPLICATION_ATOM_XML_TYPE;
        }
        return mediaType;
    }

    /**
     * クエリでの指定($format)から出力フォーマットを決定する.
     * @param format $formatの指定値
     * @return 出力フォーマット("application/json" or "application/atom+xml")
     */
    private MediaType decideOutputFormatFromQueryValue(String format) {
        MediaType mediaType = null;

        if (format.equals(FORMAT_ATOM)) {
            // $formatの指定がatomである場合
            mediaType = MediaType.APPLICATION_ATOM_XML_TYPE;
        } else if (format.equals(FORMAT_JSON)) {
            mediaType = MediaType.APPLICATION_JSON_TYPE;
        } else {
            throw DcCoreException.OData.FORMAT_INVALID_ERROR.params(format);
        }

        return mediaType;
    }

    /**
     * Acceptヘッダの指定から出力フォーマットを決定する.
     * @param format Acceptヘッダの指定値
     * @return 出力フォーマット("application/json" or "application/atom+xml")
     */
    private MediaType decideOutputFormatFromHeaderValues(String acceptHeaderValue) {
        MediaType mediaType = null;
        StringTokenizer st = new StringTokenizer(acceptHeaderValue, ",");
        while (st.hasMoreTokens()) {
            String accept = truncateAfterSemicolon(st.nextToken());
            if (isAcceptXml(accept)) {
                mediaType = MediaType.APPLICATION_ATOM_XML_TYPE;
            } else if (isAcceptJson(accept)) {
                if (mediaType == null) {
                    mediaType = MediaType.APPLICATION_JSON_TYPE;
                }
            } else {
                throw DcCoreException.OData.UNSUPPORTED_MEDIA_TYPE.params(acceptHeaderValue);
            }
        }
        return mediaType;
    }

    /**
     * 入力文字列からセミコロン以降を切り捨てる.
     * @param source Acceptヘッダの指定値をカンマで分割した文字列
     * @return セミコロンまでの文字列
     */
    private String truncateAfterSemicolon(String source) {
        String result = source;
        int index = source.indexOf(";");
        if (index >= 0) {
            result = source.substring(0, index);
        }
        return result;
    }

    private boolean isAcceptXml(String accept) {
        return (accept.equals(MediaType.APPLICATION_ATOM_XML)
                || accept.equals(MediaType.APPLICATION_XML)
                || accept.equals(MediaType.WILDCARD));
    }

    private boolean isAcceptJson(String accept) {
        return accept.equals(MediaType.APPLICATION_JSON);
    }

    /**
     * Entityの作成を Producerに依頼.
     * @param reader リクエストボディ
     * @param odataResource ODataリソース
     * @return EntityResponse
     */
    protected EntityResponse createEntity(final Reader reader, ODataCtlResource odataResource) {
        OEntityWrapper oew = getOEntityWrapper(reader, odataResource, null);

        // Entityの作成を Producerに依頼.この中であわせて、存在確認もしてもらう。
        EntityResponse res = getOdataProducer().createEntity(getEntitySetName(), oew);
        return res;
    }

    /**
     * リクエストボディからOEntityWrapperを取得する.
     * @param reader リクエストボディ
     * @param odataResource ODataリソース
     * @param metadata スキーマ定義
     * @return OEntityWrapper
     */
    public OEntityWrapper getOEntityWrapper(final Reader reader,
            ODataCtlResource odataResource,
            EdmDataServices metadata) {
        // 登録すべきOEntityを作成
        OEntity newEnt;
        if (metadata == null) {
            newEnt = createRequestEntity(reader, null);
        } else {
            newEnt = createRequestEntity(reader, null, metadata);
        }

        // ラッパにくるむ. POSTでIf-Match等 ETagを受け取ることはないのでetagはnull。
        String uuid = DcUUID.randomUUID();
        OEntityWrapper oew = new OEntityWrapper(uuid, newEnt, null);
        // 必要ならばメタ情報をつける処理
        odataResource.beforeCreate(oew);

        return oew;
    }

    /**
     * 入力ボディからOEntityオブジェクトを作成する.
     * このメソッドではロック処理をかけることができないので、データの存在チェック等はしない。
     * @param reader リクエストボディ
     * @param oEntityKey 更新対象のentityKey。新規作成時はnullを指定する
     * @return ODataエンティティ
     */
    protected OEntity createRequestEntity(final Reader reader, OEntityKey oEntityKey) {
        // スキーマ情報の取得
        EdmDataServices metadata = this.odataProducer.getMetadata();
        return createRequestEntity(reader, oEntityKey, metadata);
    }

    /**
     * 入力ボディからOEntityオブジェクトを作成する.
     * このメソッドではロック処理をかけることができないので、データの存在チェック等はしない。
     * @param reader リクエストボディ
     * @param oEntityKey 更新対象のentityKey。新規作成時はnullを指定する
     * @param metadata EdmDataServicesスキーマ定義
     * @return ODataエンティティ
     */
    protected OEntity createRequestEntity(final Reader reader, OEntityKey oEntityKey, EdmDataServices metadata) {
        return createRequestEntity(
                reader,
                oEntityKey,
                metadata,
                this.entitySetName);
    }

    /**
     * 入力ボディからOEntityオブジェクトを作成する.
     * このメソッドではロック処理をかけることができないので、データの存在チェック等はしない。
     * @param reader リクエストボディ
     * @param oEntityKey 更新対象のentityKey。新規作成時はnullを指定する
     * @param metadata EdmDataServicesスキーマ定義
     * @param entitySetNameParam EntitySet名
     * @return ODataエンティティ
     */
    protected OEntity createRequestEntity(final Reader reader,
            OEntityKey oEntityKey,
            EdmDataServices metadata,
            String entitySetNameParam) {
        // スキーマ情報の取得
        EdmEntitySet edmEntitySet = metadata.findEdmEntitySet(entitySetNameParam);
        EdmEntityType edmEntityType = edmEntitySet.getType();
        // スキーマに定義されたキーリストを取得
        List<String> keysDefined = edmEntityType.getKeys();

        // リクエストからとりあえずの仮Ontityオブジェクトを作成する。
        // このOEntityには、必須項目が入っていないような可能性もある。
        OEntity reqEntity = createOEntityFromRequest(keysDefined, metadata, reader, entitySetNameParam);
        List<OLink> links = reqEntity.getLinks();

        // TODO Staticなスキーマチェックとデフォルト値設定を行う。
        List<OProperty<?>> props = new ArrayList<OProperty<?>>();
        List<String> schemaProps = new ArrayList<String>();

        // 主キーのスキーマチェック
        if (oEntityKey != null) {
            validatePrimaryKey(oEntityKey, edmEntityType);
        }

        for (EdmProperty ep : edmEntityType.getProperties()) {
            String propName = ep.getName();
            schemaProps.add(propName);
            OProperty<?> op = null;
            try {
                // リクエストOEntityから該当プロパティを取得
                op = reqEntity.getProperty(propName);
                // リクエストボディに__published、__updatedが指定されていた場合400エラーを返却する
                if (op != null && (propName.equals(Common.P_PUBLISHED.getName())
                        || propName.equals(Common.P_UPDATED.getName()))) {
                    throw DcCoreException.OData.FIELED_INVALID_ERROR
                            .params(propName + " is management information name. Cannot request.");
                }

                if (ep.getType().isSimple()) {
                    // シンプル型の場合
                    op = getSimpleProperty(ep, propName, op);
                } else {
                    // Complex型の場合
                    op = getComplexProperty(ep, propName, op, metadata);
                }
            } catch (DcCoreException e) {
                throw e;
            } catch (Exception e) {
                op = setDefaultValue(ep, propName, op, metadata);
            }

            // 入力があったので値のチェック処理に進む。
            if (op != null && op.getValue() != null) {
                validateProperty(ep, propName, op);
            }

            if (op != null) {
                props.add(op);
            }
        }
        // DynamicPropertyの値を設定する
        int dynamicPropCount = 0;
        for (OProperty<?> property : reqEntity.getProperties()) {

            String req = property.getName();
            if (req.equals("__metadata")) {
                // リクエストボディに__metadataが指定されていた場合400エラーを返却する
                throw DcCoreException.OData.FIELED_INVALID_ERROR.params(req
                        + " is management information name. Cannot request.");
            }
            // EntityTypeに存在しないdynamicPropertyが出現した場合は、スキーマを追加登録するため、プロパティ数へカウントする。
            // 登録済みの場合は、declaredPropertyとして扱われるため、プロパティ数はカウントしない。
            if (!schemaProps.contains(req)) {
                // dynamicPropertyの要素数をカウント
                dynamicPropCount++;
                validateDynamicProperty(property);
                props.add(property);
            } else {
                // リクエストボディに指定された定義済のダイナミックプロパティ値がnullの場合は、nullとしてESへ格納するため省略しない
                // （リクエストボディで省略されている場合は、ESへの格納が不要なためプロパティ情報としても省略する）
                // ただし、プロパティ値がnull以外の場合は2重登録となるため、除外する
                if (property.getValue() == null && isRegisteredDynamicProperty(edmEntityType, req)) {
                    validateDynamicProperty(property);
                    props.add(property);
                }
            }
        }
        // リソースごとのチェック処理
        this.collectProperties(props);
        this.validate(props);

        // プロパティの要素数チェック
        if (dynamicPropCount > 0) {
            PropertyLimitChecker checker = new PropertyLimitChecker(metadata, entitySetNameParam, dynamicPropCount);
            List<CheckError> errors = checker.checkPropertyLimits(entitySetNameParam);
            if (errors.size() > 0) {
                throw DcCoreException.OData.ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED;
            }
        }

        // entity はImmutable なので、生成すべきEntityオブジェクトを再作成
        // このOEntityはスキーマに従っていることが保証されているが、キーが存在しない。
        OEntity newEnt = OEntities.createRequest(edmEntitySet, props, links);

        // keyを設定
        OEntityKey key = null;
        // 複合キーかどうかで処理を振り分け
        if (keysDefined.size() == 1) {
            // single-unnamed value
            String keyPropName = keysDefined.get(0);
            OProperty<?> keyProp = newEnt.getProperty(keyPropName);
            Object value = keyProp.getValue();
            if (value == null) {
                // 単一主キーがnullの場合、400エラー
                throw DcCoreException.OData.NULL_SINGLE_KEY;
            }
            key = OEntityKey.create(keyProp.getValue());
        } else {
            // multiple-named valueの実装
            Map<String, Object> keyMap = new HashMap<String, Object>();
            for (String keyPropName : keysDefined) {
                OProperty<?> keyProp = newEnt.getProperty(keyPropName);
                Object value = keyProp.getValue();
                if (value == null) {
                    // 複合キーの場合は、null値を許可する
                    // キー値がnullの場合、OEntityKeyの作成に失敗するため、ダミーキーを設定する
                    value = DUMMY_KEY;
                    props.remove(keyProp);
                    props.add(OProperties.string(keyPropName, (String) null));
                }
                keyMap.put(keyPropName, value);
            }
            key = OEntityKey.create(keyMap);
        }
        editProperty(props, key.toKeyString());
        // 登録すべきOEntityを作成
        // このOEntityはスキーマに従っていることが保証されており、キーも適切に設定されている。
        newEnt = OEntities.create(reqEntity.getEntitySet(), key, props, links,
                key.toKeyStringWithoutParentheses(), reqEntity.getEntitySet().getName());
        return newEnt;

    }

    /**
     * 引数で指定されたプロパティがDynamicPropertyで定義されているかをチェックする.
     * @param edmEntityType edmEntityType
     * @param propertyName プロパティ名
     * @return true:DynamicProperty, false:DeclaredProperty
     */
    protected boolean isRegisteredDynamicProperty(EdmEntityType edmEntityType, String propertyName) {
        boolean isRegisteredDynamicProperty = false;
        EdmProperty prop = edmEntityType.findDeclaredProperty(propertyName);
        NamespacedAnnotation<?> isDeclared = prop.findAnnotation(Common.DC_NAMESPACE.getUri(),
                Property.P_IS_DECLARED.getName());
        // Property/ComplexTypeProperty以外では、IsDeclaredは定義されていないため、除外する。
        if (isDeclared != null && isDeclared.getValue().equals("false")) {
            isRegisteredDynamicProperty = true;
        }
        return isRegisteredDynamicProperty;
    }

    /**
     * 主キーのバリ―デート.
     * @param oEntityKey リクエストされたKey情報
     * @param edmEntityType EntityTypeのスキーマ情報
     */
    protected void validatePrimaryKey(OEntityKey oEntityKey, EdmEntityType edmEntityType) {
        for (String key : edmEntityType.getKeys()) {
            EdmType keyEdmType = edmEntityType.findProperty(key).getType();
            if (OEntityKey.KeyType.SINGLE.equals(oEntityKey.getKeyType())) {
                // 単一主キーの場合
                if (!(oEntityKey.asSingleValue().getClass().equals(
                        EdmSimpleType.getSimple(keyEdmType.getFullyQualifiedTypeName()).getCanonicalJavaType()))) {
                    throw DcCoreException.OData.ENTITY_KEY_PARSE_ERROR;
                }
            } else {
                // 複合主キーの場合
                Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
                for (NamedValue<?> nv : nvSet) {
                    if (nv.getName().equals(key) && !(nv.getValue().getClass().equals(
                            EdmSimpleType.getSimple(keyEdmType.getFullyQualifiedTypeName())
                                    .getCanonicalJavaType()))) {
                        throw DcCoreException.OData.ENTITY_KEY_PARSE_ERROR;
                    }
                }
            }
        }
    }

    /**
     * プロパティ操作.
     * @param props プロパティ一覧
     * @param value キーの値
     */
    protected void editProperty(List<OProperty<?>> props, String value) {
    }

    /**
     * デフォルト値を設定したシンプルプロパティを取得する.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     * @return デフォルト値を設定したシンプルプロパティ
     */
    protected OProperty<?> getSimpleProperty(EdmProperty ep, String propName, OProperty<?> op) {
        // 値が設定されていなければ、デフォルト値を設定する
        if (op == null || op.getValue() == null) {
            op = setDefaultValue(ep, propName, op);
        }
        return op;
    }

    /**
     * デフォルト値を設定したComplexプロパティを取得する.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     * @param metadata スキーマ情報
     * @return デフォルト値を設定したComplexプロパティ
     */
    @SuppressWarnings("unchecked")
    protected OProperty<?> getComplexProperty(EdmProperty ep, String propName, OProperty<?> op,
            EdmDataServices metadata) {
        // ComplexTypeのスキーマ情報を取得する
        OProperty<?> newProp;
        EdmComplexType edmComplexType =
                metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());

        // dynamicPropertyの要素数をカウント
        if (op == null || op.getValue() == null) {
            newProp = setDefaultValue(ep, propName, op, metadata);
        } else {
            if (ep.getCollectionKind().equals(CollectionKind.List)) {
                // ComplexTypeが配列要素の場合は、OCollectionBuilderを作成する
                EdmCollectionType collectionType = new EdmCollectionType(CollectionKind.List, ep.getType());
                OCollection.Builder<OObject> builder = OCollections.<OObject>newBuilder(collectionType
                        .getItemType());

                if (op.getValue() instanceof OCollection<?>) {
                    // ComplexTypeプロパティを配列に追加する
                    for (OComplexObject val : (OCollection<OComplexObject>) op.getValue()) {
                        // ComplexTypeのOProperty一覧を取得する
                        List<OProperty<?>> newComplexProperties = getComplexPropertyList(ep, propName,
                                val.getProperties(), metadata);
                        builder.add(OComplexObjects.create(edmComplexType, newComplexProperties));
                    }

                    // ComplexTypeの配列要素をOCollectionプロパティとして設定する
                    newProp = OProperties.collection(ep.getName(), collectionType, builder.build());
                } else {
                    throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
                }
            } else {
                // ComplexTypeが配列でない場合は、ComplexTypeプロパティとして設定する
                List<OProperty<?>> newComplexProperties = getComplexPropertyList(ep, propName,
                        (List<OProperty<?>>) op.getValue(), metadata);
                newProp = OProperties.complex(propName, edmComplexType, newComplexProperties);
            }
        }
        return newProp;
    }

    /**
     * デフォルト値を設定したComplexプロパティ一覧を取得する.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param opList OProperty一覧
     * @param metadata スキーマ情報
     * @return デフォルト値を設定したシンプルプロパティ
     */
    protected List<OProperty<?>> getComplexPropertyList(EdmProperty ep, String propName, List<OProperty<?>> opList,
            EdmDataServices metadata) {
        // ComplexTypeのスキーマ情報を取得する
        EdmComplexType edmComplexType =
                metadata.findEdmComplexType(ep.getType().getFullyQualifiedTypeName());
        Map<String, OProperty<?>> complexProperties = new HashMap<String, OProperty<?>>();

        // 不足項目を確認するためにスキーマ情報をベースにループさせるので、OPropertyの一覧をHash形式に変換する
        for (OProperty<?> cp : opList) {
            complexProperties.put(cp.getName(), cp);
        }

        List<OProperty<?>> newComplexProperties = createNewComplexProperties(metadata, edmComplexType,
                complexProperties);

        // デフォルト値を設定したComplexTypeプロパティの一覧を返却する
        return newComplexProperties;
    }

    /**
     * ComplexTypeスキーマを参照して、必須チェックとデフォルト値の設定を行う.
     * @param metadata スキーマ情報
     * @param edmComplexType ComplexTypeのスキーマ情報
     * @param complexProperties ComplexTypePropertyのList
     * @return デフォルト値を設定したComplexTypeプロパティの一覧
     */
    protected List<OProperty<?>> createNewComplexProperties(EdmDataServices metadata,
            EdmComplexType edmComplexType,
            Map<String, OProperty<?>> complexProperties) {
        List<OProperty<?>> newComplexProperties = new ArrayList<OProperty<?>>();
        for (EdmProperty ctp : edmComplexType.getProperties()) {
            // プロパティ情報を取得する
            String compPropName = ctp.getName();
            OProperty<?> complexProperty = complexProperties.get(compPropName);
            if (ctp.getType().isSimple()) {
                // シンプル型の場合
                complexProperty = getSimpleProperty(ctp, compPropName, complexProperty);
            } else {
                // Complex型の場合
                complexProperty = getComplexProperty(ctp, compPropName, complexProperty, metadata);
            }
            if (complexProperty != null) {
                newComplexProperties.add(complexProperty);
            }
        }
        return newComplexProperties;
    }

    /**
     * デフォルト値の設定.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     * @return Oproperty
     */
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op) {
        return setDefaultValue(ep, propName, op, null);
    }

    /**
     * デフォルト値の設定.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     * @param metadata EdmDataServicesスキーマ定義
     * @return Oproperty
     */
    protected OProperty<?> setDefaultValue(EdmProperty ep, String propName, OProperty<?> op, EdmDataServices metadata) {
        // スキーマ上定義されているのに入力の存在しない Property
        // デフォルト値が定義されていればそれをいれる。
        // ComplexTypeそのものの項目、または配列の項目であればデフォルト値は設定しない
        NamespacedAnnotation<?> annotation = ep.findAnnotation(Common.DC_NAMESPACE.getUri(),
                Property.P_IS_DECLARED.getName());
        if (annotation != null && !(Boolean.valueOf(annotation.getValue().toString()))) {
            return null;
        }
        if ((ep.getType().isSimple() && !ep.getCollectionKind().equals(CollectionKind.List))
                && ep.getDefaultValue() != null) {
            op = generateDefautlProperty(ep);
        } else if (ep.isNullable()) {
            // nullableがtrueであれば。nullの入ったプロパティ
            // TODO これでいいのか？
            op = OProperties.null_(propName, ep.getType().getFullyQualifiedTypeName());
        } else {
            // nullableがfalseであれば。エラーとする
            throw DcCoreException.OData.INPUT_REQUIRED_FIELD_MISSING.params(propName);
        }
        return op;
    }

    /**
     * プロパティ項目の値をチェックする.
     * @param ep EdmProperty
     * @param propName プロパティ名
     * @param op OProperty
     */
    protected void validateProperty(EdmProperty ep, String propName, OProperty<?> op) {
        for (NamespacedAnnotation<?> annotation : ep.getAnnotations()) {
            if (annotation.getName().equals(Common.DC_FORMAT)) {
                String dcFormat = annotation.getValue().toString();
                // 正規表現チェックの場合
                if (dcFormat.startsWith(Common.DC_FORMAT_PATTERN_REGEX)) {
                    validatePropertyRegEx(propName, op, dcFormat);
                } else if (dcFormat.equals(Common.DC_FORMAT_PATTERN_URI)) {
                    validatePropertyUri(propName, op);
                } else if (dcFormat.startsWith(Common.DC_FORMAT_PATTERN_SCHEMA_URI)) {
                    validatePropertySchemaUri(propName, op);
                } else if (dcFormat.startsWith(Common.DC_FORMAT_PATTERN_CELL_URL)) {
                    validatePropertyCellUrl(propName, op);
                } else if (dcFormat.startsWith(Common.DC_FORMAT_PATTERN_USUSST)) {
                    validatePropertyUsusst(propName, op, dcFormat);
                }
            }
        }
    }

    /**
     * p:Format以外のチェック処理.
     * @param props プロパティ一覧
     * @param
     */
    public void validate(List<OProperty<?>> props) {
    }

    /**
     * プロパティの一覧取得.
     * @param props プロパティ一覧
     * @param
     */
    public void collectProperties(List<OProperty<?>> props) {
    }

    /**
     * ダイナミックプロパティ項目の値をチェックする.
     * @param property OProperty
     */
    private void validateDynamicProperty(OProperty<?> property) {
        // keyのチェック
        String key = property.getName();
        Pattern pattern = Pattern.compile(Common.PATTERN_USERDATA_KEY);
        Matcher matcher = pattern.matcher(key);
        if (!matcher.matches()) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
        }

        // 動的プロパティなのでnullは許容する
        if (property.getValue() == null) {
            return;
        }

        // String型のvalueのチェック
        EdmType type = property.getType();
        if (EdmSimpleType.STRING.equals(type)) {
            String value = property.getValue().toString();
            if (!ODataUtils.validateString(value)) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
            }
        }

        // Double型の値範囲チェック
        if (EdmSimpleType.DOUBLE.equals(type)) {
            double value = (Double) property.getValue();
            if (!ODataUtils.validateDouble(value)) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(key);
            }
        }
    }

    /**
     * プロパティ項目の値を正規表現でチェックする.
     * @param propName プロパティ名
     * @param op OProperty
     * @param dcFormat dcFormatの値
     */
    protected void validatePropertyRegEx(String propName, OProperty<?> op, String dcFormat) {
        // regEx('正規表現')から正規表現を抜き出す
        Pattern formatPattern = Pattern.compile(Common.DC_FORMAT_PATTERN_REGEX + "\\('(.+)'\\)");
        Matcher formatMatcher = formatPattern.matcher(dcFormat);
        formatMatcher.matches();
        dcFormat = formatMatcher.group(1);

        // フォーマットのチェックを行う
        Pattern pattern = Pattern.compile(dcFormat);
        Matcher matcher = pattern.matcher(op.getValue().toString());
        if (!matcher.matches()) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * プロパティ項目の値をURIかチェックする.
     * @param propName プロパティ名
     * @param op OProperty
     */
    protected void validatePropertyUri(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidUri(op.getValue().toString())) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Schema URI Format Check.
     * @param propName Property name
     * @param op OProperty
     */
    protected void validatePropertySchemaUri(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidSchemaUri(op.getValue().toString())) {
            throw DcCoreException.OData.SCHEMA_URI_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * Cell URL Format Check.
     * @param propName Property name
     * @param op OProperty
     */
    protected void validatePropertyCellUrl(String propName, OProperty<?> op) {
        if (!ODataUtils.isValidCellUrl(op.getValue().toString())) {
            throw DcCoreException.OData.CELL_URL_FORMAT_ERROR.params(propName);
        }
    }

    /**
     * プロパティ項目の値を、1つ以上のスペース区切り文字列にマッチするかチェックする.
     * @param propName プロパティ名
     * @param op OProperty
     * @param dcFormat dcFormatの値
     */
    protected void validatePropertyUsusst(String propName, OProperty<?> op, String dcFormat) {
        // dcFormatから候補をリストとして抽出.
        Pattern formatPattern = Pattern.compile(Common.DC_FORMAT_PATTERN_USUSST + "\\((.+)\\)");
        Matcher formatMatcher = formatPattern.matcher(dcFormat);
        formatMatcher.matches();
        dcFormat = formatMatcher.group(1);

        String[] allowedTokens = dcFormat.split(", ");
        for (int i = 0; i < allowedTokens.length; i++) {
            //remove single quotations.
            allowedTokens[i] = allowedTokens[i].replaceAll("\'(.+)\'", "$1");
        }
        List<String> allowedTokenList = Arrays.asList(allowedTokens);

        // 検証される文字列を配列にする
        String value = op.getValue().toString();
        if (value.indexOf("  ") > -1) {
            throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
           }
        String[] tokens = value.split(" ");
        Set<String> overlapChk = new HashSet<>();

        // 検証される文字列をループして全てマッチするか確認する
        // 1回でもマッチしないものがあったら、例外を投げる
        for (String token : tokens) {
            if (!allowedTokenList.contains(token)) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
               }
            //重複チェック
            if (overlapChk.contains(token)) {
                throw DcCoreException.OData.REQUEST_FIELD_FORMAT_ERROR.params(propName);
               } else {
                   overlapChk.add(token);
               }
        }
    }

    /**
     * OEntityKeyの正規化を行う.
     * 正規化後のOEntityKeyをtoKeyStringすると、同一キーであれば同一文字列になる。
     * @param oEntityKey もとのOEntityKey
     * @param edmEntitySet EdmEntitySet
     * @return OEntityKey 正規化されたOEntityKey
     */
    public static OEntityKey normalizeOEntityKey(OEntityKey oEntityKey, EdmEntitySet edmEntitySet) {
        EdmEntityType edmEntityType = edmEntitySet.getType();
        // スキーマに定義されたキーリストを取得
        List<String> keysDefined = edmEntityType.getKeys();

        // keyを設定
        OEntityKey key = null;
        // 複合キーかどうかで処理を振り分け
        if (keysDefined.size() == 1) {
            key = oEntityKey;
        } else {
            Map<String, Object> keyMap = new HashMap<String, Object>();
            if (OEntityKey.KeyType.COMPLEX == oEntityKey.getKeyType()) {
                // 入力キーも複合のとき
                Set<NamedValue<?>> nvSet = oEntityKey.asComplexValue();
                // multiple-named valueの実装
                for (String keyName : keysDefined) {
                    for (NamedValue<?> nv : nvSet) {
                        if (nv.getName().equals(keyName)) {
                            // 該当キーをMapに詰める処理
                            Object value = nv.getValue();
                            if (value == null) {
                                // 複合キーの場合は、null値を許可する
                                // キー値がnullの場合、OEntityKeyの作成に失敗するため、ダミーキーを設定する
                                value = DUMMY_KEY;
                            }
                            keyMap.put(keyName, value);
                        }
                    }
                }
            } else {
                // 入力キーがシングルのとき
                Object keyValue = oEntityKey.asSingleValue();
                for (String keyName : keysDefined) {
                    EdmProperty eProp = edmEntityType.findProperty(keyName);
                    Object value = null;
                    if (eProp.isNullable()) {
                        // Nullableな項目がnullだったはず
                        // キー値がnullの場合、OEntityKeyの作成に失敗するため、ダミーキーを設定する
                        value = DUMMY_KEY;
                    } else {
                        value = keyValue;
                    }
                    keyMap.put(keyName, value);
                }
            }
            // 準備したMapからOEntityKeyを作成
            key = OEntityKey.create(keyMap);
        }
        return key;
    }

    /**
     * リクエストボディReaderからOEntityを生成する.
     * @param keyPropNames エンティティキーを作成するためのプロパティの名前のリスト
     * @param reader リクエストボディ
     * @return OEntity
     */
    private OEntity createOEntityFromRequest(List<String> keyPropNames,
            EdmDataServices metadata,
            Reader reader,
            String entitySetNameParam) {
        OEntityKey keyDummy = null;

        if (keyPropNames.size() == 1) {
            // single-unnamed value
            keyDummy = OEntityKey.create("");
        }
        // TODO multiple-named valueの実装

        OEntity entity = null;

        // ODataVersion.V2だと、おそらくバグのためJSONパースが動かないため、強制的にV1としてリクエストをパースする。
        entity = convertFromString(reader, MediaType.APPLICATION_JSON_TYPE, ODataVersion.V1, metadata,
                entitySetNameParam, keyDummy);

        return entity;
    }

    /**
     * POST用のレスポンスビルダーを作成する.
     * @param ent OEntity
     * @param outputFormat Content-Type
     * @param responseStr レスポンスボディ
     * @param resUriInfo レスポンスのUriInfo
     * @param key レスポンスのエンティティキー
     * @return レスポンスビルダー
     */
    protected ResponseBuilder getPostResponseBuilder(
            OEntity ent,
            MediaType outputFormat,
            String responseStr,
            UriInfo resUriInfo,
            String key) {
        ResponseBuilder rb = Response.status(HttpStatus.SC_CREATED).entity(responseStr).type(outputFormat)
                .header(HttpHeaders.LOCATION, resUriInfo.getBaseUri().toASCIIString()
                        + getEntitySetName() + key)
                .header(ODataConstants.Headers.DATA_SERVICE_VERSION, ODataVersion.V2.asString);

        // 応答にETAGを付与
        if (ent instanceof OEntityWrapper) {
            OEntityWrapper oew2 = (OEntityWrapper) ent;
            String etag = oew2.getEtag();
            if (etag != null) {
                rb = rb.header(HttpHeaders.ETAG, "W/\"" + etag + "\"");
            }
        }
        return rb;
    }

    /**
     * レスポンスボディを作成する.
     * @param uriInfo UriInfo
     * @param resp レスポンス
     * @param format レスポンスボディのフォーマット
     * @param acceptableMediaTypes 許可するMediaTypeのリスト
     * @return レスポンスボディ
     */
    protected String renderEntityResponse(
            final UriInfo uriInfo,
            final EntityResponse resp,
            final String format,
            final List<MediaType> acceptableMediaTypes) {
        StringWriter w = new StringWriter();
        try {
            FormatWriter<EntityResponse> fw = DcFormatWriterFactory.getFormatWriter(EntityResponse.class,
                    acceptableMediaTypes, format, null);
            // UriInfo uriInfo2 = PersoniumCoreUtils.createUriInfo(uriInfo, 1);
            fw.write(uriInfo, w, resp);
        } catch (UnsupportedOperationException e) {
            throw DcCoreException.OData.FORMAT_INVALID_ERROR.params(format);
        }

        String responseStr = w.toString();

        return responseStr;

    }

    /**
     * Entity Data Modelのプロパティスキーマ情報からODataプロパティのデフォルト値インスタンスを生成する.
     * @param ep Entity Data Modelのプロパティ
     * @return デフォルト値が入ったODataプロパティのインスタンス
     */
    private OProperty<?> generateDefautlProperty(EdmProperty ep) {
        EdmType edmType = ep.getType();
        OProperty<?> op = null;

        // スキーマからDefault値を取得する。
        String defaultValue = ep.getDefaultValue();
        String propName = ep.getName();

        // Default値が特定の関数である場合は、値を生成する。
        if (EdmSimpleType.STRING.equals(edmType)) {
            // Typeが文字列でDefault値がCELLID()のとき。
            if (defaultValue.equals("CELLID()")) {
                String newCellid = DcCoreConfig.getCouchDbCellCreationTarget() + "_"
                        + UUID.randomUUID().toString().replaceAll("-", "");
                op = OProperties.string(propName, newCellid);
                // etag = newCellid;
            } else if (defaultValue.equals("UUID()")) {
                // Typeが文字列でDefault値がUUID()のとき。
                String newUuid = UUID.randomUUID().toString().replaceAll("-", "");
                op = OProperties.string(propName, newUuid);
            } else if (defaultValue.equals("null")) {
                // Typeが文字列でDefault値がnullのとき。
                op = OProperties.null_(propName, EdmSimpleType.STRING);
            } else {
                // Typeが文字列でDefault値その他の値のとき。
                op = OProperties.string(propName, defaultValue);
            }
        } else if (EdmSimpleType.DATETIME.equals(edmType)) {
            // Edm.DateTime型：
            if (null == defaultValue || defaultValue.equals("null")) {
                // defaultValueがnullまたは"null"であれば、nullを設定する
                op = OProperties.null_(propName, EdmSimpleType.DATETIME);
            } else {
                // －"\/Date(...)\/"の場合は設定値をデフォルト値にする
                // －"SYSUTCDATETIME()"の場合は現在時刻をデフォルト値にする
                // TODO この実装では Atom 出力時にDefault TimeZoneで出力されてしまう
                op = OProperties.datetime(propName,
                        new Date(getTimeMillis(defaultValue)));
            }

        } else if (EdmSimpleType.SINGLE.equals(edmType)) {
            // TypeがSINGLEでDefault値があるとき。
            op = OProperties.single(propName, Float.valueOf(defaultValue));
        } else if (EdmSimpleType.INT64.equals(edmType)) {
            // TypeがINT64でDefault値があるとき。
            op = OProperties.int64(propName, Long.valueOf(defaultValue));
        } else if (EdmSimpleType.INT32.equals(edmType)) {
            // TypeがINT32でDefault値があるとき。
            op = OProperties.int32(propName, Integer.valueOf(defaultValue));
        } else if (EdmSimpleType.BOOLEAN.equals(edmType)) {
            // TypeがBooleanでDefault値があるとき。
            op = OProperties.boolean_(propName, Boolean.parseBoolean(defaultValue));
        } else if (EdmSimpleType.DOUBLE.equals(edmType)) {
            // TypeがDoubleでDefault値があるとき。
            op = OProperties.double_(propName, Double.parseDouble(defaultValue));
        }

        return op;
    }

    private static OEntity convertFromString(final Reader body,
            final MediaType type,
            final ODataVersion version,
            final EdmDataServices metadata,
            final String entitySetName,
            final OEntityKey entityKey) {
        FormatParser<Entry> parser = DcFormatParserFactory.getParser(Entry.class, type, new Settings(version, metadata,
                entitySetName, entityKey, null, false));
        Entry entry = null;
        try {
            entry = parser.parse(body);
        } catch (DcCoreException e) {
            throw e;
        } catch (Exception e) {
            throw DcCoreException.OData.JSON_PARSE_ERROR.reason(e);
        }
        return entry.getEntity();
    }

    /**
     * ダミーキーチェック.
     * @param value チェック対象の値
     * @return true:ダミーキー false:ダミーキー以外
     */
    public static boolean isDummy(Object value) {
        boolean flag = false;
        if (value.equals(DUMMY_KEY)) {
            flag = true;
        }
        return flag;
    }

    /**
     * ダミーキーをnullに置き換えた文字列を返却する.
     * @param value 置換対象文字列
     * @return 返還後文字列
     */
    public static String replaceDummyKeyToNull(String value) {
        return value.replaceAll("'" + DUMMY_KEY + "'", "null");
    }

    /**
     * nullをダミーキーに置き換えた文字列を返却する(括弧つき).
     * @param value 置換対象文字列
     * @return 返還後文字列
     */
    public static String replaceNullToDummyKeyWithParenthesis(String value) {
        return replaceNullToDummyKey("(" + value + ")");
    }

    /**
     * nullをダミーキーに置き換えた文字列を返却する.
     * @param value 置換対象文字列
     * @return 返還後文字列
     */
    public static String replaceNullToDummyKey(String value) {
        Pattern pattern = Pattern.compile("=null([,|\\)])");
        Matcher m = pattern.matcher(value);
        return m.replaceAll("='" + DUMMY_KEY + "'$1");
    }

    /**
     * NavigationTargetKeyPropertyからEntityTypeとPropertyNameを取得する.
     * @param propertyName プロパティ名
     * @return EntityTypeとPropertyName
     */
    public static HashMap<String, String> convertNTKP(String propertyName) {
        HashMap<String, String> ntkp = null;
        Pattern pattern = Pattern.compile("_([^.]+)\\.(.+)");
        Matcher m = pattern.matcher(propertyName);
        if (m.matches()) {
            ntkp = new HashMap<String, String>();
            ntkp.put("entityType", m.group(1));
            ntkp.put("propName", m.group(2));
        }
        return ntkp;
    }

    /**
     * 引数で指定された文字列をTimeMillisの値として取得する.
     * @param timeStr TimeMillisの文字列表現(ex."/Data(...)/", "SYSUTCDATETIME()")
     * @return TimeMillisの値
     */
    private long getTimeMillis(String timeStr) {
        long timeMillis = 0;
        if (timeStr.equals(Common.SYSUTCDATETIME)) {
            timeMillis = currentTimeMillis;
        } else {
            try {
                Pattern pattern = Pattern.compile("^/Date\\((.+)\\)/$");
                Matcher match = pattern.matcher(timeStr);
                if (match.matches()) {
                    String date = match.replaceAll("$1");
                    timeMillis = Long.parseLong(date);
                }
            } catch (NumberFormatException e) {
                throw DcCoreException.OData.JSON_PARSE_ERROR.reason(e);
            }
        }
        return timeMillis;
    }

    /**
     * レスポンスボディのエスケープする.
     * @param response レスポンスボディ
     * @return エスケープしたレスポンスボディ
     */
    public String escapeResponsebody(String response) {
        return EscapeControlCode.escape(response);
    }
}
