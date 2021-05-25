#!/bin/bash

# check that parameter value results in query pattern match

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}this-unbound" \
| grep -q "${STATUS_OK}"