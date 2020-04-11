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
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import com.atomgraph.core.model.Service;
import com.atomgraph.server.model.impl.ResourceBase;
import com.atomgraph.core.util.ModelUtils;
import com.atomgraph.processor.model.TemplateCall;
import java.util.Optional;
import javax.inject.Inject;
import javax.ws.rs.container.ResourceContext;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
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
            Service service, com.atomgraph.processor.model.Application application, Ontology ontology, Optional<TemplateCall> templateCall,
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
        if (log.isDebugEnabled()) log.debug("GET GRAPH {} from GraphStore {}", getURI());
        return getResponse(DatasetFactory.create(getService().getDatasetAccessor().getModel(getURI().toString())));
    }

    @Override
    public Response post(Dataset dataset)
    {
        if (log.isDebugEnabled()) log.debug("POST GRAPH {} to GraphStore {}", getURI());
        
        Dataset newDataset = DatasetFactory.create();
        newDataset.addNamedModel(getURI().toString(), dataset.getDefaultModel()); // put request entity graph into a named graph
        
        return super.post(newDataset);
    }

    @Override
    public Response put(Dataset dataset)
    {
        Model existing = getService().getDatasetAccessor().getModel(getURI().toString());

        if (!existing.isEmpty()) // remove existing representation
        {
            EntityTag entityTag = new EntityTag(Long.toHexString(ModelUtils.hashModel(dataset.getDefaultModel())));
            ResponseBuilder rb = getRequest().evaluatePreconditions(entityTag);
            if (rb != null)
            {
                if (log.isDebugEnabled()) log.debug("PUT preconditions were not met for resource: {} with entity tag: {}", this, entityTag);
                return rb.build();
            }
        }
        
        if (log.isDebugEnabled()) log.debug("PUT GRAPH {} to GraphStore", getURI());
        getService().getDatasetAccessor().putModel(getURI().toString(), dataset.getDefaultModel());
        
        if (existing.isEmpty()) return Response.created(getURI()).build();
        else return Response.ok(dataset).build();
    }

    @Override
    public Response delete()
    {
        if (log.isDebugEnabled()) log.debug("DELETE GRAPH {} from GraphStore", getURI());
        
        getService().getDatasetAccessor().deleteModel(getURI().toString());
        
        return Response.noContent().build();
    }
    
}