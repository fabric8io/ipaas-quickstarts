package io.swagger.api.factories;

import io.swagger.api.PetApiService;
import io.swagger.api.impl.PetApiServiceImpl;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JaxRSServerCodegen", date = "2015-12-26T13:01:41.343Z")
public class PetApiServiceFactory {

   private final static PetApiService service = new PetApiServiceImpl();

   public static PetApiService getPetApi()
   {
      return service;
   }
}
