<#-- Portlet container detection -->
<#assign PORTLET=(context.attributes.portletHost!false)>

<#--
   DocumentLibrary Template "documentLibraryJS" macro.
   Client-side JavaScript to parse initial page parameters.
-->
<#macro documentLibraryJS>
<script type="text/javascript">//<![CDATA[
(function()
{
   var $combine = Alfresco.util.combinePaths;
   
   // If no location.hash exists, convert certain params in location.search to location.hash and replace the page
   var loc = window.location;
   if (loc.hash === "" && loc.search !== "")
   {
      var qs, q, url = loc.protocol + "//" + loc.host + loc.pathname, hash = "";
      
   <#if PORTLET>
      qs = {};
      <#if url.args.path??>qs.path = "${(url.args.path!"")?js_string}";</#if>
      <#if url.args.page??>qs.page = "${(url.args.page!"")?js_string}";</#if>
      <#if url.args.filter??>qs.filter = "${(url.args.filter!"")?js_string}";</#if>
   <#else>
      qs = Alfresco.util.getQueryStringParameters();
   </#if>
      
      var hashParams =
      {
         "path": true,
         "page": true,
         "filter": true
      },
         filterDataParam = "filterData";
      
      for (q in qs)
      {
         if (qs.hasOwnProperty(q) && q in hashParams)
         {
            if (q === "path")
            {
               hash += "&" + "filter=" + escape("path|" + qs[q]);
            }
            else
            {
               hash += "&" + escape(q) + "=" + escape(qs[q]);
               if (q === "filter")
               {
                  // Check for filterData in QueryString for the "filter" case
                  if (qs.hasOwnProperty(filterDataParam))
                  {
                     hash += escape("|" + qs[filterDataParam]);
                     delete qs[filterDataParam];
                  }
               }
            }
            delete qs[q];
         }
      }
      
      if (hash.length > 0)
      {
      <#if PORTLET>
         top.location.hash = hash.substring(1);
      <#else>
         url += Alfresco.util.toQueryString(qs) + "#" + hash.substring(1);
         window.location.replace(url);
      </#if>
      }
   }
})();
//]]></script>
</#macro>