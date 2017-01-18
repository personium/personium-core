
Personium
====

An open source PDS (Personal Data Store) server software.

http://personium.io/

## Components

	core                    - Core module of Personium
	engine                  - Module to enable server-side JS execution.
	cell-sweeper            - A batch program to delete the cells that are marked to be deleted.
	logback                 - A logback customization to rotate the event logs.
	logback-settings        - A shell command to run logback as a daemon process .
	es-api-2.4              - Abstraction layer to absorb version incompatibilities of ElasticSearch.
	common                  - Common modules used in the above modules.
	engine-extension-common - Common modules for implementing Engine Extension.
	client-java             - Java client. Engine internally uses it.
	client-js               - JavaScript client for web browsers.

## Documentation

http://personium.io/docs/

Wiki pages are also available.
https://github.com/personium/io/wiki

## Set up

1. Try Personium  on your machine !

	Please refer __io-vagrant-ansible__ page. ([io-vagrant-ansible](https://github.com/personium/io-vagrant-ansible))

2. Try Personium on your cloud !

	Please refer __io-setup-ansible__ page. ([io-setup-ansible](https://github.com/personium/io-setup-ansible))
	
## License

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

	Copyright 2016 FUJITSU LIMITED
