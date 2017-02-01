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
package io.personium.core.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import io.personium.core.PersoniumUnitConfig;
import io.personium.core.PersoniumCoreException;

/**
 * Davのあて先情報を管理するクラス.
 */
public class DavDestination {

    private DavRsCmp destinationRsCmp = null;
    private int destinationHierarchyNumber = 0; // 移動先の階層数
    private DavPath destinationPath = null;
    private DavRsCmp boxRsCmp = null; // box情報
    private String destinationUri = null;

    /**
     * コンストラクタ.
     * @param destinationUriString あて先のパスを示す文字列
     * @param baseUriString ベースURIを示す文字列
     * @param box あて先のBox情報
     * @throws URISyntaxException URIのパースエラー
     */
    public DavDestination(String destinationUriString, String baseUriString, DavRsCmp box) throws URISyntaxException {
        destinationUri = destinationUriString;
        URI destUri = new URI(destinationUriString);
        destinationPath = new DavPath(destUri, baseUriString);
        boxRsCmp = box;
    }

    /**
     * 移動先のUri文字列を取得する.
     * @return 移動先のUri文字列
     */
    public String getDestinationUri() {
        return destinationUri;
    }

    /**
     * 移動先のDavRsCmpを取得する.
     * @return 移動先のDavRsCmp
     */
    public DavRsCmp getDestinationRsCmp() {
        return destinationRsCmp;
    }

    /**
     * 移動先のDavCmpを取得する.
     * @return 移動先のDavCmp
     */
    public DavCmp getDestinationCmp() {
        return destinationRsCmp.getDavCmp();
    }

    /**
     * MOVEメソッドで移動する先のリソースのバリデート処理を行う.
     * @param overwrite 移動先のリソースが既に存在する場合に上書きするかどうか
     * @param davCmp 移動元リソースのDavCmp
     */
    public void validateDestinationResource(String overwrite, DavCmp davCmp) {
        List<String> destinationPaths = this.destinationPath.getResourcePath();
        DavCmp currentCmp =  this.destinationRsCmp.getDavCmp();
        DavCmp parentCmp = this.destinationRsCmp.getParent().getDavCmp();

        // 移動先の途中のペアレントリソースが存在するかをチェック
        checkHasParent(destinationPaths, this.destinationHierarchyNumber);

        // 移動先のリソースを上書きしようとしているかをチェック
        checkIsProhibitedResource(overwrite, currentCmp, parentCmp, davCmp.getType());

        // 親のリソースの子要素数が最大値に達しているかのチェック
        checkParentChildCount(currentCmp, parentCmp);

        // TODO 移動後のリソースの階層がコレクションの階層の深さの最大値を超えていないことのチェック
        checkDepthLimit();

        // 移動元/移動先の名前が違っていてもuuidが同じ場合は404エラーとする。
        // 既に同じリクエストが実行されてしまった場合を考慮して、リロードしたDavNodeのuuidが同じ場合は移動元が存在しない状態とみなす。
        if (equalsDestinationNodeId(davCmp)) {
            throw davCmp.getNotFoundException().params(davCmp.getUrl());
        }
}

    /**
     * 移動後のリソースの階層がコレクションの階層の深さの最大値を超えていないことをチェックする.
     */
    private void checkDepthLimit() {
        // TODO 移動後のリソースの階層がコレクションの階層の深さの最大値を超えていないことのチェック
    }

    /**
     * 親のリソースの子要素数が最大値に達しているかをチェックする.
     * @param currentCmp 移動対象のリソースのDavCmp
     * @param parentCmp 移動先の親リソースのDavCmp
     */
    private void checkParentChildCount(DavCmp currentCmp, DavCmp parentCmp) {
        if (!currentCmp.exists()
                && PersoniumUnitConfig.getMaxChildResourceCount() <= parentCmp.getChildrenCount()) {
            // 移動先にリソースが存在しない、かつ、移動先の親の子要素数がすでに最大値に達している場合
            // ※移動先にすでにリソースが存在する（上書き）の場合は、移動先のリソース作成時に最大値に関するチェックは行われているので、ここでは実施しない
            throw PersoniumCoreException.Dav.COLLECTION_CHILDRESOURCE_ERROR;
        }
    }

