package com.merchant.ordering.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.merchant.ordering.dto.ProductVO;
import com.merchant.ordering.entity.Category;
import com.merchant.ordering.entity.Product;
import com.merchant.ordering.mapper.CategoryMapper;
import com.merchant.ordering.mapper.ProductMapper;
import com.merchant.ordering.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品 Service 实现
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public List<ProductVO> listWithCategoryName() {
        // 查询所有商品
        List<Product> products = this.lambdaQuery()
                .orderByDesc(Product::getCreateTime)
                .list();

        // 查询所有分类，构建 id -> name 映射
        Map<Long, String> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (k1, k2) -> k1));

        // 转换为 VO
        return products.stream().map(p -> {
            ProductVO vo = new ProductVO();
            vo.setId(p.getId());
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryMap.get(p.getCategoryId()));
            vo.setName(p.getName());
            vo.setDescription(p.getDescription());
            vo.setPrice(p.getPrice());
            vo.setImage(p.getImage());
            vo.setStock(p.getStock());
            vo.setSales(p.getSales());
            vo.setStatus(p.getStatus());
            vo.setCreateTime(p.getCreateTime());
            vo.setUpdateTime(p.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }
}
