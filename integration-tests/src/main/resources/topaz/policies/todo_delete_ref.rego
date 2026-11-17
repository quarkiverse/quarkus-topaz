package todoApp.DELETE.todos.ref

import future.keywords.if

default allowed = false

# input.resource.todo is a TodoRef record that Jackson converted into the resource context
allowed if {
	ds.check({
		"object_type": "todo",
		"object_id": input.resource.todo.id,
		"relation": "owner",
		"subject_type": "user",
		"subject_id": input.user.id,
	})
}
