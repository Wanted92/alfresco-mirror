/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Mozilla Public License version 1.1 
 * with a permitted attribution clause. You may obtain a
 * copy of the License at
 *
 *   http://www.alfresco.org/legal/license.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.alfresco.repo.admin.patch;

/**
 * Manages patches applied against the repository.
 * <p>
 * Patches are injected into this class and any attempted applications are recorded
 * for later auditing.
 * 
 * @since 1.2
 * @author Derek Hulley
 */
public interface PatchService
{
    /**
     * Registers a patch with the service that executes them.
     * 
     * @param patch the patch to register
     */
    public void registerPatch(Patch patch);
    
    /**
     * Apply all outstanding patches that are relevant to the repo.
     * If there is a failure, then the patches that were applied will remain so,
     * but the process will not attempt to apply any further patches.
     * 
     * @return Returns true if all outstanding patches were applied, or false if the process
     *      was termintated before all patches could be applied.
     */
    public boolean applyOutstandingPatches();
}
