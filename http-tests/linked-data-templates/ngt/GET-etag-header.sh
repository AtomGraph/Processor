#!/bin/bash

curl -f -s -I \
  -H "Accept: application/n-triples" \
  "${BASE_URL}named-subject" \
| tr -d '\r\n' \
| grep 'ETag: "' > /dev/null