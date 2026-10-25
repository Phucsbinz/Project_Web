<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Đặt hàng</title></head><body>
<jsp:include page="nav.jsp"/>
<span class="eyebrow">THE LAST STEP TO A GOOD SIP</span><h1 class="h3">Xác nhận đơn hàng</h1><div id="order-message" hidden role="alert"></div>
<form id="checkout-form" class="card card-body gap-3" data-store-id="${availability.storeId}">
<div id="checkout-availability" class="small text-secondary" aria-live="polite"></div>
<label>Địa chỉ nhận hàng<select name="addressId" class="form-select" required><option value="">Chọn địa chỉ</option><c:forEach items="${choices.addresses}" var="a"><option value="${a.id}"><c:out value="${a.recipient_name}"/> — <c:out value="${a.address_detail}"/></option></c:forEach></select></label>
<label>Khu vực giao<select name="areaId" class="form-select" required><option value="">Chọn khu vực</option><c:forEach items="${choices.areas}" var="a"><option value="${a.id}"><c:out value="${a.name}"/></option></c:forEach></select></label>
<label>Nhà vận chuyển<select name="carrierId" class="form-select" required><option value="">Chọn nhà vận chuyển</option><c:forEach items="${choices.carriers}" var="c"><option value="${c.id}"><c:out value="${c.name}"/></option></c:forEach></select></label>
<label>Ưu đãi món<select name="productPromotionId" class="form-select"><option value="">Không dùng</option><c:forEach items="${choices.promotions}" var="p"><c:if test="${p.kind == 'PRODUCT_PERCENT'}"><option value="${p.id}"><c:out value="${p.name}"/></option></c:if></c:forEach></select></label>
<label>Ưu đãi phí giao<select name="shippingPromotionId" class="form-select"><option value="">Không dùng</option><c:forEach items="${choices.promotions}" var="p"><c:if test="${p.kind == 'SHIPPING_PERCENT'}"><option value="${p.id}"><c:out value="${p.name}"/></option></c:if></c:forEach></select></label>
<label>Điểm sử dụng (1 điểm = 1.000đ, tối đa 20%)<input class="form-control" name="points" type="number" min="0" value="0"></label>
<label>Ghi chú<input class="form-control" name="note" maxlength="500"></label>
<label>Phương thức thanh toán<select class="form-select" name="paymentMethod"><option value="COD">Thanh toán khi nhận hàng (COD)</option><c:if test="${bankEnabled}"><option value="BANK_TRANSFER">Chuyển khoản Techcombank bằng QR</option></c:if><c:if test="${vnpayEnabled}"><option value="VNPAY">Thanh toán trực tuyến VNPAY</option></c:if></select></label>
<p>Chọn chuyển khoản: QR, số tiền và nội dung chuyển sẽ hiển thị sau khi đặt đơn. Đơn chờ Admin đối soát trước khi cửa hàng xác nhận. Có thể yêu cầu trả hàng trong 3 tiếng sau giao thành công theo chính sách.</p>
<button class="btn btn-outline-primary" type="button" id="quote">Xem báo giá</button><div id="quote-detail" class="alert alert-info" hidden></div><button class="btn btn-primary" type="submit" id="confirm-order" disabled>Xác nhận báo giá và đặt đơn</button>
</form><script src="<c:url value='/assets/js/orders.js'/>"></script><script>
const form = document.querySelector('#checkout-form');
const storageKey = 'utetra.checkout.key';
let key = sessionStorage.getItem(storageKey) || crypto.randomUUID(); sessionStorage.setItem(storageKey, key);
let quoteHash = ''; let quoteGeneration=0;
function payload() {const f = new FormData(form);return {key, addressId:Number(f.get('addressId')), areaId:Number(f.get('areaId')), carrierId:Number(f.get('carrierId')), productPromotionId:f.get('productPromotionId')?Number(f.get('productPromotionId')):null, shippingPromotionId:f.get('shippingPromotionId')?Number(f.get('shippingPromotionId')):null, points:Number(f.get('points')), note:f.get('note'), paymentMethod:f.get('paymentMethod'), quoteHash};}
form.addEventListener('change',()=>{quoteHash='';quoteGeneration++;document.querySelector('#confirm-order').disabled=true;document.querySelector('#quote-detail').hidden=true;});
document.querySelector('#quote').addEventListener('click',async()=>{if(!form.reportValidity())return;try {const generation=quoteGeneration;const q=await orders.send('/checkout/quote',payload());if(generation!==quoteGeneration)return;quoteHash=q.hash;const detail=document.querySelector('#quote-detail');detail.textContent='Tiền hàng: '+q.subtotal+'đ; giảm món: '+q.productDiscount+'đ; giảm điểm: '+q.pointsDiscount+'đ; phí giao: '+q.shipping+'đ; giảm giao: '+q.shippingDiscount+'đ. Tổng thanh toán: '+q.total+'đ';detail.hidden=false;document.querySelector('#confirm-order').disabled=false;}catch(e){orders.message(e.message,true);}});
form.addEventListener('submit',async event=>{event.preventDefault();const button=document.querySelector('#confirm-order');button.disabled=true;try {const order=await orders.send('/checkout',payload());sessionStorage.removeItem(storageKey);location.href=(document.querySelector('meta[name="app-context"]')?.content||'')+'/orders/'+order.id;}catch(e){orders.message(e.message,true);button.disabled=false;}});
</script><script src="<c:url value='/assets/js/checkout-availability.js'/>"></script></body></html>