    /**
     * 移動先のパスの親リソースが存在するかどうかをチェックする.
     * @param destinationPaths 移動先のパス情報
     */
    private void checkHasParent(List<String> destinationPaths, int hierarchyNumber) {
        if (hierarchyNumber < destinationPaths.size() - 1) {
            // 移動先のパスの途中のリソースが存在しない場合、409エラーとする
            throw PersoniumCoreException.Dav.HAS_NOT_PARENT.params(destinationPaths.get(hierarchyNumber));
        }
    }

    private void checkIsProhibitedResource(String overwrite,
            DavCmp currentCmp,
            DavCmp parentCmp,
            String sourceResourceType) {
        if (currentCmp.exists()) {
            // 移動先のリソースを上書きしようとしている場合に必要なチェック

            if (DavCommon.OVERWRITE_FALSE.equalsIgnoreCase(overwrite)) {
                // OverwriteヘッダにFが指定された場合は上書き不可のためエラーとする
                throw PersoniumCoreException.Dav.DESTINATION_ALREADY_EXISTS;

            } else if (DavCmp.TYPE_COL_SVC.equals(parentCmp.getType())
                    && DavCmp.SERVICE_SRC_COLLECTION.equals(currentCmp.getName())) {
                // Serviceソースコレクションへの上書きは行えないため、エラーとする
                throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_OVERWRITE;

            } else if (!DavCmp.TYPE_DAV_FILE.equals(currentCmp.getType())) {
                // 現状の仕様ではコレクションの上書きは行えないため、エラーとする
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_OVERWRITE;
            }
        } else {
            // 移動後のリソースが新規に作成される場合に必要なチェック

            if (DavCmp.TYPE_COL_ODATA.equals(parentCmp.getType())) {
                // 親のリソースがODataコレクションであるため移動できない
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_ODATA_COLLECTION;

            } else if (DavCmp.TYPE_DAV_FILE.equals(parentCmp.getType())) {
                // 親のリソースがWebDavファイルであるため移動できない
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_FILE;

            } else if (DavCmp.TYPE_COL_SVC.equals(parentCmp.getType())) {
                // 親のリソースがServiceコレクションであるため移動できない
                throw PersoniumCoreException.Dav.RESOURCE_PROHIBITED_TO_MOVE_SERVICE_COLLECTION;
            }
        }

        if (parentCmp.getParent() != null && DavCmp.TYPE_COL_SVC.equals(parentCmp.getParent().getType())
                && !DavCmp.TYPE_DAV_FILE.equals(sourceResourceType)) {
            // ServiceSourceコレクション配下へコレクションは移動できない
            // 例）ServiceSourceコレクション/__src/collection
            throw PersoniumCoreException.Dav.SERVICE_SOURCE_COLLECTION_PROHIBITED_TO_CONTAIN_COLLECTION;
        }
    }

    /**
     * 移動先の階層情報をロードする.
     */
    public void loadDestinationHierarchy() {
        // 移動先のパスが存在するかを最上位から最下層までチェックする
        List<String> destinationPaths = this.destinationPath.getResourcePath();
        DavRsCmp parentRsCmp = boxRsCmp;
        DavRsCmp currentRsCmp = null;
        int pathIndex;
        for (pathIndex = 0; pathIndex < destinationPaths.size(); pathIndex++) {
            DavCmp parentCmp = parentRsCmp.getDavCmp();
            String resourceName = destinationPaths.get(pathIndex);
            DavCmp currentCmp = parentCmp.getChild(resourceName);
            currentRsCmp = new DavRsCmp(parentRsCmp, currentCmp);
            if (!currentCmp.exists()) {
                // 処理中の階層のリソースが存在しない
                break;
            }
            // 次の階層を参照する
            parentRsCmp = currentRsCmp;
        }

        // 階層情報をクラス変数として保持しておく
        this.destinationRsCmp = currentRsCmp;
        this.destinationHierarchyNumber = pathIndex;
    }

    /**
     * 引数で渡されたDavNodeのuuidが移動先DavNodeのuuidと同じかどうかを判定する.
     * @param davCmp 比較対象とするDavNode
     * @return 同じuuidの場合はtrue、それ以外はfalseを返す。<br />
     *         移動先DavNodeの実体が存在しない場合もfalseを返す。
     */
    public boolean equalsDestinationNodeId(DavCmp davCmp) {
        if (null != this.getDestinationCmp() && this.getDestinationCmp().exists()
                && this.getDestinationCmp().getId().equals(davCmp.getId())) {
            return true;
        }
        return false;
    }
}
