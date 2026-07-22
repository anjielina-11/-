package com.merchant.ordering.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.merchant.ordering.dto.ProductVO;
import com.merchant.ordering.entity.Product;

import java.util.List;

/**
 * 商品 Service 接口
 */
public interface ProductService extends IService<Product> {

    /**
     * 获取所有商品（含分类名称）
     */
    List<ProductVO> listWithCategoryName();
}
