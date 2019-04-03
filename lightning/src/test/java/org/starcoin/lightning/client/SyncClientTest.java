package org.starcoin.lightning.client;

import io.grpc.Channel;
import java.io.InputStream;
import javax.net.ssl.SSLException;
import org.junit.*;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.Payment;

public class SyncClientTest {

  @Test
  public void testAddInvoice() throws SSLException {
    InputStream cert=this.getClass().getClassLoader().getResourceAsStream("alice.cert");
    Channel channel=Utils.buildChannel(cert,"starcoin-firstbox",30009);
    SyncClient client = new SyncClient(channel);
    String value="abc";
    Invoice invoice = new Invoice(HashUtils.hash160(value.getBytes()),20);

    AddInvoiceResponse addInvoiceResponse=client.addInvoice(invoice);

    InputStream cert1=this.getClass().getClassLoader().getResourceAsStream("bob.cert");
    Channel channel1=Utils.buildChannel(cert1,"starcoin-firstbox",40009);
    SyncClient client1 = new SyncClient(channel1);
    System.out.println(client1.sendPayment(new Payment(addInvoiceResponse.getPaymentRequest())));

  }
}
