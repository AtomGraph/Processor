#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}optional-param" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"optional object"' \
| grep "${BASE_URL}optional-object" > /dev/null