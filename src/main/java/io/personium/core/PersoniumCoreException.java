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
package io.personium.core;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

import io.personium.core.exceptions.ODataErrorMessage;
import io.personium.core.utils.EscapeControlCode;
import io.personium.plugin.base.PluginMessageUtils.Severity;

/**
 * ログメッセージ作成クラス.
 */
@SuppressWarnings("serial")
public class PersoniumCoreException extends RuntimeException {

    /**
     * OData関連.
     */
    public static class OData {
        /**
         * JSONのパースに失敗したとき.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-OD-0001");
        /**
         * クエリのパースに失敗.
         */
        public static final PersoniumCoreException QUERY_PARSE_ERROR = create("PR400-OD-0002");
        /**
         * $fileterのパースに失敗.
         */
        public static final PersoniumCoreException FILTER_PARSE_ERROR = create("PR400-OD-0003");
        /**
         * EntityKeyのパースに失敗.
         */
        public static final PersoniumCoreException ENTITY_KEY_PARSE_ERROR = create("PR400-OD-0004");

        /**
         * $formatに指定された値が不正.
         */
        public static final PersoniumCoreException FORMAT_INVALID_ERROR = create("PR400-OD-0005");
        /**
         * リクエストデータのフォーマットが不正.
         * {0}プロパティ名
         */
        public static final PersoniumCoreException REQUEST_FIELD_FORMAT_ERROR = create("PR400-OD-0006");
        /**
         * リクエストボディのフィールド名が不正.
         * {0}:詳細メッセージ
         * 管理情報を更新しようとした時と、スキーマに存在しない値を設定しようとした時に発生
         * 注）このエラーはメッセージをソースで管理することになるため、今後はこれを使わないこと。
         */
        public static final PersoniumCoreException FIELED_INVALID_ERROR = create("PR400-OD-0007");
        /**
         * 該当Associationが存在しない.
         */
        public static final PersoniumCoreException NO_SUCH_ASSOCIATION = create("PR400-OD-0008");
        /**
         * リクエストボディの必須項目が無い.
         * {0}:プロパティ名
         */
        public static final PersoniumCoreException INPUT_REQUIRED_FIELD_MISSING = create("PR400-OD-0009");
        /**
         * リクエストボディの必須項目が無い.
         */
        public static final PersoniumCoreException KEY_FOR_NAVPROP_SHOULD_NOT_BE_SPECIFIED = create("PR400-OD-0010");
        /**
         * リクエストURLのKey指定が無い.
         */
        public static final PersoniumCoreException KEY_FOR_NAVPROP_SHOULD_BE_SPECIFIED = create("PR400-OD-0011");
        /**
         * 文字種が不正.
         * {0}:プロパティ名
         */
        public static final PersoniumCoreException INVALID_TYPE_ERROR = create("PR400-OD-0012");
        /**
         * $inlinecountに指定された値が不正.
         * {0}:inlinecountで指定された値
         */
        public static final PersoniumCoreException INLINECOUNT_PARSE_ERROR = create("PR400-OD-0013");
        /**
         * 指定されたプロパティが存在しない.
         */
        public static final PersoniumCoreException UNKNOWN_PROPERTY_APPOINTED = create("PR400-OD-0014");
        /**
         * $orderbyのパースに失敗.
         */
        public static final PersoniumCoreException ORDERBY_PARSE_ERROR = create("PR400-OD-0015");
        /**
         * 単一Keyにnullが指定された.
         */
        public static final PersoniumCoreException NULL_SINGLE_KEY = create("PR400-OD-0016");
        /**
         * $selectのパースに失敗.
         */
        public static final PersoniumCoreException SELECT_PARSE_ERROR = create("PR400-OD-0017");
        /**
         * AssociationEndの更新がリクエストされた.
         */
        public static final PersoniumCoreException NOT_PUT_ASSOCIATIONEND = create("PR400-OD-0019");
        /**
         * 登録するデータの型がelasticsearchに登録済みのデータ型と異なる.
         */
        public static final PersoniumCoreException SCHEMA_MISMATCH = create("PR400-OD-0020");
        /**
         * $batchのボディのFormatが不正.
         * ヘッダの指定誤り
         * {0}:ヘッダ名
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_HEADER_ERROR = create("PR400-OD-0021");
        /**
         * $batchのボディのFormatが不正.
         * changesetのネストが指定されていた場合
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_CHANGESET_NEST_ERROR = create("PR400-OD-0022");
        /**
         * $batchのボディのパースに失敗した場合.
         */
        public static final PersoniumCoreException BATCH_BODY_PARSE_ERROR = create("PR400-OD-0023");
        /**
         * 更新リクエストでボディのNTKPに指定されたリソースが存在しない場合.
         * {0}：NTKPで指定された値
         */
        public static final PersoniumCoreException BODY_NTKP_NOT_FOUND_ERROR = create("PR400-OD-0024");
        /**
         * $expandで指定されたNTKPがリソースとして存在しない場合.
         * {0}：$expandで指定された値
         */
        public static final PersoniumCoreException EXPAND_NTKP_NOT_FOUND_ERROR = create("PR400-OD-0025");
        /**
         * $expandのパースに失敗.
         */
        public static final PersoniumCoreException EXPAND_PARSE_ERROR = create("PR400-OD-0026");
        /**
         * すでに別のスキーマ型のIndexが作成されている場合.
         */
        public static final PersoniumCoreException ANOTHRE_SCHEMA_TYPE_ALREADY_EXISTS = create("PR400-OD-0027");
        /**
         * $linksのEntityKeyのパースに失敗.
         */
        public static final PersoniumCoreException ENTITY_KEY_LINKS_PARSE_ERROR = create("PR400-OD-0028");
        /**
         * クエリに指定された値が不正.
         */
        public static final PersoniumCoreException QUERY_INVALID_ERROR = create("PR400-OD-0029");
        /**
         * $Batchで指定されたリクエスト数が不正.
         */
        public static final PersoniumCoreException TOO_MANY_REQUESTS = create("PR400-OD-0030");
        /**
         * $links登録で1:1を指定.
         */
        public static final PersoniumCoreException INVALID_MULTIPLICITY = create("PR400-OD-0031");

