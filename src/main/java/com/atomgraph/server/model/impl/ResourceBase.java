/**
 *  Copyright 2012 Martynas Jusevičius <martynas@atomgraph.com>
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
package com.atomgraph.server.model.impl;

import com.atomgraph.core.MediaTypes;
import org.apache.jena.ontology.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.update.UpdateRequest;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.Path;
import javax.ws.rs.core.*;
import com.atomgraph.core.model.Service;
import com.atomgraph.core.model.impl.QueriedResourceBase;
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.TemplateCall;
import com.atomgraph.server.util.InsertDataBuilder;
import com.atomgraph.spinrdf.vocabulary.SPIN;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Response.Status;
import org.apache.jena.sparql.util.ClsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of generic read-write Linked Data resources.
 * Configured declaratively using sitemap ontology to provide Linked Data layer over a SPARQL endpoint.
 * Supports pagination on containers (implemented using SPARQL query solution modifiers).
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="http://jena.apache.org/documentation/javadoc/jena/org/apache/jena/ontology/OntResource.html">OntResource</a>
 * @see <a href="http://www.w3.org/TR/sparql11-query/#solutionModifiers">15 Solution Sequences and Modifiers</a>
 */
@Path("/")
public class ResourceBase extends QueriedResourceBase implements com.atomgraph.server.model.Resource, com.atomgraph.server.model.QueriedResource
{
    private static final Logger log = LoggerFactory.getLogger(ResourceBase.class);
        
    private final com.atomgraph.processor.model.Application application;
    private final Ontology ontology;
    private final Optional<TemplateCall> templateCall;
    private final OntResource ontResource;
    private final ResourceContext resourceContext;
    private final HttpHeaders httpHeaders;
    private final QuerySolutionMap querySolutionMap;
    private final Query query;
    private final Resource queryResource, updateResource;
    private final UpdateRequest update;

    /**
     * Public JAX-RS instance. Suitable for subclassing.
     * If the request URI does not match any URI template in the sitemap ontology, 404 Not Found is returned.
     * 
     * If the matching template extends <code>ldt:Document</code>, this resource becomes a page resource and
     * HATEOS metadata is added (relations to the container and previous/next page resources).
     * 
     * @param application LDT application
     * @param mediaTypes mediaTypes
     * @param uriInfo URI information of the current request
     * @param request current request
     * @param service SPARQL service
     * @param ontology LDT ontology
     * @param templateCall templateCall
     * @param httpHeaders HTTP headers of the current request
     * @param resourceContext resource context
     */
    @Inject
    public ResourceBase(@Context UriInfo uriInfo, @Context Request request, MediaTypes mediaTypes,
            Service service, com.atomgraph.processor.model.Application application, Optional<Ontology> ontology, Optional<TemplateCall> templateCall,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
        this(uriInfo, request, mediaTypes, uriInfo.getAbsolutePath(),
                service, application, ontology.orElse(null), templateCall,
                httpHeaders, resourceContext);
    }

