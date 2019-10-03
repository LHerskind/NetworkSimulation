import Dandelion.DandelionNode;
import Dandelion.Network;
import utils.KnowledgeUtil;

import java.util.ArrayList;

public class Main {


    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        String nodeType = "Grin";

        Network network = new Network(64, 8, 6000, 10, nodeType);

        network.startNodes();
        network.waitForRun();

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Time in seconds: " + elapsed / 1000);

        network.printStats();

        ArrayList<DandelionNode> observerNodes = new ArrayList<>();
        observerNodes.add(network.getNode(0));
        observerNodes.add(network.getNode(1));
        observerNodes.add(network.getNode(2));

        KnowledgeUtil.analysis(observerNodes, false, false);
    }

}
