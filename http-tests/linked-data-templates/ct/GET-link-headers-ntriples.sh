#!/bin/bash

curl -f -s -I \
  -H "Accept: application/n-triples" \
  "${BASE_URL}default-subject" \
| tr -d '\r\n' \
| grep 'Link: <https://www.w3.org/ns/ldt/core/templates#Document>; rel=https://www.w3.org/ns/ldt#template' \
| grep 'Link: <https://www.w3.org/ns/ldt/core/templates#>; rel=https://www.w3.org/ns/ldt#ontology' \
| grep "Link: <${BASE_URL}>; rel=https://www.w3.org/ns/ldt#base" > /dev/null