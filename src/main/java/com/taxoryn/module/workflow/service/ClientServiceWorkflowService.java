package com.taxoryn.module.workflow.service;

import com.taxoryn.core.response.PagedResponse;
import com.taxoryn.module.client.entity.ClientServiceType;
import com.taxoryn.module.workflow.dto.AssignWorkflowRequest;
import com.taxoryn.module.workflow.dto.ClientServicePeriodDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowDto;
import com.taxoryn.module.workflow.dto.ClientServiceWorkflowStepDto;
import com.taxoryn.module.workflow.dto.CreateServicePeriodRequest;
import com.taxoryn.module.workflow.dto.GenerateWorkflowRequest;
import com.taxoryn.module.workflow.dto.ServiceWorkflowTemplateDto;
import com.taxoryn.module.workflow.dto.UpdateWorkflowPriorityRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStatusRequest;
import com.taxoryn.module.workflow.dto.UpdateWorkflowStepStatusRequest;
import com.taxoryn.module.workflow.dto.WorkflowFilterRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ClientServiceWorkflowService {

    ClientServicePeriodDto createServicePeriod(CreateServicePeriodRequest request);

    List<ClientServicePeriodDto> getServicePeriods(UUID clientServiceId);

    ClientServiceWorkflowDto generateWorkflow(GenerateWorkflowRequest request);

    ClientServiceWorkflowDto getWorkflowById(UUID workflowId);

    ClientServiceWorkflowDto getActiveWorkflowForService(UUID clientServiceId);

    List<ClientServiceWorkflowDto> getWorkflowsForService(UUID clientServiceId);

    PagedResponse<ClientServiceWorkflowDto> getWorklist(WorkflowFilterRequest filter, Pageable pageable);

    ClientServiceWorkflowDto updateWorkflowStatus(UUID workflowId, UpdateWorkflowStatusRequest request);

    ClientServiceWorkflowStepDto updateStepStatus(UUID workflowId, UUID stepId, UpdateWorkflowStepStatusRequest request);

    ClientServiceWorkflowDto assignWorkflow(UUID workflowId, AssignWorkflowRequest request);

    ClientServiceWorkflowDto assignStep(UUID workflowId, UUID stepId, AssignWorkflowRequest request);

    ClientServiceWorkflowDto updateWorkflowPriority(UUID workflowId, UpdateWorkflowPriorityRequest request);

    List<ServiceWorkflowTemplateDto> getWorkflowTemplates(ClientServiceType serviceType);
}
