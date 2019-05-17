#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}non-match" \
| grep -q "${STATUS_NOT_FOUND}"