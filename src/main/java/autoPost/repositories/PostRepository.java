package autoPost.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import autoPost.entities.Post;
import autoPost.entities.User;

public interface PostRepository extends CrudRepository<Post, Integer> {

	List<Post> findByUserId(int userID);
	
	List<Post> findByGroupIdAndUserId(int groupID, int userId);
	
	List<Post> deleteByGroupIdAndUserId(int groupID, int userId);

	void deleteAllByUserIdAndGroupId(int userId, int groupId);

	Post findByIdAndUserId(int postId, int userId);
}
