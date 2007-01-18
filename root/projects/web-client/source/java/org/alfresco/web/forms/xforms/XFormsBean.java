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
package org.alfresco.web.forms.xforms;

import java.io.*;
import java.util.*;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.avm.AVMNodeConverter;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.domain.PropertyValue;
import org.alfresco.service.cmr.avm.AVMNodeDescriptor;
import org.alfresco.service.cmr.avm.AVMService;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.alfresco.web.app.Application;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.app.servlet.ajax.InvokeCommand;
import org.alfresco.web.bean.FileUploadBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.bean.wcm.AVMBrowseBean;
import org.alfresco.web.bean.wcm.AVMConstants;
import org.alfresco.web.bean.wcm.AVMNode;
import org.alfresco.web.forms.*;
import org.alfresco.web.ui.common.Utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.chiba.xml.events.ChibaEventNames;
import org.chiba.xml.events.DOMEventNames;
import org.chiba.xml.events.XFormsEventNames;
import org.chiba.xml.events.XMLEvent;
import org.chiba.xml.xforms.ChibaBean;
import org.chiba.xml.xforms.XFormsElement;
import org.chiba.xml.xforms.connector.http.AbstractHTTPConnector;
import org.chiba.xml.xforms.core.Instance;
import org.chiba.xml.xforms.core.ModelItem;
import org.chiba.xml.xforms.core.Model;
import org.chiba.xml.xforms.core.UpdateHandler;
import org.chiba.xml.xforms.core.impl.DefaultValidatorMode;
import org.chiba.xml.xforms.exception.XFormsException;
import org.chiba.xml.xforms.ui.RepeatItem;
import org.chiba.xml.xforms.ui.Upload;
import org.chiba.xml.ns.NamespaceConstants;
import org.springframework.util.FileCopyUtils;

import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.ls.*;
import org.xml.sax.SAXException;

/**
 * Bean for interacting with the chiba processor from the ui using ajax requests.
 * Manages the chiba bean lifecycle.
 */
public class XFormsBean
{

   /////////////////////////////////////////////////////////////////////////////
   
   /**
    */
   class XFormsSession implements FormProcessor.Session
   {

      private final Document formInstanceData;
      private final Form form;

      private ChibaBean chibaBean;
      private final SchemaFormBuilder schemaFormBuilder;
      private final Set<NodeRef> uploads = new HashSet<NodeRef>();
      private final List<XMLEvent> eventLog = new LinkedList<XMLEvent>();

      public XFormsSession(final Document formInstanceData,
                           final Form form,
                           final String baseUrl)
      {
         this.formInstanceData = formInstanceData;
         this.form = form;
         this.schemaFormBuilder = 
            new SchemaFormBuilder("/ajax/invoke/XFormsBean.handleAction",
                                  SchemaFormBuilder.SubmitMethod.POST,
                                  baseUrl);
      }

      public void addUpload(final NodeRef nr)
      {
         this.uploads.add(nr);
      }

      public NodeRef[] getUploadedFiles()
      {
         return (NodeRef[])this.uploads.toArray(new NodeRef[0]);
      }

      public void destroy()
      {
         this.uploads.clear();
         try
         {
            this.chibaBean.shutdown();
         }
         catch (XFormsException xfe)
         {
            LOGGER.debug(xfe);
         }
      }

      public Form getForm()
      {
         return this.form;
      }

      public Document getFormInstanceData()
      {
         return this.formInstanceData;
      }
   }

   /////////////////////////////////////////////////////////////////////////////

   private static final Log LOGGER = LogFactory.getLog(XFormsBean.class);

   private XFormsSession xformsSession;
   private Schema2XFormsProperties schema2XFormsProperties;
   private AVMBrowseBean avmBrowseBean;
   private AVMService avmService;
   private NodeService nodeService;

   public XFormsBean()
   {
   }

   /**
    * @param schema2XFormsProperties the schema2XFormsProperties to set.
    */
   public void setSchema2XFormsProperties(final Schema2XFormsProperties schema2XFormsProperties)
   {
      this.schema2XFormsProperties = schema2XFormsProperties;
   }

