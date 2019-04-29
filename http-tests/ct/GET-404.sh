#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}non-existing" \
| egrep -q "${STATUS_NOT_FOUND}"