package autoPost;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import autoPost.service.UserService;


@SpringBootApplication
public class autoPostApp {
	
	@Autowired
	UserService userService;
	
	public static void main(String[] args) {
		SpringApplication.run(autoPostApp.class, args);
	}
	
	@PostConstruct
	public void createDefaultUser(){
		long users = userService.getNumberOfUsers();
		if(users == 0)
			userService.insertUser("fb_id", "token", "token_secret");
	}

}
