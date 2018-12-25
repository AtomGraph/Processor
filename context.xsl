<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
    <!ENTITY a      "http://atomgraph.com/ns/core#">
    <!ENTITY ap     "http://atomgraph.com/ns/processor#">
    <!ENTITY ldt    "https://www.w3.org/ns/ldt#">
    <!ENTITY sd     "http://www.w3.org/ns/sparql-service-description#">
    <!ENTITY srv    "http://jena.hpl.hp.com/Service#">
]>

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:a="&a;"
xmlns:ap="&ap;"
xmlns:ldt="&ldt;"
xmlns:sd="&sd;"
xmlns:srv="&srv;"
>
  
    <xsl:output method="xml" indent="yes"/>

    <xsl:param name="sd:endpoint"/>
    <xsl:param name="a:graphStore"/>
    <xsl:param name="ldt:ontology"/>
    <xsl:param name="srv:queryAuthUser"/>
    <xsl:param name="srv:queryAuthPwd"/>
    <xsl:param name="a:preemptiveAuth"/>
    <xsl:param name="ap:sitemapRules"/>

    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="Context">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>

            <xsl:if test="$sd:endpoint">
                <Parameter name="&sd;endpoint" value="{$sd:endpoint}" override="false"/>
            </xsl:if>
            <xsl:if test="$a:graphStore">
                <Parameter name="&a;graphStore" value="{$a:graphStore}" override="false"/>
            </xsl:if>
            <xsl:if test="$ldt:ontology">
                <Parameter name="&ldt;ontology" value="{$ldt:ontology}" override="false"/>
            </xsl:if>
            <xsl:if test="$srv:queryAuthUser">
                <Parameter name="&srv;queryAuthUser" value="{$srv:queryAuthUser}" override="false"/>
            </xsl:if>
            <xsl:if test="$srv:queryAuthPwd">
                <Parameter name="&srv;queryAuthPwd" value="{$srv:queryAuthPwd}" override="false"/>
            </xsl:if>
            <xsl:if test="$a:preemptiveAuth">
                <Parameter name="&a;preemptiveAuth" value="{$a:preemptiveAuth}" override="false"/>
            </xsl:if>
            <xsl:if test="$ap:sitemapRules">
                <Parameter name="&ap;sitemapRules" value="{$ap:sitemapRules}" override="false"/>
            </xsl:if>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>