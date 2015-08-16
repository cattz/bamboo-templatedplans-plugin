[#if build.buildDefinition.customConfiguration.get('custom.bamboo.template.list')?has_content ]
    [@ui.bambooInfoDisplay titleKey='Template plan configuration' float=false height='160px']
        
        [@ww.label label='Is a template ?' ]
            [@ww.param name='value']${build.buildDefinition.customConfiguration.get('custom.bamboo.template.enabled')?if_exists}[/@ww.param]
        [/@ww.label]
    [/@ui.bambooInfoDisplay]
[/#if]