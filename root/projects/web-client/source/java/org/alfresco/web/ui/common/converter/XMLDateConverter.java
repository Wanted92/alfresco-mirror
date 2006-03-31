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
package org.alfresco.web.ui.common.converter;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.DateTimeConverter;

import org.alfresco.util.ISO8601DateFormat;

/**
 * Converter class to convert an XML date representation into a Date
 * 
 * @author gavinc
 */
public class XMLDateConverter extends DateTimeConverter
{
   /**
    * <p>The standard converter id for this converter.</p>
    */
   public static final String CONVERTER_ID = "org.alfresco.faces.XMLDataConverter";

   /**
    * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
    */
   public Object getAsObject(FacesContext context, UIComponent component, String value)
   {
      return ISO8601DateFormat.parse(value);
   }

   /**
    * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.Object)
    */
   public String getAsString(FacesContext context, UIComponent component, Object value)
   {
      String str = null;
      
      if (value instanceof String)
      {
         Date date = ISO8601DateFormat.parse((String)value);
         str = super.getAsString(context, component, date);
      }
      else
      {
         str = super.getAsString(context, component, value);
      }
      
      return str;
   }

   /**
    * @see javax.faces.convert.DateTimeConverter#getTimeZone()
    */
   @Override
   public TimeZone getTimeZone()
   {
      // Note: this forces the display of the date to the server's timezone - it does not
      //       take into account any client specific timezone
      return TimeZone.getDefault();
   }

   /**
    * @see javax.faces.convert.DateTimeConverter#getLocale()
    */
   @Override
   public Locale getLocale()
   {
      // get the locale set in the client
      FacesContext context = FacesContext.getCurrentInstance();
      Locale locale = context.getViewRoot().getLocale();
      if (locale == null)
      {
         // else use server locale as the default
         locale = Locale.getDefault();
      }
      
      return locale;
   }
}
