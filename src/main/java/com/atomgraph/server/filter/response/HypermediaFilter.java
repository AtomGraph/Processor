/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atomgraph.server.filter.response;

import com.atomgraph.core.util.Link;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import com.atomgraph.processor.util.TemplateCall;
import com.atomgraph.processor.vocabulary.DH;
import com.atomgraph.processor.vocabulary.LDT;
import com.atomgraph.server.exception.OntClassNotFoundException;
import com.atomgraph.server.provider.OntologyProvider;
import com.atomgraph.server.vocabulary.XHV;
import java.net.URISyntaxException;
import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.vocabulary.SPL;

/**
 * A filter that adds HATEOAS transitions to the RDF query result.
 * 
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{
    private static final Logger log = LoggerFactory.getLogger(HypermediaFilter.class);
            
    //@Context javax.ws.rs.core.Application system;
    @Context Providers providers;
    @Context UriInfo uriInfo;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        // do not process hypermedia if the response is a redirect or 201 Created or 404 Not Found
        if (response.getStatusType().getFamily().equals(REDIRECTION) || response.getStatusType().equals(CREATED) ||
                response.getStatusType().equals(NOT_FOUND) ||
                response.getEntity() == null || (!(response.getEntity() instanceof Model)))
            return response;
        
        //TemplateCall templateCall = getTemplateCall();
        //if (templateCall == null) return response;
            
        //Resource state = templateCall.build();
        /*
        Resource absolutePath = state.getModel().createResource(request.getAbsolutePath().toString());
        if (!state.equals(absolutePath)) // add hypermedia if there are query parameters
        {
            state.addProperty(C.viewOf, absolutePath).
                addProperty(RDF.type, C.View);

            if (templateCall.hasArgument(DH.limit)) // pages must have limits
            {
                if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} dh:pageOf {}", state, absolutePath);
                state.addProperty(DH.pageOf, absolutePath).
                    addProperty(RDF.type, DH.Page); // do we still need dh:Page now that we have core:View?

                addPrevNextPage(templateCall, absolutePath, state);
            }
        }
        */
        
        String ontologyURI = getOntologyURI(response.getHttpHeaders());
        if (ontologyURI == null) return response; // no Link header with type=ontology was present

        OntModel ontModel = new OntologyProvider(OntDocumentManager.getInstance(), ontologyURI, OntModelSpec.OWL_MEM, false).
                getOntology().getOntModel();
        
        Resource state = ModelFactory.createDefaultModel().createResource(request.getAbsolutePath().toString());
        if (response.getStatusType().getFamily().equals(Response.Status.Family.SUCCESSFUL) &&
                getArgument(state, DH.forClass) != null)
        {
            String forClassURI = getArgument(state, DH.forClass).getURI();
            OntClass forClass = ontModel.getOntClass(forClassURI);
            if (forClass == null) throw new OntClassNotFoundException("OntClass '" + forClassURI + "' not found in sitemap");

            state.addProperty(DH.instance, addInstance(state.getModel(), forClass)); // connects instance state to CONSTRUCTed template
        }

        if (log.isDebugEnabled()) log.debug("Added Number of HATEOAS statements added: {}", state.getModel().size());
        response.setEntity(state.getModel().add((Model)response.getEntity()));
        
        return response;
    }
        
    public String getOntologyURI(MultivaluedMap<String, Object> headers)
    {
        List<Object> links  = headers.get("Link");
        
        if (links != null)
            for (Object obj : links)
            {
                try
                {
                    Link link = Link.valueOf(obj.toString());
                    if (link.getRel() != null && link.getRel().equals(LDT.ontology.getURI()) &&
                            link.getHref() != null)
                        return link.getHref().toString();
                }
                catch (URISyntaxException ex)
                {
                    throw new WebApplicationException(ex);
                }
            }
        
        return null;
    }

    public void addPrevNextPage(TemplateCall templateCall, Resource absolutePath, Resource state)
    {
        if (templateCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
        if (absolutePath == null) throw new IllegalArgumentException("Resource cannot be null");
        if (state == null) throw new IllegalArgumentException("Resource cannot be null");
        
        Long limit = templateCall.getArgumentProperty(DH.limit).getLong();            
        Long offset = Long.valueOf(0);
        if (templateCall.hasArgument(DH.offset)) offset = templateCall.getArgumentProperty(DH.offset).getLong();

        if (offset >= limit)
        {
            com.atomgraph.core.util.StateBuilder prevBuilder = TemplateCall.fromResource(state);
            Resource prev = prevBuilder.replaceProperty(DH.offset, ResourceFactory.createTypedLiteral(offset - limit)).
                build().
                addProperty(DH.pageOf, absolutePath).
                addProperty(RDF.type, DH.Page).
                addProperty(XHV.next, state);

            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", state, prev);
            state.addProperty(XHV.prev, prev);
        }

        com.atomgraph.core.util.StateBuilder nextBuilder = TemplateCall.fromResource(state);
        Resource next = nextBuilder.replaceProperty(DH.offset, ResourceFactory.createTypedLiteral(offset + limit)).
                build().
                addProperty(DH.pageOf, absolutePath).
                addProperty(RDF.type, DH.Page).
                addProperty(XHV.prev, state);

        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", state, next);
        state.addProperty(XHV.next, next);
    }
    
    public Resource getArgument(Resource resource, Property predicate)
    {
	if (resource == null) throw new IllegalArgumentException("Resource cannot be null");
	if (predicate == null) throw new IllegalArgumentException("Property cannot be null");
        
        StmtIterator it = resource.listProperties(LDT.arg);
        
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                Resource arg = stmt.getObject().asResource();
                if (arg.getProperty(SPL.predicate).getResource().equals(predicate)) return arg;
            }
        }
        finally
        {
            it.close();
        }
        
        return null;
    }
    
    public Resource addInstance(Model targetModel, OntClass forClass)
    {
        if (log.isDebugEnabled()) log.debug("Invoking constructor on class: {}", forClass);
        return new ConstructorBase().construct(forClass, targetModel);
    }

    /*
    public TemplateCall getTemplateCall()
    {
        if (!getUriInfo().getMatchedResources().isEmpty())
            return ((com.atomgraph.server.model.Resource)getUriInfo().getMatchedResources().get(0)).getTemplateCall();
        
        return null;
    }
    
    public OntModelSpec getOntModelSpec()
    {
        return system instanceof Application ? ((Application)system).getOntModelSpec() : null;
    }
    */

    public Providers getProviders()
    {
        return providers;
    }
 
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}