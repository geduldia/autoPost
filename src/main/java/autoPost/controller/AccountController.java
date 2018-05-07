package autoPost.controller;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AccountController {
	
	@Autowired
	private HttpSession session;
	
	@RequestMapping(value="/account")
	public ModelAndView account(){
		ModelAndView mv = new ModelAndView("account");
		return mv;
	}
}
