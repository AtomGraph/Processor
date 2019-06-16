#!/bin/bash

if [ -z "$ENDPOINT" ] ; then
    echo '$ENDPOINT not set'
    exit 1
fi
if [ -z "$GRAPH_STORE" ] ; then
    echo '$GRAPH_STORE not set'
    exit 1
fi
if [ -z "$ONTOLOGY" ] ; then
    echo '$ONTOLOGY not set'
    exit 1
fi

# if user-defined location mapping exists, append it to system location mapping

if [ -f "$CUSTOM_LOCATION_MAPPING" ] ; then
    cat "$CUSTOM_LOCATION_MAPPING" >> "$LOCATION_MAPPING"
    cat "$LOCATION_MAPPING"
fi

# set Context variables (which are used in $CATALINA_HOME/conf/Catalina/localhost/ROOT.xml)

if [ -n "$ENDPOINT" ] ; then
    ENDPOINT_PARAM="--stringparam sd:endpoint $ENDPOINT "
fi
if [ -n "$GRAPH_STORE" ] ; then
    GRAPH_STORE_PARAM="--stringparam a:graphStore $GRAPH_STORE "
fi
if [ -n "$ONTOLOGY" ] ; then
    ONTOLOGY_PARAM="--stringparam ldt:ontology $ONTOLOGY "
fi
if [ -n "$AUTH_USER" ] ; then
    AUTH_USER_PARAM="--stringparam srv:queryAuthUser $AUTH_USER "
fi
if [ -n "$AUTH_PWD" ] ; then
    AUTH_PWD_PARAM="--stringparam srv:queryAuthPwd $AUTH_PWD "
fi
if [ -n "$PREEMPTIVE_AUTH" ] ; then
    PREEMPTIVE_AUTH_PARAM="--stringparam a:preemptiveAuth $PREEMPTIVE_AUTH "
fi
if [ -n "$SITEMAP_RULES" ] ; then
    SITEMAP_RULES_PARAM="--stringparam ap:sitemapRules $SITEMAP_RULES "
fi

# $CATALINA_HOME must be the WORKDIR at this point

transform="xsltproc \
  --output conf/Catalina/localhost/ROOT.xml \
  $ENDPOINT_PARAM \
  $GRAPH_STORE_PARAM \
  $ONTOLOGY_PARAM \
  $AUTH_USER_PARAM \
  $AUTH_PWD_PARAM \
  $PREEMPTIVE_AUTH_PARAM \
  $SITEMAP_RULES_PARAM \
  conf/Catalina/localhost/context.xsl \
  conf/Catalina/localhost/ROOT.xml"

eval "$transform"

# run Tomcat

if [ -z "$JPDA_ADDRESS" ] ; then
    catalina.sh run
else
    catalina.sh jpda run
fi