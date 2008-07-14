<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/requestutils.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/generic-paged-results.lib.js">
<import resource="classpath:alfresco/templates/webscripts/org/alfresco/repository/discussions/topicpost.lib.js">

/**
 * Fetches all posts found in the forum.
 */
function getTopicPostList(node, tag, index, count)
{
   // query information
   var luceneQuery = " +TYPE:\"{http://www.alfresco.org/model/forum/1.0}topic\"" +
                       " +PATH:\"" + node.qnamePath + "/*\" ";
   var sortAttribute = "@{http://www.alfresco.org/model/content/1.0}created";
     
   // is a tag selected?
   if (tag != null)
   {
      luceneQuery += " +PATH:\"/cm:taggable/cm:" + tag /*ISO9075.encode(tag)*/ + "/member\" ";
   }
   
   // get the data
   return getPagedResultsDataByLuceneQuery(node, luceneQuery, sortAttribute, false, index, count, getTopicPostData);
}

function main()
{
   // get requested node
   var node = getRequestNode();
   if (status.getCode() != status.STATUS_OK)
   {
      return;
   }

   // process additional parameters
   var index = args["startIndex"] != undefined ? parseInt(args["startIndex"]) : 0;
   var count = args["pageSize"] != undefined ? parseInt(args["pageSize"]) : 10;

   // selected tag
   var tag = args["tag"] != undefined && args["tag"].length > 0 ? args["tag"] : null;

   model.data = getTopicPostList(node, tag, index, count);
    
   model.contentFormat = (args["contentFormat"] != undefined) ? args["contentFormat"] : "full";
}

main();
