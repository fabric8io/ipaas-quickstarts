package io.fabric8.quickstarts.springbootkeycloak;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class IPAddressController {
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value = "/ipaddress", method = RequestMethod.GET)
    public IPAddress ipaddress() throws UnknownHostException {
        return new IPAddress(counter.incrementAndGet(), InetAddress.getLocalHost().getHostAddress());
    }
}
