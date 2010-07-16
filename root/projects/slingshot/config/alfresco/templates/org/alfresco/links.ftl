<#include "include/alfresco-template.ftl" />
<@templateHeader>
   <@script type="text/javascript" src="${url.context}/js/alfresco-resizer.js"></@script>
   <script type="text/javascript">//<![CDATA[
      new Alfresco.widget.Resizer("Links");
   //]]></script>
   <@script type="text/javascript" src="${page.url.context}/components/links/linksdiscuss-common.js"></@script>
</@>

<@templateBody>
   <div id="alf-hd">
      <@region id="header" scope="global" protected=true />
      <@region id="title" scope="template" protected=true />
      <@region id="navigation" scope="template" protected=true />
   </div>

   <div id="bd">
      <div class="yui-t1" id="alfresco-links">
         <div id="yui-main">
            <div class="yui-b" id="alf-content">
               <@region id="links" scope="template" />
            </div>
         </div>
         <div class="yui-b" id="alf-filters">
            <@region id="filters" scope="template" protected=true />
            <@region id="tags" scope="template" protected=true />
         </div>
      </div>
   </div>   
</@>

<@templateFooter>
   <div id="alf-ft">
      <@region id="footer" scope="global" protected=true />
   </div>
</@>