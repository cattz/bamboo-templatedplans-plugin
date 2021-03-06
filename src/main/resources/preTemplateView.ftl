[#if build.buildDefinition.customConfiguration.get('custom.bamboo.template.list')?has_content ]
    [@ui.bambooInfoDisplay titleKey='Template plan configuration' float=false height='160px']
        [@ww.label label='Template' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.list')?if_exists}[/@ww.param]
        [/@ww.label]

        [@ww.label label='Template Tasks Definition' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.task')?if_exists}[/@ww.param]
        [/@ww.label]

        [@ww.label label='Template Artifact Definition' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.artifacts')?if_exists}[/@ww.param]
        [/@ww.label]

        [@ww.label label='Template Requirements Definition' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.requirements')?if_exists}[/@ww.param]
        [/@ww.label]

        [@ww.label label='Template Configuration Definition' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.config')?if_exists}[/@ww.param]
        [/@ww.label]
    [/@ui.bambooInfoDisplay]
[/#if]