#!/bin/bash

# intentionally corrupt N-Triples syntax should give Bad Request

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/n-triples" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<${BASE_URL_WRITABLE}default-subject-post> http://example.com/default-predicate "default object POST" .
EOF
) \
| grep -q "${STATUS_BAD_REQUEST}"