        /**
         * EnitityTypeの階層数、内包プロパティ数の制限を超えた.
         */
        public static final PersoniumCoreException ENTITYTYPE_STRUCTUAL_LIMITATION_EXCEEDED = create("PR400-OD-0032");

        /**
         * EnitityType数の制限を超えた.
         */
        public static final PersoniumCoreException ENTITYTYPE_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0033");

        /**
         * $batchのボディのFormatが不正.
         * リクエストパスの指定誤り
         * {0}:リクエストパス
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_PATH_ERROR = create("PR400-OD-0034");

        /**
         * $batchのボディのFormatが不正.
         * $batchで受付できないメソッドを指定された
         * {0}:メソッド
         */
        public static final PersoniumCoreException BATCH_BODY_FORMAT_METHOD_ERROR = create("PR400-OD-0035");

        /**
         * クエリのパースに失敗.
         * {0}:失敗したクエリ
         */
        public static final PersoniumCoreException QUERY_PARSE_ERROR_WITH_PARAM = create("PR400-OD-0036");

        /**
         * $batch内全体で指定された$topの値の合計が上限値を超えた.
         */
        public static final PersoniumCoreException BATCH_TOTAL_TOP_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0037");

        /**
         * $linksが作成可能な最大件数をオーバー.
         */
        public static final PersoniumCoreException LINK_UPPER_LIMIT_RECORD_EXEED = create("PR400-OD-0038");

        /**
         * 指定された$expandの値の合計が上限値を超えた.
         */
        public static final PersoniumCoreException EXPAND_COUNT_LIMITATION_EXCEEDED = create("PR400-OD-0039");

        /**
         * $orderbyクエリに配列型のプロパティが指定された.
         */
        public static final PersoniumCoreException CANNOT_SPECIFY_THE_LIST_TYPE_TO_ORDERBY = create("PR400-OD-0040");

        /**
         * リクエストヘッダー{0}の値{1}が正しくない.
         */
        public static final PersoniumCoreException BAD_REQUEST_HEADER_VALUE = create("PR400-OD-0041");

        /**
         * 未サポートの操作が実行された 詳細：{0}.
         */
        public static final PersoniumCoreException OPERATION_NOT_SUPPORTED = create("PR400-OD-0042");

        /**
         * 未知の演算子が指定された場合.
         */
        public static final PersoniumCoreException UNSUPPORTED_QUERY_OPERATOR = create("PR400-OD-0043");

        /**
         * 未知の関数が指定された場合.
         */
        public static final PersoniumCoreException UNSUPPORTED_QUERY_FUNCTION = create("PR400-OD-0044");

        /**
         * 未知のプロパティを指定した場合.
         */
        public static final PersoniumCoreException UNKNOWN_QUERY_KEY = create("PR400-OD-0045");

        /**
         * プロパティのデータ型とは異なる書式の値が指定された場合.
         */
        public static final PersoniumCoreException OPERATOR_AND_OPERAND_TYPE_MISMATCHED = create("PR400-OD-0046");

        /**
         * プロパティのデータ型の範囲外の値が指定された場合.
         */
        public static final PersoniumCoreException UNSUPPORTED_OPERAND_FORMAT = create("PR400-OD-0047");

        /**
         * 検索値のアンエスケープができなかった場合.
         */
        public static final PersoniumCoreException OPERATOR_AND_OPERAND_UNABLE_TO_UNESCAPE = create("PR400-OD-0048");

        /**
         * Cell URL Invalid format.
         * {0} property name
         */
        public static final PersoniumCoreException CELL_URL_FORMAT_ERROR = create("PR400-OD-0049");
        /**
         * Schema URI Invalid format.
         * {0} property name
         */
        public static final PersoniumCoreException SCHEMA_URI_FORMAT_ERROR = create("PR400-OD-0050");

