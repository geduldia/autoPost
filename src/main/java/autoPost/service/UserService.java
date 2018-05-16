package autoPost.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import autoPost.entities.User;
import autoPost.repositories.UserRepository;

@Service
public class UserService {
	
	@Autowired
	private UserRepository repo;
	
	

	public int checkForUser(String fbId){
		return repo.findIdByFbId(fbId);
	}

	

	public int insertUser(String faceBookID, String token, String tokenSecret) {
		User user = new User(faceBookID, token, tokenSecret);
		User inserted = repo.save(user);
		return inserted.getId();
	}



	public User getUser(int userId) {
		return repo.findOne(userId);
	}



	public long getNumberOfUsers() {
		return repo.count();
	}
}
