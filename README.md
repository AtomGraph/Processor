AtomGraph Processor is a Java backend for building declarative, read-write Linked Data applications. If you have a triplestore with RDF data that you want to serve Linked Data from, or write RDF to over a RESTful HTTP interface, AtomGraph Processor is the only component you need.

What AtomGraph Processor provides for users as out-of-the-box generic features:
* declarative control of published and accepted data using URI and SPARQL templates
* pagination on container resources, with ordering by property columns
* control of RDF input quality with SPARQL-based constraints
* SPARQL endpoint and Graph Store Protocol endpoint
* HTTP content negotiation and caching
* a separate RDF resource for every application state, following the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) principle

AtomGraph's direct use of semantic technologies results in extemely extensible and flexible design and leads the way towards declarative Web development. You can forget all about broken hyperlinks and concentrate on building great apps on quality data. For more details, see [articles and presentations](../../wiki/Articles-and-presentations) about AtomGraph.

For a compatible frontend framework for end-user applications, see [AtomGraph Web-Client](../../../Web-Client). The Client extends Processor with RDF rendering and form-based input functionality.

Getting started
===============

* [how AtomGraph Processor works](../../wiki/How-Processor-works)
* [installing AtomGraph Processor](../../wiki/Installation)
* [configuring AtomGraph Processor](../../wiki/Configuration)
* [JavaDoc](http://graphity.github.io/graphity-processor/apidocs)

For full documentation, see the [wiki index](../../wiki).

Docker
------

Processor is available from Docker Hub as [`atomgraph/processor`](https://hub.docker.com/r/atomgraph/processor/) image.
It accepts the following environment variables (that become webapp context parameters):
* `ENDPOINT`
* `GRAPH_STORE`
* `ONTOLOGY`
* `AUTH_USER`
* `AUTH_PWD`
* `PREEMPTIVE_AUTH`
* `SITEMAP_RULES`
* `LOCATION_MAPPING`

Run the container like this:

     docker run -p 8080:8080 \
       -e ENDPOINT="http://dbpedia.org/sparql" \
       -e GRAPH_STORE="http://dbpedia.org/service" \
       -e ONTOLOGY="https://www.w3.org/ns/ldt/core/templates#" \
       -v "~/WebRoot/AtomGraph/Processor/src/main/resources/log4j.properties":"/usr/local/tomcat/webapps/ROOT/WEB-INF/classes/log4j.properties" \
       atomgraph/processor

Maven
-----

Processor 1.1.4 artifact is not yet released on Maven central.

No permanent storage!
---------------------

AtomGraph Processor does *not* include permanent RDF storage. By default it is configured to read the dataset from a file, therefore creating/updating data will have no effect.

In order to store data permanently, you need to set up a [triplestore](http://en.wikipedia.org/wiki/Triplestore) and configure the webapp with its SPARQL endpoint.
For open-source, we recommend trying Apache Jena's [Fuseki](https://jena.apache.org/documentation/fuseki2/); for commercial, see [Dydra](http://dydra.com).

Demonstration
=============

Here's the Linked Data output of the [root resource](../../wiki/Document-hierarchy) description when AtomGraph Processor is run as a standalone webapp (for
brevity, all URIs are relativized against the webapp's base URI):

```
@prefix sioc:  <http://rdfs.org/sioc/ns#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix foaf:  <http://xmlns.com/foaf/0.1/> .
@prefix dct:   <http://purl.org/dc/terms/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix gc:    <http://atomgraph.com/ns/client#> .
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix void:  <http://rdfs.org/ns/void#> .
@prefix ldt:   <https://www.w3.org/ns/ldt#> .

# services

<sparql>
        a                   ldt:SPARQLEndpoint ;
        dct:title           "SPARQL endpoint" ;
        sioc:has_space      <> .

<service>
        a                   ldt:GraphStore ;
        dct:title           "Graph Store Protocol endpoint" ;
        sioc:has_space      <> .

# root container

<>
        a                   ldt:Container ;
        rdfs:seeAlso        <sparql> , <http://atomgraph.com> ;
        dct:description     "Generic Linked Data processor" ;
        dct:title           "AtomGraph Processor" ;
        foaf:maker          <http://atomgraph.com/#company> .

# child containers

<ontologies/>
        a                   ldt:Container ;
        ldt:slug            "ontologies" ;
        dct:title           "Ontologies" ;
        sioc:has_parent     <> .

<queries/>
        a                   ldt:Container ;
        ldt:slug            "queries" ;
        dct:title           "Queries" ;
        sioc:has_parent     <> .

<templates/>
        a                   ldt:Container ;
        ldt:slug            "templates" ;
        dct:title           "Templates" ;
        sioc:has_parent     <> .

# page

<?offset=0&limit=20>
        a                   ldt:Page ;
        ldt:limit            "20"^^xsd:long ;
        ldt:offset           "0"^^xsd:long ;
        ldt:pageOf           <> ;
        <http://www.w3.org/1999/xhtml/vocab#next>
                            <?offset=20&limit=20> .
```

Support
=======

Please [report issues](../../issues) if you've encountered a bug or have a feature request.

Commercial AtomGraph consulting, development, and support are available from [AtomGraph](http://atomgraph.com).

Community
=========

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop AtomGraph and declarative Linked Data architecture in general.
