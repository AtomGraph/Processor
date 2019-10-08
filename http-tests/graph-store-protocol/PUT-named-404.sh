#!/bin/bash

curl -w "%{http_code}\n" -f -s -G \
  -X PUT \
  -H "Content-Type: application/n-triples" \
  "${BASE_URL_WRITABLE}service" \
--data-urlencode "graph=${BASE_URL_WRITABLE}non-existing" \
| grep -q "${STATUS_NOT_FOUND}"