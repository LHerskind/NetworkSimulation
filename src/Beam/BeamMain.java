package Beam;

import utils.KnowledgeUtil;

import java.util.ArrayList;

public class BeamMain {


    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        Network network = new Network(128, 8, 6000, 10);

        network.startNodes();
        network.waitForRun();

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Time in seconds: " + elapsed / 1000);

        network.printStats();

        ArrayList<BeamNode> observerNodes = new ArrayList<>();
        observerNodes.add(network.getNode(0));
        observerNodes.add(network.getNode(1));
        observerNodes.add(network.getNode(2));

        for(BeamNode node : observerNodes){
            // node.printKnowledge();
        }

        KnowledgeUtil.analysis(observerNodes);


    }

}
