#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}invalid-update" \
| grep -q "${STATUS_INTERNAL_SERVER_ERROR}"