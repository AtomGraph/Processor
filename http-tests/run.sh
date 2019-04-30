#!/bin/bash

set -e

export STATUS_OK=200
export STATUS_DELETE_SUCCESS='200|204'
export STATUS_PATCH_SUCCESS='200|201|204'
export POST_SUCCESS='20201|204'
export STATUS_POST_SUCCESS='200|201|204'
export PUT_SUCCESS='201|204'
export STATUS_PUT_SUCCESS='200|201|204'
export STATUS_CREATED=201
export STATUS_NO_CONTENT=204
export STATUS_UPDATED='201|204'
export DELETE_SUCCESS=204
export STATUS_BAD_REQUEST=400
export STATUS_UNAUTHORIZED=401
export STATUS_NOT_FOUND=404
export STATUS_NOT_ACCEPTABLE=406
export STATUS_UNSUPPORTED_MEDIA=415

if [[ -z "$BASE_URL" ]]
then
  export BASE_URL="http://localhost:8080/"
fi
if [[ -z "$BASE_URL_WRITABLE" ]]
then
  export BASE_URL_WRITABLE="http://localhost:8081/"
fi

if [[ -z "$ENDPOINT_URL" ]]
then
  export ENDPOINT_URL="http://localhost:3030/ds/"
fi
if [[ -z "$ENDPOINT_URL_WRITABLE" ]]
then
  export ENDPOINT_URL_WRITABLE="http://localhost:3031/ds/"
fi

SCRIPTS=$(find ./*/ -name '*.sh*')

for script_pathname in $SCRIPTS
do
  echo -n "$script_pathname";
  script_filename=$(basename "$script_pathname")
  script_directory=$(dirname "$script_pathname")
  ( cd $script_directory || exit;
    bash -e "$script_filename";
  )
  if [[ $? == "0" ]]
  then
    echo "   ok"
  else
    echo "   failed";
    (( STORE_ERRORS += 1))
  fi
done