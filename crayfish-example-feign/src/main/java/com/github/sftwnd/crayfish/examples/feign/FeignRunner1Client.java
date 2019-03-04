package com.github.sftwnd.crayfish.examples.feign;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Component
@FeignClient(name = "feign-runner1-client")
@Headers( value = {"Accept: text/plain", "charset: utf-8"} )
public interface FeignRunner1Client {

  //@RequestLine("GET ")
    @Headers(value = {"Content-Type: text/plain"})
    @RequestMapping(
             method = RequestMethod.GET
            ,path = "${feign-runner1-client.path:/}"
            ,headers = {"Accept=text/plain", "charset=utf-8"}
    )
    String page();

}