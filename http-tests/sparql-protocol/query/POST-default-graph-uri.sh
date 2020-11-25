#!/bin/bash

# SPARQL query with specified default graph URI

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=CONSTRUCT WHERE { ?s ?p ?o }" \
  --data-urlencode "default-graph-uri=${BASE_URL}graphs/name/" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "${BASE_URL}named-subject" \
| grep -v "${BASE_URL}default-subject" > /dev/null