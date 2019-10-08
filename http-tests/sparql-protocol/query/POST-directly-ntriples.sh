#!/bin/bash

# use conneg to request N-Triples as the preferred format; send query as POST body

(
curl -f -s \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
  --data-binary @- <<EOF
CONSTRUCT { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } { GRAPH <${BASE_URL}graph-name> { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } }
EOF
) \
| rapper -q --input nquads --output nquads /dev/stdin - \
| tr -s '\n' '\t' \
| grep '"named object"' \
| grep "${BASE_URL}named-object" > /dev/null