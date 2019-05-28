#!/bin/bash

curl -f -s -I \
  "${BASE_URL}super" \
| tr -d '\r\n' \
| grep 'Link: <https://github.com/AtomGraph/Processor/blob/develop/http-tests/custom#DefaultSubjectSuperTemplate>; rel=https://www.w3.org/ns/ldt#template' > /dev/null