/*
 * Personium
 * Copyright 2014-2022 Personium Project Authors
 * - FUJITSU LIMITED
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
//Create XMLHttpRequest Object.
function createHttpRequest() {
    //Win IE
    if(window.ActiveXObject) {
        try {
            //MSXML2 or later
            return new ActiveXObject("Msxml2.XMLHTTP");
        } catch (e) {
            try {
                //MSXML
                return new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e2) {
                return null;
            }
         }
    } else if(window.XMLHttpRequest) {
        //Other than Win IE
        return new XMLHttpRequest();
    } else {
        return null;
    }
}

// Reflect the required parameters on the display.
function reflectRequestParameters() {
    // get parameters
    var arg = new Object;
    var pair = location.search.substring(1).split('&');
    for (var i = 0; i < pair.length; i++) {
        var kv = pair[i].split('=');
        arg[kv[0]] = decodeURIComponent(kv[1].replace(/\+/g," "));
    }

    // reflect parameters on the display.
    var queryParamNames = ["response_type", "client_id", "redirect_uri",
            "state", "scope", "access_token"];
    for (var i = 0; i < queryParamNames.length; i++) {
        var paramName = queryParamNames[i];
        if (document.getElementById(paramName) && arg[paramName]) {
            document.getElementById(paramName).value = arg[paramName];
        }
    }
    if (arg["error_description"]) {
        document.getElementById("error_description").textContent = arg["error_description"];
    }
}

//Access the file and check the received contents.
function requestFile(method , appFileName , dataFileName , async ) {
    //Create XMLHttpRequest Object
    var apphttpoj = createHttpRequest();
    var datahttpoj = createHttpRequest();

    //Open method
    apphttpoj.open( method , appFileName , async );
    datahttpoj.open( method , dataFileName , async );

    //Events to be activated upon reception
    apphttpoj.onreadystatechange = function() {
        //4:Message received
        if (apphttpoj.readyState==4) {
            //Callback
            app_on_loaded(apphttpoj);
        }
    };
    datahttpoj.onreadystatechange = function() {
        //4:Message received
        if (datahttpoj.readyState==4) {
            //Callback
            data_on_loaded(datahttpoj);
        }
    };

    //Send method
    apphttpoj.send(null);
    datahttpoj.send(null);
}

//Callback method.
function app_on_loaded(oj) {
    //Get response
    var res  = oj.responseText;

    var data= JSON.parse(res || "null");
    //Display html
    document.getElementById("logo").src = data.Image;
    document.getElementById("appName").textContent = data.DisplayName;
    document.getElementById("description").textContent = data.Description;
}

//Callback method.
function data_on_loaded(oj) {
    //Get response
    var res  = oj.responseText;

    var data= JSON.parse(res || "null");
    //Display html
    document.getElementById("userimg").src = data.Image;
    document.getElementById("dataUserName").textContent = data.DisplayName;
}

// Cancel button.
function onCancel() {
    document.getElementById("cancel_flg").value = "true";
    document.form.submit();
}
