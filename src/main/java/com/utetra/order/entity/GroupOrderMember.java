package com.utetra.order.entity;
import jakarta.persistence.*;
@Entity @Table(name="group_order_members",uniqueConstraints=@UniqueConstraint(columnNames={"group_order_id","user_id"}))
public class GroupOrderMember {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="group_order_id",nullable=false) public Long groupOrderId;
 @Column(name="user_id",nullable=false) public Long userId;
 @Column(name="display_name",nullable=false,length=100) public String displayName;
 @Column(name="is_confirmed",nullable=false) public boolean confirmed;
 @Column(name="order_id") public Long orderId;
 @Column(name="billed_subtotal",precision=14,scale=2) public java.math.BigDecimal billedSubtotal;
 @Column(name="discount_share",precision=14,scale=2) public java.math.BigDecimal discountShare;
 @Column(name="shipping_share",precision=14,scale=2) public java.math.BigDecimal shippingShare;
 @Column(name="amount_due",precision=14,scale=2) public java.math.BigDecimal amountDue;
 @Lob @Column(name="items_json",nullable=false,columnDefinition="TEXT") public String itemsJson;
}
