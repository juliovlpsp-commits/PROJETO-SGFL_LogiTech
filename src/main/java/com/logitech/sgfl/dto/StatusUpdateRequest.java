package com.logitech.sgfl.dto;

import com.logitech.sgfl.enums.StatusEntrega;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {

    @NotNull(message = "O novo estado é obrigatório")
    private StatusEntrega status;

    public StatusEntrega getStatus() { return status; }
    public void setStatus(StatusEntrega status) { this.status = status; }
}
