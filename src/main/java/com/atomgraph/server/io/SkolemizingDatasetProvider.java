/*
 * Copyright 2019 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.server.io;

import com.atomgraph.processor.util.Skolemizer;
import com.atomgraph.server.exception.SkolemizationException;
import java.util.Iterator;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.jena.ontology.Ontology;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dataset provider that skolemizes read triples in each graph against class URI templates in an ontology.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SkolemizingDatasetProvider extends ValidatingDatasetProvider
{

    private static final Logger log = LoggerFactory.getLogger(SkolemizingDatasetProvider.class);
    
    @Context private Request request;
    @Context UriInfo uriInfo;

    @Override
    public Dataset process(Dataset dataset)
    {
        dataset = super.process(dataset); // validation
        
        process(dataset.getDefaultModel());
        
        Iterator<String> it = dataset.listNames();
        while (it.hasNext())
        {
            String graphURI = it.next();
            process(dataset.getNamedModel(graphURI));
        }
        
        return dataset;
    }
    
    public Model process(Model model)
    {
        ResIterator it = model.listSubjects();
        try
        {
            while (it.hasNext())
            {
                Resource resource = it.next();
                process(resource);
            }
        }
        finally
        {
            it.close();
        }

        if (getOntology().isPresent()) return skolemize(getOntology().get(), getUriInfo().getBaseUriBuilder(), getUriInfo().getAbsolutePathBuilder(), model);
        else return model;
    }
    
    public Resource process(Resource resource)
    {
        return resource;
    }
    
    public Model skolemize(Ontology ontology, UriBuilder baseUriBuilder, UriBuilder absolutePathBuilder, Model model)
    {
        try
        {
            return new Skolemizer(ontology, baseUriBuilder, absolutePathBuilder).build(model); // not optimal to create Skolemizer for each Model
        }
        catch (IllegalArgumentException ex)
        {
            throw new SkolemizationException(ex, model);
        }
    }

    public Request getRequest()
    {
        return request;
    }
    
    public UriInfo getUriInfo()
    {
        return uriInfo;
    }
    
}
