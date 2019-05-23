#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# append new resource description

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/named-predicate> "named object POST" .
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/another-predicate> "another object POST" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}named-subject-post" \
| tr -d '\n' \
| grep '"named object POST"' \
| grep '"another object POST"' > /dev/null