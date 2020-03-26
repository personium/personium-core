#!/bin/bash -x

echo 'Remove -SNAPSHOT started.'

# Delete -SNAPSHOT
sed -i 's|\-SNAPSHOT||' pom.xml


git branch
git --version
git diff
git add .

git config --global user.name "Personium Bot"
git config --global user.email "personium.io@gmail.com"

VERSION=$(
  sed -n 's|^    <version>\([0-9]\+\.[0-9]\+\.[0-9]\+\)</version>|\1|p' pom.xml
)
echo ${VERSION}

git commit -m "Update to v${VERSION}"

cat << EOS >~/.netrc
machine github.com
login $GITHUB_USER
password $GITHUB_TOKEN
EOS

git remote -v
git push origin master

echo 'Suceeded!'
