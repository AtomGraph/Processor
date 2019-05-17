#!/bin/bash

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL}non-existing" \
| grep -q "${STATUS_NOT_FOUND}"