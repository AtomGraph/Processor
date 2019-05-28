#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}optional-default-param" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"optional default object"' \
| grep -v "${BASE_URL}optional-default-object" > /dev/null