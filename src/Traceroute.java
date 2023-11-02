import org.shortpasta.icmp2.IcmpPingRequest;
import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.IcmpPingUtil;

import java.util.ArrayList;
import java.util.List;

public class Traceroute {

    public static void main(String[] args) {
        String host = "facebook.com"; // Đặt tên miền mặc định là facebook.com

        List<String> tracerouteResults = performTraceroute(host);
        for (String result : tracerouteResults) {
            System.out.println(result);
        }
    }

    private static List<String> performTraceroute(String host) {
        List<String> results = new ArrayList<>();
        IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
        request.setHost(host);

        for (int ttl = 1; ttl <= 30; ttl++) {
            request.setTtl(ttl);
            IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);

            if (response.getTimeoutFlag()) {
                results.add(String.format("%2d     *        *        *        *", ttl));
            } else {
                String resultLine = String.format("%2d     %3d ms     %3d ms     %3d ms     %s", ttl, response.getRtt(), response.getRtt(), response.getRtt(), response.getHost());
                results.add(resultLine);

                if (response.getSuccessFlag()) {
                    results.add("Traceroute completed.");
                    break;
                }
            }
        }

        return results; // Trả về kết quả là danh sách các dòng traceroute
    }
}
