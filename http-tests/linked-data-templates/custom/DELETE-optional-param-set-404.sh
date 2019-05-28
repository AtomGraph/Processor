#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# check that parameter value results in query pattern non-match

curl -w "%{http_code}\n" -f -s \
  -X DELETE \
  "${BASE_URL_WRITABLE}optional-param?object=non-matching-literal" \
| grep -q "${STATUS_NOT_FOUND}"