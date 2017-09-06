package com.github.sftwnd.crayfish.examples.feign.authorization;

import com.github.sftwnd.crayfish.examples.feign.authorization.format.StructuredBalanceInfo;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Component
@FeignClient(name = "bis-brt-balance-service")
public interface BisBrtBalanceService {

    @RequestMapping(
            method = RequestMethod.GET
           ,path = "/ps/v1/bis-brt-balance/customers/{customerId}/availableBalance?customerDatabaseId=999"
           ,headers = {"Accept=application/json", "charset=utf-8"}
           )
    StructuredBalanceInfo balances(@PathVariable("customerId") String customerId, @RequestHeader("rtMacroRegionId") int rtMacroRegionId);

}
