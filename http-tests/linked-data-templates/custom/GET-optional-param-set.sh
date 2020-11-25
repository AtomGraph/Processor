#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}optional-param?object=optional%20object" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"optional object"' \
| grep -v "${BASE_URL}optional-object" > /dev/null