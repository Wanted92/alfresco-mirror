/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
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
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * FolderRules template.
 * 
 * @namespace Alfresco
 * @class Alfresco.FolderRules
 */
(function()
{

   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom;

   /**
    * FolderRules constructor.
    * 
    * @return {Alfresco.FolderRules} The new FolderRules instance
    * @constructor
    */
   Alfresco.FolderRules = function FolderRules_constructor()
   {
      Alfresco.FolderRules.superclass.constructor.call(this, "Alfresco.FolderRules", null, ["button"]);

      /* Decoupled event listeners */
      YAHOO.Bubbling.on("folderRulesDetailsAvailable", this.onFolderRulesDetailsAvailable, this);
      YAHOO.Bubbling.on("folderRulesDetailsChanged", this.onFolderRulesDetailsChanged, this);

      return this;
   };
   
   YAHOO.extend(Alfresco.FolderRules, Alfresco.component.Base,
   {
      /**
       * Object container for initialization options
       *
       * @property options
       * @type object
       */
      options:
      {
         /**
          * nodeRef of folder being viewed
          * 
          * @property nodeRef
          * @type string
          */
         nodeRef: null,
         
         /**
          * Current siteId.
          * 
          * @property siteId
          * @type string
          */
         siteId: "",

         /**
          * Current folder name.
          *
          * @property folderName
          * @type string
          */
         folderName: "",

         /**
          * Path to current folder.
          *
          * @property pathToFolder
          * @type string
          */
         pathToFolder: "",

         /**
          * Rules linked, inherited or directly related to the folder.
          *
          * @property rules
          * @type Array
          */
         rules: null,

         /**
          * Folder who's rules are linked to the folder specified by nodeRef.
          *
          * @property linkedFolder
          * @type {object}
          */
         linkedFolder: null
      },

      /**
       * Fired by YUILoaderHelper when required component script files have
       * been loaded into the browser.
       *
       * @override
       * @method onComponentsLoaded
       */
      onComponentsLoaded: function FolderRules_onComponentsLoaded()
      {
         YAHOO.util.Event.onDOMReady(this.onReady, this, true);
      },
      
      /**
       * Fired by YUI when parent element is available for scripting.
       * Template initialisation, including instantiation of YUI widgets and event listener binding.
       *
       * @method onReady
       */
      onReady: function FolderRules_onReady()
      {
         // Save references to Dom elements
         this.widgets.inheritedRulesList = Dom.get("inherited-rules-container");

         // Fire event to inform any listening components that the data is ready
         YAHOO.Bubbling.fire("folderDetailsAvailable",
         {
            folderDetails:
            {
               nodeRef: this.options.nodeRef,
               location:
               {
                  path: this.options.pathToFolder
               },
               fileName: this.options.folderName,
               type: "folder"
            }
         });

         // Fire event to inform any listening components that the data is ready
         this._fireFolderRulesDetailsAvailable();

         // Display inherited rules
         this._displayInheritedRules();
      },

      /**
       * Event handler called when the "folderRulesDetailsAvailable" event is received
       *
       * @method onFolderRulesDetailsAvailable
       * @param layer
       * @param args
       */
      onFolderRulesDetailsAvailable: function RulesHeader_onFolderRulesDetailsAvailable(layer, args)
      {
         var folderRulesData = args[1].folderRulesDetails;

         if((!this.options.linkedFolder && folderRulesData.linkedFolder) ||
            (this.options.linkedFolder && !folderRulesData.linkedFolder) ||
            (this.options.linkedFolder && folderRulesData.linkedFolder && this.options.linkedFolder.nodeRef != folderRulesData.linkedFolder.nodeRef))
         {
            // Refresh page since new components needs to be rendered
            document.location.refresh();
         }

         if(this.options.rules && folderRulesData.rules && this.options.rules.length != folderRulesData.rules.length)
         {
            // Remember rule information
            this.options.linkedFolder = folderRulesData.linkedFolder;
            this.options.rules = folderRulesData.rules;
            this._displayInheritedRules();
         }
      },


      /**
       * Checks if any rules are inherited and then hides or show them
       *
       * @method _displayInheritedRules
       * @param layer
       * @param args
       */
      _displayInheritedRules: function RulesHeader__displayInheritedRules(layer, args)
      {
         // Check if there are inherited rules
         var rules = this.options.rules;
         if (rules && rules.length > 0)
         {
            // Give the inherit button the correct state/icon depending
            for (var i = 0, l = rules.length; i < l; i++)
            {
               if (rules[i].inheritedFolder)
               {
                  // Found an inherited rule make sure the component is displayed
                  Dom.removeClass(this.widgets.inheritedRulesList, "hidden");
                  return;
               }
            }
         }

         // Found no inherited rules make sure the component is hidden
         Dom.addClass(this.widgets.inheritedRulesList, "hidden");
      },

      /**
       * Event called when another component has changed the details of a rule on a folder
       * which requires ui to reload itself.
       *
       * @method onFolderRulesDetailsChanged
       * @param layer
       * @param args
       * @private
       */
      onFolderRulesDetailsChanged: function RulesHeader_onFolderRulesDetailsChanged(layer, args)
      {
         // Load rule information form server
         var nodeRefAsUrl = this.options.nodeRef.replace("://", "/"),
            prevNoOfRules = this.options.rules ? this.options.rules.length : 0;
         Alfresco.util.Ajax.jsonGet(
         {
            url: Alfresco.constants.PROXY_URI_RELATIVE + "api/node/" + nodeRefAsUrl + "/ruleset/rules",
            successCallback:
            {
               fn: function(response, p_prevNoOfRules)
               {
                  if (response.json)
                  {
                     this.options.rules = response.json.data;
                     if ((prevNoOfRules == 0 && p_prevNoOfRules != 0) ||
                         (prevNoOfRules != 0 && p_prevNoOfRules == 0))
                     {
                        // Reload page so appropriate components will be displayed in stead of the current ones
                        window.location.reload();
                     }
                     else
                     {
                        this._fireFolderRulesDetailsAvailable();   
                     }
                  }
               },
               obj: prevNoOfRules,
               scope: this
            },
            failureMessage:this.msg("message.getRuleFailure", this.name)
         });
      },

      /**
       * @method _fireFolderRulesDetailsAvailable
       * @private
       */
      _fireFolderRulesDetailsAvailable: function RulesHeader__fireFolderRulesDetailsAvailable()
      {
         YAHOO.Bubbling.fire("folderRulesDetailsAvailable",
         {
            folderRulesDetails:
            {
               rules: this.options.rules,
               linkedFolder: this.options.linkedFolder
            }
         });
      }

   });
})();