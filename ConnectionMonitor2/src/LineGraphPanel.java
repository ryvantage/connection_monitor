
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author rwils
 */
public class LineGraphPanel extends ChartPanel {

    public LineGraphPanel(JFreeChart lineChart) {
        super(lineChart);
    }

    public static void main(String[] args) {
        
        dataset = new DefaultCategoryDataset();
        
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Connection Results",
                "Second", "Time (ms)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        
        JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        LineGraphPanel lgp = new LineGraphPanel(lineChart);
        jsp.setLeftComponent(lgp);
        lgp.setPreferredSize(new java.awt.Dimension(560, 367));
        
        jsp2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        jsp.setRightComponent(jsp2);
        jsp.setDividerLocation(800);
        
        JFrame frame = new JFrame();
        frame.add(jsp);
        frame.setSize(1200,600);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        new ProcessPingTask("ufl.edu");
        new ProcessPingTask("192.168.1.1");
    }

    static JSplitPane jsp2;
    static DefaultCategoryDataset dataset;
    static Integer startSecond;
    static boolean first = true;
    
    static class ProcessPingTask extends SwingWorker<Void, Integer> {

        final String server;
        JTextArea textArea;

        public ProcessPingTask(String server) {
            this.server = server;
            textArea = new JTextArea();
            textArea.setEditable(false);
            if(first)
                jsp2.setLeftComponent(textArea);
            else {
                jsp2.setRightComponent(textArea);
                jsp2.setDividerLocation(200);
            }
            first = false;
            execute();
        }
        
        @Override
        protected Void doInBackground() throws Exception {

            try {
                ProcessBuilder builder = new ProcessBuilder(
                        "cmd.exe", "/c", "ping " + server + " -t");
                builder.redirectErrorStream(true);
                Process p = builder.start();
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while (true) {
                    line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                    Pattern pattern = Pattern.compile(".*time=([\\d]+)ms.*");
                    Matcher matcher = pattern.matcher(line);
                    if(matcher.matches()) {
                        int time = Integer.parseInt(matcher.group(1));
                        int currentSecond = (int)(System.currentTimeMillis() / (long)1000);
                        if(startSecond == null)
                            startSecond = currentSecond;
                        int second = currentSecond + 1 - startSecond;
                        publish(time, second);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
            
            return null;
        }

        @Override
        protected void process(List<Integer> chunks) {
            dataset.addValue(chunks.get(0), server, chunks.get(1));
            textArea.append(server + " | time: " + chunks.get(0) + "\n");
        }

        @Override
        protected void done() {
            try {
                get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        
    }
}