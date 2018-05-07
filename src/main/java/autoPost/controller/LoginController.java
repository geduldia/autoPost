package autoPost.controller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.stereotype.Controller;

@Controller
public class LoginController extends ConnectController {
	
	private FacebookAccount facebookAccount; 

	@Inject
	public LoginController(ConnectionFactoryLocator connectionFactoryLocator,
			ConnectionRepository connectionRepository, FacebookAccount facebookAccount) {
		super(connectionFactoryLocator, connectionRepository);
		this.facebookAccount = facebookAccount;
	}
	
	/**
	 * PostConstruct method, used to add an interceptor to handle facebook auth.
	 */
	@PostConstruct
	public void twitterAccountInterceptor() {
		this.addInterceptor(facebookAccount);
	}

	@Override
	protected String connectView(String providerId) {
		return "redirect:/account";
	}

	
	@Override
	protected String connectedView(String providerId) {
		return "redirect:/account";
	}
}
