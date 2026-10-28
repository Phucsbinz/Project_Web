package com.utetra.order;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import com.utetra.order.service.QrPublicOrigin;
class QrPublicOriginTest {
 @Test void productionNeverFallsBackToLocal(){assertThatThrownBy(()->QrPublicOrigin.resolve("http://127.0.0.1:8080",null,false)).isInstanceOf(RuntimeException.class);}
 @Test void liveCloudflareIsPreferred(){assertThat(QrPublicOrigin.resolve("http://127.0.0.1:8080","https://tea-test.trycloudflare.com",false)).isEqualTo("https://tea-test.trycloudflare.com");}
 @Test void isolatedTestsCanUseTheirOwnServer(){assertThat(QrPublicOrigin.resolve("http://127.0.0.1:8882",null,true)).isEqualTo("http://127.0.0.1:8882");}
 @Test void insecureOrMalformedPublicOriginsAreRejected(){for(String origin:new String[]{"http://example.com","https://localhost","https://127.0.0.1","https://user@example.com","https://example.com/path"})assertThatThrownBy(()->QrPublicOrigin.resolve(origin,null,false)).isInstanceOf(RuntimeException.class);}
}
