#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}non-existing" \
| egrep -q "${STATUS_NOT_FOUND}"