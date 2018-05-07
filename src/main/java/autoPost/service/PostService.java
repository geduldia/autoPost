package autoPost.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import autoPost.entities.Post;
import autoPost.repositories.PostRepository;

@Service
public class PostService {
	
	@Autowired
	private PostRepository repo;
	
	public List<Post> getPostsByUser(int userID){
		return repo.findByUserIdOrderByDateAsc(userID);
	}
	
	public List<Post> getGroupPosts(int groupId, int userId){
		return repo.findByGroupIdAndUserId(groupId, userId);
	}

	public void deleteGroupPosts(int groupId, int userId){
		repo.delete(getGroupPosts(groupId, userId));
	}


	public void savePost(Post post) {
		repo.save(post);	
	}

	public void deletePost(int postId, int userId) {
		repo.delete(getPost(postId, userId));
	}

	public Post getPost(int postId, int userId) {
		return repo.findByIdAndUserId(postId, userId);
	}
	


}
