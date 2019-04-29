#!/bin/bash

curl -f -s \
  -H "Accept: application/n-triples" \
  "${BASE_URL}default-subject" \
| tr -s '\n' '\t' \
| grep '"default object"' \
| grep 'http://localhost:8080/default-object' > /dev/null