#!/bin/bash

# check that parameter value results in query pattern match

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}inherited-param?object=inherited%20object" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"inherited object"' \
| grep -v "${BASE_URL}inherited-object" > /dev/null