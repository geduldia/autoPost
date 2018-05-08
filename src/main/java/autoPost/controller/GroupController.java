package autoPost.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import autoPost.entities.Post;
import autoPost.entities.PostGroup;
import autoPost.entities.User;
import autoPost.postCreation.MalformedTSVFileException;
import autoPost.postCreation.PostFactory;
import autoPost.service.GroupService;
import autoPost.service.PostService;
import autoPost.service.UserService;

@Controller
@RequestMapping(value = "/groups")
public class GroupController {

	@Value("${autoPost.parser.dateformats}")
	private String dateFormats;

	@Autowired
	private HttpSession session;

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserService userService;

	@Autowired
	private PostService postService;

	private int groupsPerPage = 15;
	private int postsPerPage = 15;

	/**
	 * A HTTP GET request handler, responsible for serving /groups/view. Lists
	 * all groups of the current user read from the database.
	 * 
	 * @param page
	 *            Request param containing the page number (default is 1)
	 * @return View containing the groups overview
	 */
	@RequestMapping(value = "/view")
	public ModelAndView viewGroups(@RequestParam(name = "page", defaultValue = "1") int page) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userID = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		ModelAndView mv = new ModelAndView("groups");
		List<PostGroup> groups = groupService.getGroupsForUser(userID);
		if (groups.size() <= groupsPerPage) {
			mv.addObject("groups", groups);
			return mv;
		}
		int pages = (int) StrictMath.ceil((double) groups.size() / groupsPerPage);
		int offset = groupsPerPage * (page - 1);
		int endset = (offset + groupsPerPage > groups.size() ? (groups.size()) : (offset + groupsPerPage));
		mv.addObject("groups", groups.subList(offset, endset));
		mv.addObject("pages", pages);
		mv.addObject("page", page);
		return mv;
	}

	/**
	 * A HTTP POST request handler, responsible for serving /groups/delete.
	 * Deletes the given groups (and all referring posts) and redirects to the
	 * groups overview.
	 * 
	 * @param toDelete
	 *            Request param containing the groupIds of the groups to delete
	 * @return View containing the groups overview
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	public ModelAndView deleteGroups(@RequestParam("groupId") List<Integer> toDelete) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		for (int groupId : toDelete) {
			groupService.deleteGroup(groupId, userId);
		}
		return new ModelAndView("redirect:/groups/view");
	}

	/**
	 * A HTTP GET request handler, responsible for serving
	 * /toggleStatus/{groupId}. Toggles the activation-state of the group,
	 * referenced by {groupId}. If the group is enabled after the toggle, the
	 * PostScheduler is called to schedule all Posts in the (now enabled)
	 * group..
	 * 
	 * @param groupId
	 *            path param containing the groupId
	 * @return Redirect-view to the referer (group-details or groups-overview)
	 * 
	 */
	@RequestMapping(value = "/toggleStatus/{groupId}", method = RequestMethod.GET)
	public ModelAndView toggleStatus(@PathVariable int groupId, HttpServletRequest request) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		groupService.toggleStatus(groupId, userId);
		String referer = request.getHeader("Referer");
		return new ModelAndView("redirect:" + referer);
	}

	/**
	 * A HTTP GET request handler responsible for serving /delete/{groupId}.
	 * Deletes the group referenced by groupId (and all referring posts) from
	 * the database and redirects to the groups overview
	 * 
	 * @param groupId
	 *            path param containing the groupID of the group to delete
	 * @return View containing the groups overview
	 */
	@RequestMapping(value = "/delete/{groupId}")
	public ModelAndView deleteGroup(@PathVariable int groupId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		if (groupService.getGroup(groupId, userId) == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Group with the ID # " + groupId + " does not exist!");
			return mv;
		}
		// postService.deleteGroupPosts(groupId, userId);
		groupService.deleteGroup(groupId, userId);
		ModelAndView mv = new ModelAndView("redirect:/groups/view");
		return mv;
	}

	/**
	 * A HTTP GET request handler responsible for serving /groups/view/{groupId}
	 * This method is called to view a single group and its Posts in detail. It
	 * reads all relevant information from the database and displays it as a
	 * view. If no group with the requested ID is found, an error is displayed.
	 * 
	 * @param groupId
	 *            path param containing the groupId
	 * @param page
	 *            request param containing the page-number of the posts-list
	 *            (default is 1)
	 * @return View containing the group details
	 * 
	 */
	@RequestMapping(value = "/view/{groupId}")
	public ModelAndView viewGroup(@PathVariable int groupId,
			@RequestParam(name = "page", defaultValue = "1") int page) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		PostGroup group = groupService.getGroup(groupId, userId);
		if (group == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Group with the ID # " + groupId + " does not exist!");
			return mv;
		}
		List<Post> posts = group.getPosts();
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
		ModelAndView mv = new ModelAndView("group");
		if (posts.size() <= postsPerPage) {
			mv.addObject("group", group);
			mv.addObject("posts", posts);
			mv.addObject("mode", "view");
			return mv;
		}
		int pages = (int) StrictMath.ceil((double) posts.size() / postsPerPage);
		int offset = postsPerPage * (page - 1);
		int endset = (offset + postsPerPage > posts.size() ? (posts.size()) : (offset + postsPerPage));
		mv.addObject("group", group);
		mv.addObject("posts", posts.subList(offset, endset));
		mv.addObject("pages", pages);
		mv.addObject("page", page);
		mv.addObject("mode", "view");
		return mv;
	}

	/**
	 * A HTTP GET request handler responsible for serving /groups/add. Returns a
	 * view containing the form to add a new, empty group
	 * 
	 * @return View containing the group-creation form
	 */
	@RequestMapping(value = "/add")
	public ModelAndView addGroup() {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		ModelAndView mv = new ModelAndView("group");
		mv.addObject("mode", "add");
		return mv;
	}

	/**
	 * A HTTP POST request handler responsible for serving /groups/add This
	 * method gets POSTed as the group-creation form is submitted. All input-
	 * field values are passed as parameters and checked for validity. Upon
	 * success a new group is added to the database.
	 *
	 * @param name
	 *            POST param containing the name of the new group
	 * @param description
	 *            POST param containing a description of the new group
	 * @return Redirect-view if successful, else error-view
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public ModelAndView saveGroup(@RequestParam("name") String name, @RequestParam("description") String description) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		User user = userService.getUser(userId);
		if (description.equals("")) {
			description = null;
		} else if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}
		if (name.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group name may be no longer then 255 characters.");
			return mv;
		}
		PostGroup group = new PostGroup(name, description, user);
		group = groupService.saveGroup(group);
		ModelAndView mv = new ModelAndView("redirect:/groups/view/" + group.getId());
		mv.addObject("mode", "view");
		return mv;
	}

	/**
	 * A HTTP GET request responsible for serving /groups/edit/{groupId} This
	 * method is responsible to present the group-creation form with all values
	 * prefilled into the according input-field.
	 * 
	 * @param groupId
	 *            path param containing the groupId of the group to edit
	 * @return View containing the group-creation form with prefilled values
	 */
	@RequestMapping(value = "/edit/{groupId}")
	public ModelAndView editGroup(@PathVariable int groupId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		PostGroup group = groupService.getGroup(groupId, userId);
		ModelAndView mv = new ModelAndView("group");
		mv.addObject("group", group);
		mv.addObject("mode", "edit");
		return mv;
	}

	/**
	 * A HTTP POST request responsible for serving /groups/edit/{groupId}. This
	 * method gets POSTed as the group-editing form is submitted. All input-
	 * field values are passed as parameters and checked for validity. Upon
	 * success the referenced group gets updated in the database or an error is
	 * shown.
	 * 
	 * @param groupId
	 * @param name
	 * @param description
	 * @return
	 */
	@RequestMapping(value = "/edit/{groupId}", method = RequestMethod.POST)
	public ModelAndView saveGroup(@PathVariable int groupId, @RequestParam("name") String name,
			@RequestParam("description") String description) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		PostGroup group = groupService.getGroup(groupId, userId);
		if (group == null) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "A Group with the ID # " + groupId + " does not exist!");
			return mv;
		}
		if (description.equals("")) {
			description = null;
		}
		if (description.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group description may be no longer then 255 characters.");
			return mv;
		}
		if (name.length() > 255) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The group name may be no longer then 255 characters.");
			return mv;
		}
		group.setDescription(description);
		group.setName(name);
		groupService.saveGroup(group);
		ModelAndView mv = new ModelAndView("redirect:/groups/view/" + groupId);
		mv.addObject("mode", "view");
		return mv;
	}

	@RequestMapping(value = "/copy/year/{groupId}")
	public ModelAndView copyToNextYear(@PathVariable int groupId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		User user = userService.getUser(userId);
		PostGroup group = groupService.getGroup(groupId, userId);
		PostGroup updatedGroup = new PostGroup(group.getName(), group.getDescription(), user);
		List<Post> updatedPosts = new ArrayList<Post>();
		Post updatedPost;
		String timeString = null;
		for (Post post : group.getPosts()) {
			LocalDateTime time = LocalDateTime.parse(post.getDate(),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			time = time.plusYears(1);
			timeString = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			updatedPost = new Post(post.getContent(), timeString, updatedGroup);
			updatedPost.setImg(post.getImg());
			updatedPosts.add(updatedPost);
		}
		String newYear = timeString.substring(0, 4);
		updatedGroup.setPosts(updatedPosts);
		updatedGroup.setName(group.getName() + "_" + newYear);
		groupService.saveGroup(updatedGroup);
		ModelAndView mv = new ModelAndView("redirect:/groups/view");
		return mv;
	}

	@RequestMapping(value = "/copy/date/{groupId}")
	public ModelAndView copyToReferenceDate(@PathVariable int groupId,
			@RequestParam(name = "page", defaultValue = "1") int page) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		ModelAndView mv = new ModelAndView("copy");
		PostGroup group = groupService.getGroup(groupId, userId);
		if (group.getPosts().size() <= postsPerPage) {
			mv.addObject("group", group);
			mv.addObject("posts", group.getPosts());
			return mv;
		}
		int pages = (int) StrictMath.ceil((double) group.getPosts().size() / postsPerPage);
		int offset = postsPerPage * (page - 1);
		int endset = (offset + postsPerPage > group.getPosts().size() ? (group.getPosts().size())
				: (offset + postsPerPage));
		mv.addObject("group", group);
		mv.addObject("posts", group.getPosts().subList(offset, endset));
		mv.addObject("pages", pages);
		mv.addObject("page", page);
		return mv;
	}

	@RequestMapping(value = "/copy/date/{groupId}", method = RequestMethod.POST)
	public ModelAndView copyToReferenceDatePost(@PathVariable int groupId, @RequestParam("name") String name,
			@RequestParam("description") String description, @RequestParam("date") String date,
			@RequestParam("time") String time, @RequestParam("referencePostId") int referencePostId) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
						// session.getAttribute("account")).get("userId"));
		User user = userService.getUser(userId);
		PostGroup group = groupService.getGroup(groupId, userId);
		PostGroup copyGroup = new PostGroup(name, description, user);
		Post referencePost = postService.getPost(referencePostId, userId);
		LocalDateTime refPostTime = LocalDateTime.parse(referencePost.getDate(),
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		if (time.matches("^[0-9]{2}:[0-9]{2}$")) {
			time = time + ":00";
		}
		LocalDateTime updatedTime = LocalDateTime.parse(date + " " + time,
				DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
		long seconds = ChronoUnit.SECONDS.between(refPostTime, updatedTime);
		List<Post> copyPosts = new ArrayList<Post>();
		for (Post post : group.getPosts()) {
			Post copyPost = new Post(post.getContent(), post.getDate(), copyGroup);
			LocalDateTime then = LocalDateTime.parse(post.getDate(),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
			then = then.plusSeconds(seconds);
			copyPost.setDate(then.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			copyPosts.add(copyPost);
			copyGroup.setPosts(copyPosts);
		}
		groupService.saveGroup(copyGroup);
		ModelAndView mv = new ModelAndView("redirect:/groups/view/" + copyGroup.getId());
		return mv;
	}

	@RequestMapping(value = "/import/{importer}")
	public ModelAndView importGroup(@PathVariable String importer) {
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");

		if (!Arrays.asList("tsvFile", "googleDocs").contains(importer)) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "An importer of type " + importer + " does not exist.");
			return mv;
		}
		ModelAndView mv = new ModelAndView("import");
		mv.addObject("importer", importer);
		return mv;
	}

	@RequestMapping(value = "/import/tsvFile", method = RequestMethod.POST)
	public ModelAndView importTsvFile(@RequestParam("source") MultipartFile source, @RequestParam("name") String name,
			@RequestParam("description") String description, @RequestParam("delay") int delay,
			@RequestParam("encoding") String encoding) throws MalformedTSVFileException {
		System.out.println("import");
		// if (session.getAttribute("account") == null)
		// return new ModelAndView("redirect:/account");
		int userId = 1;// Integer.parseInt(((Map<String, String>)
		// session.getAttribute("account")).get("userId"));
		File file;
		try {
			file = File.createTempFile("upload-", ".tsv");
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(source.getBytes());
			fos.close();
		} catch (Exception e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "The uploaded file could not be opened.");
			return mv;
		}
		User user = userService.getUser(userId);
		PostFactory factory = new PostFactory(dateFormats);
		PostGroup group = null;
		try{			
			group = factory.getPostsFromTSVFile(file, name, description, delay, encoding);		
		}
		catch (MalformedTSVFileException e) {
			ModelAndView mv = new ModelAndView("error");
			mv.addObject("error", "Parsing error, " + e.getMessage());
			return mv;
		}	
		group.setUser(user);
		groupService.saveGroup(group);
		ModelAndView mv = new ModelAndView("redirect:/groups/view/"+group.getId());
		return mv;
	}

}