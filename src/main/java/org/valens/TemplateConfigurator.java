
/*
 * Copyright (C) 2015 Valens Soft
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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