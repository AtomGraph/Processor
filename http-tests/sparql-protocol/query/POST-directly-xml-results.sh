#!/bin/bash

# use conneg to request N-Triples as the preferred format; send query as POST body

(
curl -f -s \
  -H "Content-Type: application/sparql-query" \
  -H "Accept: application/sparql-results+xml" \
  "${BASE_URL}sparql" \
  --data-binary @- <<EOF
SELECT * { GRAPH <${BASE_URL}graphs/name/> { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } }
EOF
) \
| tr -s '\n' '\t' \
| grep "<literal>named object</literal>" \
| grep "<uri>${BASE_URL}named-object</uri>" > /dev/null