        /**
         * 該当EntitySetが存在しない.
         */
        public static final PersoniumCoreException NO_SUCH_ENTITY_SET = create("PR404-OD-0001");
        /**
         * 該当Entityが存在しない.
         */
        public static final PersoniumCoreException NO_SUCH_ENTITY = create("PR404-OD-0002");
        /**
         * 該当リソースが存在しない.
         */
        public static final PersoniumCoreException NOT_FOUND = create("PR404-OD-0000");
        /**
         * 該当Navigation Propertyが存在しない.
         */
        public static final PersoniumCoreException NOT_SUCH_NAVPROP = create("PR404-OD-0003");
        /**
         * 関係するデータが存在するエンティティへの操作.
         */
        public static final PersoniumCoreException CONFLICT_HAS_RELATED = create("PR409-OD-0001");
        /**
         * リンクが既に存在する.
         */
        public static final PersoniumCoreException CONFLICT_LINKS = create("PR409-OD-0002");
        /**
         * エンティティが既に存在する.
         */
        public static final PersoniumCoreException ENTITY_ALREADY_EXISTS = create("PR409-OD-0003");
        /**
         * 複合キーのエンティティに対して$linksを削除した時に同名のエンティティが既に存在する.
         */
        public static final PersoniumCoreException CONFLICT_UNLINKED_ENTITY = create("PR409-OD-0004");
        /**
         * 単一キーのエンティティに対して$linksを追加した時に同名のエンティティが既に存在する.
         */
        public static final PersoniumCoreException CONFLICT_DUPLICATED_ENTITY = create("PR409-OD-0005");
        /**
         * AssociationEndのLink登録時にすでに同一の関連が存在する.
         */
        public static final PersoniumCoreException CONFLICT_DUPLICATED_ENTITY_RELATION = create("PR409-OD-0006");
        /**
         * If-Matchヘッダの指定が無い.
         */
        public static final PersoniumCoreException HEADER_NOT_EXIST = create("PR412-OD-0001");
        /**
         * 該当EntityのEtagがマッチしない.
         */
        public static final PersoniumCoreException ETAG_NOT_MATCH = create("PR412-OD-0002");
        /**
         * .
         */
        public static final PersoniumCoreException CONFLICT_NP = create("PR412-OD-0003");
        /**
         * 未サポートのメディアタイプが指定された.
         */
        public static final PersoniumCoreException UNSUPPORTED_MEDIA_TYPE = create("PR415-OD-0001");
        /**
         * プロパティ名の重複を検出した.
         */
        public static final PersoniumCoreException DUPLICATED_PROPERTY_NAME = create("PR500-OD-0001");
        /**
         * 内部データの矛盾を検出した.
         */
        public static final PersoniumCoreException DETECTED_INTERNAL_DATA_CONFLICT = create("PR500-OD-0002");
    }

    /**
     * WebDAV関連.
     * TODO WebDavのエラーはWebDavの仕様に合わせて実装する。
     */
    public static class Dav {

        /**
         * XMLのパースに失敗したとき.
         */
        public static final PersoniumCoreException XML_ERROR = create("PR400-DV-0001");
        /**
         * XMLの内容がおかしいとき.
         */
        public static final PersoniumCoreException XML_CONTENT_ERROR = create("PR400-DV-0002");
        /**
         * Depthが0,1,infinity以外のとき.
         * {0}:Depthヘッダの値
         */
        public static final PersoniumCoreException INVALID_DEPTH_HEADER = create("PR400-DV-0003");
        /**
         * ROLEが存在しない時.
         */
        public static final PersoniumCoreException ROLE_NOT_FOUND = create("PR400-DV-0004");
        /**
         * Roleと紐付くBOXが存在しないとき.
         * {0}:BOX URL
         */
        public static final PersoniumCoreException BOX_LINKED_BY_ROLE_NOT_FOUND = create("PR400-DV-0005");
        /**
         * XMLのバリデートに失敗したとき.
         */
        public static final PersoniumCoreException XML_VALIDATE_ERROR = create("PR400-DV-0006");
        /**
         * コレクションの子要素が多すぎる場合.
         */
        public static final PersoniumCoreException COLLECTION_CHILDRESOURCE_ERROR = create("PR400-DV-0007");
        /**
         * コレクションの階層が深すぎる場合.
         */
        public static final PersoniumCoreException COLLECTION_DEPTH_ERROR = create("PR400-DV-0008");
        /**
         * ヘッダに不正な値が設定されている場合.
         * {0}:ヘッダのキー
         * {1}:ヘッダの値
         */
        public static final PersoniumCoreException INVALID_REQUEST_HEADER = create("PR400-DV-0009");
        /**
         * 必須ヘッダの指定が無い場合.
         * {0}:ヘッダのキー
         */
        public static final PersoniumCoreException REQUIRED_REQUEST_HEADER_NOT_EXIST = create("PR400-DV-0010");
        /**
         * 移動元のリソースとして__srcが指定された場合.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_MOVE =
                create("PR400-DV-0011");
        /**
         * 移動先のリソースとして、既存のリソースが指定された場合.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_OVERWRITE = create("PR400-DV-0012");
        /**
         * 移動先のリソースとして、ODataコレクション配下のパスが指定された場合.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION =
                create("PR400-DV-0013");
        /**
         * 移動先のリソースとして、ファイル配下のパスが指定された場合.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_FILE = create("PR400-DV-0014");
        /**
         * BoxはMOVEメソッドでの移動対象とはできない.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_BOX = create("PR400-DV-0015");
        /**
         * 移動先のリソースとして、Serviceコレクション配下のパスが指定された場合.
         */
        public static final PersoniumCoreException RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION =
                create("PR400-DV-0016");
        /**
         * 移動先のリソースとして__srcが指定された場合.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE =
                create("PR400-DV-0017");
        /**
         * 移動元がコレクションで、移動先のリソースとしてサービスソースコレクションが指定された場合.
         */
        public static final PersoniumCoreException SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION =
                create("PR400-DV-0018");

