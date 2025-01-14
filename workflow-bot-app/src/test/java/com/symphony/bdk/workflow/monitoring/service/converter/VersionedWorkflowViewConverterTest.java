package com.symphony.bdk.workflow.monitoring.service.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.workflow.api.v1.dto.WorkflowView;
import com.symphony.bdk.workflow.versioning.model.VersionedWorkflow;

import org.junit.jupiter.api.Test;

class VersionedWorkflowViewConverterTest {

  @Test
  void apply() {
    // given
    VersionedWorkflow domain = new VersionedWorkflow();
    domain.setWorkflowId("workflow");
    domain.setVersion(1234L);
    domain.setCreatedBy(1234L);

    //when
    VersionedWorkflowViewConverter converter = new VersionedWorkflowViewConverter();
    WorkflowView view = converter.apply(domain);

    // then
    assertThat(view.getId()).isEqualTo("workflow");
    assertThat(view.getVersion()).isEqualTo(1234L);
    assertThat(view.getCreatedBy()).isEqualTo(1234L);
  }
}