   /**
    * @param avmBrowseBean the avmBrowseBean to set.
    */
   public void setAvmBrowseBean(final AVMBrowseBean avmBrowseBean)
   {
      this.avmBrowseBean = avmBrowseBean;
   }
   
   /**
    * @param avmService the avmService to set.
    */
   public void setAvmService(final AVMService avmService)
   {
      this.avmService = avmService;
   }

   /** @param xformsSession the current session */
   public void setXFormsSession(final XFormsSession xformsSession)
      throws FormBuilderException,
      XFormsException
   {
      this.xformsSession = xformsSession;

      final FacesContext facesContext = FacesContext.getCurrentInstance();
      final ExternalContext externalContext = facesContext.getExternalContext();
      final HttpServletRequest request = (HttpServletRequest)
         externalContext.getRequest();
      final ServletContext servletContext = (ServletContext)
         externalContext.getContext();
      
      final ChibaBean chibaBean = new ChibaBean();
      chibaBean.setConfig(servletContext.getRealPath("/WEB-INF/chiba.xml"));
      XFormsBean.storeCookies(request.getCookies(), chibaBean);
      chibaBean.setXMLContainer(this.getXFormsDocument());

      final EventTarget et = (EventTarget)
         chibaBean.getXMLContainer().getDocumentElement();
      final EventListener el = new EventListener()
      {
         public void handleEvent(final Event e)
         {
            final XMLEvent xmle = (XMLEvent)e;
            XFormsBean.LOGGER.debug("received event " + xmle.getType() + ": " + xmle);
            XFormsBean.this.xformsSession.eventLog.add(xmle);
         }
      };
      // interaction events my occur during init so we have to register before
      et.addEventListener(ChibaEventNames.LOAD_URI, el, true);
      et.addEventListener(ChibaEventNames.RENDER_MESSAGE, el, true);
      et.addEventListener(ChibaEventNames.REPLACE_ALL, el, true);
      et.addEventListener(XFormsEventNames.REQUIRED, el, true);
      et.addEventListener(XFormsEventNames.OPTIONAL, el, true);
      et.addEventListener(XFormsEventNames.VALID, el, true);
      et.addEventListener(XFormsEventNames.INVALID, el, true);
      et.addEventListener(XFormsEventNames.OUT_OF_RANGE, el, true);

      chibaBean.init();

      // register for notification events
      et.addEventListener(XFormsEventNames.SUBMIT, el, true);
      et.addEventListener(XFormsEventNames.SUBMIT_DONE, el, true);
      et.addEventListener(XFormsEventNames.SUBMIT_ERROR, el, true);
      et.addEventListener(ChibaEventNames.STATE_CHANGED, el, true);
      et.addEventListener(ChibaEventNames.PROTOTYPE_CLONED, el, true);
      et.addEventListener(ChibaEventNames.ID_GENERATED, el, true);
      et.addEventListener(ChibaEventNames.ITEM_INSERTED, el, true);
      et.addEventListener(ChibaEventNames.ITEM_DELETED, el, true);
      et.addEventListener(ChibaEventNames.INDEX_CHANGED, el, true);
      et.addEventListener(ChibaEventNames.SWITCH_TOGGLED, el, true);
      this.xformsSession.chibaBean = chibaBean;
   }

   /**
    * Initializes the chiba process with the xform and registers any necessary
    * event listeners.
    */
   public XFormsSession createSession(final Document formInstanceData,
                                      final Form form)
   {
      if (LOGGER.isDebugEnabled())
      {
         LOGGER.debug("initializing xforms session with form " + form.getName() +
                      " and instance data " + formInstanceData);
      }

      final FacesContext facesContext = FacesContext.getCurrentInstance();
      final ExternalContext externalContext = facesContext.getExternalContext();
      final HttpServletRequest request = (HttpServletRequest)
         externalContext.getRequest();

      final String baseUrl = (request.getScheme() + "://" + 
                              request.getServerName() + ':' + 
                              request.getServerPort() + 
                              request.getContextPath());
      return this.new XFormsSession(formInstanceData, form, baseUrl);
   }

