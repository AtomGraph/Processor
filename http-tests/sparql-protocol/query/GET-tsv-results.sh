#!/bin/bash

# use conneg to request TSV results as the preferred format

curl -f -s -G \
  -H "Accept: text/tab-separated-values" \
  "${BASE_URL}sparql" \
  --data-urlencode "query=SELECT * { GRAPH <${BASE_URL}graphs/name/> { <${BASE_URL}named-subject> <http://example.com/named-predicate> ?o } }" \
| tr -s '\r\n' '|' \
| grep "?o|\"named object\"|<${BASE_URL}named-object>|" > /dev/null