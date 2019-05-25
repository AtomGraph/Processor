#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL_WRITABLE}invalid-update" \
| grep -q "${STATUS_INTERNAL_SERVER_ERROR}"