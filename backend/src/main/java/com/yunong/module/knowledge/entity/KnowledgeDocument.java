package com.yunong.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yunong.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_documents")
public class KnowledgeDocument extends BaseEntity {

    @TableId
    private String id;
    private String title;
    private String content;
    private String category;
    private String tags;
    private String sourceUrl;
    private Integer version;
    private String status;
    private String authorId;
    private String embedding; // pgvector vector(1536) 存储为字符串
}
