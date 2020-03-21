#!/bin/bash

# request N-Triples twice - supply ETag second time and expect 303 Not Modified

etag=$(
curl -f -s -I -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}named-subject" \
| grep 'ETag' \
| sed -En 's/^ETag: (.*)/\1/p')

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}named-subject" \
-H "If-None-Match: $etag" \
| grep -q "${STATUS_NOT_MODIFIED}"