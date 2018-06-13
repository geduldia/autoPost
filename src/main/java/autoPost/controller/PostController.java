package autoPost.controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import autoPost.entities.Post;
import autoPost.entities.PostGroup;
import autoPost.entities.User;
import autoPost.service.GroupService;
import autoPost.service.PostService;
import autoPost.service.UserService;

@Controller
@RequestMapping(value = "/posts")
public class PostController {

	@Inject
	private HttpSession session;

	@Autowired
	private PostService postService;

	@Autowired
	private GroupService groupService;

	@Autowired
	UserService userService;

	private int postsPerPage = 12;

	/**
	 * A HTTP GET request handler, responsible for serving /posts/view. Lists
	 * all posts of the current user read from the database.
	 * 
	 * @param page
	 *            Request param containing the page number (default is 1)
	 * @return View containing the posts overview
	 */
	@RequestMapping(value = "/view")
	public ModelAndView viewTweets(@RequestParam(name = "page", defaultValue = "1") int page) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String,String>)
						// session.getAttribute("account")).get("userId"));

		List<Post> posts = postService.getPostsByUser(userId);
		// reorder postsList: upcoming posts first - past posts second
		LocalDateTime now = LocalDateTime.now();
		List<Post> old = new ArrayList<Post>();
		List<Post> upcoming = new ArrayList<Post>();
		for (Post post : posts) {
			DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			LocalDateTime date = LocalDateTime.parse(post.getDate(), dtFormatter);
			if (date.isBefore(now)) {
				old.add(post);
			} else {
				upcoming.add(post);
			}
		}
		posts.removeAll(old);
		posts.addAll(old);
		ModelAndView mv = new ModelAndView("posts");
		if (posts.size() <= postsPerPage) {
			mv.addObject("posts", posts);
			return mv;
		}
		int pages = (int) StrictMath.ceil((double) posts.size() / postsPerPage);
		int offset = postsPerPage * (page - 1);
		int endset = (offset + postsPerPage > posts.size() ? (posts.size()) : (offset + postsPerPage));
		mv.addObject("posts", posts.subList(offset, endset));
		mv.addObject("pages", pages);
		mv.addObject("page", page);
		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving /posts/delete.
	 * Deletes the given posts (referenced by postIds) and redirects to the
	 * posts overview.
	 * 
	 * @param postIds
	 *            POST param containing the postIds of the posts to delete
	 * @return View containing the posts overview
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public ModelAndView deletePosts(@RequestParam("postId") List<Integer> postIds, HttpServletRequest request) {
		System.out.println("delete Posts");
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		for (Integer postId : postIds) {
			postService.deletePost(postId, userId);
		}
		String referer = request.getHeader("referer");
		return new ModelAndView("redirect:" + referer);
	}

	/**
	 * A HTTP GET request handler responsible for serving /posts/delete/{postId}
	 * Deletes the post referred by $postID and redirects to the posts overview
	 * 
	 * @param postId
	 * @return View containing the posts overview
	 */
	@RequestMapping(value = "/delete/{postId}", method = RequestMethod.GET)
	public ModelAndView deletePosts(@PathVariable int postId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		if (postService.getPost(postId, userId) == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Post with the ID # " + postId + " does not exist!");
			return mv;
		}
		postService.deletePost(postId, userId);
		return new ModelAndView("redirect:/posts/view");
	}

	/**
	 * A HTTP GET request handler responsible for serving /posts/view/{postId}
	 * This method is called to view a single post in detail. It reads all
	 * relevant information from the database and displays it as a view. If no
	 * post with the requested ID is found, an error is displayed.
	 * 
	 * @param postId
	 * @return A view containing the post details
	 */
	@RequestMapping(value = "/view/{postId}")
	public ModelAndView viewPost(@PathVariable int postId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		Post post = postService.getPost(postId, userId);
		if (post == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Post with the ID # " + postId + " does not exist!");
			return mv;
		}
		ModelAndView mv = new ModelAndView("post");
		mv.addObject("post", post);
		mv.addObject("mode", "view");
		return mv;
	}

	// edit a given post
	@RequestMapping(value = "/edit/{postId}")
	public ModelAndView editPost(@PathVariable int postId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		Post post = postService.getPost(postId, userId);
		if (post == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Post with the ID # " + postId + " does not exist!");
			return mv;
		}
		ModelAndView mv = new ModelAndView("post");
		mv.addObject("post", post);
		mv.addObject("mode", "edit");
		return mv;
	}

	// save an edited post
	@RequestMapping(value = "/save/{postId}", method = RequestMethod.POST)
	public ModelAndView savePost(@PathVariable int postId, @RequestParam String content, @RequestParam String date,
			@RequestParam String time, @RequestParam String img, @RequestParam(defaultValue = "0.0") float latitude,
			@RequestParam(defaultValue = "0.0") float longitude) {
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		Post post = postService.getPost(postId, userId);
		if (post == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Post with the ID # " + postId + " does not exist!");
			return mv;
		}
		post.setContent(content);
		if (!date.matches("^[0-9]{4}(-[0-9]{2}){2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The post date must match the pattern: YYYY-MM-DD");
			return mv;
		}
		if (time.matches("^[0-9]{2}:[0-9]{2}$")) {
			time = time + ":00";
		} else if (!time.matches("^[0-9]{2}:[0-9]{2}:[0-9]{2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet time must match the pattern: HH:MM:SS");
			return mv;
		}
		post.setDate(date + " " + time);
		if (!img.isEmpty()) {
			try {
				if (ImageIO.read(new URL(img)) == null) {
					ModelAndView mv = new ModelAndView("error");
					mv.addObject("error", "Sorry, but the selected image has an unsupported format.");
					return mv;
				}
			} catch (Exception e) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "The image URL must be a valid url.");
				return mv;
			}
		} else {
			img = null;
		}
		post.setImg(img);
		if (longitude < -180 || longitude > 180) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The longitude must be between -180 and 180");
		}
		if (latitude < -90 || latitude > 90) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The latitude must be between -90 and +90");
		}

		postService.savePost(post);
		int groupId = post.getGroup().getId();
		return new ModelAndView("redirect:/groups/view/" + groupId);
	}

	// add to a specific group
	@RequestMapping(value = "/add/{groupId}")
	public ModelAndView addPostToGroup(@PathVariable int groupId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		ModelAndView mv = new ModelAndView("post");
		PostGroup group = groupService.getGroup(groupId, userId);
		mv.addObject("group", group);
		mv.addObject("mode", "add");
		return mv;
	}

	// add a new post (to a new group or an existing group)
	@RequestMapping(value = "/add")
	public ModelAndView addPost() {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		ModelAndView mv = new ModelAndView("post");
		List<PostGroup> groups = groupService.getGroupsForUser(userId);
		mv.addObject("mode", "add");
		mv.addObject("groups", groups);
		return mv;
	}

	// save a new created Post (for a new created group or an existing group)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public ModelAndView saveNewPost(@RequestParam String group, @RequestParam String content, @RequestParam String date,
			@RequestParam String time, @RequestParam String img) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		User user = userService.getUser(userId);
		PostGroup mappedGroup;
		try {
			int groupId = Integer.parseInt(group);
			mappedGroup = groupService.getGroup(groupId, userId);
			if (mappedGroup == null) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "A Group with the ID # " + groupId + " does not exist!");
				return mv;
			}
		} catch (NumberFormatException e) {
			mappedGroup = new PostGroup(group, null, user);
		}
		if (!date.matches("^[0-9]{4}(-[0-9]{2}){2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The post date must match the pattern: YYYY-MM-DD");
			return mv;
		}
		if (time.matches("^[0-9]{2}:[0-9]{2}$")) {
			time = time + ":00";
		} else if (!time.matches("^[0-9]{2}:[0-9]{2}:[0-9]{2}$")) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The tweet time must match the pattern: HH:MM:SS");
			return mv;
		}
		Post post = new Post(content, date + " " + time, mappedGroup);
		if (!img.isEmpty()) {
			try {
				if (ImageIO.read(new URL(img)) == null) {
					ModelAndView mv = new ModelAndView("error");
					mv.addObject("error", "Sorry, but the selected image has an unsupported format.");
					return mv;
				}
			} catch (Exception e) {
				ModelAndView mv = new ModelAndView("error");
				mv.addObject("error", "The image URL must be a valid url.");
				return mv;
			}
		} else {
			img = null;
		}
		post.setImg(img);
		mappedGroup.addPost(post);
		groupService.saveGroup(mappedGroup);
		postService.savePost(post);
		return new ModelAndView("redirect:/groups/view/" + mappedGroup.getId());
	}

}
