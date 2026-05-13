package com.auraspark.note.core.mapper;

import com.auraspark.note.core.entity.FileItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FileMapper extends BaseMapper<FileItem> {
}
