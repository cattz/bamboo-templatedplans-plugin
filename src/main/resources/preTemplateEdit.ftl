[@ui.bambooSection title='Template list' ]
    [@ww.select name='custom.bamboo.template.list' 
            label='Template'  list=templates
            description='Template list' emptyOption="true" /]

    [@ww.checkbox label='Template Tasks Definition'               name='custom.bamboo.template.task' toggle='true' description='Remove tasks not checking out code and adding them from the template' /]

    [@ww.checkbox label='Template Artifact Definition' name='custom.bamboo.template.artifacts' toggle='true' description='Removes the original artifact definitions and copies from template' /]

    [@ww.checkbox label='Attempt to maintain task states' name='custom.bamboo.template.states' toggle='true' description='Attempt to maintain task state for tasks with same name in current job and template' /]

[/@ui.bambooSection ]


