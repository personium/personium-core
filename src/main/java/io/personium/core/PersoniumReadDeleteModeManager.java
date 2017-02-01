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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.PathSegment;

import io.personium.core.model.lock.ReadDeleteModeLockManager;

/**
 * PCSの動作モードを参照するクラス.
 */
public class PersoniumReadDeleteModeManager {
    private PersoniumReadDeleteModeManager() {
    }

    /**
     * ReadDeleteOnlyモード時の許可メソッド.
     */
    static final Set<String> ACCEPT_METHODS = new HashSet<String>(
            Arrays.asList(
                    HttpMethod.GET,
                    HttpMethod.DELETE,
                    HttpMethod.OPTIONS,
                    HttpMethod.HEAD,
                    io.personium.common.utils.PersoniumCoreUtils.HttpMethod.PROPFIND,
                    "REPORT"
                    )
            );

    /**
     * ReadDeleteOnlyモード中の実行可能メソッドかを確認する.
     * @param method リクエストメソッド
     * @return boolean true:許可メソッド false:非許可メソッド
     */
    public static boolean isAllowedMethod(String method) {
        // ReadDeleteOnlyモードでなければ処理を許可する
        if (!ReadDeleteModeLockManager.isReadDeleteOnlyMode()) {
            return true;
        }

        if (!ACCEPT_METHODS.contains(method)) {
            return false;
        }

        return true;
    }

    /**
     * PCSの動作モードがReadDeleteOnlyモードの場合は、参照系リクエストのみ許可する.
     * 許可されていない場合は例外を発生させてExceptionMapperにて処理する.
     * @param method リクエストメソッド
     * @param pathSegment パスセグメント
     */
    public static void checkReadDeleteOnlyMode(String method, List<PathSegment> pathSegment) {
        // ReadDeleteOnlyモードでなければ処理を許可する
        if (!ReadDeleteModeLockManager.isReadDeleteOnlyMode()) {
            return;
        }

        // 認証処理はPOSTメソッドだが書き込みは行わないので例外として許可する
        if (isAuthPath(pathSegment)) {
            return;
        }

        // $batchはPOSTメソッドだが参照と削除のリクエストも実行可能であるため
        // $batch内部で書き込み系処理をエラーとする
        if (isBatchPath(pathSegment)) {
            return;
        }

        // ReadDeleteOnlyモード時に、許可メソッドであれば処理を許可する
        if (ACCEPT_METHODS.contains(method)) {
            return;
        }

        throw PersoniumCoreException.Server.READ_DELETE_ONLY;
    }

    private static boolean isBatchPath(List<PathSegment> pathSegment) {
        String lastPath = pathSegment.get(pathSegment.size() - 1).getPath();
        if ("$batch".equals(lastPath)) {
            return true;
        }
        return false;
    }

    private static boolean isAuthPath(List<PathSegment> pathSegment) {
        // 認証のパスは/cell名/__auth または /cell名/__authz のためサイズを2とする
        if (pathSegment.size() == 2) {
            String lastPath = pathSegment.get(1).getPath();
            if ("__auth".equals(lastPath) || "__authz".equals(lastPath)) {
                return true;
            }
        }
        return false;
    }
}
