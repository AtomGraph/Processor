#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -X PUT \
  -H "Accept: application/n-triples" \
  -H "Content-Type: application/n-triples" \
  "${BASE_URL_WRITABLE}non-match" \
| grep -q "${STATUS_CREATED}"