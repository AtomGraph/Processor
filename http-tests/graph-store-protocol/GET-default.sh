#!/bin/bash

# GET the default graph
# the value of ?default is not required, as per the GSP specification

curl -w "%{http_code}\n" -f -s -G \
  "${BASE_URL}service" \
  --data-urlencode "default=true" \
| grep -q "${STATUS_OK}"