package Dandelion;

public class Message {

    private Transaction message;
    private DandelionNode sender;
    private boolean stem;

    private int hash = 0;

    public Message(DandelionNode sender, Transaction message, boolean stem) {
        this.sender = sender;
        this.message = message;
        this.stem = stem;
    }

    Message copy(){
        return new Message(sender, message, stem);
    }

    void setSender(DandelionNode sender){
        this.sender = sender;
        this.hash = 0;
    }

    void setStem(boolean stem){
        this.stem = stem;
        this.hash = 0;
    }

    public void setMessage(Transaction message){
        this.message = message;
        this.hash = 0;
    }

    public boolean isStem(){
        return this.stem;
    }

    public Transaction getMessage(){
        return this.message;
    }

    public DandelionNode getSender(){
        return this.sender;
    }

    @Override
    public String toString(){
        String stem_string = this.stem ? "stem" : "fluff";
        return this.sender.getNodeName() +" (" + stem_string + ") -> " + this.message;
    }

    @Override
    public boolean equals(Object o){
        if(o == null){
            return false;
        }

        if(this == o){
            return true;
        }

        if(o instanceof Message){
            Message other = (Message) o;
            boolean b = this.stem == other.isStem();
            boolean c = this.message == other.getMessage();
            return b && c;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode(){
        if (hash == 0){
            int result = this.message.hashCode();
            if(this.stem){
                result +=1;
            }
            hash = result;
        }
        return hash;
    }
}
