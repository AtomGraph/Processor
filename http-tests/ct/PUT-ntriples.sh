#!/bin/bash

# re-initialize writable dataset
cat ../dataset-write.trig \
| curl -f -s \
  -X PUT \
  --data-binary @- \
  -H "Content-Type: application/trig" \
  "${ENDPOINT_URL_WRITABLE}data" > /dev/null

# append new resource description

(
curl -w "%{http_code}\n" -f -s \
     -X PUT \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}default-subject" <<EOF
<${BASE_URL_WRITABLE}default-subject> <http://example.com/default-predicate> "default object PUT" .
<${BASE_URL_WRITABLE}default-subject-put> <http://example.com/another-predicate> "another object PUT" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}default-subject" \
| grep '"default object PUT"' > /dev/null