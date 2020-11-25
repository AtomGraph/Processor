#!/bin/bash

# SPARQL query with specified named graph URI, directly as POST body

# separate URL-encoding step because we cannot combine -G with --data-binary
encoded_url=$(curl -w "%{url_effective}\n" -G -s -o /dev/null \
--data-urlencode "default-graph-uri=${BASE_URL}graphs/name/" \
  "${BASE_URL}sparql")

(
curl -f -s \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/n-triples" \
  "${encoded_url}" \
  --data-binary @- <<EOF
CONSTRUCT WHERE { ?s ?p ?o }
EOF
) \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "${BASE_URL}named-subject" \
| grep -v "${BASE_URL}default-subject" > /dev/null