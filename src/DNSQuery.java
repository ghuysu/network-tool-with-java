import org.xbill.DNS.*;

public class DNSQuery {
    public static void main(String[] args) throws TextParseException {
        // Tạo yêu cầu DNS
        Name name = Name.fromString("giahuy.com");
        Record[] records = new Lookup(name, Type.A).run();

        if (records == null) {
            System.out.println("Không tìm thấy thông tin DNS cho tên miền.");
        } else {
            for (Record record : records) {
                if (record instanceof ARecord) {
                    ARecord a = (ARecord) record;
                    System.out.println("IP Address: " + a.getAddress());
                }
            }
        }
    }
}