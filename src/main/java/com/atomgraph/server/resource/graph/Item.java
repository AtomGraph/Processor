/*
 * Copyright 2014 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.server.resource.graph;

import com.atomgraph.core.MediaTypes;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import com.atomgraph.core.model.Service;
import com.atomgraph.server.model.impl.ResourceBase;
import com.atomgraph.processor.model.TemplateCall;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.container.ResourceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Named graph resource.
 * Implements direct graph identification of the SPARQL Graph Store Protocol.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 * @see com.atomgraph.core.model.GraphStore
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update/#direct-graph-identification">4.1 Direct Graph Identification</a>
 */
public class Item extends ResourceBase // TO-DO: extends GraphStore
{
    
    private static final Logger log = LoggerFactory.getLogger(Item.class);
    
    @Inject
    public Item(@Context UriInfo uriInfo, @Context Request request, @Context MediaTypes mediaTypes,
            Service service, com.atomgraph.processor.model.Application application, Optional<Ontology> ontology, Optional<TemplateCall> templateCall,
            @Context HttpHeaders httpHeaders, @Context ResourceContext resourceContext)
    {
        super(uriInfo, request, mediaTypes,
                service, application, ontology, templateCall,
                httpHeaders, resourceContext);
        if (log.isDebugEnabled()) log.debug("Constructing {} as direct indication of GRAPH {}", getClass(), uriInfo.getAbsolutePath());
    }
    
    @Override
    public Response get()
    {
        if (!getService().getDatasetAccessor().containsModel(getURI().toString()))
        {
            if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} not found", getURI());
            throw new NotFoundException("Named graph not found");
        }

        Model model = getService().getDatasetAccessor().getModel(getURI().toString());
        if (log.isDebugEnabled()) log.debug("GET Graph Store named graph with URI: {} found, returning Model of size(): {}", getURI(), model.size());
        return getResponse(model);
    }

    @Override
    public Response post(Model model)
    {
        boolean existingGraph = getService().getDatasetAccessor().containsModel(getURI().toString());

        // is this implemented correctly? The specification is not very clear.
        if (log.isDebugEnabled()) log.debug("POST Model to named graph with URI: {} Did it already exist? {}", getURI(), existingGraph);
        getService().getDatasetAccessor().add(getURI().toString(), model);

        if (existingGraph) return Response.ok().build();
        else return Response.created(getURI()).build();
    }

    @Override
    public Response put(Model model)
    {
        boolean existingGraph = getService().getDatasetAccessor().containsModel(getURI().toString());

        if (log.isDebugEnabled()) log.debug("PUT Model to named graph with URI: {} Did it already exist? {}", getURI(), existingGraph);
        getService().getDatasetAccessor().putModel(getURI().toString(), model);

        if (existingGraph) return Response.ok().build();
        else return Response.created(getURI()).build();
    }

    @Override
    public Response delete()
    {
        if (!getService().getDatasetAccessor().containsModel(getURI().toString()))
        {
            if (log.isDebugEnabled()) log.debug("DELETE named graph with URI {}: not found", getURI());
            throw new NotFoundException("Named graph not found");
        }
        else
        {
            if (log.isDebugEnabled()) log.debug("DELETE named graph with URI: {}", getURI());
            getService().getDatasetAccessor().deleteModel(getURI().toString());
            return Response.noContent().build(); // TO-DO: NoContentException?
        }
    }
    
}