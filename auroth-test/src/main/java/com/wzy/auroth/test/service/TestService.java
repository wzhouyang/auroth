package com.wzy.auroth.test.service;

import com.wzy.auroth.test.consul.register.po.User;
import com.wzy.auroth.thrift.annotation.TReference;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    @Autowired
    private Tracer tracer;

    @TReference(host = "localhost", port = 8080)
    private HelloService.Client helloServiceClient;

    public void test() throws TException {
        Span currentSpan = this.tracer.getCurrentSpan();
        if (currentSpan == null) {
            currentSpan = this.tracer.createSpan("testSpan");
            this.tracer.continueSpan(currentSpan);
        } else {
            currentSpan = this.tracer.createSpan("testSpan", currentSpan);
            this.tracer.continueSpan(currentSpan);
        }
        User user = helloServiceClient.getUser(1L);
        System.out.println(user);
    }
}
