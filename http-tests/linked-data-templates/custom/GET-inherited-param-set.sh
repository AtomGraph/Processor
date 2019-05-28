#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}inherited-param?object=inherited%20object" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"inherited object"' \
| grep -v "${BASE_URL}inherited-object" > /dev/null