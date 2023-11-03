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
        JTextArea outputArea = new JTextArea(10, 50);
        outputArea.setEditable(false);

        // Thêm JComboBox cho kiểu truy vấn
        String[] queryTypes = {"A", "AAAA", "MX", "CNAME", "TXT", "NS", "PTR", "SRV", "SOA"};
        JComboBox<String> queryTypeComboBox = new JComboBox<>(queryTypes);

        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputArea.setText("");
                String domain = inputField.getText();
                String queryType = queryTypeComboBox.getSelectedItem().toString();

                SimpleResolver resolver = null; // Đặt máy chủ DNS tại đây
                try {
                    resolver = new SimpleResolver("8.8.8.8");
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }

                try {
                    Lookup lookup = new Lookup(domain, Type.value(queryType));
                    lookup.setResolver(resolver);
                    Record[] records = lookup.run();
                    for (Record record : records) {
                        if (queryType.equals("A") && record instanceof ARecord) {
                            ARecord aRecord = (ARecord) record;
                            outputArea.append(aRecord.getAddress().getHostAddress() + "\n");
                        } else if (queryType.equals("AAAA") && record instanceof AAAARecord) {
                            AAAARecord aaaaRecord = (AAAARecord) record;
                            outputArea.append(aaaaRecord.getAddress().getHostAddress() + "\n");
                        } else if (queryType.equals("MX") && record instanceof MXRecord) {
                            MXRecord mxRecord = (MXRecord) record;
                            outputArea.append("Priority: " + mxRecord.getPriority() + ", Mail Server: " + mxRecord.getTarget() + "\n");
                        } else if (queryType.equals("CNAME") && record instanceof CNAMERecord) {
                            CNAMERecord cnameRecord = (CNAMERecord) record;
                            outputArea.append("Canonical Name: " + cnameRecord.getTarget() + "\n");
                        } else if (queryType.equals("TXT") && record instanceof TXTRecord) {
                            TXTRecord txtRecord = (TXTRecord) record;
                            outputArea.append("Text Data: " + txtRecord.rdataToString() + "\n");
                        } else if (queryType.equals("NS") && record instanceof NSRecord) {
                            NSRecord nsRecord = (NSRecord) record;
                            outputArea.append("Name Server: " + nsRecord.getTarget() + "\n");
                        } else if (queryType.equals("PTR") && record instanceof PTRRecord) {
                            PTRRecord ptrRecord = (PTRRecord) record;
                            outputArea.append("Reverse Lookup Pointer: " + ptrRecord.getTarget() + "\n");
                        } else if (queryType.equals("SRV") && record instanceof SRVRecord) {
                            SRVRecord srvRecord = (SRVRecord) record;
                            outputArea.append("Service Priority: " + srvRecord.getPriority() + ", Target: " + srvRecord.getTarget() + "\n");
                        } else if (queryType.equals("SOA") && record instanceof SOARecord) {
                            SOARecord soaRecord = (SOARecord) record;
                            outputArea.append("Name Server: " + soaRecord.getHost() + ", Admin: " + soaRecord.getAdmin() + "\n");
                        }
                    }
                } catch (Exception ex) {
                    outputArea.setText("Error: " + ex.getMessage());
                }
            }
        });

        panel.add(label);
        panel.add(inputField);
        panel.add(queryTypeComboBox);
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
        JTextArea outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);

        traceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String host = inputField.getText();
                outputArea.setText("");
                if (isValidDomain(host) || isValidIpv4(host)) {
                    List<String> tracerouteResults = performTraceroute(host);
                    for (String result : tracerouteResults) {
                        outputArea.append(result + "\n");
                    }
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

    private static List<String> performTraceroute(String host) {
        List<String> results = new ArrayList<>();
        IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
        request.setHost(host);

        for (int ttl = 1; ttl <= 30; ttl++) {
            request.setTtl(ttl);
            IcmpPingResponse response1 = IcmpPingUtil.executePingRequest(request);
            IcmpPingResponse response2 = IcmpPingUtil.executePingRequest(request);
            IcmpPingResponse response3 = IcmpPingUtil.executePingRequest(request);

            if (response1.getTimeoutFlag() && response2.getTimeoutFlag() && response3.getTimeoutFlag()) {
                results.add(String.format("%2d       *          *          *        Request timed out.", ttl));
            }

            if (!response1.getTimeoutFlag() && !response2.getTimeoutFlag() && !response3.getTimeoutFlag()) {
                results.add(String.format("%2d     %3d ms     %3d ms     %3d ms     %s", ttl, response1.getRtt(), response2.getRtt(), response3.getRtt(), response1.getHost()));

                if (response1.getSuccessFlag()) {
                    results.add("Traceroute completed.");
                    break;
                }
            }

            if (response1.getTimeoutFlag() && !response2.getTimeoutFlag() && !response3.getTimeoutFlag()) {
                results.add(String.format("%2d       *        %3d ms     %3d ms     %s", ttl, response2.getRtt(), response3.getRtt(), response2.getHost()));

                if (response2.getSuccessFlag()) {
                    results.add("Traceroute completed.");
                    break;
                }
            }

            if (!response1.getTimeoutFlag() && response2.getTimeoutFlag() && !response3.getTimeoutFlag()) {
                results.add(String.format("%2d     %3d ms       *        %3d ms     %s", ttl, response1.getRtt(), response3.getRtt(), response1.getHost()));

                if (response1.getSuccessFlag()) {
                    results.add("Traceroute completed.");
                    break;
                }
            }

            if (!response1.getTimeoutFlag() && !response2.getTimeoutFlag() && response3.getTimeoutFlag()) {
                results.add(String.format("%2d     %3d ms     %3d ms       *        %s", ttl, response1.getRtt(), response2.getRtt(), response1.getHost()));

                if (response1.getSuccessFlag()) {
                    results.add("Traceroute completed.");
                    break;
                }
            }
        }

        return results; // Trả về kết quả là danh sách các dòng traceroute
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