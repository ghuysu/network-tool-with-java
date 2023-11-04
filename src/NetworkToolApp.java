import org.shortpasta.icmp2.IcmpPingRequest;
import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.IcmpPingUtil;
import org.shortpasta.icmp2.util.ArgUtil;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class NetworkToolApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Network Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setResizable(false);
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel pingPanel = createPingPanel();
        JPanel dnsQueryPanel = createDnsQueryPanel();
        JPanel scanIpPanel = createScanIpPanel();
        JPanel traceroutePanel = createTraceroutePanel();

        tabbedPane.addTab("Ping", pingPanel);
        tabbedPane.addTab("DNS Query", dnsQueryPanel);
        tabbedPane.addTab("Scan IP", scanIpPanel);
        tabbedPane.addTab("Traceroute", traceroutePanel);

        frame.add(tabbedPane);
        frame.setVisible(true);
    }

    private static JPanel createPingPanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter IP Address or Domain:");
        JTextField inputField = new JTextField(20);
        JButton pingButton = new JButton("Ping");
        JTextArea outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        pingButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String input = inputField.getText();
                    outputArea.setText("");
                    if (isValidDomain(input) || isValidIpv4(input))
                    {
                        final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
                        request.setHost(input);
                        for (int count = 1; count <= 4; count++) { // Repeat 4 times by default
                            final IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);
                            final String formattedResponse = IcmpPingUtil.formatResponse(response);
                            String text = outputArea.getText();
                            outputArea.setText(text + formattedResponse + "\n");
                            Thread.sleep(1000);
                        }
                    }
                    else
                        outputArea.setText(input + " is not a valid domain.");
                }
                catch (Exception ex){
                    System.out.println(ex);
                }
            }
        });

        panel.add(label);
        panel.add(inputField);
        panel.add(pingButton);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane);

        return panel;
    }

    private static JPanel createDnsQueryPanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter Domain:");
        JTextField inputField = new JTextField(20);
        JButton queryButton = new JButton("Query");
        JTextArea outputArea = new JTextArea(10, 48);
        outputArea.setEditable(false);

        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputArea.setText("");
                String domain = inputField.getText();
                if(!isValidDomain(domain))
                {
                    outputArea.setText("Domain is invalid! Please enter a correct domain!");
                    return;
                }
                SimpleResolver resolver = null;
                try {
                    resolver = new SimpleResolver("8.8.8.8");
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }

                try {
                    StringBuilder resultBuilder = new StringBuilder();
                    Lookup lookup = new Lookup(domain);
                    lookup.setResolver(resolver);
                    Record[] records;
                    boolean foundRecords = false;

                    for (String queryType : new String[]{"A", "AAAA", "MX", "CNAME", "TXT", "NS", "PTR", "SRV", "SOA"}) {
                        lookup = new Lookup(domain, Type.value(queryType));
                        lookup.setResolver(resolver);
                        records = lookup.run();
                        if (records != null) {
                            for (Record record : records) {
                                if (queryType.equals("A") && record instanceof ARecord) {
                                    ARecord aRecord = (ARecord) record;
                                    resultBuilder.append("IPv4: ").append(aRecord.getAddress().getHostAddress()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("AAAA") && record instanceof AAAARecord) {
                                    AAAARecord aaaaRecord = (AAAARecord) record;
                                    resultBuilder.append("IPv6: ").append(aaaaRecord.getAddress().getHostAddress()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("MX") && record instanceof MXRecord) {
                                    MXRecord mxRecord = (MXRecord) record;
                                    resultBuilder.append("Priority: ").append(mxRecord.getPriority())
                                            .append(", Mail Server: ").append(mxRecord.getTarget()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("CNAME") && record instanceof CNAMERecord) {
                                    CNAMERecord cnameRecord = (CNAMERecord) record;
                                    resultBuilder.append("Canonical Name: ").append(cnameRecord.getTarget()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("TXT") && record instanceof TXTRecord) {
                                    TXTRecord txtRecord = (TXTRecord) record;
                                    resultBuilder.append("Text Data: ").append(txtRecord.rdataToString()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("NS") && record instanceof NSRecord) {
                                    NSRecord nsRecord = (NSRecord) record;
                                    resultBuilder.append("Name Server: ").append(nsRecord.getTarget()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("PTR") && record instanceof PTRRecord) {
                                    PTRRecord ptrRecord = (PTRRecord) record;
                                    resultBuilder.append("Reverse Lookup Pointer: ").append(ptrRecord.getTarget()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("SRV") && record instanceof SRVRecord) {
                                    SRVRecord srvRecord = (SRVRecord) record;
                                    resultBuilder.append("Service Priority: ").append(srvRecord.getPriority())
                                            .append(", Target: ").append(srvRecord.getTarget()).append("\n");
                                    foundRecords = true;
                                } else if (queryType.equals("SOA") && record instanceof SOARecord) {
                                    SOARecord soaRecord = (SOARecord) record;
                                    resultBuilder.append("Name Server: ").append(soaRecord.getHost());
                                    foundRecords = true;
                                }
                            }
                        }
                    }

                    if (!foundRecords) {
                        outputArea.setText("No records found for the domain.");
                    } else {
                        outputArea.setText(resultBuilder.toString());
                    }
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        panel.add(label);
        panel.add(inputField);
        panel.add(queryButton);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane);
        return panel;
    }


    private static JPanel createScanIpPanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter IP Range:");
        JTextField startIpField = new JTextField(10);
        JTextField endIpField = new JTextField(10);
        JButton scanButton = new JButton("Scan");
        JTextArea outputArea = new JTextArea(10, 52);
        outputArea.setEditable(false);

        // Thêm JLabel và JTextField cho số IP reachable và số IP không thể reachable
        JLabel reachableLabel = new JLabel("Reachable IP:");
        JTextField reachableCountField = new JTextField(5);
        reachableCountField.setEditable(false);

        JLabel unreachableLabel = new JLabel("Unreachable IP:");
        JTextField unreachableCountField = new JTextField(5);
        unreachableCountField.setEditable(false);

        scanButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String startIp = startIpField.getText();
                String endIp = endIpField.getText();
                unreachableCountField.setText("0");
                reachableCountField.setText("0");
                outputArea.setText("");
                if (!isValidIpv4(startIp)) {
                    outputArea.setText(startIp + " is not a valid IPv4");
                } else if (!isValidIpv4(endIp)) {
                    outputArea.setText(endIp + " is not a valid IPv4");
                } else {
                    SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            String[] startIpParts = startIp.split("\\.");
                            int start = Integer.parseInt(startIpParts[3]);

                            String[] endIpParts = endIp.split("\\.");
                            int end = Integer.parseInt(endIpParts[3]);

                            int numThreads = Runtime.getRuntime().availableProcessors(); // Số lõi CPU có thể sử dụng
                            ExecutorService executor = Executors.newFixedThreadPool(numThreads);

                            // Tạo một danh sách để lưu trữ các công việc ping
                            List<Future<String>> futures = new ArrayList<>();

                            for (int i = start; i <= end; i++) {
                                String scanip = startIpParts[0] + "." + startIpParts[1] + "." + startIpParts[2] + "." + i;

                                // Sử dụng một Callable để thực hiện ping và trả về kết quả
                                Callable<String> pingTask = () -> {
                                    IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
                                    request.setHost(scanip);
                                    IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);
                                    return scanip + ": " + IcmpPingUtil.formatResponse(response);
                                };

                                // Thêm công việc ping vào danh sách công việc
                                futures.add(executor.submit(pingTask));
                            }

                            // Đợi cho đến khi tất cả các công việc hoàn thành và cập nhật giao diện với kết quả
                            for (Future<String> future : futures) {
                                String message = future.get(); // Lấy kết quả từ công việc ping
                                publish(message); // Đưa thông điệp vào EDT để cập nhật giao diện
                            }

                            // Đóng bộ executor khi hoàn thành
                            executor.shutdown();

                            return null;
                        }

                        @Override
                        protected void process(List<String> chunks) {
                            for (String chunk : chunks) {
                                if(chunk.contains("Error"))
                                    unreachableCountField.setText(String.valueOf(Integer.parseInt(unreachableCountField.getText()) + 1));
                                else
                                    reachableCountField.setText(String.valueOf(Integer.parseInt(reachableCountField.getText()) + 1));

                                outputArea.append(chunk+ "\n");
                            }
                        }
                    };

                    worker.execute();

                }
            }
        });


        panel.add(label);
        panel.add(startIpField);
        panel.add(new JLabel(" to "));
        panel.add(endIpField);
        panel.add(scanButton);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane);

        panel.add(reachableLabel);
        panel.add(reachableCountField);
        panel.add(unreachableLabel);
        panel.add(unreachableCountField);

        return panel;
    }


    private static JPanel createTraceroutePanel() {
        JPanel panel = new JPanel();
        JLabel label = new JLabel("Enter IP Address or Domain:");
        JTextField inputField = new JTextField(20);
        JButton traceButton = new JButton("Trace");
        JTextArea outputArea = new JTextArea(17, 52);
        outputArea.setEditable(false);

        traceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = inputField.getText();
                outputArea.setText("");
                if (isValidDomain(host) || isValidIpv4(host)) {
                    SwingWorker<Void, String> tracerouteWorker = new SwingWorker<Void, String>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
                            request.setHost(host);

                            for (int ttl = 1; ttl <= 30; ttl++) {
                                request.setTtl(ttl);

                                IcmpPingResponse[] responses = new IcmpPingResponse[3];
                                boolean allTimeout = true;

                                for (int i = 0; i < 3; i++) {
                                    responses[i] = IcmpPingUtil.executePingRequest(request);
                                    allTimeout &= responses[i].getTimeoutFlag();
                                }

                                if (allTimeout) {
                                    publish(String.format("%2d       *          *          *        Request timed out.", ttl));
                                } else {
                                    StringBuilder resultLine = new StringBuilder(String.format("%2d", ttl));

                                    for (IcmpPingResponse response : responses) {
                                        if (!response.getTimeoutFlag()) {
                                            resultLine.append(String.format("     %3d ms", response.getRtt()));
                                        } else {
                                            resultLine.append("       *   ");
                                        }
                                    }

                                    if (!responses[0].getTimeoutFlag()) {
                                        resultLine.append(String.format("     %s", responses[0].getHost()));
                                    } else if (!responses[1].getTimeoutFlag()) {
                                        resultLine.append(String.format("     %s", responses[1].getHost()));
                                    } else if (!responses[2].getTimeoutFlag()) {
                                        resultLine.append(String.format("     %s", responses[2].getHost()));
                                    }
                                    publish(resultLine.toString());

                                    if (responses[0].getSuccessFlag() || responses[1].getSuccessFlag() || responses[2].getSuccessFlag()) {
                                        publish("Traceroute completed.");
                                        break;
                                    }
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void process(List<String> chunks) {
                            for (String chunk : chunks) {
                                outputArea.append(chunk + "\n");
                            }
                        }
                    };

                    tracerouteWorker.execute();
                } else {
                    outputArea.setText(host + " is not a valid domain or IP address.");
                }
            }
        });

        panel.add(label);
        panel.add(inputField);
        panel.add(traceButton);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        panel.add(scrollPane);

        return panel;
    }

    public static boolean isValidDomain(String domain) {
        // Sử dụng biểu thức chính quy để kiểm tra tên miền
        String domainRegex = "^((?!-)[A-Za-z0-9-]{1,63}(?<!-)\\.)+[A-Za-z]{2,6}$";
        Pattern pattern = Pattern.compile(domainRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(domain);

        return matcher.matches();
    }

    public static boolean isValidIpv4(String ip)
    {
        String regex = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

}