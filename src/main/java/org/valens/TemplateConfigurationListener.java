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

import com.atlassian.bamboo.event.BambooErrorEvent;
import com.atlassian.bamboo.event.BuildConfigurationUpdatedEvent;
import com.atlassian.bamboo.event.HibernateEventListener;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.build.BuildDefinition;
import com.atlassian.bamboo.build.BuildDefinitionManager;
import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.build.SimpleLogEntry;
import com.atlassian.bamboo.caching.DashboardCachingManager;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainStage;
import com.atlassian.bamboo.chains.cache.ImmutableChainStage;
import com.atlassian.bamboo.event.ServerStartedEvent;
import com.atlassian.bamboo.fieldvalue.BuildDefinitionConverter;
import com.atlassian.bamboo.fieldvalue.TaskConfigurationUtils;
import com.atlassian.bamboo.plan.PlanHelper;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinition;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionImpl;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionManager;
import com.atlassian.bamboo.plan.cache.CachedPlanManager;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plan.cache.ImmutableJob;
import com.atlassian.bamboo.plan.cache.ImmutableTopLevelPlan;
import com.atlassian.bamboo.repository.RepositoryConfigurationService;
import com.atlassian.bamboo.repository.RepositoryDefinition;
import com.atlassian.bamboo.task.TaskConfigurationService;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.task.TaskDefinitionImpl;
import com.atlassian.bamboo.task.TaskManager;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementSet;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementSetImpl;
import com.atlassian.event.Event;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.spring.container.ContainerManager;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author IHutuleac
 */
public class TemplateConfigurationListener implements HibernateEventListener
{

    private String SELECTED_TEMPLATE = "custom.bamboo.template.list";
    private String SELECTED_TEMPLATE_TASK = "custom.bamboo.template.task";
    private String SELECTED_TEMPLATE_ARTIFACTS = "custom.bamboo.template.artifacts";
    private String SELECTED_TEMPLATE_STATES = "custom.bamboo.template.states";
    private String SELECTED_TEMPLATE_REQUIREMENTS = "custom.bamboo.template.requirements";

    protected static final Logger log = LoggerFactory.getLogger(TemplateConfigurationListener.class);
    private PlanManager planManager;
    private CachedPlanManager cachedPlanManager;
    private BuildDefinitionConverter buildDefinitionConverter;
    private TransactionTemplate transactionTemplate;
    private TaskConfigurationService taskConfigurationService;
    private BuildDefinitionManager buildDefinitionManager;
    private DashboardCachingManager dashboardCachingManager;
    private ArtifactDefinitionManager artifactDefinitionManager;
    private RepositoryConfigurationService repositoryConfigurationService;

    public RepositoryConfigurationService getRepositoryConfigurationService()
    {
        return repositoryConfigurationService;
    }

    public void setRepositoryConfigurationService(RepositoryConfigurationService repositoryConfigurationService)
    {
        this.repositoryConfigurationService = repositoryConfigurationService;
    }

    private ArtifactDefinition convertDefinition(ArtifactDefinition artifactDefinition, Job job)
    {
        ArtifactDefinitionImpl definition = new ArtifactDefinitionImpl();

        definition.setName(artifactDefinition.getName());
        definition.setLocation(artifactDefinition.getLocation());
        definition.setCopyPattern(artifactDefinition.getCopyPattern());
        definition.setSharedArtifact(artifactDefinition.isSharedArtifact());
        definition.setProducerJob(job);

        return definition;
    }

    private void logMap(Map map)
    {
        log.warn(map.toString());

        for (Object s : map.keySet())
        {
            log.warn("[" + s + "] = [" + map.get(s) + "]");
        }
    }

    @Override
    public Class[] getHandledEventClasses()
    {
        return new Class[]
        {
            BuildConfigurationUpdatedEvent.class
        };
    }

