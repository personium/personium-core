
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

Personium is licensed under the Apache License, Version 2.0. See [LICENSE.txt](./LICENSE.txt)
