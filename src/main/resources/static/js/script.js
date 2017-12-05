function ConfirmDeleteGroups(event) {
	if (!$("input:checked").length) {
		event.preventDefault();
		alert("no groups selected");
		return;
	}

	return confirm("Are you sure you want to delete the selected groups and all referring posts?");

};

function ConfirmDeletePosts(event) {
	if (!$("input:checked").length) {
		event.preventDefault();
		alert("no posts selected");
		return;
	}
	if (confirm("Are you sure you want to delete all selected posts?")) {
		return true;
	}
	return false;
};


function ConfirmDeletePost(event){
	if(!confirm("Are you sure you want to delete this post?")){
		event.preventDefault();
		return;
	}
	else{
		return true;
	}

};

$("#selectAll").click(function() {

	if ($(this).is(':checked')) {
		$("input[type='checkbox']").prop('checked', true);
	} else {
		$("input[type='checkbox']").prop('checked', false);
	}

});


function ConfirmDeleteGroup(event){
	return confirm("Are you sure you want to delete this group and all referring posts?");
};

$("#selectAll").click(function() {

	if ($(this).is(':checked')) {
		$("input[type='checkbox']").prop('checked', true);
	} else {
		$("input[type='checkbox']").prop('checked', false);
	}

});
