/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.test;

import com.google.common.primitives.Ints;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ControlPort;
import se.sics.kompics.Event;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Init.None;
import se.sics.kompics.Kompics;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.VirtualNetworkChannel;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class NetworkTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTest.class);
    //private static final String STARTED = "STARTED";
    private static final String STOPPED = "STOPPED";
    private static final String SENDING = "SENDING";
    private static final String RECEIVED = "RECEIVED";
    private static final String ACKED = "ACKED";
    private static final String SENT = "SENT";
    private static final String FAIL = "FAIL";
    private static final int NUM_MESSAGES = 100;
    private static final int BATCH_SIZE = 10;
    private static final AtomicInteger WAIT_FOR = new AtomicInteger(NUM_MESSAGES);
    private static NetworkGenerator nGen;
    private static int numNodes;
    private static AtomicInteger msgId = new AtomicInteger(0);
    private static ConcurrentMap<Integer, String> messageStatus = new ConcurrentSkipListMap<Integer, String>();
    private static int startPort = 33000;
    private static Transport[] protos;

    public static synchronized void runTests(NetworkGenerator nGen, int numNodes, Transport[] protos) {
        LOG.info("******************** Running All Test ********************");
        NetworkTest.nGen = nGen;
        NetworkTest.numNodes = numNodes;
        NetworkTest.protos = protos;
        WAIT_FOR.set(NUM_MESSAGES);

        msgId.set(0);
        messageStatus.clear();
        TestUtil.reset(10000); //10 sec timeout for all the connections to be dropped properly

        Kompics.createAndStart(LauncherComponent.class, 8, 50);

        for (int i = 0; i < numNodes; i++) {
            LOG.info("Got {}/{} STOPPED.", i, numNodes);
            TestUtil.waitFor(STOPPED);
        }
        Kompics.shutdown();

        assertEquals(NUM_MESSAGES * numNodes, messageStatus.size());
        for (String s : messageStatus.values()) {
            assertEquals(ACKED, s);
        }
    }

    public static synchronized void runAtLeastTests(NetworkGenerator nGen, int numNodes, Transport[] protos) {
        LOG.info("******************** Running AT LEAST Test ********************");
        NetworkTest.nGen = nGen;
        NetworkTest.numNodes = numNodes;
        NetworkTest.protos = protos;
        WAIT_FOR.set(1);

        msgId.set(0);
        messageStatus.clear();
        TestUtil.reset(10000); //10 sec timeout for all the connections to be dropped properly

        Kompics.createAndStart(LauncherComponent.class, 8, 50);

        for (int i = 0; i < numNodes; i++) {
            LOG.info("Got {}/{} STOPPED.", i, numNodes);
            TestUtil.waitFor(STOPPED);
        }
        Kompics.shutdown();

        assertTrue(numNodes <= messageStatus.size());
    }

    public static class LauncherComponent extends ComponentDefinition {

        public LauncherComponent() {
            Address[] nodes = new Address[numNodes];
            InetAddress ip = null;
            try {
                ip = InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException ex) {
                LOG.error("Aborting test.", ex);
                System.exit(1);
            }
            
            for (int i = 0; i < numNodes; i++) {
                nodes[i] = new Address(ip, startPort + i, Ints.toByteArray(i));
                Component net = nGen.generate(myProxy, nodes[i]);
                VirtualNetworkChannel vnc = VirtualNetworkChannel.connect(net.provided(Network.class));
                Component scen = create(ScenarioComponent.class, new ScenarioInit(nodes[i], nodes));
                vnc.addConnection(Ints.toByteArray(i), scen.required(Network.class));
            }
            //startPort = startPort + numNodes; // Don't start the same ports next time
            // Some network components shut down asynchronously -.-
        }
        private final ComponentProxy myProxy = new ComponentProxy() {
            @Override
            public <P extends PortType> void trigger(Event e, Port<P> p) {
                LauncherComponent.this.trigger(e, p);
            }

            @Override
            public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent) {
                return LauncherComponent.this.create(definition, initEvent);
            }

            @Override
            public <T extends ComponentDefinition> Component create(Class<T> definition, None initEvent) {
                return LauncherComponent.this.create(definition, initEvent);
            }

            @Override
            public void destroy(Component component) {
                LauncherComponent.this.destroy(component);
            }

            @Override
            public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative) {
                return LauncherComponent.this.connect(positive, negative);
            }

            @Override
            public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive) {
                return LauncherComponent.this.connect(negative, positive);
            }

            @Override
            public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive) {
                LauncherComponent.this.disconnect(negative, positive);
            }

            @Override
            public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative) {
                LauncherComponent.this.disconnect(positive, negative);
            }

            @Override
            public Negative<ControlPort> getControlPort() {
                return LauncherComponent.this.control;
            }
        };
    }

    public static class ScenarioComponent extends ComponentDefinition {

        public final Address self;
        public final Address[] nodes;
        private final Positive<Network> net = requires(Network.class);
        private int msgCount = 0;
        private int ackCount = 0;
        private Random rand = new Random(0);
        private Map<UUID, Integer> pending = new TreeMap<UUID, Integer>();

        public ScenarioComponent(ScenarioInit init) {
            self = init.self;
            nodes = init.nodes;

            Handler<Start> startHandler = new Handler<Start>() {
                @Override
                public void handle(Start event) {
                    for (int i = 0; i < BATCH_SIZE; i++) {
                        sendMessage();
                    }
                }
            };
            subscribe(startHandler, control);

            Handler<Ack> ackHandler = new Handler<Ack>() {
                @Override
                public void handle(Ack event) {
                    LOG.debug("Got Ack {}", event);
                    messageStatus.put(event.msgId, ACKED);
                    ackCount++;

                    if (ackCount >= WAIT_FOR.get()) {
                        TestUtil.submit(STOPPED);
                        return;
                    }

                    if (msgCount < NUM_MESSAGES) {
                        for (int i = 0; i < BATCH_SIZE; i++) {
                            sendMessage();
                        }
                    }
                }
            };
            subscribe(ackHandler, net);

            Handler<TestMessage> msgHandler = new Handler<TestMessage>() {
                @Override
                public void handle(TestMessage event) {
                    LOG.debug("Got message {}", event);
                    messageStatus.put(event.msgId, RECEIVED);
                    trigger(event.ack(), net);
                }
            };
            subscribe(msgHandler, net);

            Handler<MessageNotify.Resp> notifyHandler = new Handler<MessageNotify.Resp>() {

                @Override
                public void handle(MessageNotify.Resp event) {
                    Integer msgId = pending.remove(event.msgId);
                    assertNotNull(msgId);
                    messageStatus.replace(msgId, SENDING, SENT);
                    LOG.debug("Message {} was sent.", msgId);
                }
            };
            subscribe(notifyHandler, net);
        }

        private void sendMessage() {
            int id = msgId.getAndIncrement();
            if (messageStatus.putIfAbsent(id, SENDING) != null) {
                LOG.error("Key {} was already present in messageStatus!", id);
                TestUtil.submit(FAIL);
            }
            Transport proto = NetworkTest.protos[rand.nextInt(NetworkTest.protos.length)];
            TestMessage msg = new TestMessage(self, nodes[rand.nextInt(nodes.length)], id, proto);
            MessageNotify.Req req = MessageNotify.create(msg);
            pending.put(req.getMsgId(), id);
            trigger(req, net);
            msgCount++;
        }
    }

    public static class ScenarioInit extends Init<ScenarioComponent> {

        public final Address self;
        public final Address[] nodes;

        public ScenarioInit(Address self, Address[] nodes) {
            this.self = self;
            this.nodes = nodes;
        }
    }

    public static class TestMessage extends Message {

        public final int msgId;

        public TestMessage(Address src, Address dst, int id, Transport p) {
            super(src, dst, p);
            this.msgId = id;
        }

        public Ack ack() {
            return new Ack(this.getDestination(), this.getSource(), msgId, this.getProtocol());
        }
    }

    public static class Ack extends Message {

        public final int msgId;

        public Ack(Address src, Address dst, int id, Transport p) {
            super(src, dst, p);
            this.msgId = id;
        }
    }
}
