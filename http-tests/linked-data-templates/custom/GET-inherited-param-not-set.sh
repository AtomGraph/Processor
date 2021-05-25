#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}inherited-param" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"inherited object"' \
| grep "${BASE_URL}inherited-object" > /dev/null