#!/bin/bash -e

if [ -z "$1" ]; then
  echo 'Usage: bash update-copyright-year.sh <target directory>' 2>&1
  exit 1
fi

CURRENT_YEAR=$(date '+%Y')
echo "Update Copyright year to ${CURRENT_YEAR}"

# 2014 -> 2014-2022
find "$1" -name '*' -type f \
  | xargs sed -i -e "s|\(Copyright [0-9]\{4\}\)\( Personium\)|\1-${CURRENT_YEAR}\2|"

# 2014-2020 -> 2014-2022
find "$1" -name '*' -type f \
  | xargs sed -i -e "s|\(Copyright [0-9]\{4\}-\)\?[0-9]\{4\}\( Personium\)|\1${CURRENT_YEAR}\2|"

echo "Done!"