#!/bin/bash

curl -w "%{http_code}\n" -f -s -G \
  -X PUT \
  -H "Content-Type: application/n-triples" \
  "${BASE_URL_WRITABLE}graphs/non-existing/" \
| grep -q "${STATUS_CREATED}"