import org.shortpasta.icmp2.IcmpPingRequest;
import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.IcmpPingUtil;

public class Traceroute {

    public void performTraceroute(String input) {
        int ttl;
        final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
        request.setHost(input);
        final IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);
        final String formattedResponse = IcmpPingUtil.formatResponse(response);
        ttl = Integer.parseInt(formattedResponse.split("TTL=", 2)[1]);
        for(int i = 1; i <= ttl; i++)
        {
            IcmpPingRequest req = IcmpPingUtil.createIcmpPingRequest();
            request.setHost(input);
            request.setTtl(ttl);

            IcmpPingResponse res = IcmpPingUtil.executePingRequest(req);

            if (response.getErrorMessage() != null) {
                break;
            }

            String intermediateHopAddress = res.getHost();
            System.out.println("Hop " + i + ": " + intermediateHopAddress);

        }
    }


    public static void main(String[] args) {
        String target = "facebook.com";
        Traceroute traceroute = new Traceroute();
        traceroute.performTraceroute(target);
    }
}
