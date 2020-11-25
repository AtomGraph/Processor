#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# delete resource

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}default-subject" \
| grep -q "${STATUS_NO_CONTENT}"

# check that deleted resource is really gone

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL_WRITABLE}default-subject" \
| grep -q "${STATUS_NOT_FOUND}"