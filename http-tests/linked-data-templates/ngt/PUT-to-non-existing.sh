#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# set new resource description

(
curl -w "%{http_code}\n" -f -s \
     -X PUT \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}non-existing" <<EOF
<${BASE_URL_WRITABLE}non-existing> <http://example.com/default-predicate> "new object PUT" .
<${BASE_URL_WRITABLE}non-existing-put> <http://example.com/another-predicate> "another new object PUT" .
EOF
) \
| grep -q "${STATUS_CREATED}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}non-existing" \
| grep '"new object PUT"' > /dev/null