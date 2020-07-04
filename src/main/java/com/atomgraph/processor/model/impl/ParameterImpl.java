/*
 * Copyright 2016 Martynas Jusevičius <martynas@atomgraph.com>.
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
package com.atomgraph.processor.model.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.vocabulary.RDF;
import com.atomgraph.processor.vocabulary.LDT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.processor.model.Parameter;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class ParameterImpl extends com.atomgraph.spinrdf.model.impl.ArgumentImpl implements Parameter
{
    
    private static final Logger log = LoggerFactory.getLogger(ParameterImpl.class);

    public static Implementation factory = new Implementation() 
    {
        
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph)
        {
            if (canWrap(node, enhGraph))
            {
                return new ParameterImpl(node, enhGraph);
            }
            else
            {
                throw new ConversionException( "Cannot convert node " + node.toString() + " to Parameter: it does not have rdf:type ldt:Parameter or equivalent");
            }
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg)
        {
            if (eg == null) throw new IllegalArgumentException("EnhGraph cannot be null");
            
            return eg.asGraph().contains(node, RDF.type.asNode(), LDT.Parameter.asNode());
        }
    };
    
    public ParameterImpl(Node node, EnhGraph enhGraph)
    {
        super(node, enhGraph);
    }
    
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[<").
        append(getPredicate().getURI()).
        append(">");
        if (getDefaultValue() != null)
            sb.append(", ").
            append(getDefaultValue());
        if (getValueType() != null)
            sb.append(", ").
            append(getValueType());
        sb.append("]");
        return sb.toString();
    }
    
}
