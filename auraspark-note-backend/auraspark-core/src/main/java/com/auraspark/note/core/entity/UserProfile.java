package com.auraspark.note.core.entity;

import com.auraspark.note.core.config.HashIdConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_profiles")
public class UserProfile {
    @TableId(type = IdType.ASSIGN_ID)
    @HashIdConfig.EncodedId
    private Long id;
    @HashIdConfig.EncodedId
    private Long userId;
    private String nickname;
    private String avatar;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
