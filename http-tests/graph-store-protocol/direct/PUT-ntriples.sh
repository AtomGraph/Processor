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
  "${BASE_URL_WRITABLE}graphs/name/" <<EOF
<${BASE_URL_WRITABLE}named-subject-put> <http://example.com/default-predicate> "named object PUT" .
<${BASE_URL_WRITABLE}named-subject-put> <http://example.com/another-predicate> "another named object PUT" .
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s -G \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}graphs/name/" \
| tr -d '\n' \
| grep '"named object PUT"' \
| grep -v '"named object"' > /dev/null