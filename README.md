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

No permanent storage!
---------------------

Graphity Processor does *not* include permanent RDF storage. By default it is configured to read the dataset from a file, therefore creating/updating data will have no effect.

In order to store data permanently, you need to set up a [triplestore](http://en.wikipedia.org/wiki/Triplestore) and configure the webapp with its SPARQL endpoint.
For open-source, we recommend trying Jena's [TDB](http://jena.apache.org/documentation/tdb/); for commercial, see [Dydra](http://dydra.com).

Demonstration
=============

An instance of Graphity Client (which embeds all of the Processor's functionality) runs for demonstration purposes on [Linked Data Hub](http://linkeddatahub.com). See the DBPedia Linked Data description of Sir [Tim Berners-Lee](http://linkeddatahub.com/?uri=http%3A%2F%2Fdbpedia.org%2Fresource%2FTim_Berners-Lee).

_Note: the server is not production-grade and DBPedia is often unstable._

Support
=======

Please [report issues](../../issues) if you've encountered a bug or have a feature request.

Commercial Graphity consulting, development, and support are available from [GraphityHQ](http://graphityhq.com).

Community
=========

Please join the W3C [Declarative Linked Data Apps Community Group](http://www.w3.org/community/declarative-apps/) to discuss
and develop Graphity and declarative Linked Data architecture in general.