        /**
         * リソースが存在しないとき.
         */
        public static final PersoniumCoreException RESOURCE_NOT_FOUND = create("PR404-DV-0001");
        /**
         * BOXが存在しないとき.
         * {0}:BOX名
         */
        public static final PersoniumCoreException BOX_NOT_FOUND = create("PR404-DV-0002");
        /**
         * CELLが存在しないとき.
         */
        public static final PersoniumCoreException CELL_NOT_FOUND = create("PR404-DV-0003");
        /**
         * メソッドが受け付けられないとき.
         */
        public static final PersoniumCoreException METHOD_NOT_ALLOWED = create("PR405-DV-0001");
        /**
         * Depthがinfinityのとき.
         */
        public static final PersoniumCoreException PROPFIND_FINITE_DEPTH = create("PR403-DV-0001");
        /**
         * コレクション削除時に子リソースがある場合は削除失敗.
         */
        public static final PersoniumCoreException HAS_CHILDREN = create("PR403-DV-0003");
        /**
         * コレクション・ファイル名が不正なとき.
         */
        public static final PersoniumCoreException RESOURCE_NAME_INVALID = create("PR403-DV-0004");
        /**
         * 移動元と移動先が同じ場合.
         * {0}:Destination ヘッダの値
         */
        public static final PersoniumCoreException DESTINATION_EQUALS_SOURCE_URL = create("PR403-DV-0005");

        /**
         * コレクション・ファイルのPUT・MKCOL・MOVE時に親リソースが存在しない時.
         */
        public static final PersoniumCoreException HAS_NOT_PARENT = create("PR409-DV-0001");
        /**
         * File already exists.
         * <p>
         * {0} : File name
         */
        public static final PersoniumCoreException FILE_ALREADY_EXISTS = create("PR409-DV-0002");
        /**
         * 該当リソースのEtagがマッチしない.
         */
        public static final PersoniumCoreException ETAG_NOT_MATCH = create("PR412-DV-0001");
        /**
         * Overwriteヘッダで"F"が指定されたが移動先のリソースが既に存在する時.
         */
        public static final PersoniumCoreException DESTINATION_ALREADY_EXISTS = create("PR412-DV-0002");
        /**
         * Rangeヘッダ指定誤り.
         */
        public static final PersoniumCoreException REQUESTED_RANGE_NOT_SATISFIABLE = create("PR416-DV-0001");
        /**
         * ファイルシステムの矛盾を検知.
         */
        public static final PersoniumCoreException FS_INCONSISTENCY_FOUND = create("PR500-DV-0001");
        /**
         * Boxから辿ってidで検索して、Davデータに不整合があった場合.
         */
        public static final PersoniumCoreException DAV_INCONSISTENCY_FOUND = create("PR500-DV-0002");
        /**
         * Boxから辿ってidで検索して、Davデータに不整合があった場合.
         */
        public static final PersoniumCoreException DAV_UNAVAILABLE = create("PR503-DV-0001");
    }

    /**
     * サービスコレクションのエラー.
     */
    public static class ServiceCollection {
        /**
         * Personium-Engineの接続に失敗した場合.
         */
        public static final PersoniumCoreException SC_ENGINE_CONNECTION_ERROR = create("PR500-SC-0001");
        /**
         * ファイルのオープンに失敗した場合(未使用_実装内容に言及しているため、使用しないこと).
         */
        public static final PersoniumCoreException SC_FILE_OPEN_ERROR = create("PR500-SC-0002");
        /**
         * ファイルのクローズに失敗した場合(未使用_実装内容に言及しているため、使用しないこと).
         */
        public static final PersoniumCoreException SC_FILE_CLOSE_ERROR = create("PR500-SC-0003");

        /**
         * ファイルのクローズに失敗した場合(未使用_実装内容に言及しているため、使用しないこと).
         */
        public static final PersoniumCoreException SC_IO_ERROR = create("PR500-SC-0004");
        /**
         * その他のエラー.
         */
        public static final PersoniumCoreException SC_UNKNOWN_ERROR = create("PR500-SC-0005");
        /**
         * サービス呼出しで不正なHTTPレスポンスが返却された場合のエラー.
         */
        public static final PersoniumCoreException SC_INVALID_HTTP_RESPONSE_ERROR = create("PR500-SC-0006");
    }

    /**
     * SentMessage受信API呼出し時のエラー.
     */
    public static class SentMessage {
        /**
         * ToRelationに指定されたリソースが存在しない場合.
         * {0}：指定された値
         */
        public static final PersoniumCoreException TO_RELATION_NOT_FOUND_ERROR = create("PR400-SM-0001");
        /**
         * ToRelationに指定されたリソースに紐付くExtCellが存在しない場合.
         * {0}：指定された値
         */
        public static final PersoniumCoreException RELATED_EXTCELL_NOT_FOUND_ERROR = create("PR400-SM-0002");
        /**
         * 送信先URLが最大送信許可数を超えた場合.
         */
        public static final PersoniumCoreException OVER_MAX_SENT_NUM = create("PR400-SM-0003");
        /**
         * When the Box corresponding to the schema can not be found from the schema-authenticated token.
         * {0}:Schema
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS = create("PR400-SM-0004");

        /**
         * リクエストに失敗した場合.
         */
        public static final PersoniumCoreException SM_CONNECTION_ERROR = create("PR500-SM-0001");
        /**
         * ボディのパースに失敗した場合.
         */
        public static final PersoniumCoreException SM_BODY_PARSE_ERROR = create("PR500-SM-0002");
    }

