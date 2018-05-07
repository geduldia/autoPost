package autoPost.controller;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectInterceptor;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.User;
import org.springframework.social.facebook.api.UserOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.WebRequest;

import autoPost.service.UserService;

@Component
public class FacebookAccount implements ConnectInterceptor<Facebook>{
	
	private ConnectionRepository repository;
	private HttpSession session;
	private UserService userService;
	
	@Inject
	public FacebookAccount(ConnectionRepository connectionRepository, HttpSession session, UserService userService) {
		this.repository = connectionRepository;
		this.session = session;
		this.userService = userService;
	}
	


	public void postConnect(Connection<Facebook> facebookConnection, WebRequest arg1) {
		Facebook facebook = facebookConnection.getApi();
		UserOperations userOperations = facebook.userOperations();
		User user = userOperations.getUserProfile();
		String fbId = user.getId();
		int userId = userService.checkForUser(fbId);
		ConnectionData fbConnectionData = facebookConnection.createData(); 
		if (userId == -1) {
			
			String token = fbConnectionData.getAccessToken();
			String tokenSecret = fbConnectionData.getSecret();
			userId = userService.insertUser(fbId, token, tokenSecret);
		}

		Map<String, String> account = new HashMap<String, String>();
		account.put("userId", Integer.toString(userId));
		account.put("fbId", fbId);
		account.put("firstName", user.getFirstName());
		account.put("lastName", user.getLastName());
		account.put("url", user.getWebsite());
		account.put("imageUrl", fbConnectionData.getImageUrl());
		session.setAttribute("account", account);
		repository.removeConnection(facebookConnection.getKey());
	}



	public void preConnect(ConnectionFactory<Facebook> arg0, MultiValueMap<String, String> arg1, WebRequest arg2) {
		// TODO Auto-generated method stub
		
	}

	
}
