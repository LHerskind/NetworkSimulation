package Grin;

import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Transaction;

import java.util.HashSet;

public class GrinNode extends DandelionNode {

    long lastMerge = 0;
    long mergeWait = 2000;

    HashSet<Transaction> stemPool = new HashSet<>();

    public GrinNode(String name, int id) {
        super(name, id);
        if(lastMerge == 0){
            lastMerge = System.currentTimeMillis();
        }
    }

    @Override
    protected void broadcast(Message message){
        if(message.isStem()){
            stemPool.add(message.getMessage());
            if(lastMerge + mergeWait <= System.currentTimeMillis()){
                Transaction tx = new Transaction();
                for(Transaction stemTx : stemPool){
                    tx = tx.add(stemTx);
                }
                Message to_send = new Message(this, tx, false);
                for(DandelionNode peer : this.peers){
                    if(!peer.equals(message.getSender())){
                        peer.deliver_message(to_send);
                    }
                }
                delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
                stemPool.clear();
                lastMerge = System.currentTimeMillis();
            }
        }else {
            Message to_send = message.copy();
            to_send.setSender(this);
            to_send.setStem(false);
            for (DandelionNode peer : this.peers) {
                if (!peer.equals(message.getSender())) {
                    peer.deliver_message(to_send);
                }
            }
            delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
        }
    }

}
