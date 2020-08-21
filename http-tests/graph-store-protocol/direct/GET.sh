#!/bin/bash

# GET the directly identified named graph

curl -w "%{http_code}\n" -f -s -G \
-H "Accept: text/turtle" \
  "${BASE_URL}graphs/name/" \
| grep -q "${STATUS_OK}"