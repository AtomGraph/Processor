#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# check that parameter value results in query pattern match

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}optional-param?object=optional%20object" \
| grep -q "${STATUS_NO_CONTENT}"

# check that resource is gone

curl -w "%{http_code}\n" -f -s \
  "${BASE_URL_WRITABLE}optional-param?object=optional%20object" \
| grep -q "${STATUS_NOT_FOUND}"