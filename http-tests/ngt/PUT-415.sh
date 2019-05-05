#!/bin/bash

(
curl -w "%{http_code}\n" -f -s \
     -X PUT \
     -H "Accept: application/n-quads" \
     -H "Content-Type: application/not-accepted" --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}> <http://example.com/named-predicate> "named object" .
EOF
) | grep -q "$STATUS_UNSUPPORTED_MEDIA"