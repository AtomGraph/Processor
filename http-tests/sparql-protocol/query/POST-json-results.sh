#!/bin/bash

# use conneg to request XML results as the preferred format

curl -f -s \
  -H "Accept: application/sparql-results+json" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=SELECT * { GRAPH <${BASE_URL}graphs/name/> { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } }" \
| tr -s '\n' '\t' \
| grep "{ \"type\": \"literal\" , \"value\": \"named object\" }" \
| grep "{ \"type\": \"uri\" , \"value\": \"${BASE_URL}named-object\" }" > /dev/null