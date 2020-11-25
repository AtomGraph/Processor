#!/bin/bash

# query string invalid

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=WHATEVER" \
| grep -q "${STATUS_BAD_REQUEST}"