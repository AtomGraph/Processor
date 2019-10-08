#!/bin/bash

# SPARQL query

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=DESCRIBE *" \
| grep -q "${STATUS_OK}"