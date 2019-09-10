/*
 * Copyright 2015 Martynas Jusevičius <martynas@atomgraph.com>.
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

import org.apache.jena.reasoner.TriplePattern;
import org.apache.jena.reasoner.rulesys.ClauseEntry;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.PrintUtil;
import java.util.List;

/**
 *
 * @author Martynas Jusevičius {@literal <martynas@atomgraph.com>}
 */
public class RulePrinter
{

    public static String print(List<Rule> rules)
    {
        StringBuilder buff = new StringBuilder();

        for (Rule rule : rules)
        {
            buff.append(print(rule));
        }
        
        return buff.toString();
    }
    
    public static String print(Rule rule)
    {
        StringBuilder buff = new StringBuilder();
        buff.append("[ ");
        if (rule.getName() != null)
        {
            buff.append(rule.getName());
            buff.append(": ");
        }
        if (rule.isBackward())
        {
            for ( ClauseEntry aHead : rule.getHead() )
            {
                buff.append( print( aHead ) );
                buff.append( " " );
            }
            buff.append("<- ");
            for ( ClauseEntry aBody : rule.getBody() )
            {
                buff.append( print( aBody ) );
                buff.append( " " );
            }
        }
        else
        {
            for ( ClauseEntry aBody : rule.getBody() )
            {
                buff.append( print( aBody ) );
                buff.append( " " );
            }
            buff.append("-> ");
            for ( ClauseEntry aHead : rule.getHead() )
            {
                buff.append( print( aHead ) );
                buff.append( " " );
            }
        }
        buff.append("]");
        return buff.toString();
    }
    
    public static String print(ClauseEntry entry)
    {
        if (entry instanceof TriplePattern)
        {
            TriplePattern tp = (TriplePattern)entry;
            StringBuilder buff = new StringBuilder();
            buff.append("(");
            
            if (tp.getSubject().isURI())
                buff.append("<").
                    append(tp.getSubject().getURI()).
                    append(">").
                    toString();
            else buff.append(PrintUtil.print(tp.getSubject()));
            
            buff.append(" ");
            
            if (tp.getPredicate().isURI())
                buff.append("<").
                    append(tp.getPredicate().getURI()).
                    append(">").
                    toString();
            else buff.append(PrintUtil.print(tp.getPredicate()));
            
            buff.append(" ");
            
            if (tp.getObject().isURI())
                buff.append("<").
                    append(tp.getObject().getURI()).
                    append(">").
                    toString();
            else buff.append(PrintUtil.print(tp.getObject()));
            
            buff.append(")");
            return buff.toString();
        }
        
        return PrintUtil.print(entry);
    }
    
}
