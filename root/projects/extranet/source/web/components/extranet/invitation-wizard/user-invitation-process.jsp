<%@ page import="org.alfresco.connector.*" %>
<%@ page import="org.alfresco.extranet.*" %>
<%@ page import="org.alfresco.extranet.database.*" %>
<%@ page import="org.alfresco.extranet.ldap.*" %>
<%@ page import="org.alfresco.extranet.webhelpdesk.*" %>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.context.support.WebApplicationContextUtils" %>
<%@ page buffer="0kb" contentType="text/html;charset=UTF-8" autoFlush="true"%>
<%@ page isELIgnored="false" %>
<%@ taglib uri="/WEB-INF/tlds/alf.tld" prefix="alf" %>
<%
	// get services
	InvitationService invitationService = ExtranetHelper.getInvitationService(request);
	UserService userService = ExtranetHelper.getUserService(request);
	SyncService syncService = ExtranetHelper.getSyncService(request);
	
	// get the invitation hash
	String hash = (String) request.getParameter("hash");
	
	// determine the user who has been invited
	DatabaseInvitedUser invitedUser = invitationService.getInvitedUserFromHash(hash);
		
	// properties
	String invitedUserId = request.getParameter("invitedUserId");
	String userId = request.getParameter("userId");
	String password = request.getParameter("password");
	String firstName = request.getParameter("firstName");
	String lastName = request.getParameter("lastName");
	String email = request.getParameter("email");
	String whdUserId = request.getParameter("whdUserId");
	String alfrescoUserId = request.getParameter("alfrescoUserId");
	
	// command processing
	if(userId == null)
	{
	}
	else
	{
		// check to see if this user id is available
		boolean available = syncService.isUserIdAvailable(userId);
		if(available)
		{
			// process the invited user
			invitationService.processInvitedUser(invitedUserId, userId, password);
			
			// load the user
			DatabaseUser dbUser = (DatabaseUser) userService.getUser(userId);
			
			// populate user
			dbUser.setFirstName(firstName);
			dbUser.setLastName(lastName);
			dbUser.setEmail(email);
			
			// save the user
			// TODO: This needs to push to all systems
			userService.update(dbUser);
		}
	}	
%>
<html>
   <head>
   	<title>Welcome to Alfresco Network, <%=invitedUser.getFirstName()%> <%=invitedUser.getLastName()%>!</title>
   </head>
   <body>
      <h2>Welcome to Alfresco Network!</h2>
      <p>
      	  Congratulations!  Your account '<b><%=userId%></b>' has been created.
      	  <br/>
      	  <br/>
      	  <a href="/extranet">Proceed to Alfresco Network</a>
      	  <br/>
      </p>      	
   </body>
</html>
