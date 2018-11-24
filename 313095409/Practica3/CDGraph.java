import org.graphstream.graph.Graph;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import org.graphstream.graph.Node;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.View;
import org.graphstream.graph.implementations.SingleGraph;
import java.util.Comparator;
import java.text.Collator;


public class CDGraph extends Thread{

    private Graph graph;
    private Set<CDNode> nodes;
    private ConcurrentLinkedDeque<String[]> list;
    private boolean active;
    private JFrame frame;
    private String source;
    private String destination;

    public CDGraph(Graph g){
        this.graph = g;
        this.nodes = new HashSet<CDNode>();
        list = new ConcurrentLinkedDeque<String[]>();
        this.active = true;
    }

    public void setSource(String source){
        this.source = source;
    }

    public void setDestination(String destination){
        this.destination = destination;
    }

    public void run(){
        graph.display();
        for( Node i : graph.getEachNode() ){
            i.addAttribute("ui.label", i.getId());
            CDNode cdn = null;
                cdn = new CDNode(this, i, CDNode.Type.SOURCE);
                if(i.getId().equals(source)){
            }else if(i.getId().equals(destination)){
                cdn = new CDNode(this, i, CDNode.Type.DESTINATION);
            }else{
                cdn = new CDNode(this, i);
            }
            new Thread(cdn).start();
            nodes.add(cdn);
        }
        this.createFrame();
        while(active || !list.isEmpty()){
            while(!list.isEmpty()){
                String[] tmp = list.poll();
                graph.getNode(tmp[0]).setAttribute("ui.class",  tmp[1]);
            }
            sleep(100);
        }
    }

    public void addChangeColor(String nodeId, String color){
        list.add(new String[]{nodeId, color});
    }

    private void createFrame(){
        frame = new JFrame("Práctica 3");
        frame.setSize(800, 800);
        frame.setLocationRelativeTo(null);                       // centramos la ventana en la pantalla

        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(null);

        frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 517, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
        jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGap(0, 342, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanel1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(frame.getContentPane());
        frame.getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
        );

        frame.pack();

        JButton button = new JButton("Detener");
        button.setSize(800, 30);
        button.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                stopAction();
            }
        });
        jPanel1.add(button);

        Iterator<CDNode> iterator = nodes.iterator();
        int y = 30;
        while(iterator.hasNext()){
            JComponent component = iterator.next();
            component.setSize(760, 30);
            component.setLocation(30, y);
            y+=35;
            jPanel1.add(component);
        }
        frame.setVisible(true);

    }

    private void sleep(int ms){
        try{
            Thread.sleep(ms);
        }catch(Exception ex){
        }
    }

    public void stopAll(){
        Iterator<CDNode> iterator = nodes.iterator();
        while(iterator.hasNext()){
            iterator.next().stop();
        }
        active = false;
        while(this.isAlive()){
            System.out.println("Finalizando cambios en la grafica espere...");
            sleep(1000);
        }
        renderComputacionesEquivalentes();
    }

    private void stopAction(){
        this.stopAll();
    }

    /**
    *  Termina el metedo para localizar todas las computaciones equivalentes
    *  Las computaciones equivalentes que tienen que encontrar de ley son [A, B, E, F, D] y [A, E, B, F, D]
    */
    private void renderComputacionesEquivalentes(){
        Graph equivalentes = new SingleGraph("Equivalentes");
        CDNode nDestino = null;

        Iterator<CDNode> iterator = nodes.iterator();
        //Busamos en CDNode de destino en nodes
        while(iterator.hasNext()) {
          CDNode actual = iterator.next();
          if (actual.getNode().getId().equals(destination)) {
            nDestino = actual;
            break;
          }
        }

        //Obtenemos sus mensajes
        LinkedList<Message> recibidos = nDestino.recibidos();
        LinkedList<LinkedList<String>> recorridos = new LinkedList();

        //Obtenemos los recorridos que terminan en el destino de los mensajes obtenidos
        for (Message m : recibidos) {
          if (m.getRecorrido().getLast().equals(destination)) {
            recorridos.add(m.getRecorrido());
          }
        }

        LinkedList<LinkedList<String>> compEquivalentes = new LinkedList();

        //A cada recorrido lo ordenamos y a los recorridos de una lista sin el recorrido orginal
        //y si el recorrido ordenado es igual a alguno de los recorridos ordenados, el original se
        //agrega a la lista de computaciones equivalantes.
        for (LinkedList<String> re : recorridos) {

          LinkedList<LinkedList<String>> recorridos_ = (LinkedList<LinkedList<String>>)recorridos.clone();
          LinkedList<String> r = (LinkedList<String>)re.clone();

          while(recorridos_.remove(r)){}

          for (LinkedList<String> r_ : recorridos_) {
            if (r.equals(r_)) {
              while(recorridos_.remove(r_)){}
            }
          }

          r.sort(new Comparator<String>(){
                  @Override
                      public int compare(String o1,String o2){
                          return Collator.getInstance().compare(o1,o2);
                      }
                  });

          for (LinkedList<String> r_ : recorridos_) {
            LinkedList<String> r__ = (LinkedList<String>) r_.clone();

            r__.sort(new Comparator<String>(){
                    @Override
                        public int compare(String o1,String o2){
                            return Collator.getInstance().compare(o1,o2);
                        }
                    });

            if (r.equals(r__)) {
              compEquivalentes.add(re);
              break;
            }
          }
        }

        //Quitamos los repetidos
        LinkedList<LinkedList<String>> eq = new LinkedList();
        for (LinkedList<String> r : compEquivalentes) {
          if (!eq.contains(r))
            eq.add(r);
        }

        System.out.println("Computaciones equivalentes:");
        for (LinkedList<String> r : eq) {
          System.out.println(r);
        }

        int index = 0;
        int subG = 1;
        int subId = 1;
        boolean firstNodeSubGraph = true;
        for (LinkedList<String> r : eq) {
          for (String n : r) {
            equivalentes.addNode("Sub"+subG+"_"+n+subId);
            equivalentes.getNode("Sub"+subG+"_"+n+subId).addAttribute("ui.label","Sub"+subG+"_"+n+subId);
            if (!firstNodeSubGraph) {
              equivalentes.addEdge(subG+":"+(index-1)+":"+index,index-1,index);
            }else{
              firstNodeSubGraph = false;
            }
            index++;
            subId++;
          }
          firstNodeSubGraph = true;
          subId = 0;
          subG++;
        }
        equivalentes.display();
    }
}
