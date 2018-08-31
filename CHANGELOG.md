## 1.6.15
IMPROVEMENTS:
* Upgrade to JAX-RS 2.1.([#237](https://github.com/personium/personium-core/issues/237))
* Make default value of "relayhtmlurl" and "authorizationhtmlurl" configurable in the personium-unit-config.properties file.([#240](https://github.com/personium/personium-core/issues/240))
* Unit-level restriction against two cell properties, relayhtmlurl and authorizationhtmlurl.([#241](https://github.com/personium/personium-core/issues/241))

## 1.6.14
IMPROVEMENTS:
* GET against Cell root URL should return HomeApp HTML.([#234](https://github.com/personium/personium-core/issues/234))
* GET against authorization endpoint should return relayed HTML content from preconfigured remote URL.([#235](https://github.com/personium/personium-core/issues/235))

## 1.6.13
IMPROVEMENTS:
* Make PluginException correspond to PersoniumCoreAuthnException.([#231](https://github.com/personium/personium-core/issues/231))

## 1.6.12
BUG FIXES:
* It is necessary to modify the plugin mechanism.([#224](https://github.com/personium/personium-core/issues/224))
* System does not shutdown cleanly.([#226](https://github.com/personium/personium-core/issues/226))

IMPROVEMENTS:
* Change EventInfo in Timer Rule from required to optional.([#225](https://github.com/personium/personium-core/issues/225))

## 1.6.11
BUG FIXES:
* URI returned by response is not URL-encoded.([#214](https://github.com/personium/personium-core/issues/214))
* "_Relation._Box.Name" can not be acquired normally by ExtRole list retrieval.([#220](https://github.com/personium/personium-core/issues/220))

## 1.6.10
IMPROVEMENTS:
* Implement box export pre-function.([#182](https://github.com/personium/personium-core/issues/182), [#185](https://github.com/personium/personium-core/issues/185), [#188](https://github.com/personium/personium-core/issues/188))
* Correspondence of Bar file ver 2.0.([#190](https://github.com/personium/personium-core/issues/190))
* Thread pool support for Box installation function.([#210](https://github.com/personium/personium-core/issues/210))

## 1.6.9
IMPROVEMENTS:
* Corresponds to authorization code grant flow.([#213](https://github.com/personium/personium-core/issues/213))
  * Only a part of the function is implemented. Refer to https://github.com/personium/personium-core/issues/213 for details.

## 1.6.8
BUG FIXES:
* Error occurs when importing snapshot of the cell into different unit.([#183](https://github.com/personium/personium-core/issues/183))
* Token on receiving a message does not include schema information.([#197](https://github.com/personium/personium-core/issues/197))

IMPROVEMENTS:
* Update pom.xml.([#191](https://github.com/personium/personium-core/issues/191))
* HTTP Proxy support.([#193](https://github.com/personium/personium-core/issues/193))
* Close HttpClient.([#195](https://github.com/personium/personium-core/issues/195))
* Improve eventbus and event processing.([#199](https://github.com/personium/personium-core/issues/199))
* Introduce timer event.([#201](https://github.com/personium/personium-core/issues/201))
* Enable select of message broker used on EventBus.([#203](https://github.com/personium/personium-core/issues/203))
* Use embedded message broker on test.([#205](https://github.com/personium/personium-core/issues/205))
* Allow only positive number for EventObject in case of timer rule.([#207](https://github.com/personium/personium-core/issues/207))

## 1.6.7
BUG FIXES:
* Authority of the token acquired by Google account authentication is incorrect.([#173](https://github.com/personium/personium-core/issues/173))
* Some item's value of event are garbled when action 'exec', 'relay' or 'relay.event' is executed.([#175](https://github.com/personium/personium-core/issues/175))

IMPROVEMENTS:
* Unify the values of CollectionKind.([#84](https://github.com/personium/personium-core/issues/84))
* Can't set personium-localcell:/__ctl/Role to EventObject on rule with _Box.Name.([#177](https://github.com/personium/personium-core/issues/177))
* Convert personium-localunit scheme to https scheme on EventSubject.([#179](https://github.com/personium/personium-core/issues/179))

## 1.6.6
BUG FIXES:
* Message API does not send to other Unit.([#37](https://github.com/personium/personium-core/issues/37))
* Cell recursive deletion does not delete WebDAV files.([#71](https://github.com/personium/personium-core/issues/71))
* The body check of CellExport API is incorrect.([#104](https://github.com/personium/personium-core/issues/104))
* BAR file installation does not allow specifying ACL on file basis.([#131](https://github.com/personium/personium-core/issues/131))
* MainBox will not be imported when importing into cell different from exported cell.([#157](https://github.com/personium/personium-core/issues/157))
* When importing to a cell different from the exported cell, contents in the cell move.([#158](https://github.com/personium/personium-core/issues/158))
* Accessing cell during cell import returns 500 error.([#161](https://github.com/personium/personium-core/issues/161))

IMPROVEMENTS:
* Append "/" to the end of URL returned by Box URL acquisition API.([#102](https://github.com/personium/personium-core/issues/102))
* Box URL API does not support CORS.([#112](https://github.com/personium/personium-core/issues/112))
* Let RelayAction, ExecAction be triggered with appropriate access tokens.([#169](https://github.com/personium/personium-core/issues/169))

## 1.6.5
IMPROVEMENTS:
* Change matching of EventInfo to forward match.([#149](https://github.com/personium/personium-core/issues/149))
* Change Type value of Internal Event for odata operation.([#150](https://github.com/personium/personium-core/issues/150))
* Enable to set any string to EventObject in case of external event.([#151](https://github.com/personium/personium-core/issues/151))

## 1.6.4
IMPROVEMENTS:
* Limit Unit User privilege.([#66](https://github.com/personium/personium-core/issues/66))
* Define string constants.([#144](https://github.com/personium/personium-core/issues/144))

## 1.6.3
IMPROVEMENTS:
* Event has no date and time of when the event occurred.([#132](https://github.com/personium/personium-core/issues/132))
* Don't publish rule event in case of external event.([#134](https://github.com/personium/personium-core/issues/134))
* Add new Action 'relay.event' to Rule.([#140](https://github.com/personium/personium-core/issues/140))

BUG FIXES:
* Event log is output to unknown folder.([#136](https://github.com/personium/personium-core/issues/136))
* Scheme of object of internal event in box install is not personium-localcell.([#138](https://github.com/personium/personium-core/issues/138))

## 1.6.2
BUG FIXES:
* Accept Arbitrary String for User OData's __id.([#98](https://github.com/personium/personium-core/issues/98))
* Box installation can not create file directly under the box.([#125](https://github.com/personium/personium-core/issues/125))

IMPROVEMENTS:
* It cannot know a connecting WebSocket state.([#120](https://github.com/personium/personium-core/issues/120))
* Put an upper limit on derived number of times from a source event.([#123](https://github.com/personium/personium-core/issues/123))
* Convert scheme of X-Personium-Box-Schema header from personim-localunit to https in ExecAction.([#127](https://github.com/personium/personium-core/issues/127))

## 1.6.1
BUG FIXES:
* Modify API execution privilege of NullResource.([#95](https://github.com/personium/personium-core/issues/95))

IMPROVEMENTS:
* Enable to receive events with WebSocket.([#116](https://github.com/personium/personium-core/issues/116))

## 1.6.0
NEW FEATURES:
* Event Processing Functions
  * New Cell control objects "Rule" is introduced for managing event processing rules.
  see ([#89](https://github.com/personium/personium-core/issues/89))    

BREAKING CHANGES:
* Interfaces of Messaging and external event acceptance API's are changed: 
  * RequestRelation is now replaced with RequestObjects.
  see ([#103](https://github.com/personium/personium-core/issues/103))
  * External event JSON structure has changed.
  * External events are no longer logged by default without any Rules settings.

IMPROVEMENTS:
* httpclient lib version updated. ([#41](https://github.com/personium/personium-core/issues/41))
* "unitUser.issuers" item in the unit config file now supports URL's with personium-localunit scheme. ([#99](https://github.com/personium/personium-core/issues/99))

## 1.5.8
IMPROVEMENTS:
* Add Box recursive delete function.([#58](https://github.com/personium/personium-core/issues/58))

## 1.5.7
BUG FIXES:
* Add value check processing of RequireSchemaAuthz attribute.([#78](https://github.com/personium/personium-core/issues/78))
* Change execution privilege of GetBoxURL API.([#82](https://github.com/personium/personium-core/issues/82))
* DateTime literal format used in OData $filter is not implemented correctly.([#86](https://github.com/personium/personium-core/issues/86))

IMPROVEMENTS:
* When Unit URL is changed, cells' hidden parameter `Owner` in elasticsearch will be wrong URL.([#40](https://github.com/personium/personium-core/issues/40))
* Unify the linefeed code of file to LF.([#77](https://github.com/personium/personium-core/issues/77))
* Recursive delete of WebDAV collection without user OData.([#68](https://github.com/personium/personium-core/issues/68))

## 1.5.6
IMPROVEMENTS:
* OData with N:N relationship also deletes $links at the same time when deleting entities.([#65](https://github.com/personium/personium-core/issues/65))
* recursive delete of OData Service Collection.([#69](https://github.com/personium/personium-core/issues/69))

## 1.5.5
IMPROVEMENTS:
* Add Cell export import function.(Release.)([#43](https://github.com/personium/personium-core/issues/43))

## 1.5.4
BUG FIXES:
* Memcached Changes the behavior at update failure.([#35](https://github.com/personium/personium-core/issues/35))
* REST API error handling improvement.([#26](https://github.com/personium/personium-core/issues/26))
* Incorrect argument is passed to constructor ODataResource(). ([#44](https://github.com/personium/personium-core/issues/44))

IMPROVEMENTS:
* Add Cell export import function.(Prerelease. Partial abnormal case and tests are not implemented yet.)([#43](https://github.com/personium/personium-core/issues/43))

## 1.5.3
IMPROVEMENTS:
* Improve error output level when installing bar files.([#34](https://github.com/personium/personium-core/issues/34))

BUG FIXES:
* (Provisional coping) Memcached Changes the behavior at update failure.([#35](https://github.com/personium/personium-core/issues/35))

## 1.5.2
BUG FIXES:
* When multiple accesses to the same Box occurred, 500 Errors occurred.([#28](https://github.com/personium/personium-core/issues/28))

IMPROVEMENTS:
* Enable to switch accessible data scope of token from Cell-admin app to general app.([#21](https://github.com/personium/personium-core/issues/21))
* Organize PersoniumUnitConfig.([#32](https://github.com/personium/personium-core/issues/32))

## 1.5.1
IMPROVEMENTS:
* Make relationship registration possible by role. ([#24](https://github.com/personium/personium-core/issues/24))
* If ads: none, unnecessary logs are output.([#25](https://github.com/personium/personium-core/issues/25))
* WebDAV file encryption.([#27](https://github.com/personium/personium-core/issues/27))

## 1.5.0
BUG FIXES:
* Unavailability of TLS 1.2.
 ([#3](https://github.com/personium/personium-client-java/issues/3))

IMPROVEMENTS:
* Enable specification of class URL in RequestRelation of message API.
 ([#19](https://github.com/personium/personium-core/issues/19))
* Change OAuth2Token authentication API endpoint.
 ([#20](https://github.com/personium/personium-core/issues/20))

## 1.4.6
BUG FIXES:
* Implementation of BoxBound function of message API.
 ([#17](https://github.com/personium/personium-core/issues/17))

## 1.4.5
BUG FIXES:
* Fixed about Executing Cell Recursive Delete API does not delete OData in cell. ([#13](https://github.com/personium/personium-core/issues/13))

## 1.4.4

MODIFICATIONS FOR RELIABILITY:
* Add test codes for Accept Weak ETag. ([#5](https://github.com/personium/personium-core/issues/5))

## 1.4.3

IMPROVEMENTS:
* Pluggable architecture for authentication APIs. ([#4](https://github.com/personium/personium-core/issues/4))
* Accept Weak ETag for WebDAV. ([#5](https://github.com/personium/personium-core/issues/5))

MODIFICATIONS FOR SECURITY:
* Abolished temporarily Unit User Promotion `PROPPATCH` API. ([#11](https://github.com/personium/personium-core/issues/11)).

BUG FIXES:
* Test bar file resources are not proper in 1.4.2 . ([#7](https://github.com/personium/personium-core/issues/7))
* Remove PMD warnings and Checkstyle errors. ([#8](https://github.com/personium/personium-core/issues/8))
* Fixed a problem about Cell Administoration privilege `p:root`.

## 1.4.2

BREAKING CHANGES:

 - Rename package name and some parameters from `com.fujitsu.dc` or `dc` to `io.personium` or `personium` . ([#1](https://github.com/personium/personium-core/issues/1))

  * Changed parameters' names are below:

 |#  |Name           | V1.4.1 and previous | 1.4.2 or later | Example or Usage |
 |:-:|:--------------|:------------------------|:---------------|:---------|
 | 1 |Unit Configuration file name|dc-config.properties|personium-unit-config.properties||
 | 2 |XML name space/URN|`xmlns:dc='urn:x-dc1:xmlns'` |`xmlns:p='urn:x-personium:xmlns'`| WebDAV Property data, bar file document or Authn API|
 | 3 |HTTP request header|`X-Dc-Xxxxx`|`X-Personium-Xxxxx`| All APIs with original request header |
 | 4 |POST request parameters' key|`dc_xxxxx`|`p_xxxxx`| APIs with post parameters  | 
 | 5 |Core APIs for Engine Javascript|`dc.xxxx`|`_p.xxxxx`| Engine service script (server-side logic)|

IMPROVEMENTS:
 - Cell level ACL inherits to all Boxes in Cells when you set Box previliges to Cell. ([#2](https://github.com/personium/personium-core/issues/2))
   * Enabled to set the Box previleges (ex. `D:read` or `D:write` ) to Cell ACL.
   * In addition, renamed Cell administoration previlege name from `dc:all` to `p:root`.


 
## 1.4.1

IMPROVEMENTS:
  - core *[UriUtils.java, CellEsImpl.java,TokenEndPointResource.java etc]* :<br>
    A new custom URL scheme `personium-localunit` is introduced for more flexible server operation.<br> 
    It is now supported in major APIs handling URL.
   * You can write `personium-localunit:/cell1/` instead of `https://myunit.example/cell1/` in fields such as  `Box.Schema` or `ExtCell.Url` when referring within the same Unit.
 
   * In previous versions, Unit FQDN change requires data conversion in most cases.
   * By using this new URL scheme, links among Cells on a Unit can be kept without data conversion after Unit FQDN change.
 
BUG FIXES:

  - core *[DcEngineSvcCollectionResource.java etc]*, engine *[FsServiceResourceSourceManager.java etc]*: <br>
    The script file could not be loaded for enabling custom API was fixed. (Issue #27)

  - core *[DavCmpFsImpl.java]*:<br> 
    The bug that the content length is always reset to 0 at updating WebDAV file was fixed.(Issue #29)

## 1.4.0

BREAKING CHANGES:
  - core, engine *[dc-config-default.properties]*:<br>

   Changed unit configuration file keys' prefixes from `com.fujitsu.dc.*` to `io.personium.*`. It is necessary for V1.3.X users to replace all keys' prefixes in `dc-config.properties` file.

IMPROVEMENTS:
  - core *[CellCmpFsImpl.java, BoxCmpFsImpl.java, DavCmpFsImpl.java etc]*, es-api-2.4 *[userdata.json etc]*:<br>

   Supports [elasticsearch v2.4.1](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/release-notes-2.4.1.html). <br>
   * The `es-api-2.4` module is newly developed for elasticsearch v2.4.X support.
   * Personium V1.4.0 (or later) must run with es-api-2.4 module.
   * Current es-api-2.4 supports ElasticSearch v2.4.1.
   * For the incompatibilities between elasticsearch v1.3.X and v2.4.X, the registration formats of data (OData / WebDAV) were changed. (Show in details below.)

BACKWARD INCOMPATIBILITIES:
  - core *[CellCmpFsImpl.java, BoxCmpFsImpl.java, DavCmpFsImpl.java etc]*, es-api-2.4 *[userdata.json etc]*:<br>
   For supporting [elasticsearch v2.4.1](https://www.elastic.co/guide/en/elasticsearch/reference/2.4/release-notes-2.4.1.html), the registration formats of inner data (OData / WebDAV) were changed. <br>
   For these incompatibilities, V1.4.0 cannot run with the same data construction in server as V1.3.X. To upgrade version from V1.3.X, inner data must be converted in new rules below.

   * The WebDAV data are stored in file system. <br>
     Stored directory path is set by `dc-config.properties`,  the default path is `/personium_nfs/dc-core/dav`.
   * Elasticsearch mapping definition key `"l"` was replaced to `"ll"` in `UserData` object.<br>
     (The other objects which have `"l.*"` mapping keys are not changed.)


## 1.3.25

IMPROVEMENTS:
  - core *[Common.java, Box.java, ExtCell.java, ODataUtils.java etc]*:<br>
    Improved formats of Box object's `"Schema"` and ExtCell object's `"Name"`. <br>
    The definitions of these formats are below.<br>

|Object  |Property |Format name  |Proper format definition                      |Examples           |
|:-------|:--------|:------------|:---------------------------------------------|:------------------|
|Box     |Schema   |`Schema URI` |`Cell URL` or URN.                            |http://fqdn/adc/<br>urn:x-dc1:adc|
|ExtCell |Name     |`Cell URL`   |Normalized URL with http(s) scheme and **trailing slash**.|http://fqdn/adc/|

   * The formats of URI, URN and URL are based on [RFC3986](https://tools.ietf.org/html/rfc3986).
   * **Trailing slash** is the character `/` which is in the end of URL.
   * These values are validated at the time of creating/updating `Box` or `ExtCell`.
   * If the above check fails, the response code will be `400 Bad Request`.

## 1.3.24

IMPROVEMENTS:
  - core *[pom.xml etc]*:<br>
   Enabled Java 8 compilation.
   * Compilation on Java 8 used to fail due to incompatibility between Java 7 and 8. Now it is fixed and both Java 7 and 8 can compile the source.
   * Also checked it surely runs on Java 8.

  - core *[Account.java, Common.java, AbstractODataResource.java etc]*:<br>
   Fixed the known issue in V1.3.23 about [Create Account API](https://github.com/personium/io/wiki/Account#create).
   * Account object's `"Type"` value can be validated when you create a new Account.
   * Available values are:<br> `"basic"`,`"oidc:google"` or these space-separated values such as `"oidc:google basic"`.
   * If the above check fails, the response code will be `400 Bad Request`. 

  - core *[TokenEndPointResource.java etc]*:<br>
   Fixed the bug about password authentication.
   * Added checking that Account object's `"Type"` value contains `"basic"` at the time of password authentication.
   * If the above check fails, the response code will be `400 Bad Request`. 

## 1.3.23

IMPROVEMENTS:

  - core *[IdToken.java, TokenEndPointResource.java etc]*:<br>
   Supports [OpenID Connect](http://openid.net/connect/) (OIDC). In V1.3.23, supporting ID provider is **Google** only.
   * When creating OIDC Account, post [Create Account API](https://github.com/personium/io/wiki/Account#create) with  `"Type"` key parameter in the request body such as `{"Name":"[GMAIL_ADDRESS]", "Type":"oidc:google"}`. Account name **must be Gmail address** (ex. `example@gmail.com`) to use Google OIDC. When you create an account which is authenticated with both basic ID/PW and OIDC, separate two values with **space** such as `{"Type":"basic oidc:google"}`. (Without setting of `"Type"` value, the default is `basic`).
   * When utilizing OIDC Authentication, need to set `com.fujitsu.dc.core.oidc.google.trustedClientIds=[CLIENT_ID]` configuration by `dc-confing.properties` file. (You can get client IDs by registering your apps to [Google developer Console](https://console.developers.google.com/home).) If you want to set multiple client IDs, these values must be separated with **space**. You can also configure wild card `*`, but this setting causes more security risks, so we strongly recommend for debugging use only. (Without setting, default value is `*`.)
   * When authenticating an account with OIDC, use [Authentication API](https://github.com/personium/io/wiki/Authentication-and-Authorization) with request body `"grant_type=urn:x-dc1:oidc:google&id_token=[ID_TOKEN]"` . If you need more information with __OAuth2.0 ID Token__, refer to [Google Developers site](https://developers.google.com/identity/protocols/OpenIDConnect).

  - core *[DavDestination.java, DavMoveResource.java, DavCollectionResource.java, DavCmpEsImpl.java, etc.]*:<br>
   * MOVE method([RFC2518](https://tools.ietf.org/html/rfc2518#section-8.9)) for WedDAV collections and stored files are implemented. (Some restrictions apply.) 
   * MOVE method requires `Destination:` header which is absolute URI expressing the name or the directory to be changed to. MOVE method can be used by below items:

    1. WebDAV collections.
    2. OData collections.
    3. Service collections.
    4. WebDAV collection files.
    5. Service collection files under `/__src` directory.

   * If you need more information this API, refer to [Collection Move API Documentation](https://github.com/personium/io/wiki/Collection#move).

   #####API examples:

   ######Rename collection (end slash is required)
   ```curl
   curl -X MOVE "http://[FQDN]/[cell]/[box]/[collection]/[old_name]/" 
   -H "Destination:http://[FQDN]/[cell]/[box]/[collection]/[new_name]/" -i -k -s
   ```

   ######Rename file 
   ```curl
   curl -X MOVE "http://[FQDN]/[cell]/[box]/[collection]/[dir]/old.txt"
   -H "Destination:http://[FQDN]/[cell]/[box]/[collection]/[dir]/new.txt" -i -k -s
   ```

   ######Move file 
   ```curl
   curl -X MOVE "http://[FQDN]/[cell]/[box]/[collection]/[from]/file.txt"
   -H "Destination:http://[FQDN]/[cell]/[box]/[collection]/[to]/file.txt" -i -k -s
   ```

  - core *[UserSchemaODataProducer.java ]*:<br>
    PUT methods to change the name of following items are implemented.
   * EntityType
   * Property
   * ComplexTypeProperty
   * AssociationEnd

KNOWN ISSUES:
  - core :
   When [creating new Account](https://github.com/personium/io/wiki/Account#create), the posted `"Type"` value is not validated. New account can be created whatever the `"Type"` value is, such mistaking values as `"Type": "basic oidc:facebook"` (not supported ID Provider), `"Type": "basic_oidc:google"` (separated by underscore) etc.

## 1.3.22a

BACKWARD INCOMPATIBILITIES:

  - core *[DcCoreAuthnException.java]*:
     Response code for authentication failure with OAuth 2.0 (__auth endpoint) has been changed as follows.

    | Versions         | Response code & header      |
    | :-----------     | :-------------------------- |
    | Prior to V1.3.22 | 401 with/without authentication header depending on authentication type. |
    | V1.3.22          | 401 with header "WWW-Authenticate: xxxxx" |
    | From V1.3.22a    | Basic authentication: 400 with header "WWW-Authenticate: Basic".  Client authentication: See KNOWN ISSUES below. |


KNOWN ISSUES:

  - core :
    Response code for client authentication failure with OAuth 2.0 (__auth endpoint) should be 401 and include 
    "WWW-Authenticate" response header. However current version of personium.io returns response code 400 without 
    authenticate header due to compatibility for existing applications.


## 1.3.22

IMPROVEMENTS:

  - core *[EsQueryHandler.java]*:
    Implemented `ne` (not equal) operator for OData $filter query. List of supported operators and functions follows.

    | Operator | Description           | Example                                                                  | Note |
    | :------- | :-------------------  | :----------------------------------------------------------------------- | :--- |
    | eq       | Equal                 |  \$filter=itemKey eq 'searchValue'  <br/> \$filter=itemkey eq 10         |      |
    | ne       | Not equal             | $filter=itemKey ne 'searchValue'                                         |      |
    | gt       | Greater than          | $filter=itemKey gt 1000                                                  |      |
    | ge       | Greater than or equal | $filter=itemKey ge 1000                                                  |      |
    | lt       | Less than             | $filter=itemKey lt 1000                                                  |      |
    | le       | Less than or equal    | $filter=itemKey le 1000                                                  |      |
    | gt       | Greater than          | $filter=itemKey gt 1000                                                  |      |
    | and      | Logical and           | $filter=itemKey eq 'searchValue1' and itemKey2 eq 'searchValue2'         |      |
    | or       | Logical or            | $filter=itemKey eq 'searchValue1' or itemKey2 eq 'searchValue2'          |      |
    | ()       | Precedence grouping   | $filter=itemKey eq 'searchValue' or (itemKey gt 500 and itemKey lt 1500) |      |

    | Function    | Description        | Example                                       | Note                         |
    | :---------- | :----------------- | :-------------------------------------------- | :--------------------------- |
    | startswith  |                    | $filter=startswith(itemKey, 'searchValue')    | Null value is not supported. |
    | substringof |                    | $filter=substringof('searchValue1', itemKey1) | Null value is not supported. |

BUG FIXES:

  - core *[EsQueryHandler.java, DcOptionsQueryParser.java, DcCoreExceptoin.java]*:
    Unexpected result or error was retunred when unsupported operator or function is specified in query. Now returns Bad request (400).

  - core *[EsQueryHandler.java]*:
    No data was returned when searching with query that contains control codes as an operand. Fixed.

BACKWARD INCOMPATIBILITIES:

  - core *[EsQueryHandler.java, DcOptionsQueryParser.java, FilterConditionValidator.java, DcCoreExceptoin.java]*:
    Due to the above improvements and bug fixes, $filter behavior has been changed as follows:

    || When undefined property is specified as query operand. |
    |:--- |:----|
    | Prior to V1.3.22 | Nothing is Returned. |
    | From V1.3.22     | Bad Request(400) |
 
    || When the format of operand value is different from the type of property. |
    |:--- |:----|
    | Prior to V1.3.22 | If the operand value is castable to the type of assocaiated property, the operand is treated as valid.<br/>If not castable, retunrs Bad Request(400).  |
    | From V1.3.22     | Bad Request(400) |

    || When operand value is out of range for the type of property.|
    |:--- |:---- |
    | Prior to V1.3.22 | The operand value is treated as a valid operand, but may cause either unexpected result or error.|
    | From V1.3.22     | Bad Request(400) |

    || To search data including \\ (back-slash) |
    |:--- |:---- |
    | Prior to V1.3.22 | No escaping is required in query value.. |
    | From V1.3.22     | Escaping '\' (back-slash) required, such as '\\\\' |




## 1.3.21

BREAKING CHANGES:

  - core *[AccessConetxt.java, etc.]*:
    Supports Basic authentication (user, password) with Authorization header to allow access to the resource.

BACKWARD INCOMPATIBILITIES:

  - core *[BoxPrivilege.java, ODataSvcCollectionResource.java, ODataSvcSchemaResource.java]*:
    Added `alter-schema` privilege.  Prior to 1.3.21, OData schema can be changed with `write` privilege,  but from 1.3.21, `alter-schema` privilege is required to change the schema.

  - core *[BoxUrlResource.java]*:
    Changed response code of "Get Box URL" API from 302 to 200 to prevent redirection to the "Location URL" on some environment.

IMPROVEMENTS:

  - core *[DcCoreConfig.java, AuthUtils.java]*:
    Password salt was hard-coded and the same value was used for every personium runtime,  so that it could be a threat in terms of security. Now it can be specified with individual value in dc-config.properties.

  - core *[BinaryDataAccessor.java]*:
    Corrected file write operation to ensure that the data is flushed and synced to the storage device.

BUG FIXES:

  - core *[DcEngineSvcCollectionResource.java]*, engine *[DcResponse.java]*: 
    Status code 500 was returned when "Transfer-Encoding: chuncked" header was given on engine response. Fixed.

  - core *[AccessContext.java, DcCoreAuthzException.java, etc.]*:
    Authentication and authorization behavior is corrected to comply with HTTP RFC.

