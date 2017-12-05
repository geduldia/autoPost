package autoPost.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class HomeControllers {
	
	@RequestMapping(value="/")
	public String index(){
		return "redirect:/home";
	}
	
	@RequestMapping(value="/home")
	public ModelAndView home(){
		ModelAndView mv = new ModelAndView("home");
		return mv;
	}
}
