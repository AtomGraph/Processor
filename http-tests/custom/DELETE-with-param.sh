#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# check that parameter value results in query pattern match

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}default-subject?object=default%20object" \
| grep -q "${STATUS_NO_CONTENT}"