/*
 * Copyright 2016 Martynas Jusevičius <martynas@graphity.org>.
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
package org.graphity.processor.util;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.vocabulary.RDF;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class Implementation extends org.apache.jena.enhanced.Implementation
{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(Implementation.class);
    
    private final Node type;
    private Constructor constructor;

    public Implementation(Node type, Class implClass) throws NoSuchMethodException
    {
    	if (type == null) throw new IllegalArgumentException("Node cannot be null");
    	if (implClass == null) throw new IllegalArgumentException("Class cannot be null");
        
        this.type = type;
        constructor = implClass.getConstructor(Node.class, EnhGraph.class);        
    }
    
    @Override
    public EnhNode wrap(Node node, EnhGraph enhGraph)
    {
        if (canWrap(node, enhGraph))
        {
            try
            {
                return (EnhNode)getConstructor().newInstance(node, enhGraph);
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
            {
                if (log.isErrorEnabled()) log.error("Class {} instance cannot be created for Node: {}", node, enhGraph);
            }
        }

        throw new ConversionException( "Cannot convert node " + node.toString() + " to OntClass: it does not have rdf:type owl:Class or equivalent");
    }

    @Override
    public boolean canWrap(Node node, EnhGraph enhGraph)
    {
    	if (enhGraph == null) throw new IllegalArgumentException("EnhGraph cannot be null");
        
        return enhGraph.asGraph().contains(node, RDF.type.asNode(), getType());
    }

    public Node getType()
    {
        return type;
    }
    
    public Constructor getConstructor()
    {
        return constructor;
    }
    
}
