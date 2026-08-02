package com.vetsoftware.app.goodsreceipt.domain;

public class GoodsReceiptNotFoundException extends RuntimeException {
  public GoodsReceiptNotFoundException(Long id) {
    super("Goods receipt not found: " + id);
  }
}
