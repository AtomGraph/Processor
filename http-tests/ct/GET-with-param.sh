#!/bin/bash

# check that unrecognized parameters are allowed

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}default-subject?param=value" \
| grep -q "${STATUS_OK}"