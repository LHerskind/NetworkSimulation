package BeamMine;

import Beam.BeamNode;
import Dandelion.DandelionNode;
import Dandelion.Message;
import Dandelion.Transaction;

public class MyBeamNode extends BeamNode {

    public MyBeamNode(String name, int id) {
        super(name, id);
    }

    private Transaction takeFee(Transaction tx){
        if(tx.hasFee()){
            String tx_id = this.id + "_" + this.message_counter++;
            Transaction my_tx = new Transaction(tx_id, false, false);
            my_tx.addFeeInput(tx.getFee());
            return tx.add(my_tx);
        } else {
            return tx;
        }
    }

    @Override
    protected void generateMessage(){
        if(Math.random() < this.generate_probability){
            String tx_id = this.id + "_" + this.message_counter++;
            Transaction tx = new Transaction(tx_id, false, true);
            Message message = new Message(this, tx, true);
            this.addMessageToSend(message);
        }
    }

    @Override
    protected void unicast(Message message, DandelionNode peer){
        stemPool.add(message.getMessage());
        delivered_to_me_messages_map.putIfAbsent(message.hashCode(), message);
        if(lastStem + stemWait <= System.currentTimeMillis()){
            Transaction tx = new Transaction();
            for(Transaction stemTx : stemPool){
                stemTx = takeFee(stemTx);
                tx = tx.add(stemTx);
            }
            Message to_send = new Message(this, tx, true);
            stemPool.clear();
            peer.deliver_message(to_send);
            delivered_to_me_messages_map.putIfAbsent(to_send.hashCode(), to_send);
            lastStem = System.currentTimeMillis();
        }
    }

}