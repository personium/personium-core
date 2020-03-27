#!/bin/bash -x

echo 'Remove -SNAPSHOT started.'

# Delete -SNAPSHOT
sed -i 's|\-SNAPSHOT||' pom.xml

# git commit and push
VERSION=$(
  sed -n 's|^    <version>\([0-9]\+\.[0-9]\+\.[0-9]\+\)</version>|\1|p' pom.xml
)
echo ${VERSION}

git diff
git add .
git commit -m "Update to v${VERSION}"
git push origin master

echo 'Suceeded!'
