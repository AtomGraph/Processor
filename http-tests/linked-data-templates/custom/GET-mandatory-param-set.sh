#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}mandatory-param?object=mandatory%20object" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"mandatory object"' \
| grep -v "${BASE_URL}mandatory-object" > /dev/null