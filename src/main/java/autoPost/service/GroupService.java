package autoPost.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import autoPost.entities.PostGroup;
import autoPost.repositories.GroupRepository;

@Service
public class GroupService {

	@Autowired
	private GroupRepository repo;
	
	
	public List<PostGroup> getGroupsForUser(int userID){
		return repo.findByUserId(userID);
	}
	
	public PostGroup getGroup(int groupId, int userId){
		return repo.findByIdAndUserId(groupId, userId);
	}

	
	public PostGroup saveGroup(PostGroup group){
		return repo.save(group);
	}

	public void deleteGroup(int groupId, int userId) {
		System.out.println("##################################################### delete Group");
		repo.delete(getGroup(groupId, userId));
	}

	public boolean getStatus(int groupId, int userId){
		return repo.findByIdAndUserId(groupId, userId).isEnabled();
	}
	
	public void toggleStatus(int groupId, int userId){
		PostGroup group = getGroup(groupId, userId);
		setStatus(groupId, userId, !group.isEnabled());
	}

	public void setStatus(int groupId, int userId, boolean b) {
		PostGroup group = getGroup(groupId, userId);
		group.setEnabled(b);
		repo.save(group);
	}

	public void deleteAll(int userId) {
		List<PostGroup> groups = repo.findByUserId(userId);
		repo.delete(groups);
	}
	
}
