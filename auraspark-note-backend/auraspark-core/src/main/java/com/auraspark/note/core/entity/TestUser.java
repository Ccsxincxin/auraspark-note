package com.auraspark.note.core.entity;

import com.auraspark.note.core.config.HashIdConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("test_user")
public class TestUser {
    @TableId
    @HashIdConfig.EncodedId
    private Long id;
    private String name;
    private String email;
}