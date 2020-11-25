#!/bin/bash

# GET the named graph
# request N-Triples twice - supply ETag second time and expect 303 Not Modified

etag=$(
curl -f -s -I -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}service" \
  --data-urlencode "graph=${BASE_URL}graphs/name/" \
| grep 'ETag' \
| sed -En 's/^ETag: (.*)/\1/p')

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}service" \
  --data-urlencode "graph=${BASE_URL}graphs/name/" \
  -H "If-None-Match: $etag" \
| grep -q "${STATUS_NOT_MODIFIED}"