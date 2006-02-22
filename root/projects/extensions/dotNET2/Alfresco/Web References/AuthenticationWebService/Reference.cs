﻿//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
//     Runtime Version:2.0.50727.42
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

// 
// This source code was auto-generated by Microsoft.VSDesigner, Version 2.0.50727.42.
// 
#pragma warning disable 1591

namespace Alfresco.AuthenticationWebService {
    using System.Diagnostics;
    using System.Web.Services;
    using System.ComponentModel;
    using System.Web.Services.Protocols;
    using System;
    using System.Xml.Serialization;
    
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Web.Services", "2.0.50727.42")]
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    [System.Web.Services.WebServiceBindingAttribute(Name="AuthenticationServiceSoapBinding", Namespace="http://www.alfresco.org/ws/service/authentication/1.0")]
    public partial class AuthenticationService : System.Web.Services.Protocols.SoapHttpClientProtocol {
        
        private System.Threading.SendOrPostCallback startSessionOperationCompleted;
        
        private System.Threading.SendOrPostCallback endSessionOperationCompleted;
        
        private bool useDefaultCredentialsSetExplicitly;
        
        /// <remarks/>
        public AuthenticationService() {
            this.Url = global::Alfresco.Properties.Settings.Default.Alfresco_AuthenticationWebService_AuthenticationService;
            if ((this.IsLocalFileSystemWebService(this.Url) == true)) {
                this.UseDefaultCredentials = true;
                this.useDefaultCredentialsSetExplicitly = false;
            }
            else {
                this.useDefaultCredentialsSetExplicitly = true;
            }
        }
        
        public new string Url {
            get {
                return base.Url;
            }
            set {
                if ((((this.IsLocalFileSystemWebService(base.Url) == true) 
                            && (this.useDefaultCredentialsSetExplicitly == false)) 
                            && (this.IsLocalFileSystemWebService(value) == false))) {
                    base.UseDefaultCredentials = false;
                }
                base.Url = value;
            }
        }
        
        public new bool UseDefaultCredentials {
            get {
                return base.UseDefaultCredentials;
            }
            set {
                base.UseDefaultCredentials = value;
                this.useDefaultCredentialsSetExplicitly = true;
            }
        }
        
        /// <remarks/>
        public event startSessionCompletedEventHandler startSessionCompleted;
        
        /// <remarks/>
        public event endSessionCompletedEventHandler endSessionCompleted;
        
        /// <remarks/>
        [System.Web.Services.Protocols.SoapDocumentMethodAttribute("http://www.alfresco.org/ws/service/authentication/1.0/startSession", RequestNamespace="http://www.alfresco.org/ws/service/authentication/1.0", ResponseNamespace="http://www.alfresco.org/ws/service/authentication/1.0", Use=System.Web.Services.Description.SoapBindingUse.Literal, ParameterStyle=System.Web.Services.Protocols.SoapParameterStyle.Wrapped)]
        [return: System.Xml.Serialization.XmlElementAttribute("startSessionReturn")]
        public AuthenticationResult startSession(string username, string password) {
            object[] results = this.Invoke("startSession", new object[] {
                        username,
                        password});
            return ((AuthenticationResult)(results[0]));
        }
        
        /// <remarks/>
        public void startSessionAsync(string username, string password) {
            this.startSessionAsync(username, password, null);
        }
        
        /// <remarks/>
        public void startSessionAsync(string username, string password, object userState) {
            if ((this.startSessionOperationCompleted == null)) {
                this.startSessionOperationCompleted = new System.Threading.SendOrPostCallback(this.OnstartSessionOperationCompleted);
            }
            this.InvokeAsync("startSession", new object[] {
                        username,
                        password}, this.startSessionOperationCompleted, userState);
        }
        
