package com.auraspark.note.ai.entity;

import com.auraspark.note.core.config.HashIdConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {
    @TableId(type = IdType.ASSIGN_ID)
    @HashIdConfig.EncodedId
    private Long id;
    @HashIdConfig.EncodedId
    private Long conversationId;
    private String role;
    private String content;
    private Integer tokens;
    private Boolean compressed;
    private Integer branch;
    @HashIdConfig.EncodedId
    private Long versionOf;
    private Boolean deleted;
    private LocalDateTime createdAt;
}

