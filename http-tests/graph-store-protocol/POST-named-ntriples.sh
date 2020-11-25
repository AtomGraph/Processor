#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# separate URL-encoding step because we cannot combine -G with --data-binary
encoded_url=$(curl -w "%{url_effective}\n" -G -s -o /dev/null \
--data-urlencode "graph=${BASE_URL_WRITABLE}graphs/name/" \
  "${BASE_URL_WRITABLE}service")

# append new triples to the named graph

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${encoded_url}" <<EOF
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/default-predicate> "named object POST" .
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/another-predicate> "another named object POST" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s -G \
  -H "Accept: application/n-triples" \
  "${encoded_url}" \
| tr -d '\n' \
| grep '"named object POST"' \
| grep '"another named object POST"' > /dev/null