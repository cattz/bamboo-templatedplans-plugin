package org.valens;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseBuildConfigurationAwarePlugin;
import com.atlassian.bamboo.v2.build.configuration.MiscellaneousBuildConfigurationPlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.google.common.collect.Maps;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TemplateConfigurator extends BaseBuildConfigurationAwarePlugin implements MiscellaneousBuildConfigurationPlugin {
     
    private PlanManager planManager;
    protected static final Logger log = LoggerFactory.getLogger( TemplateConfigurator.class );
    
    @Override
    public boolean isApplicableTo(@NotNull Plan plan)
    {
        return plan instanceof Job;
    }
        
    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull BuildConfiguration bc, @Nullable Plan plan) {
        
        Map<String, String> result = Maps.newLinkedHashMap();
        super.populateContextForEdit( context, bc, plan );
        
        if(planManager!=null){
            for ( TopLevelPlan p : planManager.getAllPlansUnrestricted())
            {
                for(Job j : p.getAllJobs())
                {
                    log.debug( "populateContextForEdit  " +  p.getProject() + " - " + p.getName());
                    result.put( j.getName(), j.getKey());
                }
                
            }
            
        }
       
        context.put("templates", result);
    }
            
    public PlanManager getPlanManager() {

        return this.planManager;
    }

    public void setPlanManager( PlanManager planManager ) {
        log.debug( "setPlanManager  " +  planManager.toString());
        this.planManager = planManager;
    }
}