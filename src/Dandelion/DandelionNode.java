package Dandelion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class DandelionNode extends Thread{

    protected String name;
    protected int id;

    protected ArrayList<DandelionNode> peers = new ArrayList<DandelionNode>();
    protected HashMap<Integer, Message> delivered_to_me_messages_map = new HashMap<Integer, Message>();
    protected HashMap<Integer, Message> message_created = new HashMap<Integer, Message>();

    protected ConcurrentLinkedQueue<Message> mQueue = new ConcurrentLinkedQueue<>();
    protected List<Message> messages_to_send = new ArrayList<Message>();

    protected DandelionNode stem_node = null;
    protected boolean is_stem = false;

    protected long epoch_start = 0;
    protected long epoch_length = 10000;

    protected int message_counter = 0;

    protected int number_of_rounds = 0;
    protected int sleep_between_rounds = 0;
    protected double generate_probability = 0;

    public DandelionNode(String name, int id){
        this.name = name;
        this.id = id;
    }

    public HashMap<Integer, Message> getDeliveredMessages(){
        return this.delivered_to_me_messages_map;
    }

    public HashMap<Integer, Message> getMessagesCreated(){
        return this.message_created;
    }

    protected void addMessageCreated(Message msg){
        this.message_created.putIfAbsent(msg.hashCode(), msg);
    }

    protected void nextEpoch(){
        long elapsed_time = System.currentTimeMillis() - this.epoch_start;
        if (epoch_start == 0 || elapsed_time > this.epoch_length){
            epoch_start = System.currentTimeMillis();
            Random rand = new Random();
            this.stem_node = peers.get(rand.nextInt(this.peers.size()));
            this.is_stem = Math.random() < 0.9;
        }
    }

    public int numberOfDelivedMessages(){
        return delivered_to_me_messages_map.size();
    }

    String getNodeName(){
        return this.name;
    }

    public int getNodeId(){
        return this.id;
    }

    protected void addMessageToSend(Message message){
        this.messages_to_send.add(message);
    }

    public void deliver_message(Message message){
        this.mQueue.add(message);
    }

    public void addPeer(DandelionNode peer){
        this.peers.add(peer);
    }

    public ArrayList<DandelionNode> getPeers(){
        return this.peers;
    }

    protected void broadcast(Message message){
        Message to_send = message.copy();
        to_send.setSender(this);
        to_send.setStem(false);
        for (DandelionNode peer : this.peers){
            if(!peer.equals(message.getSender())){
                peer.deliver_message(to_send);
            }
        }
        delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
    }

    protected void unicast(Message message, DandelionNode peer){
        Message to_send = new Message(this, message.getMessage(), true);
        peer.deliver_message(to_send);
        delivered_to_me_messages_map.putIfAbsent(to_send.hashCode(), to_send);
    }

    protected void  receiver(){
        if(this.mQueue.isEmpty()){ // No received messages
            return;
        }

        Message message = this.mQueue.poll();
        boolean is_seen_before = this.delivered_to_me_messages_map.containsKey(message.hashCode());

        if (is_seen_before){
            // We have seen the message before, if we saw it as broadcast, do nothing.
            // If we have seen the message before, we need to see if it is stem or not. Is it stem we need to broadcast.
            // If it is not stem then we need to see if the previously received was a broadcast, if not, then we broadcast.
            if(message.isStem()){
                broadcast(message);
            } else if (this.delivered_to_me_messages_map.get(message.hashCode()).isStem()) {
                broadcast(message);
            }
        } else {
            // Totally new message, just handle it based on the state we are in.
            if(this.is_stem && message.isStem()){
                unicast(message, this.stem_node);
            } else {
                broadcast(message);
            }
        }
    }

    public void printKnowledge(){
        System.out.println("---------------------------------");
        System.out.println("Printing the knowledge of " + this);
        for (Message msg : delivered_to_me_messages_map.values()){
            System.out.println(msg);
        }
    }

    protected void generateMessage(){
        if(Math.random() < this.generate_probability){
            String tx_id = this.id + "_" + this.message_counter++;
            Transaction tx = new Transaction(tx_id, false, false);
            Message message = new Message(this, tx, true);
            this.addMessageToSend(message);
        }
    }

    public void init(int number_of_rounds, int sleep_between_rounds){
        this.number_of_rounds = number_of_rounds;
        this.sleep_between_rounds = sleep_between_rounds;
        // TODO: We changed this!
        this.generate_probability = 1.0 / (10*60000.0 / this.sleep_between_rounds);
    }

    protected void handleHiccups(){
        // To take care of forcemerge for Grin and Beam nodes.
    }

    public void run(){
        try{
            int i = 0;

            while(i < this.number_of_rounds){
                nextEpoch();
                generateMessage();

                if (!messages_to_send.isEmpty()){ // Do we have some messages that we should send?
                    Message msg = messages_to_send.remove(0);
                    addMessageCreated(msg);
                    if(msg.isStem()){
                        unicast(msg, this.stem_node);
                    } else {
                        broadcast(msg);
                    }
                }
                if (!mQueue.isEmpty()){
                    receiver();
                }

                handleHiccups();

                i++;
                Thread.sleep(this.sleep_between_rounds);
            }

        } catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    @Override
    public String toString(){
        ArrayList<String> peer_names = new ArrayList<String>();
        for (DandelionNode node : this.peers){
            peer_names.add(node.getNodeName());
        }
        return this.name + " ("+this.stem_node.name + "): " + peer_names.toString();
    }

}
