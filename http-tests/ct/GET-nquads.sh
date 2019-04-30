#!/bin/bash

curl -f -v \
  -H "Accept: application/n-quads" \
  "${BASE_URL}default-subject" # \
#| tr -s '\n' '\t' \
#| grep '"default object"' \
#| grep 'http://localhost:8080/default-object' > /dev/null