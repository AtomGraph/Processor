#!/bin/bash

# check that error responses include RDF description of the error

curl -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}non-match" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "<http://www.w3.org/2011/http#sc> <http://www.w3.org/2011/http-statusCodes#NotFound> ." > /dev/null