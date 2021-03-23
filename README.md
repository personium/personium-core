
Personium
====

An open source PDS (Personal Data Store) server software.

https://personium.io/

## Documentation  
[English Documentation](http://personium.io/docs/en/)  
[Japanese Documentation](http://personium.io/docs/ja/)  

## personium-core

`personium-core` is one of components of which Personium Unit consists.

### Requirements

The component `personium-core` needs backends (Elasticsearch, activeMQ, etc.) to be launched, please refer to [personium/ansible](https://github.com/personium/ansible) to prepare them manually.

### Launch

There are two options to launch `personium-core`.

#### Using war file

You can clone and building with below command.

```bash
git clone https://github.com/personium/personium-core
cd personium-core
mvn package
```

After compiling, you can get war file on target folder. ( `target/personium-core.war` )

#### Using docker image

You can build docker container image including `personium-core` based on Tomcat image with below command.

```bash
docker build . -t personium-core
```

After building, you can launch personium-core in docker container.

```bash
docker run -d -p 8080:8080 personium-core
```

You can mount volume to use specivied configuration.

```bash
docker run -d -p 8080:8080 -v /path/to/config.properties:/personium/personium-core/conf/personium-unit-config.properties personium-core
```

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

	Copyright 2017 FUJITSU LIMITED
