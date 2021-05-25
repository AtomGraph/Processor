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

import com.atomgraph.processor.model.TemplateCall;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import javax.ws.rs.ext.Provider;
import com.atomgraph.processor.vocabulary.C;
import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.Family.REDIRECTION;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A filter that adds HATEOAS transitions to the RDF query result.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see <a href="https://www.ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm">Representational State Transfer (REST): chapter 5</a>
 */
@Provider
public class HypermediaFilter implements ContainerResponseFilter
{
    private static final Logger log = LoggerFactory.getLogger(HypermediaFilter.class);

    @Context Providers providers;
    @Context UriInfo uriInfo;
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response)
    {
        if (request == null) throw new IllegalArgumentException("ContainerRequest cannot be null");
        if (response == null) throw new IllegalArgumentException("ContainerResponse cannot be null");
        
        // do not process hypermedia if the response is a redirect or 201 Created or 404 Not Found
        if (response.getStatusInfo().getFamily().equals(REDIRECTION) || response.getStatusInfo().equals(CREATED) ||
                response.getStatusInfo().equals(NOT_FOUND) || response.getStatusInfo().equals(INTERNAL_SERVER_ERROR) || 
                response.getEntity() == null || (!(response.getEntity() instanceof Model)))
            return;
        
        Optional<TemplateCall> templateCall = getTemplateCall();
        if (!templateCall.isPresent()) return;
        
        Resource state = templateCall.get().build();
        Resource absolutePath = state.getModel().createResource(request.getUriInfo().getAbsolutePath().toString());
        if (!state.equals(absolutePath)) state.addProperty(C.stateOf, absolutePath);

        Resource requestUri = state.getModel().createResource(request.getUriInfo().getRequestUri().toString());
        if (!state.equals(requestUri)) // add hypermedia if there are query parameters
            state.addProperty(C.viewOf, requestUri). // needed to lookup response state by request URI without redirection
                addProperty(RDF.type, C.View);

        if (log.isDebugEnabled()) log.debug("Added Number of HATEOAS statements added: {}", state.getModel().size());
        Model newEntity = ((Model)response.getEntity());
        newEntity.add(state.getModel());
        response.setEntity(newEntity);
    }

    public Optional<TemplateCall> getTemplateCall()
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