   /**
    * Writes the xform out to the http servlet response.  This allows
    * us to use the browser to parse the xform using XMLHttpRequest.
    */
   public void getXForm() 
      throws IOException,
      XFormsException
   {
      LOGGER.debug(this + ".getXForm()");
      final FacesContext context = FacesContext.getCurrentInstance();
      final ResponseWriter out = context.getResponseWriter();
      final ChibaBean chibaBean = this.xformsSession.chibaBean;
      final Node xformsDocument = chibaBean.getXMLContainer();
      XMLUtil.print(xformsDocument, out);
   }

   /**
    * sets the value of a control in the processor.
    *
    * @param id the id of the control in the host document
    * @param value the new value
    * @return the list of events that may result through this action
    */
   public void setXFormsValue() 
      throws XFormsException, IOException
   {
      final FacesContext context = FacesContext.getCurrentInstance();
      final Map requestParameters = context.getExternalContext().getRequestParameterMap();
      final String id = (String)requestParameters.get("id");
      final String value = (String)requestParameters.get("value");

      LOGGER.debug(this + ".setXFormsValue(" + id + ", " + value + ")");
      final ChibaBean chibaBean = this.xformsSession.chibaBean;
      if (chibaBean.getContainer().lookup(id) instanceof Upload)
      {
         chibaBean.updateControlValue(id, null, value, value.getBytes());
      }
      else
      {
         chibaBean.updateControlValue(id, value);
      }

      final ResponseWriter out = context.getResponseWriter();
      XMLUtil.print(this.getEventLog(), out);
      out.close();
   }

   /**
    * sets the value of a control in the processor.
    *
    * @param id the id of the control in the host document
    * @param value the new value
    * @return the list of events that may result through this action
    */
   public void setRepeatIndeces() 
      throws XFormsException, IOException
   {
      final FacesContext context = FacesContext.getCurrentInstance();
      final Map requestParameters = context.getExternalContext().getRequestParameterMap();
      final String repeatIds = (String)requestParameters.get("repeatIds");
      LOGGER.debug(this + ".setRepeatIndeces(" + repeatIds + ")");
      for (String id : repeatIds.split(","))
      {
         final int index = Integer.parseInt((String)requestParameters.get(id));
         LOGGER.debug(this + ".setRepeatIndex(" + id + ", " + index + ")");
         final ChibaBean chibaBean = this.xformsSession.chibaBean;
         chibaBean.updateRepeatIndex(id, index);
      }
      final ResponseWriter out = context.getResponseWriter();
      XMLUtil.print(this.getEventLog(), out);
      out.close();
   }

   /**
    * fires an action associated with a trigger.
    *
    * @param id the id of the control in the host document
    */
   public void fireAction() 
      throws XFormsException, IOException
   {
      final FacesContext context = FacesContext.getCurrentInstance();
      final Map requestParameters = context.getExternalContext().getRequestParameterMap();
      final String id = (String)requestParameters.get("id");

      LOGGER.debug(this + ".fireAction(" + id + ")");
      final ChibaBean chibaBean = this.xformsSession.chibaBean;
      chibaBean.dispatch(id, DOMEventNames.ACTIVATE);

      final ResponseWriter out = context.getResponseWriter();
      XMLUtil.print(this.getEventLog(), out);
      out.close();
   }

   /**
    * handles submits and sets the instance data.
    */
   public void handleAction() 
   {
      LOGGER.debug(this + ".handleAction");
      try
      {
         final FacesContext context = FacesContext.getCurrentInstance();
         final HttpServletRequest request = (HttpServletRequest)
            context.getExternalContext().getRequest();
         final Document result = XMLUtil.parse(request.getInputStream());
         final Document instanceData = this.xformsSession.getFormInstanceData();
         Element documentElement = instanceData.getDocumentElement();
         if (documentElement != null)
         {
            instanceData.removeChild(documentElement);
         }

         documentElement = result.getDocumentElement();
         this.xformsSession.schemaFormBuilder.removePrototypeNodes(documentElement);
         documentElement = (Element)instanceData.importNode(documentElement, true);
         instanceData.appendChild(documentElement);

         final ResponseWriter out = context.getResponseWriter();
         XMLUtil.print(instanceData, out);
         out.close();
      }
      catch (Throwable t)
      {
         LOGGER.error(t.getMessage(), t);
      }
   }

