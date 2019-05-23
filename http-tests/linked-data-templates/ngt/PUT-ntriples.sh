#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# append new resource description

(
curl -w "%{http_code}\n" -f -s \
     -X PUT \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}named-subject" <<EOF
<${BASE_URL_WRITABLE}named-subject> <http://example.com/named-predicate> "named object PUT" .
<${BASE_URL_WRITABLE}named-subject-put> <http://example.com/another-predicate> "another object PUT" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}named-subject" \
| grep '"named object PUT"' > /dev/null