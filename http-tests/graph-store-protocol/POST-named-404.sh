#!/bin/bash

# separate URL-encoding step because we cannot combine -G with --data-binary
encoded_url=$(curl -w "%{url_effective}\n" -G -s -o /dev/null \
--data-urlencode "graph=${BASE_URL_WRITABLE}non-existing" \
  "${BASE_URL_WRITABLE}service")

curl -w "%{http_code}\n" -f -s \
  -H "Content-Type: application/n-triples" \
  "${encoded_url}" \
| grep -q "${STATUS_NOT_FOUND}"