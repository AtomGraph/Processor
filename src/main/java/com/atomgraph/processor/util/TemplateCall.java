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
package com.atomgraph.processor.util;

import com.atomgraph.core.util.StateBuilder;
import com.atomgraph.processor.exception.ParameterException;
import com.atomgraph.processor.model.Template;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.spinrdf.model.SPINFactory;
import com.atomgraph.processor.model.Parameter;
import com.atomgraph.processor.vocabulary.LDT;
import com.sun.jersey.api.uri.UriComponent;
import java.util.Iterator;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.spinrdf.vocabulary.SPL;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCall extends com.atomgraph.core.util.StateBuilder
{
    
    private final Template template;
    private final Resource original;
    
    protected TemplateCall(Resource resource, Template template)
    {
        super(UriBuilder.fromUri(resource.getURI()), resource.getModel());
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
        this.original = resource;
        this.template = template;
    }
    
    public static TemplateCall fromUri(String uri, Model model, Template template)
    {
        if (uri == null) throw new IllegalArgumentException("URI String cannot be null");
        if (model == null) throw new IllegalArgumentException("Model cannot be null");
        
        return new TemplateCall(model.createResource(uri), template);
    }

    public static TemplateCall fromResource(Resource resource, Template template)
    {
        return new TemplateCall(resource, template);
    }
    
    protected Resource getOriginal()
    {
        return original;
    }
    
    public final Template getTemplate()
    {
        // SPIN uses Template registry instead:
        // return SPINModuleRegistry.get().getTemplate(s.getResource().getURI(), getModel());
        return template;
    }
    
    public String getURI()
    {
        return getResource().getURI();
    }
    
    public TemplateCall applyArguments(MultivaluedMap<String, String> queryParams)
    {
        if (queryParams == null) throw new IllegalArgumentException("Query parameter map cannot be null");

        // iterate query params to find unrecognized ones
        Set<String> argNames = queryParams.keySet();
        for (String argName : argNames)
        {
            Parameter param = getTemplate().getParameterMap().get(argName);
            if (param == null) throw new ParameterException(argName, getTemplate());
        }
        
        // iterate parameters to find those that match query argument names
        Set<String> paramNames = getTemplate().getParameterMap().keySet();
        for (String paramName : paramNames)
        {
            Parameter param = getTemplate().getParameterMap().get(paramName);
            if (queryParams.containsKey(paramName))
            {
                List<String> argValues = queryParams.get(paramName);
                for (String argValue : argValues)
                    arg(param, RDFNodeFactory.createTyped(argValue, param.getValueType()));
            }
        }
        
        return this;
    }
    
    public TemplateCall applyDefaults()
    {
        Iterator<Parameter> paramIt = getTemplate().getParameters().values().iterator();
        
        while (paramIt.hasNext())
        {
            Parameter param = paramIt.next();
            RDFNode defaultValue = param.getDefaultValue();
            if (defaultValue != null && !hasArgument(param.getPredicate()))
                arg(param, defaultValue);
        }
        
        return this;
    }

    public StmtIterator listArguments()
    {
        return getResource().listProperties(LDT.arg);
    }
    
    public boolean hasArgument(Property predicate)
    {
        return getArgument(predicate) != null;
    }
    
    public Resource getArgument(Property predicate) // TO-DO: create model class Argument
    {
        if (predicate == null) throw new IllegalArgumentException("Property cannot be null");
        
        StmtIterator it = getResource().listProperties(LDT.arg);
        
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                Resource arg = stmt.getObject().asResource();
                if (arg.getProperty(SPL.predicate).getResource().equals(predicate)) return arg;
            }
        }
        finally
        {
            it.close();
        }
        
        return null;
    }
    
    public boolean hasArgument(String varName, RDFNode object)
    {
        return getArgument(varName, object) != null;
    }
    
    public Resource getArgument(String varName, RDFNode object)
    {
        if (varName == null) throw new IllegalArgumentException("Var name String cannot be null");
        if (object == null) throw new IllegalArgumentException("RDFNode cannot be null");
        
        StmtIterator it = getResource().listProperties(LDT.arg);
        
        try
        {
            while (it.hasNext())
            {
                Statement stmt = it.next();
                Resource arg = stmt.getObject().asResource();
                if (arg.getProperty(SPL.predicate).getResource().getLocalName().equals(varName) &&
                        arg.getProperty(RDF.value).getObject().equals(object)) return arg;
            }
        }
        finally
        {
            it.close();
        }
        
        return null;
    }

    public Statement getArgumentProperty(Property predicate)
    {
        Resource arg = getArgument(predicate);
        if (arg != null) return arg.getRequiredProperty(RDF.value);
        
        return null;
    }
    
    public TemplateCall arg(Parameter param, RDFNode value)
    {
        if (param == null) throw new IllegalArgumentException("Parameter cannot be null");
        if (value == null) throw new IllegalArgumentException("RDFNode cannot be null");

        Resource arg = StateBuilder.fromUri(getOriginal().getURI(), getResource().getModel()).
            property(param.getPredicate(), value).
            build();
        
        return arg(arg.addProperty(RDF.type, param).
            addLiteral(LDT.paramName, param.getPredicate().getLocalName()).
            addProperty(SPL.predicate, param.getPredicate()).
            addProperty(RDF.value, value));
    }
    
    public TemplateCall arg(Resource arg)
    {
        if (arg == null) throw new IllegalArgumentException("Resource cannot be null");
        
        getResource().addProperty(LDT.arg, arg);

        String paramName = arg.getPropertyResourceValue(SPL.predicate).getLocalName();
        RDFNode value = arg.getProperty(RDF.value).getObject();
        String encodedValue = value.toString(); // not a reliable serialization
        // we URI-encode values ourselves because Jersey 1.x fails to do so: https://java.net/jira/browse/JERSEY-1717
        if (value.isURIResource()) encodedValue = UriComponent.encode(value.asResource().getURI(), UriComponent.Type.UNRESERVED);
        if (value.isLiteral()) encodedValue = UriComponent.encode(value.asLiteral().getString(), UriComponent.Type.UNRESERVED);
        getUriBuilder().queryParam(paramName, encodedValue);

        return this;
    }
    
    public TemplateCall validateOptionals()
    {
        Set<Entry<Property, Parameter>> paramEntries = getTemplate().getParameters().entrySet();
        for (Entry<Property, Parameter> entry : paramEntries)
        {
            if (!hasArgument(entry.getKey()) && !entry.getValue().isOptional())
                throw new ParameterException(entry.getValue(), getTemplate());
        }
        
        return this;
    }
    
    public QuerySolutionMap getQuerySolutionMap()
    {
        QuerySolutionMap qsm = new QuerySolutionMap();
        
        org.spinrdf.model.TemplateCall spinTemplateCall = SPINFactory.asTemplateCall(getTemplate().getQuery());
        if (spinTemplateCall != null)
        {
            qsm = spinTemplateCall.getInitialBinding();
            
            List<org.spinrdf.model.Argument> spinArgs = spinTemplateCall.getTemplate().getArguments(false);
            // add SPIN Arguments that match LDT Arguments (by predicate)
            for (org.spinrdf.model.Argument spinArg : spinArgs)
                if (getTemplate().getParameters().containsKey(spinArg.getPredicate()) && hasArgument(spinArg.getPredicate()))
                {
                    Parameter param = getTemplate().getParameters().get(spinArg.getPredicate());
                    qsm.add(param.getVarName(), getArgumentProperty(param.getPredicate()).getObject());
                }
        }
                
        return qsm;
    }
    
    /*
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(build().getURI()).
        append(">").
        append("[<").
        append(getTemplate().getURI()).
        append(">").
        append("]").
        toString();
    }
    */

}