    @Override
    public void handleEvent(final Event event)
    {

        transactionTemplate.execute(new TransactionCallback<Object>()
        {
            @Override
            public Object doInTransaction()
            {
                if (event instanceof BuildConfigurationUpdatedEvent)
                {
                    BuildConfigurationUpdatedEvent buildConfigurationUpdatedEvent = (BuildConfigurationUpdatedEvent) event;

                    String key = buildConfigurationUpdatedEvent.getBuildPlanKey();
                    log.debug("Received BuildConfigurationUpdatedEvent  " + key);

                    for (ImmutableTopLevelPlan p : cachedPlanManager.getPlans(ImmutableTopLevelPlan.class))
                    {

                        ImmutableChain planChain = (ImmutableChain) p;
                        for (ImmutableChainStage stage : planChain.getAllStages())
                        {
                            for (ImmutableJob immutableJob : stage.getJobs())
                            {
                                Job job = (Job) planManager.getPlanByKey(immutableJob.getKey());
                                Map<String, String> customConfiguration = job.getBuildDefinition().getCustomConfiguration();
                                if (customConfiguration.containsKey(SELECTED_TEMPLATE) && customConfiguration.get(SELECTED_TEMPLATE).contains("="))
                                {

                                    boolean stateTask = true;
                                    if (customConfiguration.get(SELECTED_TEMPLATE_TASK) == null || customConfiguration.get(SELECTED_TEMPLATE_TASK).toString().equalsIgnoreCase("false"))
                                    {
                                        stateTask = false;
                                    }

                                    boolean stateArtifacts = true;
                                    if (customConfiguration.get(SELECTED_TEMPLATE_ARTIFACTS) == null || customConfiguration.get(SELECTED_TEMPLATE_ARTIFACTS).toString().equalsIgnoreCase("false"))
                                    {
                                        stateArtifacts = false;
                                    }

                                    boolean stateStates = true;
                                    if (customConfiguration.get(SELECTED_TEMPLATE_STATES) == null || customConfiguration.get(SELECTED_TEMPLATE_STATES).toString().equalsIgnoreCase("false"))
                                    {
                                        stateStates = false;
                                    }
                                    
                                    boolean stateRequirements = true;
                                    if (customConfiguration.get(SELECTED_TEMPLATE_REQUIREMENTS) == null || customConfiguration.get(SELECTED_TEMPLATE_REQUIREMENTS).toString().equalsIgnoreCase("false"))
                                    {
                                        stateRequirements = false;
                                    }

                                    String selectedKey = (customConfiguration.get(SELECTED_TEMPLATE).toString().split("=")[1]);

                                    log.warn("Found a plan ( " + job.getKey() + " ) that has a template " + selectedKey);

                                    String[] tokens = selectedKey.split("-");
                                    String templateKey = tokens[0] + "-" + tokens[1];

                                    PlanKey planKey = PlanKeys.getPlanKey(templateKey);

                                    Chain templateChain = (Chain) planManager.getPlanByKey(planKey);

                                    for (ChainStage chainTemplateStage : templateChain.getStages())
                                    {
                                        for (Job templateJob : chainTemplateStage.getJobs())
                                        {
                                            if (selectedKey.trim().equalsIgnoreCase(templateJob.getKey().trim()))
                                            {
                                                BuildDefinition bd = job.getBuildDefinition();

                                                List<TaskDefinition> tasks = bd.getTaskDefinitions();
                                                List<TaskDefinition> newTasks = new LinkedList<TaskDefinition>();
                                                List<TaskDefinition> lst = templateJob.getBuildDefinition().getTaskDefinitions();

                                                RequirementSet rs = new RequirementSetImpl();

                                                RequirementSet requirementSetToClone = job.getRequirementSet();
                                                for (Requirement requirement : requirementSetToClone.getRequirements())
                                                {
                                                    if (requirement.getOwnerId() == -1 && stateRequirements)
                                                    {
                                                        RequirementImpl newRequirement = new RequirementImpl(requirement.getKey(), requirement.isRegexMatch(), requirement.getMatchValue(), requirement.isReadonly());
                                                        rs.addRequirement(newRequirement);
                                                    }
                                                }

                                                log.warn("Tasks size: " + lst.size());

                                                if (lst.size() > 0 && stateTask)
                                                {

                                                    HashMap<String, Boolean> stateCache = new HashMap<String, Boolean>();
                                                    for (TaskDefinition t : tasks)

                                                    {
                                                        if (t.getPluginKey().contains("task.vcs.checkout"))
                                                        {
                                                            long taskId = TaskConfigurationUtils.getUniqueId(newTasks);
                                                            TaskDefinition auxtask = new TaskDefinitionImpl(taskId,
                                                                    t.getPluginKey(),
                                                                    t.getUserDescription(),
                                                                    t.isEnabled(),
                                                                    t.getConfiguration());

                                                            auxtask.setFinalising(t.isFinalising());

                                                            newTasks.add(auxtask);
                                                        }

                                                        stateCache.put(t.getUserDescription(), t.isEnabled());
                                                    }

                                                    for (TaskDefinition t : lst)
                                                    {
                                                        if (!t.getPluginKey().contains("task.vcs.checkout"))
                                                        {
                                                            long taskId = TaskConfigurationUtils.getUniqueId(newTasks);

                                                            boolean b = t.isEnabled();
                                                            if (stateStates && t.getUserDescription() != null
                                                                    && t.getUserDescription().length() > 0 && stateCache.get(t.getUserDescription()) != null)
                                                            {
                                                                b = stateCache.get(t.getUserDescription());
                                                            }

                                                            TaskDefinition auxtask = new TaskDefinitionImpl(taskId,
                                                                    t.getPluginKey(),
                                                                    t.getUserDescription(),
                                                                    b,
                                                                    t.getConfiguration());

                                                            auxtask.setFinalising(t.isFinalising());

                                                            for (Requirement requirement : templateJob.getRequirementSet().getRequirements())
                                                            {
                                                                if (requirement.getOwnerId() == t.getId())
                                                                {
                                                                    RequirementImpl newRequirement = new RequirementImpl(requirement.getKey(), requirement.isRegexMatch(), requirement.getMatchValue(), requirement.isReadonly());
                                                                    newRequirement.setOwnerId(taskId);

                                                                    rs.addRequirement(newRequirement);
                                                                }
                                                            }

                                                            log.debug("Generated new task: " + auxtask.toString());

                                                            newTasks.add(auxtask);
                                                        }
                                                    }

                                                    requirementSetToClone = templateJob.getRequirementSet();
                                                    for (Requirement requirement : requirementSetToClone.getRequirements())
                                                    {
                                                        if (requirement.getOwnerId() == -1)
                                                        {
                                                            RequirementImpl newRequirement = new RequirementImpl(requirement.getKey(), requirement.isRegexMatch(), requirement.getMatchValue(), requirement.isReadonly());
                                                            rs.addRequirement(newRequirement);
                                                        }
                                                    }

                                                    for (Requirement requirement : rs.getRequirements())
                                                    {
                                                        log.debug("Requirement list: " + requirement);
                                                    }

                                                    job.setRequirementSet(rs);
                                                    bd.setTaskDefinitions(newTasks);
                                                    buildDefinitionManager.savePlanAndDefinition(job, bd);
                                                    planManager.savePlan(job);
                                                    log.warn("Final Task List Size: " + newTasks.size());
                                                }

                                                if (templateJob.getArtifactDefinitions().size() > 0 && stateArtifacts)
                                                {
                                                    artifactDefinitionManager.removeArtifactDefinitionsByPlan(job);

                                                    List<ArtifactDefinition> artifacts = new ArrayList<ArtifactDefinition>();
                                                    for (ArtifactDefinition artifact : templateJob.getArtifactDefinitions())
                                                    {
                                                        artifacts.add(convertDefinition(artifact, job));
                                                    }

                                                    artifactDefinitionManager.saveArtifactDefinitions(artifacts);
                                                }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }
        });
    }

    public PlanManager getPlanManager()
    {

        return this.planManager;
    }

    public void setPlanManager(PlanManager planManager)
    {
        log.debug("setPlanManager  " + planManager.toString());
        this.planManager = planManager;
    }

    public BuildDefinitionManager getBuildDefinitionManager()
    {

        return this.buildDefinitionManager;
    }

    public void setBuildDefinitionManager(BuildDefinitionManager buildDefinitionManager)
    {

        this.buildDefinitionManager = buildDefinitionManager;
    }

    public TransactionTemplate getTransactionTemplate()
    {
        return transactionTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate)
    {
        this.transactionTemplate = transactionTemplate;
    }

    public CachedPlanManager getCachedPlanManager()
    {
        return cachedPlanManager;
    }

    public void setCachedPlanManager(CachedPlanManager cachedPlanManager)
    {
        this.cachedPlanManager = cachedPlanManager;
    }

    public BuildDefinitionConverter getBuildDefinitionConverter()
    {
        return buildDefinitionConverter;
    }

    public void setBuildDefinitionConverter(BuildDefinitionConverter buildDefinitionConverter)
    {
        this.buildDefinitionConverter = buildDefinitionConverter;
    }

    public TaskConfigurationService getTaskConfigurationService()
    {
        return taskConfigurationService;
    }

    public void setTaskConfigurationService(TaskConfigurationService taskConfigurationService)
    {
        this.taskConfigurationService = taskConfigurationService;
    }

    public ArtifactDefinitionManager getArtifactDefinitionManager()
    {
        return artifactDefinitionManager;
    }

    public void setArtifactDefinitionManager(ArtifactDefinitionManager artifactDefinitionManager)
    {
        this.artifactDefinitionManager = artifactDefinitionManager;
    }
}
