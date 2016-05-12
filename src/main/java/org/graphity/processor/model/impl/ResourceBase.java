/**
 *  Copyright 2012 Martynas Jusevičius <martynas@graphity.org>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.graphity.processor.model.impl;

import com.hp.hpl.jena.graph.Node;
import org.graphity.core.util.StateBuilder;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.sparql.util.Loader;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.sun.jersey.api.core.ResourceContext;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.servlet.ServletConfig;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.jena.riot.RiotException;
import org.graphity.core.MediaTypes;
import org.graphity.core.exception.NotFoundException;
import org.graphity.processor.query.QueryBuilder;
import org.graphity.processor.update.InsertDataBuilder;
import org.graphity.core.util.Link;
import org.graphity.processor.vocabulary.GP;
import org.graphity.core.model.GraphStore;
import org.graphity.core.model.impl.QueriedResourceBase;
import org.graphity.core.model.SPARQLEndpoint;
import org.graphity.core.util.ModelUtils;
import org.graphity.core.vocabulary.G;
import org.graphity.processor.exception.QueryArgumentException;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.query.SelectBuilder;
import org.graphity.processor.util.RulePrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.vocabulary.SP;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 * Base class of generic read-write Graphity Processor resources.
 * Configured declaratively using sitemap ontology to provide Linked Data layer over a SPARQL endpoint.
 * Supports pagination on containers (implemented using SPARQL query solution modifiers).
 * 
 * @author Martynas Jusevičius <martynas@graphity.org>
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/ontology/OntResource.html">OntResource</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#solutionModifiers">15 Solution Sequences and Modifiers</a>
 */
