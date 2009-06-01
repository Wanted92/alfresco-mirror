<#macro dateFormat date>${date?string("EEE d MMM yyyy HH:mm:ss")}</#macro>
<#macro formatContent content date type index>
      <#if (index == 0)>
      <li class="first">
      <#else>
      <li>
      </#if>
      <a href="${url.context}/page/site/${content.site.shortName}/${content.browseUrl}" class="thmb"><img src="${url.context}/components/images/generic-file-32.png"/></a>
      <p><a href="${url.context}/page/site/${content.site.shortName}/${content.browseUrl}" class="theme-color-1">${content.displayName?html!""}</a>
      ${content.description}
      <span>${type} <@dateFormat date /></span></p>
   </li>
</#macro>

<#assign el=args.htmlid>
<div id="${el}-body" class="profile">
   <div id="${el}-readview">
      <div class="viewcolumn">
         <div class="header-bar">${msg("label.recentlyAdded")}</div>
         <#if (numAddedContent >0)>
         <ul class="content">
         <#list addedContent as content>
            <@formatContent content content.createdOn msg("label.createdOn") content_index/>
         </#list>
         </ul>
         <#else>
         <p>${msg("label.noAddedContent")}</p>
         </#if>
         <div class="header-bar">${msg("label.recentlyModified")}</div>
         <#if (numModifiedContent >0)>
         <ul class="content">
         <#list modifiedContent as content>
            <@formatContent content content.modifiedOn msg("label.modifiedOn") content_index/>
         </#list>
         </ul>
         <#else>
         <p>${msg("label.noModifiedContent")}</p>
         </#if>                  
      </div>
   </div>
</div>