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
package com.fujitsu.dc.core.rs;

import java.util.Properties;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.http.HttpStatus;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fujitsu.dc.common.es.EsClient;
import com.fujitsu.dc.core.DcCoreConfig;
import com.fujitsu.dc.core.model.impl.es.EsModel;

/**
 * StatusResourceに対応するJAX-RS Resource クラス.
 */
public class StatusResource {
    static Logger log = LoggerFactory.getLogger(StatusResource.class);
    /** リクエスト送信先URLを取得するプロパティのキー. */
    public static final String PROP_TARGET_URL = "io.personium.test.target";

    /**
     * GETメソッドに対する処理.
     * @return JAS-RS Response
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces("application/json")
    public Response get() {
        StringBuilder sb = new StringBuilder();

        // プロパティ一覧
        Properties props = DcCoreConfig.getProperties();
        JSONObject responseJson = new JSONObject();
        JSONObject propertiesJson = new JSONObject();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            propertiesJson.put(key, value);
        }
        responseJson.put("properties", propertiesJson);

        // Cell作成/削除
        //responseJson.put("service", checkServiceStatus());

        // Adsの死活チェック
        responseJson.put("ads", checkAds());

        // ElasticSearch Health
        EsClient client = EsModel.client();
        JSONObject esJson = new JSONObject();
        esJson.put("health", client.checkHealth());
        responseJson.put("ElasticSearch", esJson);

        sb.append(responseJson.toJSONString());
        return Response.status(HttpStatus.SC_OK).entity(sb.toString()).build();
    }

    /**
     * POSTメソッドに対する処理.
     * @return JAS-RS Response
     */
    @POST
    public Response post() {
        // プロパティリロード
        DcCoreConfig.reload();
        return Response.status(HttpStatus.SC_NO_CONTENT).build();
    }

    /**
     * Adb(MasterDatabase)のコネクション確認.
     * @return 正常に接続できればTrue
     */
    Boolean checkAds() {
        try {
            Properties p = DcCoreConfig.getEsAdsDbcpProps();
            BasicDataSourceFactory.createDataSource(p);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
