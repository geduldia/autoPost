package autoPost.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import autoPost.entities.PostGroup;

public interface GroupRepository extends CrudRepository<PostGroup, Integer>{

	List<PostGroup> findByUserId(int userID);

	PostGroup findByIdAndUserId(int groupId, int userId);

	boolean findEnabledByUserIdAndId(int userId, int groupId);

	void deleteByUserId(int userId);
	
	

}
