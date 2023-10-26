package org.shortpasta.icmp2.tool;

import org.shortpasta.icmp2.IcmpPingUtil;
import org.shortpasta.icmp2.IcmpPingRequest;
import org.shortpasta.icmp2.IcmpPingResponse;
import org.shortpasta.icmp2.util.ArgUtil;

import java.util.Scanner;

public class Ping {

  public static void main (final String[] args) {

    try {
      // Khởi tạo đối tượng Scanner để nhập địa chỉ IP hoặc tên máy chủ từ người dùng
      Scanner scanner = new Scanner(System.in);
      System.out.print("Nhập địa chỉ IP hoặc tên máy chủ: ");
      String host = scanner.nextLine();

      // Thực hiện ping
      final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
      request.setHost(host);

      for (int count = 1; count <= 4; count++) { // Repeat 4 times by default
        final IcmpPingResponse response = IcmpPingUtil.executePingRequest(request);
        final String formattedResponse = IcmpPingUtil.formatResponse(response);
        System.out.println(formattedResponse);
        Thread.sleep(1000);
      }

      // Đóng Scanner
      scanner.close();
    } catch (final Throwable t) {
      t.printStackTrace();
    }
  }
}
