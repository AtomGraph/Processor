#!/bin/bash

# SPARQL query with specified named graph URI

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=CONSTRUCT { ?s ?p ?o } WHERE { GRAPH <${BASE_URL}graphs/name/> { ?s ?p ?o } }" \
  --data-urlencode "named-graph-uri=${BASE_URL}graphs/name/" \
| rapper -q --input ntriples --output ntriples /dev/stdin - \
| tr -s '\n' '\t' \
| grep "${BASE_URL}named-subject" \
| grep -v "${BASE_URL}another-named-subject" > /dev/null