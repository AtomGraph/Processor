#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}mandatory-param?object=mandatory%20object" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"mandatory object"' \
| grep -v "${BASE_URL}mandatory-object" > /dev/null