   /**
    * Swaps model nodes to implement reordering within repeats.
    */
   public void swapRepeatItems() 
      throws Exception
   {
      final FacesContext context = FacesContext.getCurrentInstance();
      final Map requestParameters = context.getExternalContext().getRequestParameterMap();

      final String fromItemId = (String)requestParameters.get("fromItemId");
      final String toItemId = (String)requestParameters.get("toItemId");
      LOGGER.debug(this + ".swapRepeatItems(" + fromItemId + ", " + toItemId + ")");
      final ChibaBean chibaBean = this.xformsSession.chibaBean;
      this.swapRepeatItems((RepeatItem)chibaBean.getContainer().lookup(fromItemId), 
                           (RepeatItem)chibaBean.getContainer().lookup(toItemId));

      final ResponseWriter out = context.getResponseWriter();
      XMLUtil.print(this.getEventLog(), out);
      out.close();
   }

   /**
    * Provides data for a file picker widget.
    */
   @InvokeCommand.ResponseMimetype(value=MimetypeMap.MIMETYPE_XML)
   public void getFilePickerData()
      throws Exception
   {
      final FacesContext facesContext = FacesContext.getCurrentInstance();
      final ExternalContext externalContext = facesContext.getExternalContext();

      final Map requestParameters = externalContext.getRequestParameterMap();
      String currentPath = (String)requestParameters.get("currentPath");
      if (currentPath == null)
      {
         currentPath = this.getCurrentAVMPath();
      }
      else
      {
         final String previewStorePath = 
            AVMConstants.getCorrespondingPathInPreviewStore(this.getCurrentAVMPath());
         currentPath = AVMConstants.buildPath(previewStorePath,
                                              currentPath,
                                              AVMConstants.PathRelation.WEBAPP_RELATIVE);
      }
      LOGGER.debug(this + ".getFilePickerData(" + currentPath + ")");

      final Document result = XMLUtil.newDocument();
      final Element filePickerDataElement = result.createElement("file-picker-data");
      result.appendChild(filePickerDataElement);


      final AVMNodeDescriptor currentNode = this.avmService.lookup(-1, currentPath);
      if (currentNode == null)
      {
         final Element errorElement = result.createElement("error");
         errorElement.appendChild(result.createTextNode("Path " + currentPath + " not found"));
         filePickerDataElement.appendChild(errorElement);
         currentPath = this.getCurrentAVMPath();
      }
      else if (! currentNode.isDirectory())
      {
         currentPath = AVMNodeConverter.SplitBase(currentPath)[0];
      }
      
      Element e = result.createElement("current-node");
      e.setAttribute("avmPath", currentPath);
      e.setAttribute("webappRelativePath", 
                     AVMConstants.getWebappRelativePath(currentPath));
      e.setAttribute("type", "directory");
      e.setAttribute("image", "/images/icons/space_small.gif");
      filePickerDataElement.appendChild(e);

      for (Map.Entry<String, AVMNodeDescriptor> entry : 
              this.avmService.getDirectoryListing(-1, currentPath).entrySet())
      {
         e = result.createElement("child-node");
         e.setAttribute("avmPath", entry.getValue().getPath());
         e.setAttribute("webappRelativePath", 
                        AVMConstants.getWebappRelativePath(entry.getValue().getPath()));
         e.setAttribute("type", entry.getValue().isDirectory() ? "directory" : "file");
         e.setAttribute("image", (entry.getValue().isDirectory()
                                  ? "/images/icons/space_small.gif"
                                  : Utils.getFileTypeImage(facesContext, 
                                                           entry.getValue().getName(),
                                                           true)));
         filePickerDataElement.appendChild(e);
      }

      final ResponseWriter out = facesContext.getResponseWriter();
      XMLUtil.print(result, out);
      out.close();
   }
   
