package autoPost.repositories;

import org.springframework.data.repository.CrudRepository;

import autoPost.entities.User;

public interface UserRepository extends CrudRepository<User, Integer> {
	
	int findIdByFbId(String fbId);
	

	
	

}
