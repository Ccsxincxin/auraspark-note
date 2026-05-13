package com.auraspark.note.core.auth.dto;

import com.auraspark.note.ai.entity.Message;
import lombok.Data;
import java.util.List;

@Data
public class BranchMessages {
    private int branch;
    private boolean active;
    private List<Message> messages;
}

