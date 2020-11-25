#!/bin/bash

# use conneg to request N-Triples as the preferred format

curl -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}service" \
  --data-urlencode "graph=${BASE_URL}graphs/name/" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "${BASE_URL}named-subject" \
| grep -v "${BASE_URL}default-subject" > /dev/null