<script type="text/javascript">//<![CDATA[
   new Alfresco.Activities("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!""}",
      mode: "site"
   }).setMessages(
      ${messages}
   );
   new Alfresco.widget.DashletResizer("${args.htmlid}", "${instance.object.id}");
//]]></script>

<div class="dashlet activities">
   <div class="title">${msg("header")}</div>
   <div class="feed"><a id="${args.htmlid}-feedLink" href="#" target="_blank">&nbsp;</a></div>
   <div class="toolbar flat-button">
      <input id="${args.htmlid}-today" type="checkbox" name="today" value="${msg("filter.today")}" checked="checked" />
      <input id="${args.htmlid}-range" type="button" name="range" value="${msg("filter.7days")}" />
      <select id="${args.htmlid}-range-menu">
         <option value="7">${msg("filter.7days")}</option>
         <option value="14">${msg("filter.14days")}</option>                
         <option value="28">${msg("filter.28days")}</option>
      </select>
   </div>
   <div id="${args.htmlid}-activityList" class="body scrollableList" <#if args.height??>style="height: ${args.height}px;"</#if>>
   </div>
</div>