#!/bin/bash

# query string invalid, directly as POST body

(
curl -w "%{http_code}\n" -f -s \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-binary @- <<EOF
WHATEVER
EOF
) \
| grep -q "${STATUS_BAD_REQUEST}"