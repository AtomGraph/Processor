#!/bin/bash

# GET the named graph

curl -w "%{http_code}\n" -f -s -G \
  "${BASE_URL}service" \
  --data-urlencode "graph=${BASE_URL}graph-name" \
| grep -q "${STATUS_OK}"