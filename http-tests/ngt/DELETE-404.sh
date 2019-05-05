#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}non-existing" \
| grep -q "${STATUS_NOT_FOUND}"