        private void OnstartSessionOperationCompleted(object arg) {
            if ((this.startSessionCompleted != null)) {
                System.Web.Services.Protocols.InvokeCompletedEventArgs invokeArgs = ((System.Web.Services.Protocols.InvokeCompletedEventArgs)(arg));
                this.startSessionCompleted(this, new startSessionCompletedEventArgs(invokeArgs.Results, invokeArgs.Error, invokeArgs.Cancelled, invokeArgs.UserState));
            }
        }
        
        /// <remarks/>
        [System.Web.Services.Protocols.SoapDocumentMethodAttribute("http://www.alfresco.org/ws/service/authentication/1.0/endSession", RequestNamespace="http://www.alfresco.org/ws/service/authentication/1.0", ResponseNamespace="http://www.alfresco.org/ws/service/authentication/1.0", Use=System.Web.Services.Description.SoapBindingUse.Literal, ParameterStyle=System.Web.Services.Protocols.SoapParameterStyle.Wrapped)]
        public void endSession(string ticket) {
            this.Invoke("endSession", new object[] {
                        ticket});
        }
        
        /// <remarks/>
        public void endSessionAsync(string ticket) {
            this.endSessionAsync(ticket, null);
        }
        
        /// <remarks/>
        public void endSessionAsync(string ticket, object userState) {
            if ((this.endSessionOperationCompleted == null)) {
                this.endSessionOperationCompleted = new System.Threading.SendOrPostCallback(this.OnendSessionOperationCompleted);
            }
            this.InvokeAsync("endSession", new object[] {
                        ticket}, this.endSessionOperationCompleted, userState);
        }
        
        private void OnendSessionOperationCompleted(object arg) {
            if ((this.endSessionCompleted != null)) {
                System.Web.Services.Protocols.InvokeCompletedEventArgs invokeArgs = ((System.Web.Services.Protocols.InvokeCompletedEventArgs)(arg));
                this.endSessionCompleted(this, new System.ComponentModel.AsyncCompletedEventArgs(invokeArgs.Error, invokeArgs.Cancelled, invokeArgs.UserState));
            }
        }
        
        /// <remarks/>
        public new void CancelAsync(object userState) {
            base.CancelAsync(userState);
        }
        
        private bool IsLocalFileSystemWebService(string url) {
            if (((url == null) 
                        || (url == string.Empty))) {
                return false;
            }
            System.Uri wsUri = new System.Uri(url);
            if (((wsUri.Port >= 1024) 
                        && (string.Compare(wsUri.Host, "localHost", System.StringComparison.OrdinalIgnoreCase) == 0))) {
                return true;
            }
            return false;
        }
    }
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "2.0.50727.42")]
    [System.SerializableAttribute()]
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    [System.Xml.Serialization.XmlTypeAttribute(Namespace="http://www.alfresco.org/ws/service/authentication/1.0")]
    public partial class AuthenticationResult {
        
        private string usernameField;
        
        private string ticketField;
        
        /// <remarks/>
        public string username {
            get {
                return this.usernameField;
            }
            set {
                this.usernameField = value;
            }
        }
        
        /// <remarks/>
        public string ticket {
            get {
                return this.ticketField;
            }
            set {
                this.ticketField = value;
            }
        }
    }
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Web.Services", "2.0.50727.42")]
    public delegate void startSessionCompletedEventHandler(object sender, startSessionCompletedEventArgs e);
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Web.Services", "2.0.50727.42")]
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    public partial class startSessionCompletedEventArgs : System.ComponentModel.AsyncCompletedEventArgs {
        
        private object[] results;
        
        internal startSessionCompletedEventArgs(object[] results, System.Exception exception, bool cancelled, object userState) : 
                base(exception, cancelled, userState) {
            this.results = results;
        }
        
        /// <remarks/>
        public AuthenticationResult Result {
            get {
                this.RaiseExceptionIfNecessary();
                return ((AuthenticationResult)(this.results[0]));
            }
        }
    }
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Web.Services", "2.0.50727.42")]
    public delegate void endSessionCompletedEventHandler(object sender, System.ComponentModel.AsyncCompletedEventArgs e);
}

#pragma warning restore 1591