package com.github.sftwnd.crayfish.examples.feign.authorization;

import com.github.sftwnd.crayfish.examples.feign.authorization.format.StructuredBalanceInfo;
import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Component
@FeignClient(name = "bis-brt-balance-service")
@Headers( value = {"Accept: text/plain", "charset: utf-8"} )
public interface BisBrtBalanceService {

    @RequestMapping(
            method = RequestMethod.GET
           ,path = "/ps/v1/bis-brt-balance/customers/{customerId}/availableBalance?customerDatabaseId=999"
         //,headers = {"Accept=application/json", "charset=utf-8"}
           ,produces = "application/json"
           )
    StructuredBalanceInfo balances(@PathVariable("customerId") String customerId, @RequestHeader("rtMacroRegionId") int rtMacroRegionId);

}
