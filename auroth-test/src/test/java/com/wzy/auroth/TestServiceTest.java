package com.wzy.auroth;

import com.wzy.auroth.test.Application;
import com.wzy.auroth.test.service.TestService;
import org.apache.thrift.TException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class TestServiceTest {

    @Autowired
    private TestService testService;


    @Test
    public void test() throws TException {
        testService.test();
    }
}
