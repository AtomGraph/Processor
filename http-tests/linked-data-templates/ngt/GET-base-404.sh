#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}" \
| grep -q "${STATUS_NOT_FOUND}"