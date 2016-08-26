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
package org.graphity.processor.model.impl;

import com.sun.jersey.api.uri.UriTemplate;
import java.util.Map;
import javax.ws.rs.core.CacheControl;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.ontology.impl.OntClassImpl;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.graphity.processor.model.Template;
import org.graphity.processor.vocabulary.GP;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TemplateImpl extends OntClassImpl implements Template
{

    public static Implementation factory = new Implementation() 
    {
        
        @Override
        public EnhNode wrap(Node node, EnhGraph enhGraph)
        {
            if (canWrap(node, enhGraph))
            {
                return new TemplateImpl(node, enhGraph);
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
            return (profile != null)  &&  profile.isSupported( node, eg, Template.class );
            */

            return eg.asGraph().contains(node, RDF.type.asNode(), GP.Template.asNode());
        }
    };
    
    public TemplateImpl(Node n, EnhGraph g)
    {
        super(n, g);
    }

    @Override
    public UriTemplate getPath()
    {
        return new UriTemplate(getProperty(GP.path).getString());
    }

    @Override
    public String getSkolemTemplate()
    {
        return getStringValue(GP.skolemTemplate);
    }

    @Override
    public String getFragmentTemplate()
    {
        return getStringValue(GP.fragmentTemplate);
    }
    
    @Override
    public Resource getQuery()
    {
        return getPropertyResourceValue(GP.query);
    }

    @Override
    public Resource getUpdate()
    {
        return getPropertyResourceValue(GP.update);
    }

    @Override
    public Double getPriority()
    {
        return getProperty(GP.priority).getDouble();
    }

    @Override
    public Map<Property, RDFNode> getArguments()
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    /**
     * Returns <code>Cache-Control</code> HTTP header value, specified on an ontology class with given property.
     * 
     * @return CacheControl instance or null
     */
    @Override
    public CacheControl getCacheControl()
    {
        if (hasProperty(GP.cacheControl))
            return CacheControl.valueOf(getPropertyValue(GP.cacheControl).asLiteral().getString()); // will fail on bad config

	return null;
    }
    
    protected String getStringValue(Property property)
    {
	if (property == null) throw new IllegalArgumentException("Property cannot be null");

        if (hasProperty(property) && getPropertyValue(property).isLiteral())
            return getPropertyValue(property).asLiteral().getString();
        
        return null;
    }
    
}
