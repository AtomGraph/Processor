#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# replace the named graph

(
curl -w "%{http_code}\n" -f -s \
  -X PUT \
  -H "Accept: application/n-triples" \
  -H "Content-Type: application/n-triples" \
  --data-binary @- \
  "${BASE_URL}graphs/name/" <<EOF
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/default-predicate> "named object POST" .
<${BASE_URL_WRITABLE}named-subject-post> <http://example.com/another-predicate> "another named object POST" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL}graphs/name/" \
| tr -d '\n' \
| grep '"named object POST"' \
| grep -v '"named object"' > /dev/null