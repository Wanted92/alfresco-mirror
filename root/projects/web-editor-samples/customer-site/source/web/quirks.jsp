<%--
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ taglib uri="/WEB-INF/awe.tld" prefix="awe" %>
<%@ taglib uri="/WEB-INF/customer.tld" prefix="customer" %>

<%@ page buffer="16kb" contentType="text/html;charset=UTF-8" %>
<%@ page isELIgnored="false" %>

<%@ include file="includes/noderefs.jsp" %>

<html xmlns="http://www.w3.org/1999/xhtml">

   <head>
      <meta http-equiv="content-type" content="text/html;charset=utf-8" />
      <title>Alfresco Web Editor Demo</title>
      <awe:startTemplate />
      <link rel="stylesheet" type="text/css" href="customer.css" />
   </head>
   
   <%@ include file="includes/body.jsp" %>

</html>