    /**
     * ReceiveMessageAPI呼出し時のエラー.
     */
    public static class ReceivedMessage {
        /**
         * メッセージの関係登録で既に関係が存在する.
         */
        public static final PersoniumCoreException REQUEST_RELATION_EXISTS_ERROR = create("PR400-RM-0001");
        /**
         * When a Box corresponding to the schema can not be found.
         * {0}:Schema
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_SCHEMA_NOT_EXISTS = create("PR400-RM-0002");
        /**
         * When the Box corresponding to the RelationClassURL can not be found.
         * {0}:RelationClassURL
         */
        public static final PersoniumCoreException BOX_THAT_MATCHES_RELATION_CLASS_URL_NOT_EXISTS = create("PR400-RM-0003"); // CHECKSTYLE IGNORE - To maintain readability

// unnecessary.
//        /**
//         * メッセージのRequestRelationのパースに失敗.
//         */
//        public static final PersoniumCoreException REQUEST_RELATION_PARSE_ERROR = create("PR409-RM-0001");

        /**
         * 関係削除対象のRelationが存在しない.
         */
        public static final PersoniumCoreException REQUEST_RELATION_DOES_NOT_EXISTS = create("PR409-RM-0002");

// unnecessary.
//        /**
//         * メッセージのRequestRelationTargetのパースに失敗.
//         */
//        public static final PersoniumCoreException REQUEST_RELATION_TARGET_PARSE_ERROR = create("PR409-RM-0003");

        /**
         * 関係削除対象のExtCellが存在しない.
         */
        public static final PersoniumCoreException REQUEST_RELATION_TARGET_DOES_NOT_EXISTS = create("PR409-RM-0004");

        /**
         * RequestRelationとRequestRelationTargetのリンク情報が存在しない.
         */
        public static final PersoniumCoreException LINK_DOES_NOT_EXISTS = create("PR409-RM-0005");
    }

    /**
     * サーバ内部エラー.
     * サーバ側の障害やバグにより処理を続行できないときに投げる. 問題の原因を表すようなものとする。 基本的にののカテゴリの例外発生時にはWARN以上のログ出力となる
     */
    public static class Server {
        /**
         * 原因不明のエラー.
         */
        public static final PersoniumCoreException UNKNOWN_ERROR = create("PR500-SV-0000");
        /**
         * データストアへの接続に失敗したとき.
         */
        public static final PersoniumCoreException DATA_STORE_CONNECTION_ERROR = create("PR500-SV-0001");
        /**
         * データストア関連の不明なエラー.
         */
        public static final PersoniumCoreException DATA_STORE_UNKNOWN_ERROR = create("PR500-SV-0002");
        /**
         * ESへのリクエストでリトライオーバーしたとき.
         */
        public static final PersoniumCoreException ES_RETRY_OVER = create("PR500-SV-0003");
        /**
         * ファイルシステムに異常が発生したとき.
         */
        public static final PersoniumCoreException FILE_SYSTEM_ERROR = create("PR500-SV-0004");
        /**
         * データストアの検索に失敗.
         */
        public static final PersoniumCoreException DATA_STORE_SEARCH_ERROR = create("PR500-SV-0005");
        /**
         * データストアの更新に失敗し、ロールバックにも失敗した.
         */
        public static final PersoniumCoreException DATA_STORE_UPDATE_ROLLBACK_ERROR = create("PR500-SV-0006");
        /**
         * データストアの更新に失敗し、ロールバックが成功した.
         */
        public static final PersoniumCoreException DATA_STORE_UPDATE_ERROR_ROLLBACKED = create("PR500-SV-0007");

        /**
         * memcachedへの接続に失敗したとき.
         */
        public static final PersoniumCoreException SERVER_CONNECTION_ERROR = create("PR503-SV-0002");
        /**
         * Memcachedのロックステータス取得に失敗したとき.
         */
        public static final PersoniumCoreException GET_LOCK_STATE_ERROR = create("PR503-SV-0003");
        /**
         * ユニットユーザ単位のデータリストア中のとき.
         */
        public static final PersoniumCoreException SERVICE_MENTENANCE_RESTORE = create("PR503-SV-0004");
        /**
         * ReadDeleteOnlyモード状態のとき.
         */
        public static final PersoniumCoreException READ_DELETE_ONLY = create("PR503-SV-0005");
        /**
         * Adsへの接続に失敗したとき.
         */
        public static final PersoniumCoreException ADS_CONNECTION_ERROR = create("PR503-SV-0006");
    }

    /**
     * NetWork関連エラー.
     */
    public static class NetWork {
        /**
         * NetWork関連エラー.
         */
        public static final PersoniumCoreException NETWORK_ERROR = create("PR500-NW-0000");
        /**
         * HTTPリクエストに失敗.
         */
        public static final PersoniumCoreException HTTP_REQUEST_FAILED = create("PR500-NW-0001");
        /**
         * 接続先が想定外の応答を返却.
         */
        public static final PersoniumCoreException UNEXPECTED_RESPONSE = create("PR500-NW-0002");
        /**
         * 接続先が想定外の値を返却.
         */
        public static final PersoniumCoreException UNEXPECTED_VALUE = create("PR500-NW-0003");
    }

