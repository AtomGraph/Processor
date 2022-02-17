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

package com.atomgraph.server.io;

import org.apache.jena.ontology.Ontology;
import org.apache.jena.rdf.model.Model;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriBuilder;
import com.atomgraph.server.exception.SkolemizationException;
import com.atomgraph.processor.util.Skolemizer;
import javax.ws.rs.HttpMethod;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model provider that skolemizes read triples against class URI templates in an ontology.
 * 
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class SkolemizingModelProvider extends ValidatingModelProvider
{
    private static final Logger log = LoggerFactory.getLogger(SkolemizingModelProvider.class);
    
    @Context private Request request;
    
    @Override
    public Model processRead(Model model)
    {
        if (getRequest().getMethod().equalsIgnoreCase(HttpMethod.POST) || getRequest().getMethod().equalsIgnoreCase(HttpMethod.PUT))
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
        
            if (getOntology().isPresent()) return skolemize(getOntology().get(), getUriInfo().getBaseUriBuilder(), getUriInfo().getAbsolutePathBuilder(), super.processRead(model));
            else return model;
        }
        
        return super.processRead(model);
    }
    
    public Resource process(Resource resource)
    {
        return resource;
    }
    
    public Model skolemize(Ontology ontology, UriBuilder baseUriBuilder, UriBuilder absolutePathBuilder, Model model)
    {
        try
        {
            return new Skolemizer(ontology, baseUriBuilder, absolutePathBuilder).build(model);
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

}
