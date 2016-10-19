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
import com.atomgraph.processor.exception.OntologyException;
import com.atomgraph.processor.model.Argument;
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.vocabulary.LDT;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
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
                throw new ConversionException( "Cannot convert node " + node.toString() + " to Template: it does not have rdf:type ldt:Template or equivalent");
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

            return eg.asGraph().contains(node, RDF.type.asNode(), LDT.Template.asNode());
        }
    };
    
    public TemplateImpl(Node n, EnhGraph g)
    {
        super(n, g);
    }

    @Override
    public UriTemplate getPath()
    {
        Statement path = getProperty(LDT.path);
        if (path != null) return new UriTemplate(path.getString());
        
        return null;
    }

    @Override
    public String getSkolemTemplate()
    {
        return getStringValue(LDT.skolemTemplate);
    }

    @Override
    public String getFragmentTemplate()
    {
        return getStringValue(LDT.fragmentTemplate);
    }
    
    @Override
    public Resource getQuery()
    {
        return getPropertyResourceValue(LDT.query);
    }

    @Override
    public Resource getUpdate()
    {
        return getPropertyResourceValue(LDT.update);
    }

    @Override
    public Double getPriority()
    {
        Statement priority = getProperty(LDT.priority);
        if (priority != null) return priority.getDouble();
        
        return Double.valueOf(0);
    }

    @Override
    public Map<Property, Argument> getArguments()
    {
        return addSuperArguments(this, getLocalArguments());
    }
    
    @Override
    public Map<Property, Argument> getLocalArguments()
    {
        Map<Property, Argument> args = new HashMap<>();
        
        StmtIterator it = listProperties(LDT.param);
        try
        {
            while(it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().canAs(Argument.class))
                {
                    if (log.isErrorEnabled()) log.error("Unsupported Argument '{}' for Template '{}' (rdf:type ldt:Argument missing)", stmt.getObject(), getURI());
                    throw new OntologyException("Unsupported Argument '" + stmt.getObject() + "' for Template '" + getURI() + "' (rdf:type ldt:Argument missing)");
                }

                Argument arg = stmt.getObject().as(Argument.class);
                if (args.containsKey(arg.getPredicate()))
                {
                    if (log.isErrorEnabled()) log.error("Multiple Arguments with the same predicate '{}' for Template '{}' ", arg.getPredicate(), getURI());
                    throw new OntologyException("Multiple Arguments with the same predicate '" + arg.getPredicate() + "' for Template '" + getURI() + "'");
                }
                
                args.put(arg.getPredicate(), arg);
            }
        }
        finally
        {
            it.close();
        }
        
        return args;
    }
    
    protected Map<Property, Argument> addSuperArguments(Template template, Map<Property, Argument> args)
    {
        if (template == null) throw new IllegalArgumentException("Template Set cannot be null");        
        if (args == null) throw new IllegalArgumentException("Argument Map cannot be null");        
        
        ExtendedIterator<OntClass> superIt = template.listSuperClasses();
        try
        {
            while (superIt.hasNext())
            {
                OntClass superClass = superIt.next();
                if (superClass.canAs(Template.class))
                {
                    Template superTemplate = superClass.as(Template.class);
                    Map<Property, Argument> superArgs = superTemplate.getLocalArguments();
                    Iterator<Entry<Property, Argument>> entryIt = superArgs.entrySet().iterator();
                    while (entryIt.hasNext())
                    {
                        Entry<Property, Argument> entry = entryIt.next();
                        args.putIfAbsent(entry.getKey(), entry.getValue()); // reject Arguments for existing predicates
                    }
                    
                    addSuperArguments(superTemplate, args);  // recursion to super class
                }
            }
        }
        finally
        {
            superIt.close();
        }

        return args;
    }
    
    @Override
    public Map<String, Argument> getArgumentsMap()
    {
        Map<String,Argument> map = new HashMap<>();

        for (Argument argument : getArguments().values())
        {
            Property property = argument.getPredicate();
            if (property != null) map.put(property.getLocalName(), argument);
        }

        return map;
    }

    @Override    
    public Map<Property, RDFNode> getDefaultValues()
    {
        return getDefaultValues(getArguments().values());
    }
    
    public Map<Property, RDFNode> getDefaultValues(Collection<Argument> args)
    {
        if (args == null) throw new IllegalArgumentException("Argument Set cannot be null");
        
        Map<Property, RDFNode> defaultValues = new HashMap<>();
        
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
        return getLanguages(LDT.lang);
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
                    if (log.isErrorEnabled()) log.error("Illegal language value for template '{}' (ldt:language is not literal)", getURI());
                    throw new OntologyException("Illegal non-literal ldt:language value for template '" + getURI() +"'");
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
        return getPropertyResourceValue(LDT.loadClass);
    }
    
    /**
     * Returns <code>Cache-Control</code> HTTP header value, specified on an ontology class with given property.
     * 
     * @return CacheControl instance or null
     */
    @Override
    public CacheControl getCacheControl()
    {
        if (hasProperty(LDT.cacheControl))
            return CacheControl.valueOf(getPropertyValue(LDT.cacheControl).asLiteral().getString()); // will fail on bad config

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
