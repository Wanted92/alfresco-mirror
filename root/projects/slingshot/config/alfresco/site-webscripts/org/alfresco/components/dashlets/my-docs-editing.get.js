function getUserContent(contentType)
{
   var uri = "";
   switch (contentType)
   {
      // docs in all site doclibs
      case "documents":
         uri = "/slingshot/doclib/doclist/documents/node/alfresco/sites/home?filter=editingMe&max=3";
         break;
      
      // wiki pages, blog and forum posts
      case "content":
         uri = "/slingshot/dashlets/my-contents";
         break;
   }
   
   var json = remote.call(uri);   
   if (json.status == 200)
   {
      // Create the model
      var content = eval('(' + json + ')');
      if (contentType == "content")
      {
         // generate browser URLs for non document items
         // document item URLs are generated via a freemarker macro
         for (var t in content)
         {
            for (var i=0,j=content[t].items.length; i<j; i++)
            {
               var item = content[t].items[i];
               switch (item.type)
               {
                  case "blogpost":
                     item.browseUrl = "blog-postview?container=" + item.container + "&postId=" + item.name;
                     break;
                  case "wikipage":
                     item.browseUrl = "wiki-page?title=" + item.name;
                     break;
                  case "forumpost":
                     item.browseUrl = "discussions-topicview?container=" + item.container + "&topicId=" + item.name;
                     break;
               }
            }
         }
      }
      return content;
   }
   else
   {
      model[contentType] = {};
      model[contentType].error =
      {
         message: "label.error"
      };
   }

   return {
      items : []
   };
}

var contentTypes = ['documents','content'];
model.dataTypes = "";

for (var i = 0,len=contentTypes.length; i<len; i++)
{
   var contentType = contentTypes[i];
   model[contentType] = getUserContent(contentType);
}