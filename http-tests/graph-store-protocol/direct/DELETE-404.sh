#!/bin/bash

curl -w "%{http_code}\n" -f -s -G \
  -X DELETE \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}graphs/non-existing/" \
| grep -q "${STATUS_NOT_FOUND}"