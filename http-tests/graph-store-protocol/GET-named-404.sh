#!/bin/bash

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}service" \
--data-urlencode "graph=${BASE_URL}non-existing" \
| grep -q "${STATUS_NOT_FOUND}"