    /**
     * 認証系エラー.
     */
    public static class Auth {
        /**
         * パスワード文字列が不正.
         */
        public static final PersoniumCoreException PASSWORD_INVALID = create("PR400-AU-0001");
        /**
         * リクエストパラメータが不正.
         */
        public static final PersoniumCoreException REQUEST_PARAM_INVALID = create("PR400-AU-0002");
        /**
         * パスワード文字列が不正.
         */
        public static final PersoniumCoreException P_CREDENTIAL_REQUIRED = create("PR400-AU-0003");

        /**
         * ユニットユーザアクセスではない.
         */
        public static final PersoniumCoreException UNITUSER_ACCESS_REQUIRED = create("PR403-AU-0001");
        /**
         * 必要な権限が無い.
         */
        public static final PersoniumCoreException NECESSARY_PRIVILEGE_LACKING = create("PR403-AU-0002");
        /**
         * 認証ヘッダに指定されたユニットユーザではアクセセスできない.
         */
        public static final PersoniumCoreException NOT_YOURS = create("PR403-AU-0003");
        /**
         * スキーマ認証が必要.
         */
        public static final PersoniumCoreException SCHEMA_AUTH_REQUIRED = create("PR403-AU-0004");
        /**
         * このスキーマ認証ではアクセスできない.
         */
        public static final PersoniumCoreException SCHEMA_MISMATCH = create("PR403-AU-0005");
        /**
         * スキーマ認証レベルが不足.
         */
        public static final PersoniumCoreException INSUFFICIENT_SCHEMA_AUTHZ_LEVEL = create("PR403-AU-0006");
        /**
         * ルートCA証明書の設定エラー.
         */
        public static final PersoniumCoreException ROOT_CA_CRT_SETTING_ERROR = create("PR500-AN-0001");
        /**
         * リクエストパラメータが不正.
         */
        public static final PersoniumCoreException REQUEST_PARAM_CLIENTID_INVALID = create("PR400-AZ-0002");
        /**
         * リクエストパラメータが不正.
         */
        public static final PersoniumCoreException REQUEST_PARAM_REDIRECT_INVALID = create("PR400-AZ-0003");
        /**
         * JSONのパースに失敗したとき.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-AZ-0005");
        /**
         * JSONのEncodeに失敗したとき.
         */
        public static final PersoniumCoreException IDTOKEN_ENCODED_INVALID = create("PR400-AZ-0006");

    }

    /**
     * Event関連エラー.
     */
    public static class Event {
        /**
         * JSONパースに失敗.
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-EV-0001");
        /**
         * X-Personium-RequestKey の値が不正.
         */
        public static final PersoniumCoreException X_PERSONIUM_REQUESTKEY_INVALID = create("PR400-EV-0002");
        /**
         * リクエストボディの必須項目が無い.
         * {0}:プロパティ名
         */
        public static final PersoniumCoreException INPUT_REQUIRED_FIELD_MISSING = create("PR400-EV-0003");
        /**
         * リクエストデータのフォーマットが不正.
         * {0}プロパティ名
         */
        public static final PersoniumCoreException REQUEST_FIELD_FORMAT_ERROR = create("PR400-EV-0004");
        /**
         * Httpレスポンスの出力に失敗したとき.
         */
        public static final PersoniumCoreException EVENT_RESPONSE_FAILED = create("PR500-EV-0001");
        /**
         * 圧縮されたイベントログファイルがオープンできないとき.
         */
        public static final PersoniumCoreException ARCHIVE_FILE_CANNOT_OPEN = create("PR500-EV-0002");

    }

    /**
     * barファイルインストール関連エラー.
     */
    public static class BarInstall {
        /**
         * リクエストヘッダーの値が不正なとき.
         */
        public static final PersoniumCoreException REQUEST_HEADER_FORMAT_ERROR = create("PR400-BI-0001");
        /**
         * Barファイルのファイルサイズが上限値を超えているとき.
         */
        public static final PersoniumCoreException BAR_FILE_SIZE_TOO_LARGE = create("PR400-BI-0002");
        /**
         * Barファイル内エントリのファイルサイズが上限値を超えているとき.
         */
        public static final PersoniumCoreException BAR_FILE_ENTRY_SIZE_TOO_LARGE = create("PR400-BI-0003");
        /**
         * インストール対象のBoxがBox Schemaとして登録済みのとき.
         */
        public static final PersoniumCoreException BAR_FILE_BOX_SCHEMA_ALREADY_EXISTS = create("PR400-BI-0004");
        /**
         * Barファイルのファイルサイズが上限値を超えているとき.
         */
        public static final PersoniumCoreException BAR_FILE_SIZE_INVALID = create("PR400-BI-0005");
        /**
         * JSONファイルの形式が不正なとき.
         */
        public static final PersoniumCoreException JSON_FILE_FORMAT_ERROR = create("PR400-BI-0006");
        /**
         * barファイルがオープンできないとき.
         */
        public static final PersoniumCoreException BAR_FILE_CANNOT_OPEN = create("PR400-BI-0007");
        /**
         * barファイルが読み込めないとき.
         */
        public static final PersoniumCoreException BAR_FILE_CANNOT_READ = create("PR400-BI-0008");
        /**
         * barファイルの構造が正しくないとき.
         */
        public static final PersoniumCoreException BAR_FILE_INVALID_STRUCTURES = create("PR400-BI-0009");
        /**
         * Bar file structure and bar_version do not match.
         */
        public static final PersoniumCoreException BAR_FILE_STRUCTURE_AND_VERSION_MISMATCH = create("PR400-BI-0010");
        /**
         * インストール対象のBoxが登録済みのとき.
         */
        public static final PersoniumCoreException BAR_FILE_BOX_ALREADY_EXISTS = create("PR405-BI-0001");
        /**
         * Httpレスポンスの出力に失敗したとき.
         */
        public static final PersoniumCoreException BAR_FILE_RESPONSE_FAILED = create("PR500-BI-0001");
    }

