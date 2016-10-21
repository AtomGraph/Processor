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

import com.atomgraph.processor.exception.ArgumentException;
import com.atomgraph.processor.model.Argument;
import com.atomgraph.processor.model.Template;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.http.NameValuePair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class TemplateCall extends com.atomgraph.core.util.StateBuilder // implements Resource
{
    
    private final Template template;
    
    protected TemplateCall(Resource resource, Template template)
    {
        super(UriBuilder.fromUri(resource.getURI()), resource.getModel());
        if (template == null) throw new IllegalArgumentException("Template cannot be null");
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
    
    public final Template getTemplate()
    {
        // SPIN uses Template registry instead:
        // return SPINModuleRegistry.get().getTemplate(s.getResource().getURI(), getModel());
        return template;
    }

    public TemplateCall applyArguments(Map<Property, RDFNode> values)
    {
	if (values == null) throw new IllegalArgumentException("Value Map cannot be null");
        
        Iterator<Map.Entry<Property, RDFNode>> entryIt = values.entrySet().iterator();
        
        while (entryIt.hasNext())
        {
            Map.Entry<Property, RDFNode> entry = entryIt.next();
            replaceProperty(entry.getKey(), entry.getValue());
        }
        
        return this;
    }
    
    public TemplateCall applyArguments(MultivaluedMap<String, String> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter map cannot be null");

        // iterate query params to find unrecognized ones
        Set<String> paramNames = queryParams.keySet();
        for (String paramName : paramNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(paramName);
            if (arg == null) throw new ArgumentException(paramName, getTemplate());
        }
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(argName);
            if (queryParams.containsKey(argName))
            {            
                String argValue = queryParams.getFirst(argName);
                replaceProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            }
            else
                if (!arg.isOptional())
                    throw new ArgumentException(argName, getTemplate()); // TO-DO: throw as required
        }
        
        return this;
    }

    public TemplateCall applyArguments(List<NameValuePair> queryParams)
    {
	if (queryParams == null) throw new IllegalArgumentException("Query parameter list cannot be null");

        // iterate query params to find unrecognized ones
        for (NameValuePair param : queryParams)
        {
            Argument arg = getTemplate().getArgumentsMap().get(param.getName());
            if (arg == null) throw new ArgumentException(param.getName(), getTemplate());
        }
        
        // iterate arguments to find required (non-optional) ones
        Set<String> argNames = getTemplate().getArgumentsMap().keySet();
        for (String argName : argNames)
        {
            Argument arg = getTemplate().getArgumentsMap().get(argName);
            String argValue = null;
            
            for (NameValuePair param : queryParams)
                if (param.getName().equals(argName))
                    argValue = param.getValue();

            if (argValue != null)
                replaceProperty(arg.getPredicate(), RDFNodeFactory.createTyped(argValue, arg.getValueType()));
            else if (!arg.isOptional())
                throw new ArgumentException(argName, getTemplate()); // TO-DO: throw as required            
        }
        
        return this;
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
    
    /*
    @Override
    public AnonId getId()
    {
        return getResource().getId();
    }

    @Override
    public Resource inModel(Model m)
    {
        return getResource().inModel(m);
    }

    @Override
    public boolean hasURI(String uri)
    {
        return getResource().hasURI(uri);
    }

    @Override
    public String getURI()
    {
        return getResource().getURI();
    }

    @Override
    public String getNameSpace()
    {
        return getResource().getNameSpace();
    }

    @Override
    public String getLocalName()
    {
        return getResource().getLocalName();
    }

    @Override
    public Statement getRequiredProperty(Property p)
    {
        return getResource().getRequiredProperty(p);
    }

    @Override
    public Statement getProperty(Property p)
    {
        return getResource().getProperty(p);
    }

    @Override
    public StmtIterator listProperties(Property p)
    {
        return getResource().listProperties(p);
    }

    @Override
    public StmtIterator listProperties()
    {
        return getResource().listProperties();
    }

    @Override
    public Resource addLiteral(Property p, boolean o)
    {
        return getResource().addLiteral(p, o);
    }

    @Override
    public Resource addLiteral(Property p, long o)
    {
        return getResource().addLiteral(p, o);
    }

    @Override
    public Resource addLiteral(Property p, char o)
    {
        return getResource().addLiteral(p, o);
    }

    @Override
    public Resource addLiteral(Property p, double d)
    {
        return getResource().addLiteral(p, d);
    }

    @Override
    public Resource addLiteral(Property p, float d)
    {
        return getResource().addLiteral(p, d);
    }

    @Override
    public Resource addLiteral(Property p, Object o)
    {
        return getResource().addLiteral(p, o);
    }

    @Override
    public Resource addLiteral(Property p, Literal o)
    {
        return getResource().addLiteral(p, o);
    }

    @Override
    public Resource addProperty(Property p, String o)
    {
        return getResource().addProperty(p, o);
    }

    @Override
    public Resource addProperty(Property p, String o, String l)
    {
        return getResource().addProperty(p, o, l);
    }

    @Override
    public Resource addProperty(Property p, String lexicalForm, RDFDatatype datatype)
    {
        return getResource().addProperty(p, lexicalForm, datatype);
    }

    @Override
    public Resource addProperty(Property p, RDFNode o)
    {
        return getResource().addProperty(p, o);
    }

    @Override
    public boolean hasProperty(Property p)
    {
        return getResource().hasProperty(p);
    }

    @Override
    public boolean hasLiteral(Property p, boolean o)
    {
        return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasLiteral(Property p, long o)
    {
       return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasLiteral(Property p, char o)
    {
        return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasLiteral(Property p, double o)
    {
        return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasLiteral(Property p, float o)
    {
        return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasLiteral(Property p, Object o)
    {
        return getResource().hasLiteral(p, o);
    }

    @Override
    public boolean hasProperty(Property p, String o)
    {
        return getResource().hasProperty(p, o);
    }

    @Override
    public boolean hasProperty(Property p, String o, String l)
    {
        return getResource().hasProperty(p, o, l);
    }

    @Override
    public boolean hasProperty(Property p, RDFNode o)
    {
        return getResource().hasProperty(p, o);
    }

    @Override
    public Resource removeProperties()
    {
        return getResource().removeProperties();
    }

    @Override
    public Resource removeAll(Property p)
    {
        return getResource().removeAll(p);
    }

    @Override
    public Resource begin()
    {
        return getResource().begin();
    }

    @Override
    public Resource abort()
    {
        return getResource().abort();
    }

    @Override
    public Resource commit()
    {
        return getResource().commit();
    }

    @Override
    public Resource getPropertyResourceValue(Property p)
    {
        return getResource().getPropertyResourceValue(p);
    }

    @Override
    public boolean isAnon()
    {
        return getResource().isAnon();
    }

    @Override
    public boolean isLiteral()
    {
        return getResource().isLiteral();
    }

    @Override
    public boolean isURIResource()
    {
        return getResource().isURIResource();
    }

    @Override
    public boolean isResource()
    {
        return getResource().isResource();
    }

    @Override
    public <T extends RDFNode> T as(Class<T> view)
    {
        return getResource().as(view);
    }

    @Override
    public <T extends RDFNode> boolean canAs(Class<T> view)
    {
        return getResource().canAs(view);
    }

    @Override
    public Model getModel()
    {
        return getResource().getModel();
    }

    @Override
    public Object visitWith(RDFVisitor rv)
    {
        return getResource().visitWith(rv);
    }

    @Override
    public Resource asResource()
    {
        return getResource().asResource();
    }

    @Override
    public Literal asLiteral()
    {
        return getResource().asLiteral();
    }

    @Override
    public Node asNode()
    {
        return getResource().asNode();
    }
    */
    
}
