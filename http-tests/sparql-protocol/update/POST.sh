#!/bin/bash

# SPARQL update

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}sparql" \
  --data-urlencode "update=INSERT DATA {}" \
| grep -q "${STATUS_OK}"