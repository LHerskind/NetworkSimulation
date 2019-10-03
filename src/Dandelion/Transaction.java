package Dandelion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class Transaction {

    private HashSet<String> inputs = new HashSet<String>();
    private HashSet<String> outputs = new HashSet<String>();
    private String fee = "";
    private int hash = 0;

    public Transaction(String val, boolean empty, boolean have_fee){
        if (!empty){
            this.inputs.add("I_"+val);
            this.outputs.add("O_"+val);
        }
        if (have_fee){
            this.fee = "Fee_"+val;
            this.outputs.add(this.fee);
        }
    }

    public Transaction(){

    }

    public HashSet<String> getInputs(){
        return this.inputs;
    }

    public HashSet<String> getOutputs(){
        return this.outputs;
    }

    public boolean isEmpty(){
        return this.inputs.isEmpty() && this.outputs.isEmpty();
    }

    public boolean isConclusive(){
        return this.inputs.size() == 1;
    }

    private Transaction copy(){
        Transaction temp = new Transaction();
        temp.inputs.addAll(this.inputs);
        temp.outputs.addAll(this.outputs);
        return temp;
    }

    public Transaction add(Transaction other){
        Transaction temp = this.copy();
        temp.inputs.addAll(other.inputs);
        temp.outputs.addAll(other.outputs);
        temp.cutThrough();
        return temp;
    }

    public Transaction reduce(Transaction other){
        Transaction temp = this.copy();
        for (String input : other.inputs){
            temp.inputs.remove(input);
        }
        for (String output : other.outputs){
            temp.outputs.remove(output);
        }
        return temp;
    }

    private void cutThrough(){
        List<String> to_remove = new ArrayList<String>();
        for (String input : this.inputs){
            if (this.outputs.contains(input)){
                to_remove.add(input);
            }
        }
        for(String remove : to_remove){
            this.inputs.remove(remove);
            this.outputs.remove(remove);
        }
    }

    public void print(){
        System.out.println(this);
    }

    @Override
    public String toString(){
        return "(" + this.inputs.toString() + ") -> (" + this.outputs.toString() +") " + hash;
    }

    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(this == o){
            return true;
        }

        if(o instanceof Transaction){
            Transaction other = (Transaction) o;

            return other.hashCode() == this.hashCode();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        if (hash == 0) {
            int result = Arrays.deepHashCode(this.inputs.toArray());
            result = result * 31 + Arrays.deepHashCode(this.outputs.toArray());
            hash = result;
        }
        return hash;
    }

}
