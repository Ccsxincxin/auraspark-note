package com.auraspark.note.core.entity;

import com.auraspark.note.core.config.HashIdConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    @HashIdConfig.EncodedId
    private Long id;
    private String email;
    private String phone;
    private String password;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
