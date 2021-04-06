#!/bin/bash

# re-initialize writable dataset

initialize_dataset "$BASE_URL_WRITABLE" "../../dataset.trig" "$ENDPOINT_URL_WRITABLE"

# <http://example.com/constrained-predicate> succeeds SHACLConstrainedType constraint validation

(
curl -w "%{http_code}\n" -f -s \
  -H "Accept: application/n-triples" \
  -H "Content-Type: application/n-triples" \
  --data-binary @- \
  "${BASE_URL_WRITABLE}default-subject" <<EOF
<${BASE_URL_WRITABLE}default-subject-post> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://github.com/AtomGraph/Processor/blob/develop/http-tests/custom#SHACLConstrainedType> .
<${BASE_URL_WRITABLE}default-subject-post> <http://example.com/constrained-predicate> "constrained object" .
EOF
) \
| grep -q "${STATUS_OK}"