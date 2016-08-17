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

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.XSD;
import org.topbraid.spin.vocabulary.SPIN;
import org.topbraid.spin.vocabulary.SPL;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class TemplateCallArg
{
    
    public final Resource spinTemplate;
    
    public TemplateCallArg(Resource spinTemplate)
    {
	if (spinTemplate == null) throw new IllegalArgumentException("SPIN template Resource value cannot be null");        
        this.spinTemplate = spinTemplate;
    }

    public QuerySolutionMap getQuerySolutionMap(String paramName, String paramValue)
    {
        return getQuerySolutionMap(getSPINTemplate(), paramName, paramValue);
    }
    
    protected QuerySolutionMap getQuerySolutionMap(Resource spinTemplate, String paramName, String paramValue)
    {
        Statement stmt = getStatement(spinTemplate, ModelFactory.createDefaultModel().createResource(), paramName, paramValue);
        if (stmt != null)
        {
            QuerySolutionMap arg = new QuerySolutionMap();
            arg.add(stmt.getPredicate().getLocalName(), stmt.getObject());
            return arg;
        }
        
        return null;
    }

    public Statement getStatement(Resource subject, String paramName, String paramValue)
    {
        return getStatement(getSPINTemplate(), subject, paramName, paramValue);
    }
            
    protected Statement getStatement(Resource spinTemplate, Resource subject, String paramName, String paramValue)
    {
	if (spinTemplate == null) throw new IllegalArgumentException("SPIN template Resource value cannot be null");        
	if (subject == null) throw new IllegalArgumentException("Resource value cannot be null");                
	if (paramName == null) throw new IllegalArgumentException("Parameter name String cannot be null");
	if (paramValue == null) throw new IllegalArgumentException("Parameter value String cannot be null");

        Statement arg = null;

        StmtIterator constraintIt = spinTemplate.listProperties(SPIN.constraint);
        try
        {
            while (constraintIt.hasNext())
            {
                Statement stmt = constraintIt.next();
                Property predicate = stmt.getResource().getPropertyResourceValue(SPL.predicate).as(Property.class);
                if (predicate.getLocalName().equals(paramName))
                {
                    Resource valueType = stmt.getResource().getPropertyResourceValue(SPL.valueType);
                    return subject.getModel().createStatement(subject, predicate, getNodeByValueType(paramValue, valueType));
                }
            }
        }
        finally
        {
            constraintIt.close();
        }

        return arg;
    }
    
    public static final RDFNode getNodeByValueType(String value, Resource valueType)
    {
	if (value == null) throw new IllegalArgumentException("Param value cannot be null");
        
        if (valueType != null)
        {
            // if value type is from XSD namespace, value is treated as typed literal with XSD datatype
            if (valueType.getNameSpace().equals(XSD.getURI()))
            {
                RDFDatatype dataType = NodeFactory.getType(valueType.getURI());
                return ResourceFactory.createTypedLiteral(value, dataType);
            }
            // otherwise, value is treated as URI resource
            else
                return ResourceFactory.createResource(value);
        }
        else
            return ResourceFactory.createTypedLiteral(value, XSDDatatype.XSDstring);
    }

    public Resource getSPINTemplate()
    {
        return spinTemplate;
    }
    
}