    protected ResourceBase(final UriInfo uriInfo, final Request request, final MediaTypes mediaTypes, final URI uri,
            final Service service, final com.atomgraph.processor.model.Application application, final Ontology ontology, final Optional<TemplateCall> templateCall,
            final HttpHeaders httpHeaders, final ResourceContext resourceContext)
    {
        super(uriInfo, request, mediaTypes, uri, service);

        if (application == null) throw new IllegalArgumentException("Application cannot be null");
        if (ontology == null) throw new IllegalArgumentException("Ontology cannot be null");
        if (httpHeaders == null) throw new IllegalArgumentException("HttpHeaders cannot be null");
        if (resourceContext == null) throw new IllegalArgumentException("ResourceContext cannot be null");

        // we are not making permanent changes to base ontology because OntologyProvider always makes a copy
        this.application = application;
        this.ontology = ontology;
        this.ontResource = ontology.getOntModel().createOntResource(uri.toString());
        this.templateCall = templateCall;
        this.httpHeaders = httpHeaders;
        this.resourceContext = resourceContext;

        if (templateCall.isPresent())
        {
            if (log.isDebugEnabled()) log.debug("Constructing ResourceBase with matched Template: {}", templateCall.get().getTemplate());
            
            this.querySolutionMap = templateCall.get().getQuerySolutionMap();
            this.querySolutionMap.add(SPIN.THIS_VAR_NAME, ontResource); // ?this
            
            queryResource = templateCall.get().getTemplate().getQuery();
            if (queryResource != null)
            {
                if (queryResource.canAs(com.atomgraph.spinrdf.model.TemplateCall.class))
                    query = new ParameterizedSparqlString(queryResource.as(com.atomgraph.spinrdf.model.TemplateCall.class).getTemplate().getBody().getText(),
                        querySolutionMap, uriInfo.getBaseUri().toString()).asQuery();
                else
                {
                    if (queryResource.canAs(com.atomgraph.spinrdf.model.Query.class))
                        query = new ParameterizedSparqlString(queryResource.as(com.atomgraph.spinrdf.model.Query.class).getText(),
                            querySolutionMap, uriInfo.getBaseUri().toString()).asQuery();
                    else
                        query = null;
                }
            }
            else
                query = null;

            updateResource = templateCall.get().getTemplate().getUpdate();
            if (updateResource != null)
            {
                if (updateResource.canAs(com.atomgraph.spinrdf.model.TemplateCall.class))
                    update = new ParameterizedSparqlString(updateResource.as(com.atomgraph.spinrdf.model.TemplateCall.class).getTemplate().getBody().getText(),
                        querySolutionMap, uriInfo.getBaseUri().toString()).asUpdate();
                else
                {
                    if (updateResource.canAs(com.atomgraph.spinrdf.model.update.Update.class))
                        update = new ParameterizedSparqlString(updateResource.as(com.atomgraph.spinrdf.model.update.Update.class).getText(),
                            querySolutionMap, uriInfo.getBaseUri().toString()).asUpdate();
                    else
                        update = null;
                }
            }
            else
                update = null;
        }
        else
        {
            querySolutionMap = null;
            queryResource = null;
            query = null;
            updateResource = null;
            update = null;
        }
    }
    
    /**
     * Returns sub-resource class. If we want Jersey to manage the life-cycle of the sub-resource, we need to return a class, not an instance.
     * By default matches any path.
     * @see <a href="https://eclipse-ee4j.github.io/jersey.github.io/documentation/latest/jaxrs-resources.html#d0e2641">Example 3.20. Sub-resource locators returning sub-type</a>
     * 
     * @return resource object
     */
    @Path("{path: .+}")
    @Override
    public Class getSubResource()
    {
        if (getTemplateCall().isPresent() && getTemplateCall().get().getTemplate().getLoadClass() != null)
        {
            Resource javaClass = getTemplateCall().get().getTemplate().getLoadClass();
            if (!javaClass.isURIResource())
            {
                if (log.isErrorEnabled()) log.error("ldt:loadClass value of template '{}' is not a URI resource", getTemplateCall().get().getTemplate());
                throw new OntologyException("ldt:loadClass value of template '" + getTemplateCall().get().getTemplate() + "' is not a URI resource");
            }

            Class clazz = ClsLoader.loadClass(javaClass.getURI());
            if (clazz == null)
            {
                if (log.isErrorEnabled()) log.error("Java class with URI '{}' could not be loaded", javaClass.getURI());
                throw new OntologyException("Java class with URI '" + javaClass.getURI() + "' not found");
            }

            if (log.isDebugEnabled()) log.debug("Loading Java class with URI: {}", javaClass.getURI());
            return clazz;
        }

        return getClass();
    }

    @Override
    public Response get()
    {
        if (!getTemplateCall().isPresent())
        {
            if (log.isDebugEnabled()) log.debug("Resource {} has not matched any ldt:Template, returning 404 Not Found", getURI());
            throw new NotFoundException("Resource has not matched any ldt:Template");
        }
        // cannot be validated in constructor in Jersey 2.x: https://github.com/eclipse-ee4j/jersey/issues/4436
        if (getQueryResource() == null)
        {
            if (log.isErrorEnabled()) log.error("ldt:query value for template '{}' is missing", getTemplateCall().get().getTemplate());
            throw new OntologyException("ldt:query value for template '" + getTemplateCall().get().getTemplate() + "' is missing");
        }
        if (!getQueryResource().canAs(com.atomgraph.spinrdf.model.Query.class) &&
                !getQueryResource().canAs(com.atomgraph.spinrdf.model.TemplateCall.class))
        {
            if (log.isErrorEnabled()) log.error("ldt:query value for template '{}' cannot be cast to sp:Query", getQueryResource());
            throw new OntologyException("ldt:query value of template '" + getQueryResource() + "' cannot be cast to sp:Query");
        }
        
        return super.get();
    }
    
