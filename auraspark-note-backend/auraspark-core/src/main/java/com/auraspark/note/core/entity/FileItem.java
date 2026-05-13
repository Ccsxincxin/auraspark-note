package com.auraspark.note.core.entity;

import com.auraspark.note.core.config.HashIdConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("files")
public class FileItem {
    @TableId(type = IdType.ASSIGN_ID)
    @HashIdConfig.EncodedId
    private Long id;
    @HashIdConfig.EncodedId
    private Long userId;
    private String name;
    private Boolean isFolder;
    @HashIdConfig.EncodedId
    private Long parentId;
    private String format;
    private Long size;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
