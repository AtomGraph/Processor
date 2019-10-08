#!/bin/bash

# update string invalid

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}sparql" \
  --data-urlencode "update=WHATEVER" \
| grep -q "${STATUS_BAD_REQUEST}"