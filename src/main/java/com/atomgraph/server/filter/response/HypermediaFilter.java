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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import com.atomgraph.processor.util.TemplateCall;
import com.atomgraph.processor.vocabulary.C;
import com.atomgraph.processor.vocabulary.DH;
import com.atomgraph.server.vocabulary.XHV;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
    @Context Providers providers;
    @Context UriInfo uriInfo;
    
    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        // do not process hypermedia if the response is a redirect or 201 Created or 404 Not Found
        if (response.getStatusType().getFamily().equals(REDIRECTION) || response.getStatusType().equals(CREATED) ||
                response.getStatusType().equals(NOT_FOUND) || response.getStatusType().equals(INTERNAL_SERVER_ERROR) || 
                response.getEntity() == null || (!(response.getEntity() instanceof Model)))
            return response;
        
        TemplateCall templateCall = getTemplateCall();
        if (templateCall == null) return response;
        
        Resource state = templateCall.build();
        Resource absolutePath = state.getModel().createResource(request.getAbsolutePath().toString());
        if (!state.equals(absolutePath)) state.addProperty(C.stateOf, absolutePath);

        Resource requestUri = state.getModel().createResource(request.getRequestUri().toString());
        if (!state.equals(requestUri)) // add hypermedia if there are query parameters
            state.addProperty(C.viewOf, requestUri). // needed to lookup response state by request URI without redirection
                addProperty(RDF.type, C.View);

//        if (templateCall.hasArgument(DH.limit)) // pages must have limits
//        {
//            if (log.isDebugEnabled()) log.debug("Adding Page metadata: {} dh:pageOf {}", state, absolutePath);
//            state.addProperty(DH.pageOf, absolutePath).
//                addProperty(RDF.type, DH.Page); // do we still need dh:Page now that we have core:View?
//
//            addPrevNextPage(templateCall, absolutePath, state);
//        }

        if (log.isDebugEnabled()) log.debug("Added Number of HATEOAS statements added: {}", state.getModel().size());
        response.setEntity(state.getModel().add((Model)response.getEntity()));
        
        return response;
    }
        
//    public void addPrevNextPage(TemplateCall templateCall, Resource absolutePath, Resource state)
//    {
//        if (templateCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
//        if (absolutePath == null) throw new IllegalArgumentException("Resource cannot be null");
//        if (state == null) throw new IllegalArgumentException("Resource cannot be null");
//        
//        Long limit = templateCall.getArgumentProperty(DH.limit).getLong();
//        Long offset = Long.valueOf(0);
//        if (templateCall.hasArgument(DH.offset)) offset = templateCall.getArgumentProperty(DH.offset).getLong();
//
//        if (offset >= limit)
//        {
//            com.atomgraph.core.util.StateBuilder prevBuilder = TemplateCall.fromResource(state);
//            Resource prev = prevBuilder.replaceProperty(DH.offset, ResourceFactory.createTypedLiteral(offset - limit)).
//                build().
//                addProperty(DH.pageOf, absolutePath).
//                addProperty(RDF.type, DH.Page).
//                addProperty(XHV.next, state);
//
//            if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:previous {}", state, prev);
//            state.addProperty(XHV.prev, prev);
//        }
//
//        com.atomgraph.core.util.StateBuilder nextBuilder = TemplateCall.fromResource(state);
//        Resource next = nextBuilder.replaceProperty(DH.offset, ResourceFactory.createTypedLiteral(offset + limit)).
//            build().
//            addProperty(DH.pageOf, absolutePath).
//            addProperty(RDF.type, DH.Page).
//            addProperty(XHV.prev, state);
//
//        if (log.isDebugEnabled()) log.debug("Adding page metadata: {} xhv:next {}", state, next);
//        state.addProperty(XHV.next, next);
//    }

    public TemplateCall getTemplateCall()
    {
        if (!getUriInfo().getMatchedResources().isEmpty() &&
                getUriInfo().getMatchedResources().get(0) instanceof com.atomgraph.server.model.Resource)
            return ((com.atomgraph.server.model.Resource)getUriInfo().getMatchedResources().get(0)).getTemplateCall();
        
        return null;
    }
    
    public Providers getProviders()
    {
        return providers;
    }
 
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}