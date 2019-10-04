package Beam;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Transaction;

import java.util.HashSet;
import java.util.Random;

public class BeamNode extends DandelionNode {

    protected long lastStem = 0;
    protected long stemWait = 2000;
    long stemForce = 2500;
    protected HashSet<Transaction> stemPool = new HashSet<>();

    public BeamNode(String name, int id) {
        super(name, id);
        if(lastStem == 0){
            lastStem = System.currentTimeMillis();
        }
    }

    private void endingStem(){
        if(!stemPool.isEmpty()){
            Transaction tx = stemPool.iterator().next();
            Message msg = new Message(this, tx, false);
            unicast(msg, this.stem_node);
        }
    }

    @Override
    protected void nextEpoch(){
        long elapsed_time = System.currentTimeMillis() - this.epoch_start;
        if (epoch_start == 0 || elapsed_time > this.epoch_length){
            epoch_start = System.currentTimeMillis();
            Random rand = new Random();
            this.stem_node = peers.get(rand.nextInt(this.peers.size()));
            boolean temp_stem = Math.random() < 0.9;
            if(!temp_stem && this.is_stem){
                endingStem();
            }
            this.is_stem = temp_stem;
        }
    }

    @Override
    protected void unicast(Message message, DandelionNode peer){
        stemPool.add(message.getMessage());
        delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
        if(lastStem + stemWait <= System.currentTimeMillis()){
            Transaction tx = new Transaction();
            for(Transaction stemTx : stemPool){
                tx = tx.add(stemTx);
            }
            Message to_send = new Message(this, tx, true);
            stemPool.clear();
            peer.deliver_message(to_send);
            delivered_to_me_messages_map.putIfAbsent(to_send.hashCode(), to_send);
            lastStem = System.currentTimeMillis();
        }
    }

    @Override
    protected void handleHiccups(){
        if(lastStem + stemForce <= System.currentTimeMillis()){
            endingStem();
        }
    }

}