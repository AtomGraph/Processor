#!/bin/bash

(
curl -w "%{http_code}\n" -f -s \
     -X PUT \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}> <http://example.com/default-predicate> "new object PUT" .
EOF
) \
| grep -q "${STATUS_CREATED}"