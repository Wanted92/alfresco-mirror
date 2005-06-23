/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 * You may obtain a copy of the License at
 *
 *     http://www.gnu.org/licenses/lgpl.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.version.common.counter;

import org.alfresco.service.cmr.repository.StoreRef;

/**
 * Version counter DAO service interface.
 * 
 * @author Roy Wetherall
 */
public interface VersionCounterDaoService
{
    /**
     * Get the next available version number for the specified store.
     * 
     * @param storeRef  the store reference
     * @return          the next version number
     */
    public int nextVersionNumber(StoreRef storeRef);   
    
    /**
     * Gets the current version number for the specified store.
     * 
     * @param storeRef  the store reference
     * @return          the current versio number
     */
    public int currentVersionNumber(StoreRef storeRef);
    
    /**
     * Resets the version number for a the specified store.
     * 
     * WARNING: calling this method will completely reset the current 
     * version count for the specified store and cannot be undone.  
     *
     * @param storeRef  the store reference
     */
    public void resetVersionNumber(StoreRef storeRef);
}
