#!/bin/bash

curl -w "%{http_code}\n" -f -s -G \
  "${BASE_URL_WRITABLE}graphs/non-existing/" \
| grep -q "${STATUS_NOT_FOUND}"