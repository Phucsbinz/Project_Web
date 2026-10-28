package com.utetra.order.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.utetra.order.dto.OrderRequests.*;
import com.utetra.order.entity.*;
import com.utetra.order.repository.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service @Transactional(isolation=Isolation.READ_COMMITTED)
public class GroupOrderService {
 private final OrderDataRepository data; private final CommerceReferenceRepository refs;
 private final OrderWorkflowService workflow; private final BankTransferSettings bank; private final CartService carts; private final OrderSupport support; private final ObjectMapper json; private final Clock clock;
 public GroupOrderService(OrderDataRepository data,CommerceReferenceRepository refs,CartService carts,OrderSupport support,ObjectMapper json,Clock clock,BankTransferSettings bank,OrderWorkflowService workflow) {
  this.workflow=workflow;this.bank=bank;this.data=data;this.refs=refs;this.carts=carts;this.support=support;this.json=json;this.clock=clock;
 }
 private GroupOrder group(String token,boolean lock) {
  try {UUID.fromString(token);}catch(Exception e){throw OrderProblem.missing();}
  return data.query(GroupOrder.class,"where e.inviteToken=:token",Map.of("token",token),lock,1).stream().findFirst().orElseThrow(OrderProblem::missing);
 }
 private List<GroupOrderMember> members(GroupOrder g){return data.query(GroupOrderMember.class,"where e.groupOrderId=:id order by e.id",Map.of("id",g.id),false,51);}
 private void touch(GroupOrder g){g.updatedAt=clock.instant().isAfter(g.updatedAt)?clock.instant():g.updatedAt.plusNanos(1000);}
 private void open(GroupOrder g){if(!"OPEN".equals(g.status)||!g.expiresAt.isAfter(clock.instant()))throw OrderProblem.conflict("Nhóm đã chốt, hủy hoặc hết hạn.");}
 private void host(Actor a,GroupOrder g){if(!g.hostUserId.equals(a.userId()))throw OrderProblem.forbidden();}
 private void version(GroupOrder g,long version){if(g.version!=version)throw OrderProblem.conflict("Nhóm đã thay đổi. Hãy tải lại trước khi tiếp tục.");}
 private List<Item> items(GroupOrderMember m){try{return json.readValue(m.itemsJson,new TypeReference<List<Item>>(){});}catch(Exception e){throw new IllegalStateException("Invalid saved group selection",e);}}
 public static int percent(int members){return members>=10?15:members>=5?10:members>=3?5:0;}
 public static BigDecimal discount(BigDecimal subtotal,int participants){return subtotal.multiply(BigDecimal.valueOf(percent(participants))).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN).min(BigDecimal.valueOf(100000));}
 public Map<String,Object> create(Actor actor){return create(actor,"HOST");}
 public Map<String,Object> create(Actor actor,String paymentMode) {
  if(!Set.of("HOST","INDIVIDUAL").contains(paymentMode))throw OrderProblem.invalid("Cách thanh toán nhóm không hợp lệ.");
  support.buyer(actor);refs.user(actor.userId(),true);
  if(data.query(GroupOrder.class,"where e.hostUserId=:user and e.status in ('OPEN','LOCKED') and e.expiresAt>:now",Map.of("user",actor.userId(),"now",clock.instant()),false,5).size()>=5)throw OrderProblem.conflict("Bạn có tối đa 5 nhóm đang hoạt động. Hủy hoặc hoàn tất nhóm cũ trước.");
  var g=new GroupOrder();g.hostUserId=actor.userId();g.paymentMode=paymentMode;g.inviteToken=UUID.randomUUID().toString();g.createdAt=clock.instant();g.updatedAt=g.createdAt;g.expiresAt=g.createdAt.plus(Duration.ofHours(24));data.save(g);data.flush();
  enroll(actor,g);return view(actor,g.inviteToken);
 }
 private GroupOrderMember member(Actor actor,GroupOrder g){return members(g).stream().filter(m->m.userId.equals(actor.userId())).findFirst().orElseThrow(()->OrderProblem.invalid("Hãy tham gia nhóm trước khi chọn món."));}
 private void enroll(Actor actor,GroupOrder g){
  var saved=members(g);if(saved.stream().anyMatch(m->m.userId.equals(actor.userId()))){touch(g);data.flush();return;}
  if(saved.size()>=50)throw OrderProblem.invalid("Nhóm tối đa 50 thành viên.");
  var member=new GroupOrderMember();member.groupOrderId=g.id;member.userId=actor.userId();member.displayName=refs.one("select full_name from users where id=?",actor.userId()).get("full_name").toString();member.itemsJson="[]";member.confirmed=false;data.save(member);touch(g);data.flush();
 }
 private void saveSelection(Actor actor,GroupOrder g,List<Item> selection){
  var member=member(actor,g);var saved=members(g);
  int total=selection.stream().mapToInt(Item::quantity).sum()+saved.stream().filter(m->!m.userId.equals(actor.userId())).flatMap(m->items(m).stream()).mapToInt(Item::quantity).sum();
  Set<Long> owners=new HashSet<>();
  var combined=new ArrayList<Item>(selection);saved.stream().filter(m->!m.userId.equals(actor.userId())).forEach(m->combined.addAll(items(m)));
  var variantIds=combined.stream().map(Item::variantId).distinct().toList();
  if(!variantIds.isEmpty())refs.list("select distinct s.owner_user_id from product_variants v join products p on p.id=v.product_id join stores s on s.id=p.store_id where v.id in ("+String.join(",",Collections.nCopies(variantIds.size(),"?"))+")",variantIds.toArray()).forEach(row->owners.add(CommerceReferenceRepository.number(row,"owner_user_id")));
  if(owners.size()>1)throw OrderProblem.invalid("Đơn nhóm chỉ nhận món cùng một thương hiệu để có thể chọn chung chi nhánh.");
  if(total>50)throw OrderProblem.invalid("Tổng giỏ nhóm tối đa 50 ly. Hãy giảm số lượng trước khi gửi.");
  try{member.itemsJson=json.writeValueAsString(selection);}catch(Exception e){throw new IllegalStateException(e);}
  if(member.itemsJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>60000)throw OrderProblem.invalid("Lựa chọn quá dài. Hãy rút gọn ghi chú.");
  member.confirmed=!selection.isEmpty();touch(g);data.flush();
 }
 public Map<String,Object> addItem(Actor actor,String token,long version,Item raw){
  support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);version(g,version);open(g);
  var current=new ArrayList<>(items(member(actor,g)));var item=carts.validateDraft(raw);String hash=carts.configuration(item);int index=-1;
  for(int n=0;n<current.size();n++)if(carts.configuration(current.get(n)).equals(hash)){index=n;break;}
  if(index>=0){var previous=current.get(index);int qty=previous.quantity()+item.quantity();if(qty>20)throw OrderProblem.invalid("Mỗi dòng tối đa 20 ly.");current.set(index,new Item(item.variantId(),item.sugar(),item.ice(),item.toppingIds(),qty,item.note()));}else current.add(item);
  saveSelection(actor,g,current);return view(actor,token);
 }
 public Map<String,Object> removeItem(Actor actor,String token,long version,String configuration){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);version(g,version);open(g);var current=new ArrayList<>(items(member(actor,g)));if(!current.removeIf(i->carts.configuration(i).equals(configuration)))throw OrderProblem.missing();saveSelection(actor,g,current);return view(actor,token);}
 public Map<String,Object> confirm(Actor actor,String token,long version){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);version(g,version);open(g);var m=member(actor,g);var current=items(m);if(current.isEmpty())throw OrderProblem.invalid("Chọn ít nhất một món trước khi xác nhận.");for(var item:current)carts.validateDraft(item);m.confirmed=true;touch(g);data.flush();return view(actor,token);}
 public Map<String,Object> join(Actor actor,String token,long version){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);version(g,version);open(g);enroll(actor,g);return view(actor,token);}
 public Map<String,Object> leave(Actor actor,String token,long version){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);version(g,version);open(g);if(g.hostUserId.equals(actor.userId()))throw OrderProblem.invalid("Chủ nhóm cần hủy nhóm thay vì rời nhóm.");members(g).stream().filter(m->m.userId.equals(actor.userId())).findFirst().ifPresent(data::remove);touch(g);data.flush();return view(actor,token);}
 public Map<String,Object> lock(Actor actor,String token,long version) {
  support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);host(actor,g);version(g,version);open(g);
  var all=members(g).stream().filter(m->!items(m).isEmpty()).toList();if(all.size()<2)throw OrderProblem.invalid("Cần ít nhất hai người có món để đặt nhóm.");
  for(var m:all)refs.user(m.userId,false);
  var cart=data.cart(actor.userId(),true);if(cart!=null&&cart.groupOrderId!=null)throw OrderProblem.conflict("Giỏ đang thuộc nhóm khác.");
  // Replace only the host cart. Other members keep their independent carts.
  carts.replaceDraftForGroup(actor,all.stream().flatMap(m->items(m).stream()).toList());
  cart=data.cart(actor.userId(),true);cart.groupOrderId=g.id;g.status="LOCKED";data.flush();return view(actor,token);
 }
 public Map<String,Object> paymentMode(Actor actor,String token,long version,String mode){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);host(actor,g);version(g,version);open(g);if(!Set.of("HOST","INDIVIDUAL").contains(mode))throw OrderProblem.invalid("Cách thanh toán không hợp lệ.");g.paymentMode=mode;data.flush();return view(actor,token);}
 public Map<String,Object> cancel(Actor actor,String token,long version){support.buyer(actor);refs.user(actor.userId(),true);var g=group(token,true);host(actor,g);version(g,version);if(Set.of("CANCELLED","EXPIRED").contains(g.status))return view(actor,token);if("EXPIRED".equals(effectiveStatus(g))){g.status="EXPIRED";touch(g);var expiredCart=data.cart(actor.userId(),true);if(expiredCart!=null&&g.id.equals(expiredCart.groupOrderId)){expiredCart.groupOrderId=null;data.clear(expiredCart);}data.flush();return view(actor,token);}if("ORDERED".equals(g.status)){var order=data.require(Order.class,g.orderId,true);if(!"CANCELLED".equals(order.status))workflow.cancel(actor,order.id,order.version,"Chủ nhóm hủy "+code(g.id));}g.status="CANCELLED";touch(g);var cart=data.cart(actor.userId(),true);if(cart!=null&&g.id.equals(cart.groupOrderId)){cart.groupOrderId=null;data.clear(cart);}data.flush();return view(actor,token);}
 private static String code(long id){return String.format(Locale.ROOT,"UTG-%05d",id);}
 private String effectiveStatus(GroupOrder group){if(Set.of("OPEN","LOCKED").contains(group.status)){if(!group.expiresAt.isAfter(clock.instant()))return "EXPIRED";for(var row:refs.list("select updated_at from carts where group_order_id=?",group.id))if(!CommerceReferenceRepository.instant(row.get("updated_at")).plus(OrderDataRepository.CART_TTL).isAfter(clock.instant()))return "EXPIRED";}if("ORDERED".equals(group.status)&&group.orderId!=null&&"CANCELLED".equals(data.require(Order.class,group.orderId,false).status))return "CANCELLED";return group.status;}
 public List<Map<String,Object>> mine(Actor actor){return history(actor).stream().filter(group->!Set.of("CANCELLED","EXPIRED").contains(group.get("status"))).toList();}
 private List<Map<String,Object>> history(Actor actor){support.buyer(actor);refs.requireCurrent(actor,false);var result=new ArrayList<Map<String,Object>>();for(var row:refs.list("select g.*,u.full_name as host_name,c.updated_at as cart_updated_at,o.status as order_status,(select count(*) from group_order_members total where total.group_order_id=g.id) as member_count from group_orders g join users u on u.id=g.host_user_id left join carts c on c.group_order_id=g.id left join orders o on o.id=g.order_id where exists(select 1 from group_order_members m where m.group_order_id=g.id and m.user_id=?) order by g.id desc limit 20",actor.userId())){String status=row.get("status").toString();if(Set.of("OPEN","LOCKED").contains(status)&&(!CommerceReferenceRepository.instant(row.get("expires_at")).isAfter(clock.instant())||row.get("cart_updated_at")!=null&&!CommerceReferenceRepository.instant(row.get("cart_updated_at")).plus(OrderDataRepository.CART_TTL).isAfter(clock.instant())))status="EXPIRED";if("ORDERED".equals(status)&&"CANCELLED".equals(row.get("order_status")))status="CANCELLED";var item=new LinkedHashMap<String,Object>();long id=((Number)row.get("id")).longValue();item.put("token",row.get("invite_token"));item.put("code",code(id));item.put("title","Nhóm của "+Objects.toString(row.get("host_name"),"bạn"));item.put("hostName",row.get("host_name"));item.put("host",((Number)row.get("host_user_id")).longValue()==actor.userId());item.put("memberCount",row.get("member_count"));item.put("createdLabel",java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(CommerceReferenceRepository.instant(row.get("created_at"))));item.put("status",status);item.put("statusLabel",switch(status){case "OPEN"->"Đang chọn món";case "LOCKED"->"Chờ đặt đơn";case "ORDERED"->"Đã đặt đơn";case "EXPIRED"->"Đã hết hạn";default->"Đã hủy";});result.add(item);}return result;}
 @Transactional(readOnly=true) public List<Map<String,Object>> lifecycleNotices(Actor actor){return history(actor).stream().filter(group->Set.of("CANCELLED","EXPIRED").contains(group.get("status"))).limit(5).map(group->{var notice=new LinkedHashMap<String,Object>(group);String refundState="NONE";notice.put("refundPending",false);notice.put("refunded",false);
    if("CANCELLED".equals(group.get("status")))for(var row:refs.list("select p.status,p.amount,o.payment_status,o.cancelled_at from local_qr_payments p join orders o on o.id=p.order_id join group_orders g on g.order_id=o.id where g.invite_token=? and p.user_id=?",group.get("token"),actor.userId())){boolean pending="SUCCESS".equals(row.get("status"))&&"REFUND_PENDING".equals(row.get("payment_status"))&&CommerceReferenceRepository.money(row,"amount").signum()>0;boolean refunded="REFUNDED".equals(row.get("status"))&&CommerceReferenceRepository.money(row,"amount").signum()>0;notice.put("refundPending",pending);notice.put("refunded",refunded);if(pending||refunded){notice.put("refundAmount",row.get("amount"));refundState=pending?"REFUND_PENDING":"REFUNDED";}if(pending&&row.get("cancelled_at")!=null)notice.put("refundExpectedAt",CommerceReferenceRepository.instant(row.get("cancelled_at")).plus(Duration.ofMinutes(30)).toString());}
    notice.put("noticeKey",actor.userId()+":"+group.get("token")+":"+group.get("status")+("NONE".equals(refundState)?"":":"+refundState));return (Map<String,Object>)notice;}).toList();}
 public void expireGroups(){for(var row:refs.list("select id,host_user_id from group_orders where status in ('OPEN','LOCKED') and expires_at<=? order by id limit 100",clock.instant())){long user=((Number)row.get("host_user_id")).longValue();refs.one("select id from users where id=? for update",user);var g=data.require(GroupOrder.class,((Number)row.get("id")).longValue(),true);if(Set.of("OPEN","LOCKED").contains(g.status)&&!g.expiresAt.isAfter(clock.instant())){g.status="EXPIRED";touch(g);var cart=data.cart(user,true);if(cart!=null&&g.id.equals(cart.groupOrderId)){cart.groupOrderId=null;data.clear(cart);}}}data.flush();}
 @Transactional(readOnly=true) public Map<String,Object> view(Actor actor,String token) {
  support.buyer(actor);var g=group(token,false);var all=members(g);String state=effectiveStatus(g);if(Set.of("CANCELLED","EXPIRED").contains(state)){Map<String,Object> closed=new LinkedHashMap<>();closed.put("token",token);closed.put("version",g.version);closed.put("status",state);closed.put("expired","EXPIRED".equals(state));closed.put("code",code(g.id));closed.put("joined",false);closed.put("host",g.hostUserId.equals(actor.userId()));closed.put("members",List.of());closed.put("memberCount",0);closed.put("participants",0);closed.put("canCancel",false);return closed;}
  var selections=new LinkedHashMap<Long,List<Item>>();all.forEach(m->selections.put(m.userId,items(m)));
  var variantIds=selections.values().stream().flatMap(Collection::stream).map(Item::variantId).distinct().toList();
  var topIds=selections.values().stream().flatMap(Collection::stream).flatMap(i->i.toppingIds().stream()).distinct().toList();
  Map<Long,Map<String,Object>> variants=new HashMap<>(),tops=new HashMap<>();
  if(!variantIds.isEmpty())refs.list("select v.id,p.name,p.image_url,p.base_price,v.extra_price,v.size_name from product_variants v join products p on p.id=v.product_id where v.id in ("+String.join(",",Collections.nCopies(variantIds.size(),"?"))+")",variantIds.toArray()).forEach(v->variants.put(CommerceReferenceRepository.number(v,"id"),v));
  if(!topIds.isEmpty())refs.list("select id,name,price from toppings where id in ("+String.join(",",Collections.nCopies(topIds.size(),"?"))+")",topIds.toArray()).forEach(t->tops.put(CommerceReferenceRepository.number(t,"id"),t));
  Map<Long,String> paidStates=new HashMap<>();if(g.orderId!=null)for(var row:refs.list("select user_id,status from local_qr_payments where order_id=?",g.orderId))paidStates.put(((Number)row.get("user_id")).longValue(),row.get("status").toString());
  List<Map<String,Object>> people=new ArrayList<>();BigDecimal subtotal=BigDecimal.ZERO;
  for(var m:all){
   List<Map<String,Object>> lines=new ArrayList<>();BigDecimal memberTotal=BigDecimal.ZERO;
   for(var item:selections.get(m.userId)){
    var v=variants.get(item.variantId());var line=new LinkedHashMap<String,Object>();line.put("name",v==null?"Món không còn khả dụng":v.get("name"));line.put("size",v==null?"":v.get("size_name"));line.put("imageUrl",v==null?null:v.get("image_url"));line.put("configuration",carts.configuration(item));line.put("quantity",item.quantity());line.put("sugar",item.sugar());line.put("ice",item.ice());line.put("note",item.note());
    var unit=v==null?BigDecimal.ZERO:CommerceReferenceRepository.money(v,"base_price").add(CommerceReferenceRepository.money(v,"extra_price"));
    List<String> names=new ArrayList<>();for(long id:item.toppingIds()){var top=tops.get(id);if(top!=null){unit=unit.add(CommerceReferenceRepository.money(top,"price"));names.add(top.get("name").toString());}else names.add("Topping không còn khả dụng");}
    var total=unit.multiply(BigDecimal.valueOf(item.quantity()));line.put("subtotal",total);line.put("toppings",names);lines.add(line);memberTotal=memberTotal.add(total);
   }
   var person=new LinkedHashMap<String,Object>();person.put("paymentState",paidStates.getOrDefault(m.userId,""));person.put("name",m.displayName);person.put("host",m.userId.equals(g.hostUserId));person.put("me",m.userId.equals(actor.userId()));person.put("amountDue",m.amountDue);person.put("billedSubtotal",m.billedSubtotal);person.put("discount",m.discountShare);person.put("shippingShare",m.shippingShare);person.put("ordered",m.orderId!=null);person.put("orderId",m.orderId);person.put("confirmed",m.confirmed);person.put("cups",selections.get(m.userId).stream().mapToInt(Item::quantity).sum());person.put("items",lines);person.put("subtotal",memberTotal);people.add(person);subtotal=subtotal.add(memberTotal);
  }
  var result=new LinkedHashMap<String,Object>();result.put("paidCount",paidStates.values().stream().filter("SUCCESS"::equals).count());result.put("payerCount",paidStates.size());if(g.orderId!=null&&(g.hostUserId.equals(actor.userId())||all.stream().anyMatch(m->m.userId.equals(actor.userId())&&m.orderId!=null))){var order=data.require(Order.class,g.orderId,false);result.put("orderId",order.id);result.put("paymentMethod",order.paymentMethod);result.put("paymentStatus",order.paymentStatus);result.put("orderStatus",order.status);if("BANK_TRANSFER".equals(order.paymentMethod))result.put("bank",bank.instructions(g.orderId));}result.put("paymentMode",g.paymentMode);result.put("myOrdered",all.stream().anyMatch(m->m.userId.equals(actor.userId())&&m.orderId!=null));result.put("token",g.inviteToken);result.put("status",effectiveStatus(g));result.put("code",code(g.id));result.put("title","Nhóm "+code(g.id));result.put("canCancel",g.hostUserId.equals(actor.userId())&&(Set.of("OPEN","LOCKED").contains(g.status)||"ORDERED".equals(g.status)&&g.orderId!=null&&"NEW".equals(data.require(Order.class,g.orderId,false).status)));result.put("version",g.version);result.put("host",g.hostUserId.equals(actor.userId()));result.put("expired","EXPIRED".equals(effectiveStatus(g)));result.put("expiresAt",g.expiresAt.toString());int ready=(int)all.stream().filter(m->m.confirmed&&!items(m).isEmpty()).count();result.put("memberCount",all.size());result.put("participants",ready);result.put("ready",all.stream().anyMatch(m->m.userId.equals(actor.userId())&&m.confirmed));result.put("percent",percent(ready));result.put("joined",all.stream().anyMatch(m->m.userId.equals(actor.userId())));result.put("members",people);result.put("subtotal",subtotal);result.put("estimatedDiscount",discount(subtotal,ready));return result;
 }
 @Transactional(readOnly=true)
 public Map<String,Object> paymentPage(Actor actor,String token){
  support.buyer(actor);var g=group(token,false);
  boolean isHost=g.hostUserId.equals(actor.userId());
  var member=members(g).stream().filter(m->m.userId.equals(actor.userId())&&(isHost||m.orderId!=null))
      .findFirst().orElseThrow(OrderProblem::forbidden);
  if(!"ORDERED".equals(g.status)||g.orderId==null)throw OrderProblem.conflict("Nhóm chưa tạo đơn hàng.");
  var order=data.require(Order.class,g.orderId,false);
  boolean hostPays="HOST".equals(g.paymentMode);
  boolean payer=hostPays?isHost:member.orderId!=null;
  boolean hasShare=member.orderId!=null;
  Map<String,Object> result=new LinkedHashMap<>();
  result.put("token",token);result.put("name",member.displayName);result.put("orderId",order.id);
  result.put("orderCode",order.orderCode);result.put("paymentMethod",order.paymentMethod);
  result.put("paymentStatus",order.paymentStatus);result.put("orderStatus",order.status);result.put("payer",payer);
  result.put("subtotal",hostPays&&payer?order.subtotal:hasShare?member.billedSubtotal:BigDecimal.ZERO);
  result.put("discount",hostPays&&payer?order.productDiscount.add(order.pointsDiscount):hasShare?member.discountShare:BigDecimal.ZERO);
  result.put("shipping",hostPays&&payer?order.shippingGross.subtract(order.shippingDiscount):hasShare?member.shippingShare:BigDecimal.ZERO);
  result.put("amount",!payer?BigDecimal.ZERO:hostPays?order.finalAmount:member.amountDue);
  if(payer&&"BANK_TRANSFER".equals(order.paymentMethod)&&"UNPAID".equals(order.paymentStatus)&&!"CANCELLED".equals(order.status))result.put("bank",bank.instructions(order.id));
  return result;
 }
 @Transactional(readOnly=true) public List<Map<String,Object>> paymentNotices(Actor actor){support.buyer(actor);return refs.list("select g.invite_token as token,m.id as member_id,m.amount_due,o.id as order_id,o.order_code,o.payment_method,o.payment_status,g.payment_mode from group_order_members m join group_orders g on g.id=m.group_order_id join orders o on o.id=m.order_id where m.user_id=? and m.user_id<>g.host_user_id and g.status='ORDERED' and o.status='NEW' and o.payment_status='UNPAID' order by o.id desc limit 5",actor.userId()).stream().map(row->{Map<String,Object> normalized=new LinkedHashMap<>();row.forEach((key,value)->normalized.put(key.toLowerCase(Locale.ROOT),value));return normalized;}).toList();}
 public record Benefit(Long id,int participants,BigDecimal amount,String paymentMode){}
 public Benefit benefit(Actor actor,Cart cart,BigDecimal subtotal,boolean lock) {
  if(cart.groupOrderId==null)return new Benefit(null,0,BigDecimal.ZERO,"HOST");
  var g=data.require(GroupOrder.class,cart.groupOrderId,lock);host(actor,g);
  if(!"LOCKED".equals(g.status)||!g.expiresAt.isAfter(clock.instant()))throw OrderProblem.conflict("Nhóm chưa sẵn sàng hoặc đã hết hạn.");
  var all=members(g).stream().filter(m->!items(m).isEmpty()).toList();for(var m:all)refs.user(m.userId,false);
  return new Benefit(g.id,all.size(),discount(subtotal,all.size()),g.paymentMode);
 }
 private String billKey(long variant,int sugar,int ice,List<Long> toppings,String note){var v=refs.one("select p.id,p.catalog_key,p.category_id,s.owner_user_id,v.size_name from product_variants v join products p on p.id=v.product_id join stores s on s.id=p.store_id where v.id=?",variant);return support.hash(List.of(v.get("catalog_key")==null?"product:"+v.get("id"):v.get("catalog_key"),v.get("category_id"),v.get("owner_user_id"),v.get("size_name"),sugar,ice,toppings.stream().sorted().toList(),note));}
 public void ordered(Cart cart,long orderId){
  if(cart.groupOrderId==null)return;var g=data.require(GroupOrder.class,cart.groupOrderId,true);var order=data.require(Order.class,orderId,false);if("COD".equals(order.paymentMethod))g.paymentMode="HOST";
  var all=members(g).stream().filter(m->!items(m).isEmpty()).toList();Map<String,BigDecimal> prices=new HashMap<>();
  for(var line:data.items(orderId)){var tops=data.toppings(line.id).stream().map(t->t.toppingId).toList();prices.put(billKey(line.variantId,line.sugarLevel,line.iceLevel,tops,line.note),line.drinkUnitPrice.add(line.toppingUnitTotal));}
  BigDecimal sum=BigDecimal.ZERO;for(var m:all){m.billedSubtotal=BigDecimal.ZERO;for(var item:items(m)){var price=prices.get(billKey(item.variantId(),item.sugar(),item.ice(),item.toppingIds(),item.note()));if(price==null)throw OrderProblem.conflict("Không thể đối chiếu phần món nhóm với đơn hàng.");m.billedSubtotal=m.billedSubtotal.add(price.multiply(BigDecimal.valueOf(item.quantity())));}sum=sum.add(m.billedSubtotal);}
  if(sum.compareTo(order.subtotal)!=0)throw OrderProblem.conflict("Tổng phần món nhóm không khớp đơn hàng.");
  BigDecimal discountTotal=order.productDiscount.add(order.pointsDiscount),allocated=BigDecimal.ZERO;
  for(var m:all){m.discountShare=discountTotal.signum()==0?BigDecimal.ZERO:discountTotal.multiply(m.billedSubtotal).divide(order.subtotal,0,RoundingMode.DOWN);allocated=allocated.add(m.discountShare);}
  int remainder=discountTotal.subtract(allocated).intValueExact();
  if(remainder>0){var ranked=new ArrayList<>(all);ranked.sort(Comparator.<GroupOrderMember,BigDecimal>comparing(m->discountTotal.multiply(m.billedSubtotal).remainder(order.subtotal)).reversed().thenComparing(m->m.id));for(int i=0;i<remainder;i++)ranked.get(i).discountShare=ranked.get(i).discountShare.add(BigDecimal.ONE);}
  BigDecimal shippingLeft=order.shippingGross.subtract(order.shippingDiscount),total=BigDecimal.ZERO;
  for(int i=0;i<all.size();i++){var m=all.get(i);boolean last=i==all.size()-1;m.shippingShare=last?shippingLeft:order.shippingGross.subtract(order.shippingDiscount).divide(BigDecimal.valueOf(all.size()),0,RoundingMode.DOWN);shippingLeft=shippingLeft.subtract(m.shippingShare);m.amountDue=m.billedSubtotal.subtract(m.discountShare).add(m.shippingShare);m.orderId=orderId;total=total.add(m.amountDue);}
  if(total.compareTo(order.finalAmount)!=0)throw OrderProblem.conflict("Tổng phần thanh toán không khớp đơn hàng.");g.status="ORDERED";g.orderId=orderId;cart.groupOrderId=null;
 }
}
