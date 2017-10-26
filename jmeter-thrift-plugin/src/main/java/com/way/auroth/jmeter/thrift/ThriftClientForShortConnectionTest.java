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

import java.util.Random;

public class ThriftClientForShortConnectionTest extends AbstractJavaSamplerClient {
    private Random random = new Random(System.currentTimeMillis());

    @Override
    public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        String host = javaSamplerContext.getParameter("host", "localhost");
        int port = javaSamplerContext.getIntParameter("port", 15600);
        TTransport tTransport = null;
        try {
            tTransport = new TSocket(host, port);
            tTransport = new TFramedTransport(tTransport);
            tTransport.open();
            TProtocol protocol = new TBinaryProtocol(tTransport);
            TMultiplexedProtocol mp1 = new TMultiplexedProtocol(protocol,"HelloService");
            HelloService.Client client = new HelloService.Client(mp1);
            client.testString(random.nextLong());
            result.setSuccessful(true);
        } catch (TException e) {
            result.setSuccessful(false);
            throw new RuntimeException(e);
        } finally {
            if (tTransport != null)tTransport.close();
        }
        result.sampleEnd();
        return result;
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments arguments = new Arguments();
        arguments.addArgument("host", "localhost");
        arguments.addArgument("port", "15600");
        return arguments;
    }

    public static void main(String[] args) {
        ThriftClientForShortConnectionTest shortConnectionTest = new ThriftClientForShortConnectionTest();
        Arguments arguments = new Arguments();
        arguments.addArgument("host", "localhost");
        arguments.addArgument("port", "15601");
        JavaSamplerContext javaSamplerContext = new JavaSamplerContext(arguments);
        shortConnectionTest.runTest(javaSamplerContext);
    }
}