   @InvokeCommand.ResponseMimetype(value=MimetypeMap.MIMETYPE_HTML)
   public void uploadFile()
      throws Exception
   {
      LOGGER.debug(this + ".uploadFile()");
      final FacesContext facesContext = FacesContext.getCurrentInstance();
      final ExternalContext externalContext = facesContext.getExternalContext();
      final HttpServletRequest request = (HttpServletRequest)
         externalContext.getRequest();

      final ServletFileUpload upload = 
         new ServletFileUpload(new DiskFileItemFactory());
      upload.setHeaderEncoding("UTF-8");
      final List<FileItem> fileItems = upload.parseRequest(request);
      final FileUploadBean bean = new FileUploadBean();
      String uploadId = null;
      String currentPath = null;
      String filename = null;
      String returnPage = null;
      InputStream fileInputStream = null;
      for (FileItem item : fileItems)
      {
         LOGGER.debug("item = " + item);
         if (item.isFormField() && item.getFieldName().equals("upload-id"))
         {
            uploadId = item.getString();
            LOGGER.debug("uploadId is " + uploadId);
         }
         if (item.isFormField() && item.getFieldName().equals("return-page"))
         {
            returnPage = item.getString();
            LOGGER.debug("returnPage is " + returnPage);
         }
         else if (item.isFormField() && item.getFieldName().equals("currentPath"))
         {
            final String previewStorePath = 
               AVMConstants.getCorrespondingPathInPreviewStore(this.getCurrentAVMPath());
            currentPath = AVMConstants.buildPath(previewStorePath,
                                                 item.getString(),
                                                 AVMConstants.PathRelation.WEBAPP_RELATIVE);
            LOGGER.debug("currentPath is " + currentPath);
         }
         else
         {
            filename = FilenameUtils.getName(item.getName());
            fileInputStream = item.getInputStream();
            LOGGER.debug("uploading file " + filename);
         }
      }

      LOGGER.debug("saving file " + filename + " to " + currentPath);
      
      try
      {
         FileCopyUtils.copy(fileInputStream, 
                            this.avmService.createFile(currentPath, filename));
         final Map<QName, PropertyValue> props = new HashMap<QName, PropertyValue>(2, 1.0f);
         props.put(ContentModel.PROP_TITLE, new PropertyValue(DataTypeDefinition.TEXT, filename));
         props.put(ContentModel.PROP_DESCRIPTION, 
                   new PropertyValue(DataTypeDefinition.TEXT,
                                     "Uploaded for form " + this.xformsSession.getForm().getName()));
         this.avmService.setNodeProperties(currentPath + "/" + filename, props);
         this.avmService.addAspect(currentPath + "/" + filename, ContentModel.ASPECT_TITLED); 
         
         this.xformsSession.addUpload(AVMNodeConverter.ToNodeRef(-1, currentPath + "/" + filename));
         returnPage = returnPage.replace("${_FILE_TYPE_IMAGE}",
                                         Utils.getFileTypeImage(facesContext, filename, true));
      }
      catch (Exception e)
      {
         LOGGER.debug(e.getMessage(), e);
         returnPage = returnPage.replace("${_UPLOAD_ERROR}", e.getMessage());
      }

      LOGGER.debug("upload complete.  sending response: " + returnPage);
      final Document result = XMLUtil.newDocument();
      final Element htmlEl = result.createElement("html");
      result.appendChild(htmlEl);
      final Element bodyEl = result.createElement("body");
      htmlEl.appendChild(bodyEl);

      final Element scriptEl = result.createElement("script");
      bodyEl.appendChild(scriptEl);
      scriptEl.setAttribute("type", "text/javascript");
      final Node scriptText = result.createTextNode(returnPage);
      scriptEl.appendChild(scriptText);

      final ResponseWriter out = facesContext.getResponseWriter();
      XMLUtil.print(result, out);
      out.close();
   }

