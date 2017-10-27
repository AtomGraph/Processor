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
import com.atomgraph.processor.vocabulary.DH;
import java.util.Set;
import java.util.UUID;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.util.JenaUtil;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class SkolemizingModelProvider extends ValidatingModelProvider
{
    private static final Logger log = LoggerFactory.getLogger(SkolemizingModelProvider.class);
    
    @Context private Request request;
    
    @Override
    public Model process(Model model)
    {
        if (getRequest().getMethod().equalsIgnoreCase("POST"))
        {
            ResIterator it = model.listSubjects();
            try
            {
                while (it.hasNext())
                {
                    Resource resource = it.next();
                    process(resource); // add dh:slug to documents before skolemization
                }
            }
            finally
            {
                it.close();
            }
        
            return skolemize(getOntology(), getUriInfo().getBaseUriBuilder(), getUriInfo().getAbsolutePathBuilder(),
                    super.process(model));
        }
        
        return super.process(model);
    }
    
    public Resource process(Resource resource)
    {
        // add UUID as dh:slug value to instances of dh:Container and dh:Item, if they don't have one
        // unlike using STRUUID() in dh:DocumentConstructor, we can cache pages without worrying about reusing slugs
        if (!resource.hasProperty(DH.slug))
        {
            Statement typeStmt = resource.getProperty(RDF.type);
            if (typeStmt != null && typeStmt.getObject().isURIResource())
            {
                OntClass ontClass = getOntology().getOntModel().getOntClass(typeStmt.getResource().getURI());
                if (ontClass != null)
                {
                    // cannot use ontClass.hasSuperClass() here as it does not traverse the chain
                    Set<Resource> superClasses = JenaUtil.getAllSuperClasses(ontClass);
                    if (superClasses.contains(DH.Container) || superClasses.contains(DH.Item))
                        resource.addLiteral(DH.slug, UUID.randomUUID().toString());
                }
            }
        }

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
