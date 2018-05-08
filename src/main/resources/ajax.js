/*
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
 //XMLHttpRequestオブジェクト生成
  function createHttpRequest(){

    //Win ie用
    if(window.ActiveXObject){
        try {
            //MSXML2以降用
            return new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                //旧MSXML用
                return new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e2) {
                return null;
            }
         }
    } else if(window.XMLHttpRequest){
        //Win ie以外のXMLHttpRequestオブジェクト実装ブラウザ用
        return new XMLHttpRequest();
    } else {
        return null;
    }
  }

  //ファイルにアクセスし受信内容を確認します
  function requestFile(method , appFileName , dataFileName , async )
  {
    //XMLHttpRequestオブジェクト生成
    var apphttpoj = createHttpRequest();
    var datahttpoj = createHttpRequest();

    //open メソッド
    apphttpoj.open( method , appFileName , async );
    datahttpoj.open( method , dataFileName , async );

    //受信時に起動するイベント
    apphttpoj.onreadystatechange = function() {

      //readyState値は4で受信完了
      if (apphttpoj.readyState==4) {
        //コールバック
        app_on_loaded(apphttpoj);
      }
    };
    datahttpoj.onreadystatechange = function() {

        //readyState値は4で受信完了
        if (datahttpoj.readyState==4) {
          //コールバック
          data_on_loaded(datahttpoj);
        }
      };

    //send メソッド
    apphttpoj.send(null);
    datahttpoj.send(null);
  }

  //コールバック関数 ( 受信時に実行されます )
  function app_on_loaded(oj)
  {
        //レスポンスを取得
        var res  = oj.responseText;

        var data= JSON.parse(res || "null");
        //ページで表示
        document.getElementById("logo").src = data.Image;
        document.getElementById("appName").textContent = data.DisplayName;
        document.getElementById("description").textContent = data.Description;
  }

  //コールバック関数 ( 受信時に実行されます )
  function data_on_loaded(oj)
  {
        //レスポンスを取得
        var res  = oj.responseText;

        var data= JSON.parse(res || "null");
        //ページで表示
        document.getElementById("userimg").src = data.Image;
        document.getElementById("dataUserName").textContent = data.DisplayName;
  }

  // キャンセルボタン
  function onCancel() {
      document.getElementById("cancel_flg").value = "1";
      document.form.submit();
  }
