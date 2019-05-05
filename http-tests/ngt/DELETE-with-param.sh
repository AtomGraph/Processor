#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../dataset-write.trig" "$ENDPOINT_URL_WRITABLE"

# check that unrecognized parameters are allowed

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}named-subject" \
| grep -q "${STATUS_NO_CONTENT}"