package com.symphony.bdk.workflow.admin;

import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.gen.api.model.V4MessageSent;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.Workflow;
import com.symphony.bdk.workflow.validators.YamlValidator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Base64;

@Component
public class BotAdminController {

    private final MessageService messageService;
    private final WorkflowBuilder workflowBuilder;

    public BotAdminController(MessageService messageService, WorkflowBuilder workflowBuilder) {
        this.messageService = messageService;
        this.workflowBuilder = workflowBuilder;
    }

    @EventListener
    public void onMessageSent(RealTimeEvent<V4MessageSent> event)
        throws IOException, ProcessingException {
        // consider a message with an attachment as a workflow to run
        if (!event.getSource().getMessage().getAttachments().isEmpty()) {
            String streamId = event.getSource().getMessage().getStream().getStreamId();
            String messageId = event.getSource().getMessage().getMessageId();
            String attachmentId = event.getSource().getMessage().getAttachments().get(0).getId();

            byte[] attachment = messageService.getAttachment(streamId, messageId, attachmentId);
            byte[] decodedAttachment = Base64.getDecoder().decode(attachment);

            ProcessingReport report = YamlValidator.validateYamlString(new String(decodedAttachment));

            if (report.isSuccess()) {
                messageService.send(streamId,
                    "<messageML>Yaml file is <b>VALID.</b></messageML>");
            } else {
                messageService.send(streamId,
                    "<messageML>Yaml file is <b>NOT VALID!</b></messageML>");
            }

        }
    }

    /*public void onMessageSent(RealTimeEvent<V4MessageSent> event) throws IOException {
        // consider a message with an attachment as a workflow to run
        if (!event.getSource().getMessage().getAttachments().isEmpty()) {
            String streamId = event.getSource().getMessage().getStream().getStreamId();
            String messageId = event.getSource().getMessage().getMessageId();
            String attachmentId = event.getSource().getMessage().getAttachments().get(0).getId();

            byte[] attachment = messageService.getAttachment(streamId, messageId, attachmentId);
            byte[] decodedAttachment = Base64.getDecoder().decode(attachment);

            Workflow workflow = deserializeWorkflow(decodedAttachment);
            workflowBuilder.addWorkflow(workflow);

            messageService.send(streamId,
                    "<messageML>Ok, running workflow <b>" + workflow.getName() + "</b></messageML>");
        }
    }

    private Workflow deserializeWorkflow(byte[] workflow) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory()
                .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true));
        return mapper.readValue(workflow, Workflow.class);
    }*/

}
