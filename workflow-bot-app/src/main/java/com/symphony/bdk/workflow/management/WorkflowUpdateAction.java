package com.symphony.bdk.workflow.management;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowMgtAction;
import com.symphony.bdk.workflow.configuration.WorkflowDeployer;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.exception.NotFoundException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class WorkflowUpdateAction extends WorkflowAbstractAction implements WorkflowsMgtAction {
  private final WorkflowEngine<BpmnModelInstance> workflowEngine;

  public WorkflowUpdateAction(WorkflowEngine<BpmnModelInstance> workflowEngine, WorkflowDeployer deployer) {
    super(deployer);
    this.workflowEngine = workflowEngine;
  }

  @Override
  public void doAction(String content) {
    Workflow workflow = this.convertToWorkflow(content);
    if (!this.workflowExist(workflow.getId())) {
      throw new NotFoundException(String.format("Workflow %s does not exist", workflow.getId()));
    }
    workflowEngine.parseAndValidate(workflow);
    Path path = this.getWorkflowFilePath(workflow.getId());
    writeFile(content, workflow, path.toString());
  }

  @Override
  public WorkflowMgtAction action() {
    return WorkflowMgtAction.UPDATE;
  }
}