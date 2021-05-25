#!/bin/bash

# check that parameter value results in query pattern non-match

curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}optional-param?object=non-matching-literal" \
| grep -q "${STATUS_NOT_FOUND}"