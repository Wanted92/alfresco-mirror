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
package org.alfresco.repo.version;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.policy.ClassPolicy;
import org.alfresco.repo.policy.PolicyScope;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;

/**
 * Version service policy interfaces
 * 
 * @author Roy Wetherall
 */
public interface VersionServicePolicies
{
	/**
	 * Before create version policy interface.
	 */
	public interface BeforeCreateVersionPolicy extends ClassPolicy
	{
		/**
		 * Called before a new version is created for a version
		 * 
		 * @param versionableNode  reference to the node about to be versioned
		 */
	    public void beforeCreateVersion(NodeRef versionableNode);
	
	}
	
	/**
	 * On create version policy interface
	 */
	public interface OnCreateVersionPolicy extends ClassPolicy
	{
		public void onCreateVersion(
				QName classRef,
				NodeRef versionableNode, 
				Map<String, Serializable> versionProperties,
				PolicyScope nodeDetails);
	}
	
	/**
	 * Calculate version lable policy interface
	 */
	public interface CalculateVersionLabelPolicy extends ClassPolicy
	{
		public String calculateVersionLabel(
				QName classRef,
				Version preceedingVersion,
				int versionNumber,
				Map<String, Serializable>verisonProperties);
	}
}
