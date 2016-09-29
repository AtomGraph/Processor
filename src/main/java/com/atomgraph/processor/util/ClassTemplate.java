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

import org.apache.jena.ontology.OntClass;

/**
 *
 * @author Martynas Jusevičius <martynas@atomgraph.com>
 */
public class ClassTemplate implements Comparable
{

    final private OntClass ontClass;
    final private Double precedence;
    
    public ClassTemplate(OntClass ontClass, Double precedence)
    {
        if (ontClass == null) throw new IllegalArgumentException("OntClass cannot be null");
        if (precedence == null) throw new IllegalArgumentException("Double cannot be null");
        
        this.ontClass = ontClass;
        this.precedence = precedence;
    }

    public final OntClass getOntClass()
    {
        return ontClass;
    }

    public final Double getPrecedence()
    {
        return precedence;
    }
    
    @Override
    public String toString()
    {
        return new StringBuilder().
        append("[<").
        append(getOntClass().getURI()).
        append(">, ").
        append(Double.toString(getPrecedence())).
        append("]").
        toString();
    }

    @Override
    public int compareTo(Object obj)
    {
        ClassTemplate template = (ClassTemplate)obj;
        Double diff = template.getPrecedence() - getPrecedence();
        return diff.intValue();
    }
    
}