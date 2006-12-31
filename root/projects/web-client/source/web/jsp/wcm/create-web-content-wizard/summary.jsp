<%--
    Copyright (C) 2005 Alfresco, Inc.
    
    Licensed under the Mozilla Public License version 1.1 
    with a permitted attribution clause. You may obtain a
    copy of the License at
    
    http://www.alfresco.org/legal/license.txt
    
    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
    either express or implied. See the License for the specific
    language governing permissions and limitations under the
    License.
  --%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/alfresco.tld" prefix="a" %>
<%@ taglib uri="/WEB-INF/repo.tld" prefix="r" %>
<%@ page isELIgnored="false" %>
<script type="text/javascript">
  window.onload = function() { document.getElementById("wizard:finish-button").focus(); }
</script>

<h:panelGrid columns="1" cellpadding="2" style="padding-top: 4px; padding-bottom: 4px;"
             width="100%" rowClasses="wizardSectionHeading">
  <h:outputText value="&nbsp;#{msg.create_web_content_summary_content_details}" escape="false" />
</h:panelGrid>

<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" width="100%">
  <a:selectList id="form-instance-data-list" 
                multiSelect="false"
                activeSelect="true" 
                style="width:100%" 
                itemStyleClass="selectListItem">
    <a:listItem label="${WizardManager.bean.formInstanceData.name}"
                value="${WizardManager.bean.formInstanceData.name}"
                image="/images/filetypes32/xml.gif">
      <jsp:attribute name="description">
        <div>${msg.form}: ${WizardManager.bean.form.title}</div>
        <div>${msg.location}: ${WizardManager.bean.formInstanceData.sandboxRelativePath}</div>
      </jsp:attribute>
    </a:listItem>
  </a:selectList>
</h:panelGrid>

<h:panelGrid columns="1" cellpadding="2" style="padding-top: 4px; padding-bottom: 4px;"
             width="100%" rowClasses="wizardSectionHeading"
	     rendered="#{!empty WizardManager.bean.renditions}">
  <h:outputText value="&nbsp;#{msg.create_web_content_summary_rendition_details}" escape="false" />
</h:panelGrid>

<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" width="100%"
	     rendered="#{!empty WizardManager.bean.renditions}">
  <a:selectList id="rendition-list" 
		multiSelect="false"
		activeSelect="true" 
		style="width:100%" 
		itemStyleClass="selectListItem">
    <c:forEach items="${WizardManager.bean.renditions}" 
	       var="rendition" 
	       varStatus="status">
      <a:listItem id="listItem${status.index}"
		  label="${rendition.name}"
		  value="${rendition.name}"
                  image="${rendition.fileTypeImage}">
	<jsp:attribute name="description">
	  <span style="float:right;">
	    <a id="preview${status.index}"
	       href="${rendition.url}"
	       style="text-decoration: none;"
	       target="window_${status.index}_${rendition.name}">
	      <jsp:element name="img">
		<jsp:attribute name="src" trim="true">
		  <c:out value="${pageContext.request.contextPath}"/>/images/icons/preview_website.gif
		</jsp:attribute>
		<jsp:attribute name="align">absmiddle</jsp:attribute>
		<jsp:attribute name="style">border: 0px</jsp:attribute>
		<jsp:attribute name="alt">${rendition.name}</jsp:attribute>
	      </jsp:element>
	    </a>
	  </span>
	  <span>${rendition.description}</span>
	</jsp:attribute>
      </a:listItem>
    </c:forEach>
  </a:selectList>
</h:panelGrid>

<h:panelGrid columns="1" cellpadding="2" style="padding-top: 4px; padding-bottom: 4px;"
             width="100%" rowClasses="wizardSectionHeading"
	     rendered="#{!empty WizardManager.bean.uploadedFiles}">
  <h:outputText value="&nbsp;#{msg.create_web_content_summary_uploaded_files_details}" escape="false" />
</h:panelGrid>

<h:panelGrid columns="2" cellpadding="3" cellspacing="3" border="0" width="100%"
	     rendered="#{!empty WizardManager.bean.uploadedFiles}">
  <a:selectList id="uploaded-file-list" 
		multiSelect="false"
		activeSelect="true" 
		style="width:100%" 
		itemStyleClass="selectListItem">
    <a:listItems value="#{WizardManager.bean.uploadedFiles}"/>
  </a:selectList>
</h:panelGrid>

<h:panelGrid columns="1" cellpadding="2" style="padding-top: 4px; padding-bottom: 4px;"
             width="100%">
  <h:column>
    <h:selectBooleanCheckbox id="startWorkflow"
			     value="#{WizardManager.bean.startWorkflow}"/>
    <h:outputFormat value="&nbsp;#{msg.create_web_content_summary_submit_message}" escape="false">
      <f:param value="#{WizardManager.bean.numberOfSubmittableFiles}"/>
      <f:param value="#{WizardManager.bean.formInstanceData.name}"/>
    </h:outputFormat>
  </h:column>
</h:panelGrid>
