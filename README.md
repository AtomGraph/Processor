Graphity Processor is a Java backend for building declarative, read-write Linked Data applications. If you have a triplestore with RDF data that you want to serve Linked Data from, or write RDF to over a RESTful HTTP interface, Graphity Processor is the only component you need.

What Graphity Processor provides for users as out-of-the-box generic features:
* declarative control of published and accepted data using URI and SPARQL templates
* pagination on container resources, with ordering by property columns
* constructor mode with returns a CONSTRUCT template of expected data input
* control of RDF input quality with SPARQL-based constraints
* SPARQL endpoint and Graph Store Protocol endpoint
* HTTP content negotiation and caching
* a separate RDF resource for every application state, following the [HATEOAS](http://en.wikipedia.org/wiki/HATEOAS) principle

Graphity's direct use of semantic technologies results in extemely extensible and flexible design and leads the way towards declarative Web development. You can forget all about broken hyperlinks and concentrate on building great apps on quality data.

For a compatible frontend framework for end-user applications, see [Graphity Client](../graphity-client). The Client extends Processor with RDF rendering and form-based input functionality.

Getting started
===============

* [what is Linked Data](../../wiki/What-is-Linked-Data)
* [how Graphity Processor works](../../wiki/How-Graphity-works)
* [installing Graphity Processor](../../wiki/Installation)
* [extending Graphity Processor](../../wiki/Extending-Graphity)
* [configuring Graphity Processor](../../wiki/Configuration)
* [JavaDoc](http://graphity.github.io/graphity-processor/apidocs)

For full documentation, see the [wiki index](../../wiki).

Maven
-----

Graphity artifacts [`graphity-processor`](http://search.maven.org/#browse%7C2124019457) and [`graphity-core`](http://search.maven.org/#browse%7C57568460) are
released on Maven under the [`org.graphity`](http://search.maven.org/#browse%7C1400901156) group ID.

You should normally choose [Graphity Client](../../../graphity-client) as it includes both XSLT and Linked Data functionality, making it useful for end-user as
well as server applications. However, if you do not need XSLT and dependency on Saxon or want to use Client and Processor in a client-server setup, you
can choose `graphity-processor`. Dependencies to other Graphity artifacts will be resolved automagically during the Maven build processs. 

        <dependency>
            <groupId>org.graphity</groupId>
            <artifactId>processor</artifactId>
            <version>1.1.2</version>
        </dependency>        

See more about [installation](../../wiki/Installation).

No permanent storage!
---------------------

Graphity Processor does *not* include permanent RDF storage. By default it is configured to read the dataset from a file, therefore creating/updating data will have no effect.

In order to store data permanently, you need to set up a [triplestore](http://en.wikipedia.org/wiki/Triplestore) and configure the webapp with its SPARQL endpoint.
For open-source, we recommend trying Jena's [TDB](http://jena.apache.org/documentation/tdb/); for commercial, see [Dydra](http://dydra.com).

Demonstration
=============

Here's the Linked Data output of the [root resource](../../wiki/Data-hierarchy) description when Graphity Processor is run as a standalone webapp (for
brevity, all URIs are relativized against the webapp's base URI):

```
@prefix sioc:  <http://rdfs.org/sioc/ns#> .
@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .
@prefix gcs:   <http://graphity.org/gcs#> .
@prefix foaf:  <http://xmlns.com/foaf/0.1/> .
@prefix dct:   <http://purl.org/dc/terms/> .
@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
@prefix gc:    <http://graphity.org/gc#> .
@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix void:  <http://rdfs.org/ns/void#> .
@prefix gp:    <http://graphity.org/gp#> .

<graphity-processor/service>
        a               foaf:Document , gp:GraphStore ;
        dct:title       "Graph Store Protocol endpoint" ;
        sioc:has_space  <graphity-processor/> .

<graphity-processor/templates>
        a                gp:Container , sioc:Container , foaf:Document ;
        gp:slug          "templates"^^xsd:string ;
        dct:title        "Templates" ;
        sioc:has_parent  <graphity-processor/> .

<graphity-processor/ontologies?mode=http://graphity.org/gp%23ConstructMode&forClass=http://graphity.org/gp%23Item>
        a                 gp:Constructor , foaf:Document ;
        gp:constructorOf  <graphity-processor/ontologies> ;
        gp:forClass       gp:Item ;
        gp:mode           gp:ConstructMode .

<graphity-processor/ontologies>
        a                gp:Container , sioc:Container , foaf:Document ;
        gp:slug          "ontologies"^^xsd:string ;
        dct:title        "Ontologies" ;
        sioc:has_parent  <graphity-processor/> .

<graphity-processor/queries?mode=http://graphity.org/gp%23ConstructMode&forClass=http://graphity.org/gp%23Item>
        a                 gp:Constructor , foaf:Document ;
        gp:constructorOf  <graphity-processor/queries> ;
        gp:forClass       gp:Item ;
        gp:mode           gp:ConstructMode .

<graphity-processor/>
        a                gp:Space , sioc:Container , sioc:Space , gp:Container , foaf:Document ;
        rdfs:seeAlso     <graphity-processor/sparql> , gcs: , <http://semantic-web.dk> , <http://graphityhq.com> ;
        dct:description  "Generic Linked Data browser and end-user-oriented platform" ;
        dct:title        "Graphity Client" ;
        foaf:logo        <graphity-processor/static/img/graphity-logo.svg> ;
        foaf:maker       <http://graphityhq.com/#company> .

<graphity-processor/sparql>
        a               foaf:Document , gp:SPARQLEndpoint ;
        dct:title       "SPARQL endpoint" ;
        sioc:has_space  <graphity-processor/> .

<graphity-processor/templates?mode=http://graphity.org/gp%23ConstructMode&forClass=http://graphity.org/gp%23Item>
        a                 gp:Constructor , foaf:Document ;
        gp:constructorOf  <graphity-processor/templates> ;
        gp:forClass       gp:Item ;
        gp:mode           gp:ConstructMode .

<graphity-processor/?offset=0&limit=20>
        a          gp:Page , foaf:Document ;
        gp:limit   "20"^^xsd:long ;
        gp:offset  "0"^^xsd:long ;
        gp:pageOf  <graphity-processor/> ;
        <http://www.w3.org/1999/xhtml/vocab#next>
                <graphity-processor/?offset=20&limit=20> .

<graphity-processor/?mode=http://graphity.org/gp%23ConstructMode&forClass=http://graphity.org/gp%23Container>
        a                 gp:Constructor , foaf:Document ;
        gp:constructorOf  <graphity-processor/> ;
        gp:forClass       gp:Container ;
        gp:mode           gp:ConstructMode .

<graphity-processor/queries>
        a                gp:Container , sioc:Container , foaf:Document ;
        gp:slug          "queries"^^xsd:string ;
        dct:title        "Queries" ;
        sioc:has_parent  <graphity-processor/> .
```

Support
=======

Please [report issues](../../issues) if you've encountered a bug or have a feature request.

Commercial Graphity consulting, development, and support are available from [GraphityHQ](http://graphityhq.com).

Community
=========

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop Graphity and declarative Linked Data architecture in general.