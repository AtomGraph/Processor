#!/bin/bash

# use conneg to request N-Quads as the preferred format

curl -f -s \
  -H "Accept: application/n-quads; q=1.0, application/rdf+xml; q=0.9" \
  "${BASE_URL}named-subject" \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"named object"' \
| grep "${BASE_URL}named-object" > /dev/null