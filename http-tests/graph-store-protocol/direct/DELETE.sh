#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# delete directly identified named graph

curl -w "%{http_code}\n" -f -s -G \
  -X DELETE \
  "${BASE_URL_WRITABLE}graphs/name/" \
| grep -q "${STATUS_NO_CONTENT}"

# check that the graph is gone

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}graphs/name/" \
| grep -q "${STATUS_NOT_FOUND}"