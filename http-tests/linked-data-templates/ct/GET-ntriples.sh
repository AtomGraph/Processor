#!/bin/bash

# use conneg to request N-Triples as the preferred format

curl -f -s \
  -H "Accept: application/n-triples; q=1.0, application/rdf+xml; q=0.9" \
  "${BASE_URL}default-subject" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"default object"' \
| grep "${BASE_URL}default-object" > /dev/null