@Path("/")
public class ResourceBase extends QueriedResourceBase implements org.graphity.processor.model.Resource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);
        
    private final GraphStore graphStore;
    private final Ontology ontology;
    private final OntClass matchedOntClass;
    private final OntResource ontResource;
    private final ResourceContext resourceContext;
    private final HttpHeaders httpHeaders;  
    private final String orderBy;
    private final Boolean desc;
    private final Long limit, offset;
    private QueryBuilder queryBuilder;
    private UpdateRequest updateRequest;
    private final QuerySolutionMap querySolutionMap;
    private CacheControl cacheControl;

    /**
     * Public JAX-RS constructor. Suitable for subclassing.
     * If the request URI does not match any URI template in the sitemap ontology, 404 Not Found is returned.
     * 
     * If the matching ontology class is a subclass of <code>gp:Page</code>, this resource becomes a page resource and
     * HATEOS metadata is added (relations to the container and previous/next page resources).
     * 
     * @param uriInfo URI information of the current request
     * @param request current request
     * @param servletConfig webapp context
     * @param mediaTypes supported media types
     * @param endpoint SPARQL endpoint of this resource
     * @param graphStore Graph Store of this resource
     * @param ontology principal ontology
     * @param ontClass matched ontology class
     * @param httpHeaders HTTP headers of the current request
     * @param resourceContext resource context
     */
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, @Context ServletConfig servletConfig,
            @Context MediaTypes mediaTypes, @Context SPARQLEndpoint endpoint, @Context GraphStore graphStore,
            @Context Ontology ontology, @Context OntClass ontClass,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
	super(uriInfo, request, servletConfig, mediaTypes, endpoint);

        if (ontClass == null)
        {
            if (log.isDebugEnabled()) log.debug("Resource {} has not matched any template OntClass, returning 404 Not Found", getURI());
            throw new NotFoundException("Resource has not matched any template");
        }
	if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");        
	if (graphStore == null) throw new IllegalArgumentException("GraphStore cannot be null");
        if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
	if (resourceContext == null) throw new IllegalArgumentException("ResourceContext cannot be null");

        OntModel ontModel = ModelFactory.createOntologyModel(ontClass.getOntModel().getSpecification());
        ontModel.add(ontClass.getModel()); // we don't want to make permanent changes to base ontology which is cached        
        this.ontResource = ontModel.createOntResource(getURI().toString());
        this.ontology = ontology;
        this.matchedOntClass = ontClass;
        this.querySolutionMap = new QuerySolutionMap();
        this.graphStore = graphStore;
	this.httpHeaders = httpHeaders;
        this.resourceContext = resourceContext;

        if (matchedOntClass.getPropertyResourceValue(GP.query) == null)
        {
            if (log.isErrorEnabled()) log.error("Query not defined for template '{}' (gp:query missing)", matchedOntClass.getURI());
            throw new SitemapException("Query not defined for template '" + matchedOntClass.getURI() +"'");
        }
        
        // TO-DO: replace with rdfs:subClassOf inference and matchedOntClass.hasSuperClass(GP.Container, false)
        if (hasSuperClass(matchedOntClass, GP.Container)) // modifiers only apply to containers
        {
            if (uriInfo.getQueryParameters().containsKey(GP.offset.getLocalName()))
                offset = Long.parseLong(uriInfo.getQueryParameters().getFirst(GP.offset.getLocalName()));
            else
            {
                Long defaultOffset = getLongValue(matchedOntClass, GP.defaultOffset);
                if (defaultOffset != null) offset = defaultOffset;
                else offset = Long.valueOf(0);
            }

            if (uriInfo.getQueryParameters().containsKey(GP.limit.getLocalName()))
                limit = Long.parseLong(uriInfo.getQueryParameters().getFirst(GP.limit.getLocalName()));
            else limit = getLongValue(matchedOntClass, GP.defaultLimit);

            if (uriInfo.getQueryParameters().containsKey(GP.orderBy.getLocalName()))
                orderBy = uriInfo.getQueryParameters().getFirst(GP.orderBy.getLocalName());
            else orderBy = getStringValue(matchedOntClass, GP.defaultOrderBy);

            if (uriInfo.getQueryParameters().containsKey(GP.desc.getLocalName()))
                desc = Boolean.parseBoolean(uriInfo.getQueryParameters().getFirst(GP.orderBy.getLocalName()));
            else desc = getBooleanValue(matchedOntClass, GP.defaultDesc);
        }
        else
        {
            offset = limit = null;
            orderBy = null;
            desc = null;
        }
        
        if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with matched OntClass: {}", matchedOntClass);
    }

    /**
     * Post-construct initialization. Subclasses need to call super.init() first, just like with super() constructor.
     */
    @Override
    public void init()
    {
        Resource queryOrTemplateCall = getMatchedOntClass().getPropertyResourceValue(GP.query);
        if (!queryOrTemplateCall.hasProperty(RDF.type, SP.Query))
        {
            Resource spinTemplate = getSPINTemplateFromCall(queryOrTemplateCall);
            StmtIterator constraintIt = spinTemplate.listProperties(SPIN.constraint);
            try
            {
                while (constraintIt.hasNext())
                {
                    Statement stmt = constraintIt.next();
                    Resource constraint = stmt.getResource();
                    {
                        Resource predicate = constraint.getRequiredProperty(SPL.predicate).getResource();
                        String queryVarName = predicate.getLocalName();
                        String queryVarValue = getUriInfo().getQueryParameters(true).getFirst(queryVarName);
                        if (queryVarValue != null)
                        {
                            try
                            {
                                Node queryVarNode = NodeFactoryExtra.parseNode(queryVarValue);
                                querySolutionMap.add(queryVarName, getOntology().getOntModel().asRDFNode(queryVarNode));
                                // TO-DO: check value type using constraint's spl:valueType
                            }
                            catch (RiotException ex)
                            {
                                if (log.isErrorEnabled()) log.error("Invalid query param. Name: \"{}\" Value: \"{}\"", queryVarName, queryVarValue);
                                throw new QueryArgumentException(ex);
                            }
                        }
                    }
                }
            }
            finally
            {
                constraintIt.close();
            }
        }
        
	querySolutionMap.add(SPIN.THIS_VAR_NAME, getOntResource()); // ?this

        if (getRequest().getMethod().equalsIgnoreCase("PUT") || getRequest().getMethod().equalsIgnoreCase("DELETE"))
        {
            Resource updateOrTemplateCall = getMatchedOntClass().getPropertyResourceValue(GP.update);            
            if (updateOrTemplateCall == null)
            {
                if (log.isErrorEnabled()) log.error("Update not defined for template '{}' (gp:update missing)", getMatchedOntClass().getURI());
                throw new SitemapException("Update not defined for template '" + getMatchedOntClass().getURI() +"'");
            }
            updateRequest = getUpdateRequest(updateOrTemplateCall);
        }

        queryBuilder = QueryBuilder.fromQuery(getQuery(queryOrTemplateCall), ModelFactory.createDefaultModel());
        if (getMatchedOntClass().equals(GP.Container) || hasSuperClass(getMatchedOntClass(), GP.Container))
            queryBuilder = getModifiedQueryBuilder(queryBuilder, getLimit(), getOffset(), getOrderBy(), getDesc());

        cacheControl = getCacheControl(getMatchedOntClass(), GP.cacheControl);
        if (log.isDebugEnabled()) log.debug("OntResource {} gets HTTP Cache-Control header value {}", this, cacheControl);
    }
    
    public final Long getLongValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getLong();
        
        return null;
    }

    public final Boolean getBooleanValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getBoolean();
        
        return null;
    }

    public final String getStringValue(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return ontClass.getPropertyValue(property).asLiteral().getString();
        
        return null;
    }
    
    /**
     * Returns sub-resource instance.
     * By default matches any path.
     * 
     * @return resource object
     */
    @Path("{path: .+}")
    public Object getSubResource()
    {
        if (getMatchedOntClass().getPropertyResourceValue(GP.loadClass) != null)
        {
            Resource javaClass = getMatchedOntClass().getPropertyResourceValue(GP.loadClass);
            if (!javaClass.isURIResource())
            {
                if (log.isErrorEnabled()) log.error("gp:loadClass value of class '{}' is not a URI resource", getMatchedOntClass().getURI());
                throw new SitemapException("gp:loadClass value of class '" + getMatchedOntClass().getURI() + "' is not a URI resource");
            }

            Class clazz = Loader.loadClass(javaClass.getURI());
            if (clazz == null)
            {
                if (log.isErrorEnabled()) log.error("Java class with URI '{}' could not be loaded", javaClass.getURI());
                throw new SitemapException("Java class with URI '" + javaClass.getURI() + "' not found");
            }

            if (log.isDebugEnabled()) log.debug("Loading Java class with URI: {}", javaClass.getURI());
            return getResourceContext().getResource(clazz);
        }

        return this;
    }
    
    /**
     * Handles GET request and returns response with RDF description of this resource.
     * In case this resource is a container, a redirect to its first page is returned.
     * 
     * @return response with RDF description, or a redirect in case of container
     */
    @Override
    public Response get()
    {
        // we need this check to avoid building state for gp:SPARQLEndpoint and other system classes
        if (getMatchedOntClass().equals(GP.Container) || hasSuperClass(getMatchedOntClass(), GP.Container) ||
                getMatchedOntClass().equals(GP.Document) || hasSuperClass(getMatchedOntClass(), GP.Document))
        {
            // transition to a URI of another application state (HATEOAS)
            Resource state = getStateBuilder().build();

            if (!state.getURI().equals(getUriInfo().getRequestUri().toString()))
            {
                if (log.isDebugEnabled()) log.debug("Redirecting to a state transition URI: {}", state.getURI());
                return Response.seeOther(URI.create(state.getURI())).build();
            }                    
        }
        
        return super.get();
    }
    
    public StateBuilder getStateBuilder()
    {
        StateBuilder sb = StateBuilder.fromUri(getUriInfo().getAbsolutePath(), getOntResource().getOntModel());
        
        if (getLimit() != null) sb.replaceLiteral(GP.limit, getLimit());
        if (getOffset() != null) sb.replaceLiteral(GP.offset, getOffset());
        if (getOrderBy() != null) sb.replaceLiteral(GP.orderBy, getOrderBy());
        if (getDesc() != null) sb.replaceLiteral(GP.desc, getDesc());
        
        return sb;
    }
    
    /**
     * Handles POST method, stores the submitted RDF model in the default graph of default SPARQL endpoint, and returns response.
     * 
     * @param model the RDF payload
     * @return response
     */
    @Override
    public Response post(Model model)
    {
	return post(model, null);
    }
    
    /**
     * Handles POST method, stores the submitted RDF model in the specified named graph of the specified SPARQL endpoint, and returns response.
     * 
     * @param model the RDF payload
     * @param graphURI target graph name
     * @return response
     */
    public Response post(Model model, URI graphURI)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (log.isDebugEnabled()) log.debug("POSTed Model: {} to GRAPH URI: {}", model, graphURI);

	Resource created = getURIResource(model, RDF.type, GP.Document);
	if (created == null)
	{
	    if (log.isDebugEnabled()) log.debug("POSTed Model does not contain statements with URI as subject and type '{}'", GP.Document.getURI());
	    throw new WebApplicationException(Response.Status.BAD_REQUEST);
	}

        UpdateRequest insertDataRequest;
	if (graphURI != null) insertDataRequest = InsertDataBuilder.fromData(graphURI, model).build();
	else insertDataRequest = InsertDataBuilder.fromData(model).build();

        insertDataRequest.setBaseURI(getUriInfo().getBaseUri().toString());
        if (log.isDebugEnabled()) log.debug("INSERT DATA request: {}", insertDataRequest);

	getSPARQLEndpoint().post(insertDataRequest, null, null);
	
	URI createdURI = UriBuilder.fromUri(created.getURI()).build();
	if (log.isDebugEnabled()) log.debug("Redirecting to POSTed Resource URI: {}", createdURI);
	// http://stackoverflow.com/questions/3383725/post-redirect-get-prg-vs-meaningful-2xx-response-codes
	// http://www.blackpepper.co.uk/posts/201-created-or-post-redirect-get/
	//return Response.created(createdURI).entity(model).build();
	return Response.seeOther(createdURI).build();
    }

    public Resource getURIResource(Model model, Property property, Resource object)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (property == null) throw new IllegalArgumentException("Property cannot be null");
	if (object == null) throw new IllegalArgumentException("Object Resource cannot be null");

        ResIterator it = model.listSubjectsWithProperty(property, object);

	try
	{
	    while (it.hasNext())
	    {
		Resource resource = it.next();

		if (resource.isURIResource()) return resource;
	    }
	}
	finally
	{
	    it.close();
	}
	
	return null;
    }

    /**
     * Handles PUT method, stores the submitted RDF model in the default graph of default SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @return response
     */
    @Override
    public Response put(Model model)
    {
	return put(model, null);
    }
    
    /**
     * Handles PUT method, stores the submitted RDF model in the specified named graph of the specified SPARQL endpoint, and returns response.
     * 
     * @param model RDF payload
     * @param graphURI target graph name
     * @return response
     */
    public Response put(Model model, URI graphURI)
    {
	if (model == null) throw new IllegalArgumentException("Model cannot be null");
	if (log.isDebugEnabled()) log.debug("PUT Model: {} GRAPH URI: {}", model, graphURI);

	if (!model.containsResource(getOntResource()))
	{
	    if (log.isDebugEnabled()) log.debug("PUT Model does not contain statements with request URI '{}' as subject", getURI());
	    throw new WebApplicationException(Response.Status.BAD_REQUEST);
	}
	
	Model description = describe();	
	UpdateRequest deleteInsertRequest = UpdateFactory.create();
	
	if (!description.isEmpty()) // remove existing representation
	{
	    EntityTag entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(model)));
	    Response.ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
	    if (rb != null)
	    {
		if (log.isDebugEnabled()) log.debug("PUT preconditions were not met for resource: {} with entity tag: {}", this, entityTag);
		return rb.build();
	    }
	    
	    UpdateRequest request = getUpdateRequest();
	    if (log.isDebugEnabled()) log.debug("DELETE UpdateRequest: {}", request);
	    Iterator<com.hp.hpl.jena.update.Update> it = request.getOperations().iterator();
	    while (it.hasNext()) deleteInsertRequest.add(it.next());
	}
	
	UpdateRequest insertDataRequest; 
	if (graphURI != null) insertDataRequest = InsertDataBuilder.fromData(graphURI, model).build();
	else insertDataRequest = InsertDataBuilder.fromData(model).build();
	if (log.isDebugEnabled()) log.debug("INSERT DATA request: {}", insertDataRequest);
	Iterator<com.hp.hpl.jena.update.Update> it = insertDataRequest.getOperations().iterator();
	while (it.hasNext()) deleteInsertRequest.add(it.next());
	
	if (log.isDebugEnabled()) log.debug("Combined DELETE/INSERT DATA request: {}", deleteInsertRequest);
	getSPARQLEndpoint().post(deleteInsertRequest, null, null);
	
	if (description.isEmpty()) return Response.created(getURI()).build();
	else return getResponse(model);
    }

    /**
     * Handles DELETE method, deletes the RDF representation of this resource from the default SPARQL endpoint, and
     * returns response.
     * 
     * @return response
     */
    @Override
    public Response delete()
    {
	if (log.isDebugEnabled()) log.debug("DELETEing resource: {} matched OntClass: {}", this, getMatchedOntClass());
	
        UpdateRequest request = getUpdateRequest();
        if (log.isDebugEnabled()) log.debug("DELETE UpdateRequest: {}", request);
	getSPARQLEndpoint().post(request, null, null);
	
	return Response.noContent().build();
    }
    
    public final boolean hasSuperClass(OntClass subClass, OntClass superClass)
    {
        ExtendedIterator<OntClass> extIt = subClass.listSuperClasses(false);
        
        while (extIt.hasNext())
        {
            OntClass nextClass = extIt.next();
            if (nextClass.equals(superClass) || hasSuperClass(nextClass, superClass)) return true;
        }

        return false;
    }
    
    /**
     * Returns <code>Cache-Control</code> HTTP header value, specified on an ontology class with given property.
     * 
     * @param ontClass the ontology class with the restriction
     * @param property the property holding the literal value
     * @return CacheControl instance or null, if no dataset restriction was found
     */
    public CacheControl getCacheControl(OntClass ontClass, AnnotationProperty property)
    {
        if (ontClass.hasProperty(property) && ontClass.getPropertyValue(property).isLiteral())
            return CacheControl.valueOf(ontClass.getPropertyValue(property).asLiteral().getString()); // will fail on bad config

	return null;
    }

    public Query getQuery(Resource queryOrTemplateCall)
    {
	if (queryOrTemplateCall == null) throw new IllegalArgumentException("Resource cannot be null");
        
        if (queryOrTemplateCall.hasProperty(RDF.type, SP.Query)) // works only with subclass inference
        {
            Statement textStmt = queryOrTemplateCall.getRequiredProperty(SP.text);
            if (textStmt == null || !textStmt.getObject().isLiteral())
            {
                if (log.isErrorEnabled()) log.error("SPARQL string not defined for query '{}' (sp:text missing or not a string)", queryOrTemplateCall);
                throw new SitemapException("SPARQL string not defined for query '" + queryOrTemplateCall + "'");                
            }
            
            return getParameterizedSparqlString(textStmt.getString(), null,
                getUriInfo().getBaseUri().toString()).asQuery();
        }

        Resource template = getSPINTemplateFromCall(queryOrTemplateCall);
        Statement bodyStmt = template.getProperty(SPIN.body);
        if (bodyStmt == null || !bodyStmt.getObject().isResource())
        {
            if (log.isErrorEnabled()) log.error("SPIN Template '{}' does not have a body", template);
            throw new SitemapException("SPIN Template does not have a body: '" + template + "'");                            
        }
        
        return getQuery(bodyStmt.getObject().asResource());
    }

    public Resource getSPINTemplateFromCall(Resource templateCall)
    {
	if (templateCall == null) throw new IllegalArgumentException("Resource cannot be null");
        
        Statement typeStmt = templateCall.getProperty(RDF.type);
        if (typeStmt == null || !typeStmt.getObject().isResource() ||
                !typeStmt.getObject().asResource().hasProperty(RDF.type, SPIN.Template))
        {
            if (log.isErrorEnabled()) log.error("'{}' is not a valid SPIN Template call", templateCall);
            throw new SitemapException("Not a valid SPIN Template call: '" + templateCall + "'");                            
        }
        
        return typeStmt.getResource();
    }
    
    /**
     * Returns variable bindings for description query.
     * 
     * @return map with variable bindings
     */
    public QuerySolutionMap getQuerySolutionMap()
    {
        return querySolutionMap;
    }
    
    public UpdateRequest getUpdateRequest(Resource updateOrTemplateCall)
    {
	if (updateOrTemplateCall == null) throw new IllegalArgumentException("Resource cannot be null");

        if (updateOrTemplateCall.hasProperty(RDF.type, SP.Update)) // works only with subclass inference
        {
            Statement textStmt = updateOrTemplateCall.getRequiredProperty(SP.text);
            if (textStmt == null || !textStmt.getObject().isLiteral())
            {
                if (log.isErrorEnabled()) log.error("SPARQL string not defined for update '{}' (sp:text missing or not a string)", updateOrTemplateCall);
                throw new SitemapException("SPARQL string not defined for update '" + updateOrTemplateCall + "'");                
            }
            
            return getParameterizedSparqlString(textStmt.getString(), null,
                getUriInfo().getBaseUri().toString()).asUpdate();
        }

        Resource template = getSPINTemplateFromCall(updateOrTemplateCall);
        Statement bodyStmt = template.getProperty(SPIN.body);
        if (bodyStmt == null || !bodyStmt.getObject().isResource())
        {
            if (log.isErrorEnabled()) log.error("SPIN Template '{}' does not have a body", template);
            throw new SitemapException("SPIN Template does not have a body: '" + template + "'");                            
        }
        
        return getUpdateRequest(bodyStmt.getObject().asResource());
    }
    
    /**
     * Adds matched sitemap class as affordance metadata in <pre>Link</pre> header.
     * 
     * @param model response model
     * @return response builder
     */
    @Override
    public ResponseBuilder getResponseBuilder(Model model)
    {
        ResponseBuilder rb = super.getResponseBuilder(model);
        
        //rb.header("Query", getQuery().toString());
        
        Link classLink = new Link(URI.create(getMatchedOntClass().getURI()), RDF.type.getLocalName(), null);
        rb.header("Link", classLink.toString());
        
        Link ontologyLink = new Link(URI.create(getOntology().getURI()), GP.ontology.getURI(), null);
        rb.header("Link", ontologyLink.toString());

        Link baseLink = new Link(getUriInfo().getBaseUri(), G.baseUri.getURI(), null);
        rb.header("Link", baseLink.toString());
        
        Reasoner reasoner = getMatchedOntClass().getOntModel().getSpecification().getReasoner();
        if (reasoner instanceof GenericRuleReasoner)
        {
            GenericRuleReasoner grr = (GenericRuleReasoner)reasoner;
            rb.header("Rules", RulePrinter.print(grr.getRules())); // grr.getRules().toString() - prevented by JENA-1030 bug
        }
        
        return rb;
    }
    
    public List<Locale> getLanguages(Property property)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        
        List<Locale> languages = new ArrayList<>();
        StmtIterator it = getMatchedOntClass().listProperties(property);
        
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().isLiteral())
                {
                    if (log.isErrorEnabled()) log.error("Illegal language value for template '{}' (gp:language is not literal)", getMatchedOntClass().getURI());
                    throw new SitemapException("Illegal non-literal gp:language value for template '" + getMatchedOntClass().getURI() +"'");
                }
                
                languages.add(Locale.forLanguageTag(stmt.getString()));
            }
        }
        finally
        {
            it.close();
        }
        
        return languages;
    }
    
    @Override
    public List<Locale> getLanguages()
    {
        return getLanguages(GP.lang);
    }
    
    /**
     * Returns value of <samp>limit</samp> query string parameter, which indicates the number of resources per page.
     * This value is set as <code>LIMIT</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * 
     * @return limit value
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modResultLimit">15.5 LIMIT</a>
     */
    @Override
    public Long getLimit()
    {
	return limit;
    }

    /**
     * Returns value of <samp>offset</samp> query string parameter, which indicates the number of resources the page
     * has skipped from the start of the container.
     * This value is set as <code>OFFSET</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * 
     * @return offset value
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOffset">15.4 OFFSET</a>
     */
    @Override
    public Long getOffset()
    {
	return offset;
    }

    /**
     * Returns value of <samp>orderBy</samp> query string parameter, which indicates the name of the variable after
     * which the container (and the page) is sorted.
     * This value is set as <code>ORDER BY</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * Note that ordering might be undefined, in which case the same page might not contain identical resources
     * during different requests.
     * 
     * @return name of ordering variable or null, if not specified
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOrderBy">15.1 ORDER BY</a>
     */
    @Override
    public String getOrderBy()
    {
	return orderBy;
    }

    /**
     * Returns value of <samp>desc</samp> query string parameter, which indicates the direction of resource ordering
     * in the container (and the page).
     * If this method returns true, <code>DESC</code> order modifier is set if this resource is a page
     * (therefore pagination is used).
     * 
     * @return true if the order is descending, false otherwise
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOrderBy">15.1 ORDER BY</a>
     */
    @Override
    public Boolean getDesc()
    {
	return desc;
    }
    
    /**
     * Returns this resource as ontology resource.
     * 
     * @return ontology resource
     */
    public OntResource getOntResource()
    {
	return ontResource;
    }
    
    /**
     * Returns ontology class that this resource matches.
     * If the request URI did not match any ontology class, <code>404 Not Found</code> was returned.
     * 
     * @return ontology class
     */
    @Override
    public OntClass getMatchedOntClass()
    {
	return matchedOntClass;
    }

    /**
     * Returns the principal ontology.
     * 
     * @return ontology
     */
    @Override
    public Ontology getOntology()
    {
        return ontology;
    }

    /**
     * Returns the cache control of this resource, if specified.
     * The control value can be specified as a <code>gp:cacheControl</code> value restriction on an ontology class in
     * the sitemap ontology.
     * 
     * @return cache control object or null, if not specified
     */
    @Override
    public CacheControl getCacheControl()
    {
	return cacheControl;
    }
    
    /**
     * Returns query used to retrieve RDF description of this resource.
     * Query solution bindings are applied by default.
     * 
     * @return query object with applied solution bindings
     * @see #getQuerySolutionMap()
     */
    @Override
    public Query getQuery()
    {
        return getQuery(getQueryBuilder().build().toString(), getQuerySolutionMap(), getUriInfo().getBaseUri().toString());
    }

    public QueryBuilder getModifiedQueryBuilder(QueryBuilder builder, Long limit, Long offset, String orderBy, Boolean desc)
    {
	if (builder == null) throw new IllegalArgumentException("QueryBuilder cannot be null");
                
        if (builder.getSubSelectBuilders().isEmpty())
        {
            if (log.isErrorEnabled()) log.error("QueryBuilder '{}' does not contain a sub-SELECT", queryBuilder);
            throw new SitemapException("Sub-SELECT missing in QueryBuilder: " + queryBuilder +"'");
        }

        SelectBuilder subSelectBuilder = builder.getSubSelectBuilders().get(0);
        if (log.isDebugEnabled()) log.debug("Found main sub-SELECT of the query: {}", subSelectBuilder);

        try
        {
            if (log.isDebugEnabled()) log.debug("Setting OFFSET on container sub-SELECT: {}", offset);
            subSelectBuilder.replaceOffset(offset);

            if (log.isDebugEnabled()) log.debug("Setting LIMIT on container sub-SELECT: {}", limit);
            subSelectBuilder.replaceLimit(limit);

            if (orderBy != null)
            {
                try
                {
                    if (desc == null) desc = false; // ORDERY BY is ASC() by default

                    if (log.isDebugEnabled()) log.debug("Setting ORDER BY on container sub-SELECT: ?{} DESC: {}", orderBy, desc);
                    subSelectBuilder.replaceOrderBy(null). // any existing ORDER BY condition is removed first
                        orderBy(orderBy, desc);
                }
                catch (IllegalArgumentException ex)
                {
                    if (log.isWarnEnabled()) log.warn(ex.getMessage(), ex);
                }
            }
        }
        catch (NumberFormatException ex)
        {
            throw new WebApplicationException(ex);
        }
        
        return builder;
    }
    
     /**
     * Returns query used to retrieve RDF description of this resource
     * 
     * @param command query string
     * @param qsm query solution map to be applied
     * @param baseUri base URI of the query
     * @return query object
     */    
    public Query getQuery(String command, QuerySolutionMap qsm, String baseUri)
    {
	if (command == null) throw new IllegalArgumentException("Command String cannot be null");
     
        return getParameterizedSparqlString(command, qsm, baseUri).asQuery();        
    }

    /**
     * Returns Graph Store of this resource.
     * 
     * @return graph store object
     */
    public GraphStore getGraphStore()
    {
        return graphStore;
    }

    /**
     * Returns query builder, which is used to build SPARQL query to retrieve RDF description of this resource.
     * 
     * @return query builder
     */
    @Override
    public QueryBuilder getQueryBuilder()
    {
        if (queryBuilder == null) throw new IllegalStateException("QueryBuilder should not be null at this point. Check initialization in init()");
        
	return queryBuilder;
    }
    
    public UpdateRequest getUpdateRequest()
    {
        if (updateRequest == null) throw new IllegalStateException("UpdateRequest should not be null at this point. Check initialization in init()");
        
        return getUpdateRequest(updateRequest.toString(), getQuerySolutionMap(), getUriInfo().getBaseUri().toString());
    }

    public ParameterizedSparqlString getParameterizedSparqlString(String command, QuerySolutionMap qsm, String baseUri)
    {
	if (command == null) throw new IllegalArgumentException("Command String cannot be null");
        
        return new ParameterizedSparqlString(command, qsm, baseUri);
    }
    
    /**
     * Returns UpdateRequest with query bindings for the current request applied to the query string.
     * 
     * @param command command string
     * @param qsm query solution map
     * @param baseUri
     * @return update request
     */
    public UpdateRequest getUpdateRequest(String command, QuerySolutionMap qsm, String baseUri)
    {
	if (command == null) throw new IllegalArgumentException("Command String cannot be null");

        return getParameterizedSparqlString(command, qsm, baseUri).asUpdate();
    }

    /**
     * Returns HTTP headers of the current request.
     * 
     * @return header object
     */
    public HttpHeaders getHttpHeaders()
    {
	return httpHeaders;
    }

    public ResourceContext getResourceContext()
    {
        return resourceContext;
    }
    
}