/*
 * Copyright 2015 Martynas Jusevičius <martynas@graphity.org>.
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

import org.graphity.processor.util.Modifiers;

/**
 *
 * @author Martynas Jusevičius <martynas@graphity.org>
 */
public class ModifiersBase implements Modifiers
{
    
    private final Long limit, offset;
    private final String orderBy;
    private final Boolean desc;
    
    public ModifiersBase(Long limit, Long offset, String orderBy, Boolean desc)
    {
	//if (limit == null) throw new IllegalArgumentException("LIMIT cannot be null");
	//if (offset == null) throw new IllegalArgumentException("OFFSET cannot be null");
	//if (orderBy == null) throw new IllegalArgumentException("ORDER BY cannot be null");
	//if (desc == null) throw new IllegalArgumentException("DESC cannot be null");
        
        this.limit = limit;
        this.offset = offset;
        this.orderBy = orderBy;
        this.desc = desc;
    }
    
    /**
     * Returns value of <samp>limit</samp> query string parameter, which indicates the number of resources per page.
     * This value is set as <code>LIMIT</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * 
     * @return limit value
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modResultLimit">15.5 LIMIT</a>
     */
    @Override
    public Long getLimit()
    {
	return limit;
    }

    /**
     * Returns value of <samp>offset</samp> query string parameter, which indicates the number of resources the page
     * has skipped from the start of the container.
     * This value is set as <code>OFFSET</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * 
     * @return offset value
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOffset">15.4 OFFSET</a>
     */
    @Override
    public Long getOffset()
    {
	return offset;
    }

    /**
     * Returns value of <samp>orderBy</samp> query string parameter, which indicates the name of the variable after
     * which the container (and the page) is sorted.
     * This value is set as <code>ORDER BY</code> query modifier when this resource is a page (therefore
     * pagination is used).
     * Note that ordering might be undefined, in which case the same page might not contain identical resources
     * during different requests.
     * 
     * @return name of ordering variable or null, if not specified
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOrderBy">15.1 ORDER BY</a>
     */
    @Override
    public String getOrderBy()
    {
	return orderBy;
    }

    /**
     * Returns value of <samp>desc</samp> query string parameter, which indicates the direction of resource ordering
     * in the container (and the page).
     * If this method returns true, <code>DESC</code> order modifier is set if this resource is a page
     * (therefore pagination is used).
     * 
     * @return true if the order is descending, false otherwise
     * @see <a href="http://www.w3.org/TR/sparql11-query/#modOrderBy">15.1 ORDER BY</a>
     */
    @Override
    public Boolean getDesc()
    {
	return desc;
    }

}
