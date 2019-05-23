#!/bin/bash

# use conneg to request N-Quads as the preferred format

curl -f -s \
  -H "Accept: application/n-quads; q=1.0, application/rdf+xml; q=0.9" \
  "${BASE_URL}default-subject" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"default object"' \
| grep "${BASE_URL}default-object" > /dev/null