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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.graphity.processor.exception.SitemapException;
import org.graphity.processor.model.Argument;
import org.graphity.processor.model.Template;
import org.graphity.processor.vocabulary.GP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TemplateImpl extends OntClassImpl implements Template
{

    private static final Logger log = LoggerFactory.getLogger(TemplateImpl.class);

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
        if (getProperty(GP.priority) != null) return getProperty(GP.priority).getDouble();
        
        return Double.valueOf(0);
    }

    @Override
    public List<Argument> getArguments()
    {
        List<Argument> args = new ArrayList<>();

        StmtIterator it = listProperties(GP.param);
        try
        {
            while(it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().canAs(Argument.class))
                {
                    if (log.isErrorEnabled()) log.error("Unsupported Argument '{}' for Template '{}' (rdf:type gp:Argument missing)", stmt.getObject(), getURI());
                    throw new SitemapException("Unsupported Argument '" + stmt.getObject() + "' for Template '" + getURI() + "' (rdf:type gp:Argument missing)");
                }

                args.add(stmt.getObject().as(Argument.class));
            }
        }
        finally
        {
            it.close();
        }
        
        return args;
    }

    @Override
    public Map<String, Argument> getArgumentsMap()
    {
        Map<String,Argument> entry = new HashMap<>();

        for (Argument argument : getArguments())
        {
            Property property = argument.getPredicate();
            if (property != null) entry.put(property.getLocalName(), argument);
        }

        return entry;
    }

    @Override
    public Map<Property, RDFNode> getDefaultValues()
    {
        Map<Property, RDFNode> defaultValues = new HashMap<>();
        
        List<Argument> args = getArguments();
        for (Argument arg : args)
        {
            RDFNode defaultValue = arg.getDefaultValue();
            if (defaultValue != null) defaultValues.put(arg.getPredicate(), defaultValue);
        }
        
        return defaultValues;
    }
    
    @Override
    public List<Locale> getLanguages()
    {
        return getLanguages(GP.lang);
    }

    protected List<Locale> getLanguages(Property property)
    {
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        
        List<Locale> languages = new ArrayList<>();
        StmtIterator it = listProperties(property);
        
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().isLiteral())
                {
                    if (log.isErrorEnabled()) log.error("Illegal language value for template '{}' (gp:language is not literal)", getURI());
                    throw new SitemapException("Illegal non-literal gp:language value for template '" + getURI() +"'");
                }
                
                languages.add(Locale.forLanguageTag(stmt.getString()));
            }
        }
        finally
        {
            it.close();
        }
        
        return languages;
    }
        
    @Override
    public Resource getLoadClass()
    {
        return getPropertyResourceValue(GP.loadClass);
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

    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(getURI()).
        append(">: \"").
        append(getPath()).
        append("\", ").
        append(Double.toString(getPriority())).
        append("]").
        toString();
    }

}
