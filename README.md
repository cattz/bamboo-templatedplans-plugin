# bamboo-dynamicplans-plugin

# Dynamic scripted variables:

- any variable containing the following pattern will be calculated

	script: if (repository_git_branch_0.equalsIgnoreCase("master")) return "true";
	
# Conditioned scripted jobs

Dynamic Tasks De-Activation

	On each job you can enter a groovy that will be evaluated. 
	
	If return is "true" some tasks having a description matching a configurable regular expression will be disabled.
	
Dynamic Tasks Switcher

	On each job you can enter a groovy that will be evaluated and should return a regular expression
	
	Any task having a description matching this regular expression will be set accordingly to your request.

	Eg: 
		Assuming we have 3 tasks in our plan:
			develop
			master
			feature
			
		We can use the following groovy script that will return expressions to match the tasks based on which is the current branch.
		
		if (repository_git_branch_0.equalsIgnoreCase("master")) return "master";

		if (repository_git_branch_0.equalsIgnoreCase("develop")) return "develop";

		return "feature";
		
		If in the settings we have placed Desired state as Enabled the plugin will tend to enable each task when branch is switching.
		
		


	
	