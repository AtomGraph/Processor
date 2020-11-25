#!/bin/bash

# query parameter not set

curl -X POST -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  "${BASE_URL}sparql" \
| grep -q "${STATUS_BAD_REQUEST}"