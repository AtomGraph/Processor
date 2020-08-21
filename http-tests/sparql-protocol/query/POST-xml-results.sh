#!/bin/bash

# use conneg to request XML results as the preferred format

curl -f -s \
  -H "Accept: application/sparql-results+xml" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=SELECT * { GRAPH <${BASE_URL}graphs/name/> { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } }" \
| tr -s '\n' '\t' \
| grep "<literal>named object</literal>" \
| grep "<uri>${BASE_URL}named-object</uri>" > /dev/null