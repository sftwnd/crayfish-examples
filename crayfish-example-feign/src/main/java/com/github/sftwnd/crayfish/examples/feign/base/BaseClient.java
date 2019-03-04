package com.github.sftwnd.crayfish.examples.feign.base;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Profile("feign-base-client")
@FeignClient(name = "feign-base-client")
@Headers( value = {"Accept: text/plain", "charset: utf-8"} )
public interface BaseClient {

    @RequestMapping(
            path = "/"
            ,method = RequestMethod.GET
            ,produces = "text/plain"
    )
    String page();

}