#!/bin/bash

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-quads" \
     -H "Content-Type: application/not-accepted" --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}> <http://example.com/default-predicate> "default object" .
EOF
) | egrep -q "$STATUS_UNSUPPORTED_MEDIA"