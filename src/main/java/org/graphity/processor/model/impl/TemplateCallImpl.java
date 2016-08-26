/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

package org.graphity.processor.model.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Profile;
import org.apache.jena.ontology.impl.OntResourceImpl;
import org.apache.jena.vocabulary.RDF;
import org.graphity.processor.model.Template;
import org.graphity.processor.model.TemplateCall;
import org.graphity.processor.vocabulary.GP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TemplateCallImpl extends OntResourceImpl implements TemplateCall
{
    
    public static Implementation factory = new Implementation() 
    {
        
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph)
        {
            if (canWrap(node, enhGraph))
            {
                return new TemplateCallImpl(node, enhGraph);
            }
            else {
                throw new ConversionException( "Cannot convert node " + node.toString() + " to OntClass: it does not have rdf:type owl:Class or equivalent");
            }
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg)
        {
            if (eg == null) throw new IllegalArgumentException("EnhGraph cannot be null");

            /*
            // node will support being an OntClass facet if it has rdf:type owl:Class or equivalent
            Profile profile = (eg instanceof OntModel) ? ((OntModel) eg).getProfile() : null;
            return (profile != null)  &&  profile.isSupported( node, eg, TemplateCall.class );
            */
            
            return eg.asGraph().contains(node, RDF.type.asNode(), GP.TemplateCall.asNode());            
        }
    };

    public TemplateCallImpl(Node node, EnhGraph graph)
    {
        super(node, graph);
    }
        
    /*
    public TemplateCallImpl(Template template, Double precedence)
    {
        super(GP.Template.getModel().createResource().
                addProperty(GP.template, template).
                addLiteral(GP.priority, precedence).asNode(), 
                GP.Template.getModel().createResource().
                addProperty(GP.template, template).
                addLiteral(GP.priority, precedence).getOntModel().getGraph());
    }
    */
    
    @Override
    public final Template getTemplate()
    {
        return getPropertyResourceValue(GP.template).as(Template.class);
    }

    @Override
    public final Double getPrecedence()
    {
        return getProperty(GP.priority).getDouble();
    }

    /*
    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(getTemplate().getPath());
        hash = 59 * hash + Objects.hashCode(getPrecedence());
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final TemplateCall other = (TemplateCall) obj;
        if (!Objects.equals(getTemplate().getPath(), other.getTemplate().getPath())) return false;
        if (!Objects.equals(getPrecedence(), other.getPrecedence())) return false;
        return true;
    }
    */
    
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(getTemplate().getURI()).
        append(">, ").
        append(Double.toString(getPrecedence())).
        append("]").
        toString();
    }

    @Override
    public int compareTo(Object obj)
    {
        TemplateCall templateCall = (TemplateCall)obj;
        Double diff = templateCall.getPrecedence() - getPrecedence();
        return diff.intValue();
    }
    
}
