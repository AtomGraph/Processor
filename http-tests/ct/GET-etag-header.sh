#!/bin/bash

curl -f -s -I \
  -H "Accept: application/n-quads" \
  "${BASE_URL}default-subject" \
| tr -d '\r\n' \
| grep 'ETag: "' > /dev/null