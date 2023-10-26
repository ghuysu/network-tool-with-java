import org.xbill.DNS.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.UnknownHostException;

public class DNSQueryApp {
    private JFrame frame;
    private JTextField domainField;
    private JComboBox<String> queryTypeComboBox;
    private JTextArea resultArea;

    public DNSQueryApp() {
        frame = new JFrame("DNS Query App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(null);

        JLabel domainLabel = new JLabel("Domain:");
        domainLabel.setBounds(20, 20, 80, 25);
        frame.add(domainLabel);

        domainField = new JTextField();
        domainField.setBounds(100, 20, 200, 25);
        frame.add(domainField);

        JLabel queryTypeLabel = new JLabel("Query Type:");
        queryTypeLabel.setBounds(20, 60, 80, 25);
        frame.add(queryTypeLabel);

        String[] queryTypes = {"A", "AAAA", "MX", "CNAME", "TXT", "NS", "PTR", "SRV", "SOA"};
        queryTypeComboBox = new JComboBox<>(queryTypes);
        queryTypeComboBox.setBounds(100, 60, 100, 25);
        frame.add(queryTypeComboBox);

        JButton queryButton = new JButton("Query");
        queryButton.setBounds(220, 60, 80, 25);
        frame.add(queryButton);

        resultArea = new JTextArea();
        resultArea.setBounds(20, 100, 340, 150);
        resultArea.setEditable(false);
        frame.add(resultArea);

        queryButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    performDNSQuery();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        frame.setVisible(true);
    }

    private void performDNSQuery() throws UnknownHostException {
        String domain = domainField.getText();
        String queryType = queryTypeComboBox.getSelectedItem().toString();

        SimpleResolver resolver = new SimpleResolver("8.8.8.8"); // Đặt máy chủ DNS tại đây

        try {
            Lookup lookup = new Lookup(domain, Type.value(queryType));
            lookup.setResolver(resolver);
            Record[] records = lookup.run();
            resultArea.setText("");
            for (Record record : records) {
                if (queryType.equals("A") && record instanceof ARecord) {
                    ARecord aRecord = (ARecord) record;
                    resultArea.append(aRecord.getAddress().getHostAddress() + "\n");
                } else if (queryType.equals("AAAA") && record instanceof AAAARecord) {
                    AAAARecord aaaaRecord = (AAAARecord) record;
                    resultArea.append(aaaaRecord.getAddress().getHostAddress() + "\n");
                } else if (queryType.equals("MX") && record instanceof MXRecord) {
                    MXRecord mxRecord = (MXRecord) record;
                    resultArea.append("Priority: " + mxRecord.getPriority() + ", Mail Server: " + mxRecord.getTarget() + "\n");
                } else if (queryType.equals("CNAME") && record instanceof CNAMERecord) {
                    CNAMERecord cnameRecord = (CNAMERecord) record;
                    resultArea.append("Canonical Name: " + cnameRecord.getTarget() + "\n");
                } else if (queryType.equals("TXT") && record instanceof TXTRecord) {
                    TXTRecord txtRecord = (TXTRecord) record;
                    resultArea.append("Text Data: " + txtRecord.rdataToString() + "\n");
                } else if (queryType.equals("NS") && record instanceof NSRecord) {
                    NSRecord nsRecord = (NSRecord) record;
                    resultArea.append("Name Server: " + nsRecord.getTarget() + "\n");
                } else if (queryType.equals("PTR") && record instanceof PTRRecord) {
                    PTRRecord ptrRecord = (PTRRecord) record;
                    resultArea.append("Reverse Lookup Pointer: " + ptrRecord.getTarget() + "\n");
                } else if (queryType.equals("SRV") && record instanceof SRVRecord) {
                    SRVRecord srvRecord = (SRVRecord) record;
                    resultArea.append("Service Priority: " + srvRecord.getPriority() + ", Target: " + srvRecord.getTarget() + "\n");
                } else if (queryType.equals("SOA") && record instanceof SOARecord) {
                    SOARecord soaRecord = (SOARecord) record;
                    resultArea.append("Name Server: " + soaRecord.getHost() + ", Admin: " + soaRecord.getAdmin() + "\n");
                }
            }
        } catch (Exception e) {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new DNSQueryApp();
            }
        });
    }
}
