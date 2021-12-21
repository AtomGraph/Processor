/*
 * Copyright 2021 Martynas Jusevičius <martynas@atomgraph.com>.
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

package com.atomgraph.processor.util;

import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.shared.AccessDeniedException;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.Test;

/**
 *
 * @author {@literal Martynas Jusevičius <martynas@atomgraph.com>}
 */
public class OntModelReadOnlyTest
{

    private final OntModelReadOnly ontModelRO;
    
    public OntModelReadOnlyTest()
    {
        this.ontModelRO = new OntModelReadOnly(ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,
                ModelFactory.createDefaultModel().
                    add(ResourceFactory.createResource("http://s"),
                        ResourceFactory.createProperty("http://p"),
                        ResourceFactory.createResource("http://o"))));
    }
    
    public void testListOntClasses()
    {
        ExtendedIterator<OntClass> it = ontModelRO.listClasses();
        
        try
        {
            it.hasNext();
        }
        finally
        {
            it.close();
        }
    }
    
    @Test
    public void testGetResource()
    {
        StmtIterator it = ontModelRO.listStatements();
        
        try
        {
            it.next().getSubject().equals(ontModelRO.getResource("http://s"));
        }
        finally
        {
            it.close();
        }
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testAdd()
    {
        ontModelRO.add(ontModelRO.getResource("http://x"), ontModelRO.getProperty("http://y"), ontModelRO.getResource("http://z"));
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testRemove()
    {
        ontModelRO.remove(ontModelRO.getResource("http://x"), ontModelRO.getProperty("http://y"), ontModelRO.getResource("http://z"));
    }
    
    @Test(expected = AccessDeniedException.class)
    public void testGetRawModelAdd()
    {
        ontModelRO.getRawModel().add(ontModelRO.getResource("http://x"), ontModelRO.getProperty("http://x"), ontModelRO.getResource("http://x"));
    }
    
}
