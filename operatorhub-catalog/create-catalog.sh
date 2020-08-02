#!/bin/bash
#set -x

rm -rf manifests 2> /dev/null

#unpack the oc tool
oc version 2> /dev/null || tar zxf oc.tar.gz && PATH=$(pwd):$PATH

__usage="Usage:
#set variable \$CATALOG_NAME
#the script will create a file named \$CATALOG_NAME.tar.gz
#Example: this will create a file named mycatalog.tar.gz, containing package manifests for the operatorhub catalog
export CATALOG_NAME=mycatalog
./create-catalog.sh

Optionally, you can also set a \$POST_CATALOG_CREATION_SCRIPT variable with its content being a series of commands to run after the tar.gz is created
This can be useful if you want the script to push the catalog tar.gz file to a repository like Artifactory.
Example:
export CATALOG_NAME=mycatalog
export POST_CATALOG_CREATION_SCRIPT='curl -T mycatalog.tar.gz \"http://myrepo.com/mycatalog.tar.gz\"'
./create-catalog.sh
"

if [ -z "${CATALOG_NAME}" ]; then
  echo "$__usage"
  echo "\$CATALOG_NAME is not set. Exiting." >&2
  exit 1
fi

for APP_REG in redhat-operators certified-operators community-operators
do
  echo "fetching $APP_REG package manifests.."
  oc adm catalog build --manifest-dir=manifests --appregistry-org=${APP_REG}
done
echo "creating $CATALOG_NAME.tar.gz file"
tar zcf ${CATALOG_NAME}.tar.gz manifests

FILE_SIZE=$(find "$CATALOG_NAME.tar.gz" -printf "%s")
if [ -z "$FILE_SIZE" ] || [ ! -f "$CATALOG_NAME.tar.gz" ]; then
  echo "could not tar/gz the package manifests successfully. file size is $FILE_SIZE" >&2
  exit 1
fi
echo cleaning up..
rm -rf manifests 2> /dev/null
rm oc > /dev/null 2> /dev/null
echo done
