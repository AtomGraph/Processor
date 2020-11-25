#!/bin/bash

# update parameter not set

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}sparql" \
| grep -q "${STATUS_BAD_REQUEST}"