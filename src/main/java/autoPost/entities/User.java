package autoPost.entities;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity(name = "users")
public class User {
	

	@Id
	@GeneratedValue
	private int id;

	@Column(nullable = false)
	private String fbId;

	@Column(nullable = false)
	private String oauthToken;

	@Column(nullable = false)
	private String oauthTokenSecret;

	@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
	private List<PostGroup> groups;

	public User() {
		
	}

	public User(String fbId, String token, String secret) {
		this.fbId = fbId;
		this.oauthToken = token;
		this.oauthTokenSecret = secret;
	}

//	public List<PostGroup> getGroups() {
//		return groups;
//	}
//
//	public void addGroup(PostGroup group) {
//		if(groups == null){
//			groups = new ArrayList<PostGroup>();
//		}
//		groups.add(group);
//	}

	public int getId() {
		return id;
	}

	public String getFbId() {
		return fbId;
	}

	public String getOauthToken() {
		return oauthToken;
	}

	public String getOauthTokenSecret() {
		return oauthTokenSecret;
	}

	
}