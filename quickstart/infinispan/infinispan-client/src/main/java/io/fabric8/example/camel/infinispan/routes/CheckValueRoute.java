package io.fabric8.example.camel.infinispan.routes;

import org.apache.camel.builder.RouteBuilder;

public class CheckValueRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {
		
		from("direct:check")
		.transform(simple("random(1,1000)"))
		.choice()
		.when(simple("${body} > 500"))
		  .log("High priority message : ${body}")
		.otherwise()
		  .log("Low priority message : ${body}")
		.end();
		
	}

}
