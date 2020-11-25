#!/bin/bash

# SPARQL query with specified named graph URI, directly as POST body

# separate URL-encoding step because we cannot combine -G with --data-binary
encoded_url=$(curl -w "%{url_effective}\n" -G -s -o /dev/null \
--data-urlencode "named-graph-uri=${BASE_URL}graphs/name/" \
  "${BASE_URL}sparql")

(
curl -f -s \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/n-triples" \
  "${encoded_url}" \
  --data-binary @- <<EOF
CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <${BASE_URL}graphs/name/> { ?s ?p ?o } }
EOF
) \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "${BASE_URL}named-subject" \
| grep -v "${BASE_URL}another-named-subject" > /dev/null