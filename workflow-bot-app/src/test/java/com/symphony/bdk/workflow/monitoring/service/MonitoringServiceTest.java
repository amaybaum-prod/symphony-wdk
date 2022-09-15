package com.symphony.bdk.workflow.monitoring.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.symphony.bdk.workflow.api.v1.dto.ActivityInstanceView;
import com.symphony.bdk.workflow.api.v1.dto.TaskTypeEnum;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowActivitiesView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowDefinitionView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowInstView;
import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.converter.ObjectConverter;
import com.symphony.bdk.workflow.engine.WorkflowDirectGraph;
import com.symphony.bdk.workflow.engine.WorkflowNode;
import com.symphony.bdk.workflow.engine.camunda.WorkflowDirectGraphCachingService;
import com.symphony.bdk.workflow.monitoring.repository.ActivityQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.VariableQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowInstQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.WorkflowQueryRepository;
import com.symphony.bdk.workflow.monitoring.repository.domain.ActivityInstanceDomain;
import com.symphony.bdk.workflow.monitoring.repository.domain.VariablesDomain;
import com.symphony.bdk.workflow.swadl.v1.activity.message.SendMessage;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {
  @Mock
  WorkflowDirectGraphCachingService workflowDirectGraphCachingService;
  @Mock
  WorkflowQueryRepository workflowQueryRepository;
  @Mock
  WorkflowInstQueryRepository workflowInstQueryRepository;
  @Mock
  ActivityQueryRepository activityQueryRepository;
  @Mock
  VariableQueryRepository variableQueryRepository;
  @Mock
  ObjectConverter objectConverter;
  @InjectMocks
  MonitoringService service;

  @BeforeEach
  void setUp() {
  }

  @Test
  void listAllWorkflows() {
    when(workflowQueryRepository.findAll()).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowView> workflowViews = service.listAllWorkflows();
    //then
    then(workflowViews).isEmpty();
  }

  @Test
  void listWorkflowInstances() {
    when(workflowInstQueryRepository.findAllById(anyString())).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(WorkflowInstView.class))).thenReturn(Collections.emptyList());
    // when
    List<WorkflowInstView> workflowViews = service.listWorkflowInstances("id");
    //then
    then(workflowViews).isEmpty();
  }

  @Test
  void listWorkflowInstanceActivities() {
    // given
    ActivityInstanceView view1 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity1")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value1")).build();
    ActivityInstanceView view2 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity2")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value2")).build();

    when(activityQueryRepository.findAllByWorkflowInstanceId(anyString())).thenReturn(Collections.singletonList(
        ActivityInstanceDomain.builder()
            .build())); // returns at least one item, otherwise an IllegalArgumentException will be thrown
    when(objectConverter.convertCollection(anyList(), eq(ActivityInstanceView.class))).thenReturn(
        List.of(view1, view2));

    // mock graph
    WorkflowNode activity1 = new WorkflowNode();
    SendMessage sendMessage = new SendMessage();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity1.activity(sendMessage);
    activity1.id("activity1");

    WorkflowNode activity2 = new WorkflowNode();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity2.activity(sendMessage);
    activity2.id("activity2");

    WorkflowDirectGraph directGraph = new WorkflowDirectGraph();
    directGraph.registerToDictionary("activity1", activity1);
    directGraph.registerToDictionary("activity2", activity2);

    when(workflowDirectGraphCachingService.getDirectGraph("workflow")).thenReturn(directGraph);

    // mock variables
    VariablesDomain vars = new VariablesDomain();
    vars.setRevision(2);
    vars.setUpdateTime(Instant.now());
    vars.setOutputs(Maps.newHashMap("key", "value"));
    when(variableQueryRepository.findGlobalByWorkflowInstanceId(anyString())).thenReturn(vars);

    // when
    WorkflowActivitiesView workflowInstanceActivities = service.listWorkflowInstanceActivities("workflow", "instance");

    // then
    then(workflowInstanceActivities.getActivities()).hasSize(2);
    then(workflowInstanceActivities.getActivities().get(0).getOutputs()).hasSize(1);
    then(workflowInstanceActivities.getActivities().get(0).getWorkflowId()).isEqualTo("workflow");
    then(workflowInstanceActivities.getActivities().get(0).getInstanceId()).isEqualTo("instance");
    then(workflowInstanceActivities.getActivities().get(0).getActivityId()).isEqualTo("activity1");
    then(workflowInstanceActivities.getActivities().get(0).getStartDate()).isNotNull();
    then(workflowInstanceActivities.getActivities().get(0).getEndDate()).isNotNull();
    then(workflowInstanceActivities.getGlobalVariables().getRevision()).isEqualTo(2);
    then(workflowInstanceActivities.getGlobalVariables().getUpdateTime()).isNotNull();
  }

  @Test
  void listWorkflowInstanceActivities_badInstanceId_illegalArgumentException() {
    // given
    ActivityInstanceView view1 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity1")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value1")).build();
    ActivityInstanceView view2 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity2")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value2")).build();

    when(activityQueryRepository.findAllByWorkflowInstanceId(anyString())).thenReturn(Collections.emptyList());
    when(objectConverter.convertCollection(anyList(), eq(ActivityInstanceView.class))).thenReturn(
        List.of(view1, view2));

    // mock graph
    WorkflowNode activity1 = new WorkflowNode();
    SendMessage sendMessage = new SendMessage();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity1.activity(sendMessage);
    activity1.id("activity1");

    WorkflowNode activity2 = new WorkflowNode();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity2.activity(sendMessage);
    activity2.id("activity2");

    WorkflowDirectGraph directGraph = new WorkflowDirectGraph();
    directGraph.registerToDictionary("activity1", activity1);
    directGraph.registerToDictionary("activity2", activity2);

    when(workflowDirectGraphCachingService.getDirectGraph("workflow")).thenReturn(directGraph);

    // when
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
            () -> service.listWorkflowInstanceActivities("workflow", "instance"))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(
            "Either no workflow deployed with id 'workflow' is found or the instance id 'instance' is not correct"));
  }

  @Test
  void listWorkflowInstanceActivities_badWorkflowId_illegalArgumentException() {
    // given
    ActivityInstanceView view1 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity1")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value1")).build();
    ActivityInstanceView view2 = ActivityInstanceView.builder()
        .instanceId("instance")
        .activityId("activity2")
        .workflowId("workflow")
        .endDate(Instant.now())
        .startDate(Instant.now())
        .duration(Duration.ofMillis(2000))
        .type(TaskTypeEnum.MESSAGE_RECEIVED_EVENT)
        .outputs(Maps.newHashMap("key", "value2")).build();

    when(activityQueryRepository.findAllByWorkflowInstanceId(anyString())).thenReturn(Collections.singletonList(
        ActivityInstanceDomain.builder()
            .build())); // returns at least one item, otherwise an IllegalArgumentException will be thrown
    when(objectConverter.convertCollection(anyList(), eq(ActivityInstanceView.class))).thenReturn(
        List.of(view1, view2));

    // mock graph
    when(workflowDirectGraphCachingService.getDirectGraph("workflow")).thenReturn(null);

    // when
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(
            () -> service.listWorkflowInstanceActivities("workflow", "instance"))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(
            "Either no workflow deployed with id 'workflow' is found or the instance id 'instance' is not correct"));
  }

  @Test
  void getWorkflowDefinition() {
    // mock graph
    WorkflowNode activity1 = new WorkflowNode();
    SendMessage sendMessage = new SendMessage();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity1.activity(sendMessage);
    activity1.id("activity1");

    WorkflowNode activity2 = new WorkflowNode();
    sendMessage.setContent("content");
    sendMessage.setId("activity1");
    activity2.activity(sendMessage);
    activity2.id("activity2");

    WorkflowDirectGraph directGraph = new WorkflowDirectGraph();
    directGraph.registerToDictionary("activity1", activity1);
    directGraph.registerToDictionary("activity2", activity2);
    directGraph.addParent("activity2", "activity1");
    directGraph.getChildren("activity1").addChild("activity2");

    when(workflowDirectGraphCachingService.getDirectGraph("workflow")).thenReturn(directGraph);

    // when
    WorkflowDefinitionView definitionView = service.getWorkflowDefinition("workflow");

    // then
    then(definitionView.getWorkflowId()).isEqualTo("workflow");
    then(definitionView.getFlowNodes()).hasSize(2);
    then(definitionView.getFlowNodes().get(0).getChildren()).hasSize(1);
    then(definitionView.getFlowNodes().get(1).getParents()).hasSize(1);
    then(definitionView.getFlowNodes().get(0).getType()).isEqualTo(TaskTypeEnum.SEND_MESSAGE_ACTIVITY);
  }
}