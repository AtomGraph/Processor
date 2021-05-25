#!/bin/bash

# check that parameter value is parsed according its value type and results in a match

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}value-type-param?object=42" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"42"^^<http://www.w3.org/2001/XMLSchema#integer>' \
| grep -v "${BASE_URL}value-type-object" > /dev/null