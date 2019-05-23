#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# append new resource description

(
curl -w "%{http_code}\n" -f -s \
     -H "Accept: application/n-triples" \
     -H "Content-Type: application/rdf+xml" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}" <<EOF
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:ex="http://example.com/">
  <rdf:Description rdf:about="${BASE_URL_WRITABLE}named-subject-post">
    <ex:named-predicate>named object POST</ex:named-predicate>
    <ex:another-predicate>another object POST</ex:another-predicate>
  </rdf:Description>
</rdf:RDF>
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}named-subject-post" \
| tr -d '\n' \
| grep '"named object POST"' \
| grep '"another object POST"' > /dev/null