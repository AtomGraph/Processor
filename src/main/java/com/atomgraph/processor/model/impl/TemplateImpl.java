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
import com.atomgraph.processor.query.QueryBuilder;
import com.atomgraph.processor.update.ModifyBuilder;
import com.atomgraph.processor.vocabulary.LDT;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spinrdf.model.SPINFactory;
import org.spinrdf.vocabulary.SP;

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
                throw new ConversionException("Cannot convert node " + node.toString() + " to Template: it does not have rdf:type ldt:Template or equivalent");
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
        Resource langs = getPropertyResourceValue(property);
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
    public QueryBuilder getQueryBuilder(URI base)
    {
        return getQueryBuilder(base, getQuery().getModel());
    }

    @Override
    public QueryBuilder getQueryBuilder(URI base, Model commandModel)
    {
        Resource queryOrTemplateCall = getQuery();
        if (queryOrTemplateCall == null)
        {
            if (log.isErrorEnabled()) log.error("Query not defined for template '{}' (ldt:query missing)", getURI());
            throw new OntologyException("Query not defined for template '" + getURI() +"'");
        }
        
        return getQueryBuilder(queryOrTemplateCall, base, commandModel);
    }
    
    public QueryBuilder getQueryBuilder(Resource queryOrTemplateCall, URI base, Model commandModel)
    {
	if (queryOrTemplateCall == null) throw new IllegalArgumentException("Query Resource cannot be null");
	if (commandModel == null) throw new IllegalArgumentException("Model cannot be null");
        
        org.spinrdf.model.TemplateCall spinTemplateCall = SPINFactory.asTemplateCall(queryOrTemplateCall);
        if (spinTemplateCall != null)
            return QueryBuilder.fromQuery(getParameterizedSparqlString(spinTemplateCall, base).asQuery(), commandModel);
        else
        {
            org.spinrdf.model.Query query = SPINFactory.asQuery(queryOrTemplateCall);
            if (query == null)
            {
                if (log.isErrorEnabled()) log.error("Class '{}' ldt:query value '{}' is not a SPIN Query or TemplateCall", getURI(), queryOrTemplateCall);
                throw new OntologyException("Class '" + getURI() + "' ldt:query value '" + queryOrTemplateCall + "' not a SPIN Query or TemplateCall");
            }
            
            return QueryBuilder.fromQuery(getParameterizedSparqlString(query, base).asQuery(), commandModel);
        }
    }

    public ParameterizedSparqlString getParameterizedSparqlString(org.spinrdf.model.TemplateCall spinTemplateCall, URI base)
    {
	if (spinTemplateCall == null) throw new IllegalArgumentException("TemplateCall cannot be null");
	if (base == null) throw new IllegalArgumentException("Base URI cannot be null");

        if (spinTemplateCall.getTemplate() == null)
        {
            if (log.isErrorEnabled()) log.error("SPIN Template of TemplateCall '{}' not found", spinTemplateCall);
            throw new OntologyException("SPIN Template of TemplateCall '" + spinTemplateCall + "' not found");
        }
        if (spinTemplateCall.getTemplate().getBody() == null)
        {
            if (log.isErrorEnabled()) log.error("Body Command of SPIN Template '{}' not found (spin:body missing)", spinTemplateCall.getTemplate());
            throw new OntologyException("Body Command of SPIN Template '" + spinTemplateCall.getTemplate() + "' not found (spin:body missing)");
        }

        return new ParameterizedSparqlString(spinTemplateCall.getTemplate().getBody().toString(), null, base.toString());
    }

    public ParameterizedSparqlString getParameterizedSparqlString(org.spinrdf.model.Command command, URI base)
    {
	if (command == null) throw new IllegalArgumentException("Command cannot be null");
	if (base == null) throw new IllegalArgumentException("Base URI cannot be null");

        Statement textStmt = command.getRequiredProperty(SP.text);
        if (textStmt == null || !textStmt.getObject().isLiteral())
        {
            if (log.isErrorEnabled()) log.error("SPARQL string not defined for Command '{}' (sp:text missing or not a string)", command);
            throw new OntologyException("SPARQL string not defined for Command '" + command + "'");
        }

        return new ParameterizedSparqlString(textStmt.getString(), null, base.toString());
    }
    
    @Override
    public ModifyBuilder getModifyBuilder(URI base)
    {
        return getModifyBuilder(base, getUpdate().getModel());
    }
     
    @Override
    public ModifyBuilder getModifyBuilder(URI base, Model commandModel)
    {
        Resource updateOrTemplateCall = getUpdate();
        if (updateOrTemplateCall == null)
        {
            if (log.isErrorEnabled()) log.error("Update not defined for template '{}' (ldt:update missing)", getURI());
            throw new OntologyException("Update not defined for template '" + getURI() +"'");
        }

        return getModifyBuilder(updateOrTemplateCall, base, commandModel);
    }
    
    public ModifyBuilder getModifyBuilder(Resource updateOrTemplateCall, URI base, Model commandModel)
    {
	if (updateOrTemplateCall == null) throw new IllegalArgumentException("Resource cannot be null");
	if (commandModel == null) throw new IllegalArgumentException("Model cannot be null");

        org.spinrdf.model.TemplateCall spinTemplateCall = SPINFactory.asTemplateCall(updateOrTemplateCall);        
        if (spinTemplateCall != null)
            return ModifyBuilder.fromUpdate(getParameterizedSparqlString(spinTemplateCall, base).asUpdate().
                    getOperations().get(0), commandModel);
        else
        {
            org.spinrdf.model.update.Update update = SPINFactory.asUpdate(updateOrTemplateCall);
            if (update == null)
            {
                if (log.isErrorEnabled()) log.error("Class '{}' ldt:update value '{}' is not a SPIN Query or TemplateCall", getURI(), updateOrTemplateCall);
                throw new OntologyException("Class '" + getURI() + "' ldt:query value '" + updateOrTemplateCall + "' not a SPIN Query or TemplateCall");
            }
            
            return ModifyBuilder.fromUpdate(getParameterizedSparqlString(update, base).asUpdate().
                    getOperations().get(0), commandModel);
        }
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
