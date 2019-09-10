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

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.XSD;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class RDFNodeFactory
{
    
    public static final RDFNode createTyped(String value, Resource valueType)
    {
	if (value == null) throw new IllegalArgumentException("Param value cannot be null");

        // without value type, return default xsd:string value
        if (valueType == null) return ResourceFactory.createTypedLiteral(value, XSDDatatype.XSDstring);

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
    
}
