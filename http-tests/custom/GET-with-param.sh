#!/bin/bash

# check that parameter value results in query pattern match

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-quads" \
  "${BASE_URL}default-subject?object=default%20object" \
| grep -q "${STATUS_OK}"