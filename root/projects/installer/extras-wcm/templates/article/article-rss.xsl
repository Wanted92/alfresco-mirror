<?xml version="1.0" encoding="UTF-8"?>
<!--
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
-->
<xsl:stylesheet version="1.0" xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:article="http://www.alfresco.org/alfresco/article"
	xmlns:java="http://xml.apache.org/xslt/java"
	xmlns:fn="http://www.w3.org/2005/02/xpath-functions" exclude-result-prefixes="xhtml fn java article xsl">
	<xsl:output method="xml" encoding="UTF-8" indent="yes"/>

	<xsl:preserve-space elements="*"/>

	<xsl:template match="/">
		<item>
	    		<title><xsl:value-of select="/article:article/article:title"  disable-output-escaping="yes"/></title>
	    		<link>/views/pages/details.jsp?content=/content/article/<xsl:value-of select="fn:replaceAll(string($alf:form_instance_data_file_name), '.xml', '.html')"/>&amp;title=<xsl:value-of select="java:java.net.URLEncoder.encode(/article:article/article:title,'UTF-8')"  disable-output-escaping="yes"/></link>
	    		<description><xsl:value-of select="normalize-space(/article:article/article:teaser)"/></description>
	    		<pubDate>2000-01-01T12:00:00.000-00:00</pubDate>
	  	</item>
	</xsl:template>
</xsl:stylesheet>
