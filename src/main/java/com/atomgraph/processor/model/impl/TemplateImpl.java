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
import com.atomgraph.processor.model.Template;
import com.atomgraph.processor.vocabulary.LDT;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.atomgraph.processor.model.Parameter;
import java.util.stream.Collectors;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.glassfish.jersey.uri.UriTemplate;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
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
            else
            {
                throw new ConversionException("Cannot convert node " + node.toString() + " to Template: it does not have rdf:type ldt:Template or equivalent");
            }
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg)
        {
            if (eg == null) throw new IllegalArgumentException("EnhGraph cannot be null");

            return eg.asGraph().contains(node, RDF.type.asNode(), LDT.Template.asNode());
        }
    };
    
    public TemplateImpl(Node n, EnhGraph g)
    {
        super(n, g);
    }

    @Override
    public UriTemplate getMatch()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.match);
        
        if (lowest != null)
        {
            Statement path = lowest.getProperty(LDT.match);
            if (!path.getObject().isLiteral() ||
                    path.getObject().asLiteral().getDatatype() == null ||
                    !path.getObject().asLiteral().getDatatype().equals(XSDDatatype.XSDstring))
            {
                if (log.isErrorEnabled()) log.error("Class {} property {} is not an xsd:string literal", lowest.getURI(), LDT.match);
                throw new OntologyException("Class '" + lowest.getURI() + "' property '" + LDT.match + "' is not an xsd:string literal");
            }
            
            return new UriTemplate(path.getString());
        }
        else
            return null;
    }

    @Override
    public String getFragmentTemplate()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.fragment);
        if (lowest != null) return lowest.getProperty(LDT.fragment).getString();
        else return null;
    }
    
    @Override
    public Resource getQuery()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.query);
        if (lowest != null) return lowest.getPropertyResourceValue(LDT.query);
        
        if (log.isErrorEnabled()) log.error("Template '{}' is missing ldt:query value", getURI());
        throw new OntologyException("Template '" + getURI() + "' is missing ldt:query value");
    }

    @Override
    public Resource getUpdate()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.update);
        if (lowest != null) return lowest.getPropertyResourceValue(LDT.update);
        else return null;
    }

    @Override
    public Double getPriority()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.priority);
        if (lowest != null) return lowest.getProperty(LDT.priority).getDouble();
        else return Double.valueOf(0);
    }

    @Override
    public Map<Property, Parameter> getParameters()
    {
        List<Template> templates = new ArrayList<>();
        templates.add(this);
        templates.addAll(getSuperTemplates());
        
        return templates.stream().
                flatMap(t -> t.getLocalParameters().entrySet().stream()). // stream param map entries
                collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }
    
    @Override
    public Map<Property, Parameter> getLocalParameters()
    {
        Map<Property, Parameter> params = new HashMap<>();
        
        StmtIterator it = listProperties(LDT.param);
        try
        {
            while(it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().canAs(Parameter.class))
                {
                    if (log.isErrorEnabled()) log.error("Unsupported Argument '{}' for Template '{}' (rdf:type ldt:Parameter missing)", stmt.getObject(), getURI());
                    throw new OntologyException("Unsupported Argument '" + stmt.getObject() + "' for Template '" + getURI() + "' (rdf:type ldt:Parameter missing)");
                }

                Parameter param = stmt.getObject().as(Parameter.class);
                if (params.containsKey(param.getPredicate()))
                {
                    if (log.isErrorEnabled()) log.error("Multiple Arguments with the same predicate '{}' for Template '{}' ", param.getPredicate(), getURI());
                    throw new OntologyException("Multiple Arguments with the same predicate '" + param.getPredicate() + "' for Template '" + getURI() + "'");
                }
                
                params.put(param.getPredicate(), param);
            }
        }
        finally
        {
            it.close();
        }
        
        return params;
    }
    
    @Override
    public Map<String, Parameter> getParameterMap()
    {
        Map<String,Parameter> map = new HashMap<>();

        for (Parameter param : getParameters().values())
        {
            Property property = param.getPredicate();
            if (property != null) map.put(property.getLocalName(), param);
        }

        return map;
    }
    
    @Override
    public List<Locale> getLanguages()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.lang);
        if (lowest != null) return getLanguages(lowest, LDT.lang);
        else return new ArrayList<>();
    }

    protected List<Locale> getLanguages(Template template, Property property)
    {
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
        if (property == null) throw new IllegalArgumentException("Property cannot be null");
        
        List<Locale> languages = new ArrayList<>();
        Resource langs = template.getPropertyResourceValue(property);
        if (langs != null)
        {
            if (!langs.canAs(RDFList.class))
            {
                if (log.isErrorEnabled()) log.error("ldt:lang value is not an rdf:List on template '{}'", getURI());
                throw new OntologyException("ldt:lang value is not an rdf:List on template  '" + getURI() +"'");
            }

            // could use list order as priority (quality value q=)
            RDFList list = langs.as(RDFList.class);
            ExtendedIterator<RDFNode> it = list.iterator();
            try
            {
                while (it.hasNext())
                {
                    RDFNode langTag = it.next();
                    if (!langTag.isLiteral())
                    {
                        if (log.isErrorEnabled()) log.error("Non-literal language tag (ldt:lang member) on template '{}'", getURI());
                        throw new OntologyException("Non-literal language tag (ldt:lang member) on template '" + getURI() +"'");
                    }

                    languages.add(Locale.forLanguageTag(langTag.asLiteral().getString()));
                }
            }
            finally
            {
                it.close();
            }
        }
        
        return languages;
    }
        
    @Override
    public Resource getLoadClass()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.loadClass);
        if (lowest != null) return lowest.getPropertyResourceValue(LDT.loadClass);
        else return null;
    }
    
    /**
     * Returns <code>Cache-Control</code> HTTP header value, specified on an ontology class with given property.
     * 
     * @return CacheControl instance or null
     */
    @Override
    public CacheControl getCacheControl()
    {
        Template lowest = getSelfOrSuperWithProperty(LDT.cacheControl);
        if (lowest != null) return CacheControl.valueOf(lowest.getPropertyValue(LDT.cacheControl).asLiteral().getString());
        else return null;
    }

    @Override
    public List<Template> getSuperTemplates()
    {
        return getSuperTemplates(this);
    }
    
    protected List<Template> getSuperTemplates(Template template)
    {
        List<Template> superTemplates = new ArrayList<>();
        
        StmtIterator it = template.listProperties(LDT.extends_);
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                if (!stmt.getObject().isResource() || !stmt.getObject().asResource().canAs(Template.class))
                {
                    if (log.isErrorEnabled()) log.error("Template's '{}' ldt:extends value '{}' is not an LDT Template", getURI(), stmt.getObject());
                    throw new OntologyException("Template's '" + getURI() + "' ldt:extends value '" + stmt.getObject() + "' is not an LDT Template");
                }

                Template superTemplate = stmt.getObject().as(Template.class);
                superTemplates.add(superTemplate);
                superTemplates.addAll(getSuperTemplates(superTemplate));
            }
        }
        finally
        {
            it.close();
        }
        
        return superTemplates;
    }

    /**
     * Returns this template or the first template in the super-template chain that has the specified property.
     * 
     * @param property property the template needs to have
     * @return 
     */
    public Template getSelfOrSuperWithProperty(Property property)
    {
        List<Template> templates = new ArrayList<>();
        templates.add(this);
        templates.addAll(getSuperTemplates());
        
        return templates.stream().
            filter(t -> t.hasProperty(property)).
            findFirst().
            orElse(null);
    }
    
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(getURI()).
        append(">: \"").
        append(getMatch()).
        append("\", ").
        append(Double.toString(getPriority())).
        append("]").
        toString();
    }
    
}
