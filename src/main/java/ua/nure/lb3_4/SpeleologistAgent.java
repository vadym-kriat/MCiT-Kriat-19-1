package ua.nure.lb3_4;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class SpeleologistAgent extends Agent {

    private static int WORLD_SEARCH_PAUSE = 2000;
    public static int LOOK_RIGHT = 0;
    public static int LOOK_LEFT = 1;
    public static int LOOK_UP = 2;
    public static int LOOK_DOWN = 3;
    public static int MOVE = 4;
    public static int SHOOT_ARROW = 5;
    public static int TAKE_GOLD = 6;
    public static HashMap<Integer, String> actionCodes = new HashMap<Integer, String>() {{
        put(LOOK_RIGHT, "right");
        put(LOOK_LEFT, "left");
        put(LOOK_UP, "up");
        put(LOOK_DOWN, "down");
        put(MOVE, "move");
        put(SHOOT_ARROW, "shoot");
        put(TAKE_GOLD, "take");
    }};

    public static String GO_INSIDE = "go-inside";
    public static String WAMPUS_WORLD_TYPE = "wampus-world";
    public static String NAVIGATOR_AGENT_TYPE = "navigator-agent";

    public static String WORLD_DIGGER_CONVERSATION_ID = "digger-world";
    public static String NAVIGATOR_DIGGER_CONVERSATION_ID = "digger-navigator";

    private int arrowCount = 1;
    private AID wampusWorld;
    private AID navigationAgent;
    private String roomDescription = "";

    private Map<String, List<String>> expressions;
    private Random random = new Random();

    @Override
    protected void setup() {
        expressions = new HashMap<>();
        expressions.put(NavigatorAgent.BREEZE, Arrays.asList("I feel breeze here", "There is a breeze"));
        expressions.put(NavigatorAgent.PIT, Arrays.asList("I see a pit!", "There is a pit there"));
        expressions.put(NavigatorAgent.STENCH, Arrays.asList("I smell stench", "There is some stench in that room"));
        expressions.put(NavigatorAgent.WAMPUS, Arrays.asList("I see wampus!!!", "Oh, wampus is coming!!", "I think, wampus might be here"));
        expressions.put(NavigatorAgent.GOLD, Arrays.asList("I see golden spot", "I think, gold might be here"));
        expressions.put(NavigatorAgent.BUMP, Collections.singletonList("Oh, bump!"));

        addBehaviour(new WampusWorldFinder());
    }

    private String roomDescriptionToExpressions(String roomDescription) {
        if (roomDescription.isEmpty()) {
            return roomDescription;
        }

        String[] props = roomDescription.split(",");
        StringBuilder sb = new StringBuilder();
        for (String prop : props) {
            List<String> availableExpressions = expressions.get(prop);
            if (availableExpressions == null || availableExpressions.isEmpty()) {
                sb.append(prop);
            } else {
                int randomPosition = random.nextInt(availableExpressions.size());
                sb.append(availableExpressions.get(randomPosition));
            }
            sb.append(".");
        }

        return sb.toString();
    }

    private class WampusWorldFinder extends Behaviour {
        private int step = 0;

        @Override
        public void action() {
            if (step == 0) {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType(WAMPUS_WORLD_TYPE);
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        wampusWorld = result[0].getName();
                        myAgent.addBehaviour(new WampusWorldPerformer());
                        ++step;
                    } else {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean done() {
            return step == 1;
        }
    }

    private class WampusWorldPerformer extends Behaviour {

        private MessageTemplate mt;
        private int step = 0;

        @Override
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(wampusWorld);
                    cfp.setContent(GO_INSIDE);
                    cfp.setConversationId(WORLD_DIGGER_CONVERSATION_ID);
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(WORLD_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.CONFIRM) {
                            roomDescription = reply.getContent();
                            myAgent.addBehaviour(new NavigatorAgentPerformer());
                            step = 2;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 2;
        }
    }

    private class NavigatorAgentPerformer extends Behaviour {

        private int step = 0;
        private MessageTemplate mt;

        @Override
        public void action() {
            switch (step) {
                case 0: {
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType(NAVIGATOR_AGENT_TYPE);
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        if (result.length > 0) {
                            navigationAgent = result[0].getName();
                            ++step;
                        } else {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
                }
                case 1: {
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                    order.addReceiver(navigationAgent);
                    order.setContent(roomDescriptionToExpressions(roomDescription));
                    order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    step = 2;
                }
                case 2: {
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String actions = reply.getContent();
                            actions = actions.substring(1, actions.length() - 1);
                            String[] instructions = actions.split(", ");
                            if (instructions.length == 1) {
                                sendTakeGoldMessage();
                            } else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(SHOOT_ARROW))) {
                                sendShootMessage(instructions[0]);
                            } else if (instructions.length == 2 && Objects.equals(instructions[1], actionCodes.get(MOVE))) {
                                sendMoveMessage(instructions[0]);
                            } else {
                                System.out.println("ERROR ACTIONS");
                            }
                            ++step;
                        }
                    } else {
                        block();
                    }
                    break;

                }
                case 3:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        roomDescription = reply.getContent();
                        step = 1;
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 4;
        }

        private void sendShootMessage(String instruction) {
            sendActionMessage(SHOOT_ARROW, instruction);
        }

        private void sendTakeGoldMessage() {
            sendActionMessage(TAKE_GOLD, "Take");
        }

        private void sendMoveMessage(String instruction) {
            sendActionMessage(MOVE, instruction);
        }

        private void sendActionMessage(int action, String instruction) {
            ACLMessage order = new ACLMessage(action);
            order.addReceiver(wampusWorld);
            order.setContent(instruction);
            order.setConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID);
            order.setReplyWith("order" + System.currentTimeMillis());
            myAgent.send(order);
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(NAVIGATOR_DIGGER_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(order.getReplyWith()));
        }
    }
}
