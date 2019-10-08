#!/bin/bash

# query parameter not set

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}sparql" \
| grep -q "${STATUS_BAD_REQUEST}"