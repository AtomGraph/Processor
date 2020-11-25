#!/bin/bash

# check that request with "If-None-Match" header is handled correctly

etag=$(curl -f -s -I \
  -H "Accept: application/n-quads" \
  "${BASE_URL}default-subject" | \
grep -Fi "ETag" | \
cut -d " " -f 2)

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-quads" \
  -H "If-None-Match: $etag" \
  "${BASE_URL}default-subject" \
| grep -q "${STATUS_NOT_MODIFIED}"