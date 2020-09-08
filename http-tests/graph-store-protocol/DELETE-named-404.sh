#!/bin/bash

# attempt to delete non-existing named graph

curl -w "%{http_code}\n" -f -s -G \
  -X DELETE \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}service" \
--data-urlencode "graph=${BASE_URL_WRITABLE}non-existing" \
| grep -q "${STATUS_NOT_FOUND}"