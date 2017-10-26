package com.way.auroth.jmeter.thrift;

import com.wzy.auroth.test.service.HelloService;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.util.Random;

public class ThriftClientForLongConnectionTest extends AbstractJavaSamplerClient {
    private TTransport tTransport;
    private HelloService.Client client;
    private Random random = new Random(System.currentTimeMillis());

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        try {
            client.testString(random.nextLong());
            result.setSuccessful(true);
        } catch (TException e) {
            result.setSuccessful(false);
            throw new RuntimeException(e);
        }
        result.sampleEnd();
        return result;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        super.setupTest(context);
        String host = context.getParameter("host", "localhost");
        int port = context.getIntParameter("port", 15600);
        tTransport = new TSocket(host, port);
        tTransport = new TFramedTransport(tTransport);
        try {
            tTransport.open();
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        }
        TProtocol protocol = new TBinaryProtocol(tTransport);

        TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,"HelloService");
        client = new HelloService.Client(mp1);
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        super.teardownTest(context);
        if (tTransport != null) tTransport.close();
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("host", "localhost");
        arguments.addArgument("port", "15600");
        return arguments;
    }

    public static void main(String[] args) {
        ThriftClientForLongConnectionTest longConnectionTest = new ThriftClientForLongConnectionTest();
        Arguments arguments = new Arguments();
        arguments.addArgument("host", "192.168.5.82");
        arguments.addArgument("port", "8080");
        JavaSamplerContext javaSamplerContext = new JavaSamplerContext(arguments);
        longConnectionTest.setupTest(javaSamplerContext);
        longConnectionTest.runTest(javaSamplerContext);
    }
}