   private void swapRepeatItems(final RepeatItem from,
                                final RepeatItem to)
      throws XFormsException
   {
      LOGGER.debug("swapping repeat item " + from + " with " + to);

      LOGGER.debug("from instance id  " + from.getInstanceId());
      final Model model = from.getModel();
      final Instance instance = model.getInstance(from.getInstanceId());
      assert instance == to.getModel().getInstance(to.getInstanceId());
      final String fromLocationPath = from.getLocationPath();
      final String toLocationPath = to.getLocationPath();
      
      if (LOGGER.isDebugEnabled())
      {
         LOGGER.debug("from {id: " + from.getId() + ",position: " + from.getPosition() +
                      "} " + fromLocationPath + 
                      "=" + instance.getModelItem(fromLocationPath).getValue());
         LOGGER.debug("to {id:" + to.getId() + ",position: " + to.getPosition() +
                      "} " + toLocationPath + 
                      "=" + instance.getModelItem(toLocationPath).getValue());
      }

      String beforeLocation = toLocationPath;
      if (from.getPosition() < to.getPosition())
      {
         final RepeatItem beforeItem = to.getRepeat().getRepeatItem(to.getPosition() + 1);
         beforeLocation = (beforeItem != null
                           ? beforeItem.getLocationPath()
                           : to.getRepeat().getLocationPath().replaceAll("\\[position\\(\\)[\\s]*!=[\\s]*last\\(\\)]$",
                                                                         "[position()=last()]"));
      }
      LOGGER.debug("inserting node before " + beforeLocation);
      instance.insertNode(fromLocationPath, beforeLocation);

      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REBUILD, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.RECALCULATE, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REVALIDATE, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REFRESH, null);