    /**
     * Handles <code>POST</code> method. Appends the submitted RDF representation to the application's dataset.
     * 
     * @param model the RDF payload
     * @return response <code>200 OK</code>
     */
    @Override
    public Response post(Model model)
    {
        if (model == null) throw new IllegalArgumentException("Model cannot be null");

        try
        {
            UpdateRequest insertData = InsertDataBuilder.fromModel(model).
                base(getUriInfo().getBaseUri().toString()).
                graph(getURI().toString()).
                build();
            
            if (log.isDebugEnabled()) log.debug("INSERT DATA UpdateRequest");
            getService().getEndpointAccessor().update(insertData, Collections.<URI>emptyList(), Collections.<URI>emptyList());
            
            return Response.ok().build();
        }
        catch (IOException ex)
        {
            if (log.isErrorEnabled()) log.error("Error while building INSERT DATA update request for URI: {}", getURI());
            throw new InternalServerErrorException(ex);
        }
    }

    /**
     * Handles <code>PUT</code> method. Deletes the resource description (if any) and
     * appends the submitted RDF representation to the application's dataset.
     * 
     * @param model RDF payload
     * @return response <code>201 Created</code> if resource did not exist, <code>200 OK</code> if it did
     */
    @Override
    public Response put(Model model)
    {
        try
        {
            delete();
            
            return post(model);
        }
        catch (NotFoundException ex)
        {
            post(model);
            
            return Response.created(getURI()).build();
        }
    }

    /**
     * Handles <code>DELETE</code> method, deletes the RDF representation of this resource from the application's dataset, and
     * returns response.
     * 
     * @return response <code>204 No Content</code>
     */
    @Override
    public Response delete()
    {
        get(); // will throw NotFoundException if no Template matched or resource does not exist (query result is empty)

        // cannot be validated in constructor in Jersey 2.x: https://github.com/eclipse-ee4j/jersey/issues/4436
        if (getUpdateResource() != null &&
                !getUpdateResource().canAs(com.atomgraph.spinrdf.model.update.Update.class) &&
                !getUpdateResource().canAs(com.atomgraph.spinrdf.model.TemplateCall.class))
        {
            if (log.isErrorEnabled()) log.error("ldt:update value for template '{}' cannot be cast to a sp:Update", getTemplateCall().get().getTemplate());
            throw new OntologyException("ldt:update value for template '" + getTemplateCall().get().getTemplate() + "' cannot be cast to a sp:Update");
        }

        if (getUpdate() == null) return Response.status(Status.NOT_IMPLEMENTED).build();

        if (log.isDebugEnabled()) log.debug("DELETE UpdateRequest: {}", getUpdate());
        getService().getEndpointAccessor().update(getUpdate(), Collections.<URI>emptyList(), Collections.<URI>emptyList());

        return Response.noContent().build(); // subsequent GET might still return 200 OK, depending on query solution map
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
        
    /**
     * Content languages supported by the matching LDT template.
     * @see <a href="https://tools.ietf.org/html/rfc7231#section-3.1.3.2">Content-Language</a>
     * 
     * @return list of locales
     */
    @Override
    public List<Locale> getLanguages()
    {
        return getTemplateCall().get().getTemplate().getLanguages();
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
     * Returns LDT template that the URI of this resource matches.
     * If the request URI did not match any template, <code>404 Not Found</code> was returned.
     * 
     * @return ontology class
     */
    @Override
    public Optional<TemplateCall> getTemplateCall()
    {
        return templateCall;
    }
    
    /**
     * Returns the cache control of this resource, if specified.
     * The control value can be specified as <code>ldt:cacheControl</code> on templates in the sitemap ontology.
     * 
     * @return cache control object or null, if not specified
     */
    public CacheControl getCacheControl()
    {
        return getTemplateCall().get().getTemplate().getCacheControl();
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
        return query;
    }

    public Resource getQueryResource()
    {
        return queryResource;
    }
    
    /**
     * Returns update used to remove RDF description of this resource.
     * Query solution bindings are applied by default.
     * 
     * @return update object with applied solution bindings
     * @see #getQuerySolutionMap()
     */
    @Override
    public UpdateRequest getUpdate()
    {
        return update;
    }

    public Resource getUpdateResource()
    {
        return updateResource;
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
 
    @Override
    public com.atomgraph.processor.model.Application getApplication()
    {
        return application;
    }
 
    @Override
    public Ontology getOntology()
    {
        return ontology;
    }
 
}