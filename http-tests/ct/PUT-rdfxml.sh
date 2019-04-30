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
     -H "Content-Type: application/rdf+xml" \
     --data-binary @- \
    "${BASE_URL_WRITABLE}default-subject" <<EOF
<?xml version="1.0"?>
<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:ex="http://example.com/">
  <rdf:Description rdf:about="${BASE_URL_WRITABLE}default-subject">
    <ex:default-predicate>default object PUT</ex:default-predicate>
  </rdf:Description>
  <rdf:Description rdf:about="${BASE_URL_WRITABLE}default-subject-put">
    <ex:another-predicate>another object PUT</ex:another-predicate>
  </rdf:Description>
</rdf:RDF>
EOF
) \
| grep -q "${STATUS_OK}"

# check that resource is accessible

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL_WRITABLE}default-subject" \
| grep '"default object PUT"' > /dev/null