      LOGGER.debug("deleting from " + from.getLocationPath());
      // need to reload from location path since it has moved
      instance.deleteNode(from.getLocationPath());

      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REBUILD, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.RECALCULATE, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REVALIDATE, null);
      model.getContainer().dispatch(model.getTarget(), XFormsEventNames.REFRESH, null);

      if (LOGGER.isDebugEnabled())
      {
         LOGGER.debug("swapped model data, instance data after manipulation:\n " + 
                      XMLUtil.toString(instance.getInstanceDocument()));
      }
   }

   private static void rewriteInlineURIs(final Document schemaDocument,
                                         final String cwdAvmPath)
   {
      final NodeList includes = 
         schemaDocument.getElementsByTagNameNS(NamespaceConstants.XMLSCHEMA_NS, "include");
      LOGGER.debug("rewriting " + includes.getLength() + " includes");
      for (int i = 0; i < includes.getLength(); i++)
      {
         final Element includeEl = (Element)includes.item(i);
         if (includeEl.hasAttribute("schemaLocation"))
         {
            String uri = includeEl.getAttribute("schemaLocation");
            final String baseURI = (uri.charAt(0) == '/'
                                    ? AVMConstants.buildStoreUrl(cwdAvmPath)
                                    : AVMConstants.buildAssetUrl(cwdAvmPath));

            LOGGER.debug("rewriting " + uri + " as " + (baseURI + uri));
            includeEl.setAttribute("schemaLocation", baseURI + uri);
         }
      }
   }

   private Node getEventLog()
   {
      final Document result = XMLUtil.newDocument();
      final Element eventsElement = result.createElement("events");
      result.appendChild(eventsElement);
      for (XMLEvent xfe : this.xformsSession.eventLog)
      {
         final String type = xfe.getType();
         if (LOGGER.isDebugEnabled())
         {
            LOGGER.debug("adding event " + type + " to the event log");
         }

         final Element target = (Element)xfe.getTarget();

         final Element eventElement = result.createElement(type);
         eventsElement.appendChild(eventElement);
         eventElement.setAttribute("targetId", target.getAttributeNS(null, "id"));
         eventElement.setAttribute("targetName", target.getLocalName());

         final Collection properties = xfe.getPropertyNames();
         if (properties != null)
         {
            for (Object name : properties)
            {
               final Object value = xfe.getContextInfo((String)name);
               if (LOGGER.isDebugEnabled())
               {
                  LOGGER.debug("adding property {" + name + 
                               ":" + value + "} to event " + type);
               }

               final Element propertyElement = result.createElement("property");
               eventElement.appendChild(propertyElement);
               propertyElement.setAttribute("name", name.toString());
               propertyElement.setAttribute("value", 
                                            value != null ? value.toString() : null);
            }
         }

         if (LOGGER.isDebugEnabled() && XFormsEventNames.SUBMIT_ERROR.equals(type))
         {
            // debug for figuring out which elements aren't valid for submit
            LOGGER.debug("performing full revalidate");
            try
            {
               final Model model = this.xformsSession.chibaBean.getContainer().getDefaultModel();
               final Instance instance = model.getDefaultInstance();
               model.getValidator().validate(instance, "/", new DefaultValidatorMode());
               final Iterator<ModelItem> it = instance.iterateModelItems("/");
               while (it.hasNext())
               {
                  final ModelItem modelItem = it.next();
                  if (!modelItem.isValid())
                  {
                     LOGGER.debug("model node " + modelItem.getNode() + " is invalid");
                  }
                  if (modelItem.isRequired() && modelItem.getValue().length() == 0)
                  {
                     LOGGER.debug("model node " + modelItem.getNode() + " is empty and required");
                  }
               }
            }
            catch (final XFormsException xfe2)
            {
               LOGGER.debug("error performing revaliation", xfe2);
            }
         }
      }
      this.xformsSession.eventLog.clear();

      if (LOGGER.isDebugEnabled())
      {
         LOGGER.debug("generated event log:\n" + XMLUtil.toString(result));
      }
      return result;
   }

   /**
    * stores cookies that may exist in request and passes them on to processor for usage in
    * HTTPConnectors. Instance loading and submission then uses these cookies. Important for
    * applications using auth.
    */
   @SuppressWarnings("unchecked")
   private static void storeCookies(final javax.servlet.http.Cookie[] cookiesIn,
                                    final ChibaBean chibaBean){
      if (cookiesIn != null) {
         org.apache.commons.httpclient.Cookie[] commonsCookies = 
            new org.apache.commons.httpclient.Cookie[cookiesIn.length];
         for (int i = 0; i < cookiesIn.length; i += 1) {
            commonsCookies[i] =
               new org.apache.commons.httpclient.Cookie(cookiesIn[i].getDomain(),
                                                        cookiesIn[i].getName(),
                                                        cookiesIn[i].getValue(),
                                                        cookiesIn[i].getPath(),
                                                        cookiesIn[i].getMaxAge(),
                                                        cookiesIn[i].getSecure());
         }
         chibaBean.getContext().put(AbstractHTTPConnector.REQUEST_COOKIE,
                                    commonsCookies);
      }
   }
   
   private Document getXFormsDocument()
      throws FormBuilderException
   {
      final String cwdAVMPath = this.getCurrentAVMPath();

      if (LOGGER.isDebugEnabled())
      {
         LOGGER.debug("building xform for schema " + this.xformsSession.form.getName() +
                      " root element " + this.xformsSession.form.getSchemaRootElementName() +
                      " avm cwd " + cwdAVMPath);
      }

      final Locale locale = 
         Application.getLanguage(FacesContext.getCurrentInstance());
      final ResourceBundle resourceBundle = 
         this.schema2XFormsProperties.getResourceBundle(this.xformsSession.form, 
                                                        locale);
      try
      {
         final Document schemaDocument = this.xformsSession.form.getSchema();
         XFormsBean.rewriteInlineURIs(schemaDocument, cwdAVMPath);
         final String rootElementName = this.xformsSession.form.getSchemaRootElementName();
         final Document result = 
            this.xformsSession.schemaFormBuilder.buildXForm(this.xformsSession.formInstanceData, 
                                                            schemaDocument,
                                                            rootElementName,
                                                            resourceBundle);
         if (LOGGER.isDebugEnabled())
         {
            LOGGER.debug("generated xform: " + XMLUtil.toString(result));
         }
         return result;
      }
      catch (IOException ioe)
      {
         throw new FormBuilderException(ioe);
      }
      catch (SAXException saxe)
      {
         throw new FormBuilderException(saxe);
      }
   }

   private String getCurrentAVMPath()
   {
      AVMNode node = this.avmBrowseBean.getAvmActionNode();
      if (node == null)
      {
         return this.avmBrowseBean.getCurrentPath();
      }

      final String result = node.getPath();
      return node.isDirectory() ? result : AVMNodeConverter.SplitBase(result)[0];
   }
}
