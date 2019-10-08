#!/bin/bash

# SPARQL query directly as POST body

(
curl -w "%{http_code}\n" -f -s \
  -H "Content-Type: application/sparql-update" \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-binary @- <<EOF
INSERT DATA {}
EOF
) \
| grep -q "${STATUS_OK}"