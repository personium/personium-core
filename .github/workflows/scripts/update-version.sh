#!/bin/bash -xe

echo 'Update version started.'

MINOR_VERSION=$(
  sed -n 's|^    <version>[0-9]\+\.[0-9]\+\.\([0-9]\+\)-SNAPSHOT</version>|\1|p' pom.xml
)
MINOR_VERSION=$((++MINOR_VERSION))

# update version in pom.xml
sed -i \
 "s|^\(    <version>[0-9]\+\.[0-9]\+\.\)[0-9]\+-SNAPSHOT\(</version>\)|\1${MINOR_VERSION}-SNAPSHOT\2|" \
 pom.xml

if [ "${COMPONENT}" = "personium-core" -o "${COMPONENT}" = "personium-engine" ]; then
  # update version in personium-unit-config-default.properties
  sed -i \
    "s|^\(io\.personium\.core\.version=[0-9]\+\.[0-9]\+\.\)[0-9]\+|\1${MINOR_VERSION}|" \
    src/main/resources/personium-unit-config-default.properties
fi

# Git commit and push
git branch
git --version
git diff
git add .

git config --global user.name "Personium Bot"
git config --global user.email "personium.io@gmail.com"

VERSION=$(
  sed -n 's|^    <version>\([0-9]\+\.[0-9]\+\.[0-9]\+-SNAPSHOT\)</version>|\1|p' pom.xml
)
echo ${VERSION}

git commit -m "Update to v${VERSION}"

cat << EOS >~/.netrc
machine github.com
login $GITHUB_USER
password $GITHUB_TOKEN
EOS

git remote -v
git push origin develop

echo 'Suceeded!'
