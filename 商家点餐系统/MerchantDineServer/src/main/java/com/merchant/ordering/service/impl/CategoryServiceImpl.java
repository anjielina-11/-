package com.merchant.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.merchant.ordering.entity.Category;
import com.merchant.ordering.mapper.CategoryMapper;
import com.merchant.ordering.service.CategoryService;
import org.springframework.stereotype.Service;

/**
 * 商品分类 Service 实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
}
