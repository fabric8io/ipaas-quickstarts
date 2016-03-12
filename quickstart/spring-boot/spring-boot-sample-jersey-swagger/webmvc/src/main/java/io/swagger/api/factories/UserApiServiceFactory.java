package io.swagger.api.factories;

import io.swagger.api.UserApiService;
import io.swagger.api.impl.UserApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JaxRSServerCodegen", date = "2015-12-26T13:01:41.343Z")
public class UserApiServiceFactory {

   private final static UserApiService service = new UserApiServiceImpl();

   public static UserApiService getUserApi()
   {
      return service;
   }
}
