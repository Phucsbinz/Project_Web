package com.utetra.order;
import com.utetra.order.service.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CheckoutPaymentPolicyTest {
 @Test void liveCheckoutOnlyPermitsCodAndCloudflareQr(){var policy=new CheckoutPaymentPolicy("COD,LOCAL_QR");assertThatCode(()->policy.require("COD")).doesNotThrowAnyException();assertThatCode(()->policy.require("LOCAL_QR")).doesNotThrowAnyException();for(String method:new String[]{"BANK_TRANSFER","VNPAY","OTHER"})assertThatThrownBy(()->policy.require(method)).isInstanceOf(OrderProblem.class);}
 @Test void historicalProviderTestsCanExplicitlyOptIntoArchivedMethods(){var archived=new CheckoutPaymentPolicy("COD,LOCAL_QR,BANK_TRANSFER,VNPAY");assertThatCode(()->archived.require("BANK_TRANSFER")).doesNotThrowAnyException();}
}
