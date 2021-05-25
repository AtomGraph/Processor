#!/bin/bash

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/not-accepted" --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}> <http://example.com/default-predicate> "default object" .
EOF
) | grep -q "$STATUS_UNSUPPORTED_MEDIA"