#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# delete default graph

curl -w "%{http_code}\n" -f -s -G \
  -X DELETE \
  "${BASE_URL_WRITABLE}service" \
  --data-urlencode "default=true" \
| grep -q "${STATUS_NO_CONTENT}"

curl -w "%{http_code}\n" -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}service" \
  --data-urlencode "default=true" \
| grep -q "${STATUS_OK}"