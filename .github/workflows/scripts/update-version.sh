#!/bin/bash -xe

echo 'Update version started.'

# Check git version
git checkout master

MINOR_VERSION=$(
  sed -n 's|^    <version>[0-9]\+\.[0-9]\+\.\([0-9]\+\)</version>|\1|p' pom.xml
)
if [ -z "$MINOR_VERSION" ]; then
  echo 'Cannot get version.'
  exit 1
fi

MINOR_VERSION=$((++MINOR_VERSION))

# Rebase develop onto master branch after removing -SNAPSHOT
git checkout develop
git rebase master

# update version in pom.xml
sed -i \
 "s|^\(    <version>[0-9]\+\.[0-9]\+\.\)[0-9]\+\(</version>\)|\1${MINOR_VERSION}-SNAPSHOT\2|" \
 pom.xml

if [ "${COMPONENT}" = "personium-core" -o "${COMPONENT}" = "personium-engine" ]; then
  # update version in personium-unit-config-default.properties
  sed -i \
    "s|^\(io\.personium\.core\.version=[0-9]\+\.[0-9]\+\.\)[0-9]\+|\1${MINOR_VERSION}|" \
    src/main/resources/personium-unit-config-default.properties
fi

# Git commit and push

VERSION=$(
  sed -n 's|^    <version>\([0-9]\+\.[0-9]\+\.[0-9]\+-SNAPSHOT\)</version>|\1|p' pom.xml
)
echo ${VERSION}

git diff
git add .
git commit -m "Update to v${VERSION}"
git push origin develop

echo 'Suceeded!'