    /**
     * UI.
     */
    public static class UI {
        /**
         * The corresponding property does not exist.<p>
         * Property [{0}] not configured.
         */
        public static final PersoniumCoreException NOT_CONFIGURED_PROPERTY = create("PR412-UI-0001");
        /**
         * The property being set is not http(s).<p>
         * Property settings error. [{0}] should be normalized URL with http(s), personium-localunit and personium-localcell scheme. // CHECKSTYLE IGNORE - To maintain readability
         */
        public static final PersoniumCoreException PROPERTY_NOT_URL = create("PR412-UI-0002");
        /**
         * Invalid HTTP response was returned.<p>
         * Invalid HTTP response was returned from {0}.
         */
        public static final PersoniumCoreException INVALID_HTTP_RESPONSE = create("PR500-UI-0001");
        /**
         * Connection failed.<p>
         * Could not connect to {0}.
         */
        public static final PersoniumCoreException CONNECTION_FAILED = create("PR500-UI-0002");
    }

    /**
     * その他エラー.
     */
    public static class Misc {
        /**
         * File or Directory does not exist in the snapshot file.
         * <p>
         * {0} : File or Directory path in zip
         */
        public static final PersoniumCoreException NOT_FOUND_IN_SNAPSHOT = create("PR400-MC-0001");
        /**
         * Unexpected URI.
         */
        public static final PersoniumCoreException NOT_FOUND = create("PR404-MC-0001");
        /**
         * メソッドが受け付けられないとき.
         */
        public static final PersoniumCoreException METHOD_NOT_ALLOWED = create("PR405-MC-0001");
        /**
         * サーバ内の処理中にキャンセルされた場合。
         * $batchのタイムアウトで使用。
         */
        public static final PersoniumCoreException SERVER_REQUEST_TIMEOUT = create("PR408-MC-0001");
        /**
         * There is other access to the target cell when processing to the cell.
         */
        public static final PersoniumCoreException CONFLICT_CELLACCESS = create("PR409-MC-0001");
        /**
         * TODO Provisional.
         * Provisional check error of cell import.
         */
        public static final PersoniumCoreException EXPORT_CELL_EXISTS = create("PR409-MC-0002");
        /**
         * ヘッダの前提条件指定が満たされていないとき.
         */
        public static final PersoniumCoreException PRECONDITION_FAILED = create("PR412-MC-0001");
        /**
         * メソッドが未実装のとき.
         */
        public static final PersoniumCoreException METHOD_NOT_IMPLEMENTED = create("PR501-MC-0001");
        /**
         * 未実装機能.
         */
        public static final PersoniumCoreException NOT_IMPLEMENTED = create("PR501-MC-0002");
        /**
         * 同時リクエストが多すぎるとき.
         * 排他制御のタイムアウトで使用。
         */
        public static final PersoniumCoreException TOO_MANY_CONCURRENT_REQUESTS = create("PR503-SV-0001");

    }

    /**
     * Common.
     */
    public static class Common {
        /**
         * Required key missing.
         * <p>
         * {0} : Required key
         */
        public static final PersoniumCoreException REQUEST_BODY_REQUIRED_KEY_MISSING = create("PR400-CM-0001");
        /**
         * Field format error.
         * <p>
         * {0} : Field name
         * {1} : Format
         */
        public static final PersoniumCoreException REQUEST_BODY_FIELD_FORMAT_ERROR = create("PR400-CM-0002");
        /**
         * Unknown key specified.
         * <p>
         * {0} : Unknown key
         */
        public static final PersoniumCoreException REQUEST_BODY_UNKNOWN_KEY_SPECIFIED = create("PR400-CM-0003");
        /**
         * JSON parse error.
         * <p>
         * {0} : Parse string
         */
        public static final PersoniumCoreException JSON_PARSE_ERROR = create("PR400-CM-0004");
        /**
         * Executing API that is not allowed when the cell status is "import failed".
         */
        public static final PersoniumCoreException CELL_STATUS_IMPORT_FAILED = create("PR409-CM-0001");
        /**
         * Error when writing to cell is locked.
         * <p>
         * {0} : Processing that caused lock.
         */
        public static final PersoniumCoreException LOCK_WRITING_TO_CELL = create("PR409-CM-0002");
        /**
         * Failed to load the request body.
         */
        public static final PersoniumCoreException REQUEST_BODY_LOAD_FAILED = create("PR500-CM-0001");
        /**
         * File I/O error.
         * <p>
         * {0} : Overview of failed processing
         */
        public static final PersoniumCoreException FILE_IO_ERROR = create("PR500-CM-0002");
    }

