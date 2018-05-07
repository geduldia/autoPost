package autoPost.postCreation;

import autoPost.entities.Post;
import autoPost.entities.PostGroup;

public class ImportedPost extends Post{
	
	private boolean trimmed;
	
	public ImportedPost(String date, String content, String img, PostGroup group){
		super(content, date, group);
		setImg(img);
	}

	public boolean isTrimmed() {
		return trimmed;
	}

	public void setTrimmed(boolean trimmed) {
		this.trimmed = trimmed;
	}
}