    /**
     * Pluginエラー.
     */
    public static class Plugin {
        /**
         * プラグイン内部でキャッチされず、外に出てきてしまった非チェック例外に対応.
         */
        public static final PersoniumCoreException UNEXPECTED_ERROR = create("PR500-PL-0001");

    }

    String code;
    Severity severity;
    String message;
    int status;

    /**
     * インナークラスを強制的にロードする.
     * エラー分類のインナークラスが追加になったらここに追加すること.
     */
    public static void loadConfig() {
        new OData();
        new Dav();
        new ServiceCollection();
        new Server();
        new Auth();
        new Event();
        new Misc();
        new Plugin();
    }

    /**
     * コンストラクタ.
     * @param status HTTPレスポンスステータス
     * @param severityエラーレベル
     * @param code エラーコード
     * @param message エラーメッセージ
     */
    PersoniumCoreException(final String code,
            final Severity severity,
            final String message,
            final int status,
            final Throwable t) {
        super(t);
        this.code = code;
        this.severity = severity;
        this.message = message;
        this.status = status;
    }

    /**
     * コンストラクタ.
     * @param code エラーコード
     * @param severityエラーレベル
     * @param message エラーメッセージ
     * @param status HTTPレスポンスステータス
     */
    PersoniumCoreException(final String code,
            final Severity severity,
            final String message,
            final int status) {
        this(code, severity, message, status, null);
    }

    /**
     * レスポンスオブジェクトの作成.
     * @return JAX-RS応答オブジェクト
     */
    public Response createResponse() {
        // TODO エラー時、JSONを固定で指定しているが制限解除時にContent-Typeを指定するようにする！！
        return Response.status(status)
                .entity(new ODataErrorMessage(code, message))
                .type(MediaType.valueOf(MediaType.APPLICATION_JSON))
                .build();
    }

    /**
     * ログレベルを返却する.
     * @return ログレベル
     */
    public Severity getSeverity() {
        return this.severity;
    }

    /**
     * HTTPステータスコードを返却する.
     * @return HTTPステータスコード
     */
    public int getStatus() {
        return this.status;
    }

    /**
     * エラーコードを返却する.
     * @return エラーコード
     */
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    /**
     * 原因例外を追加したものを作成して返します.
     * @param t 原因例外
     * @return PersoniumCoreException
     */
    public PersoniumCoreException reason(final Throwable t) {
        // クローンを作成
        PersoniumCoreException ret = new PersoniumCoreException(this.code, this.severity, this.message, this.status, t);
        return ret;
    }

    /**
     * メッセージをパラメタ置換したものを作成して返します. エラーメッセージ上の $1 $2 等の表現がパラメタ置換用キーワードです。
     * @param params 付加メッセージ
     * @return PersoniumCoreMessage
     */
    public PersoniumCoreException params(final Object... params) {
        // 置換メッセージ作成
        String ms = MessageFormat.format(this.message, params);

        // 制御コードのエスケープ処理
        ms = EscapeControlCode.escape(ms);

        // メッセージ置換クローンを作成
        PersoniumCoreException ret = new PersoniumCoreException(this.code, this.severity, ms, this.status);
        return ret;
    }

    /**
     * ファクトリーメソッド.
     * @param code メッセージコード
     * @return PersoniumCoreException
     */
    public static PersoniumCoreException create(String code) {
        int statusCode = parseCode(code);

        // ログレベルの取得
        Severity severity = PersoniumCoreMessageUtils.getSeverity(code);
        if (severity == null) {
            // ログレベルが設定されていなかったらレスポンスコードから自動的に判定する。
            severity = decideSeverity(statusCode);
        }

        // ログメッセージの取得
        String message = PersoniumCoreMessageUtils.getMessage(code);

        return new PersoniumCoreException(code, severity, message, statusCode);
    }

    /**
     * レスポンスコードからログレベルの判定.
     * @param statusCode ステータスコード
     * @return ステータスコードから判定されたログレベル
     */
    static Severity decideSeverity(int statusCode) {
        // 設定が省略されている場合はエラーコードからログレベルを取得
        if (statusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            // 500系の場合はウォーニング（500以上はまとめてウォーニング）
            return Severity.WARN;
        } else if (statusCode >= HttpStatus.SC_BAD_REQUEST) {
            // 400系の場合はインフォ
            return Severity.INFO;
        } else {
            // それ以外の場合は考えられないのでウォーニング.
            // 200系とか300系をPersoniumCoreExceptionで処理する場合はログレベル設定をちゃんと書きましょう.
            return Severity.WARN;
        }
    }

    /**
     * メッセージコードのパース.
     * @param code メッセージコード
     * @return ステータスコードまたはログメッセージの場合は-1。
     */
    static int parseCode(String code) {
        Pattern p = Pattern.compile("^PR(\\d{3})-\\w{2}-\\d{4}$");
        Matcher m = p.matcher(code);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "message code should be in \"PR000-OD-0000\" format. code=[" + code + "].");
        }
        return Integer.parseInt(m.group(1));
    }
}
