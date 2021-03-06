package org.starcoin.lightning.client;

import static junit.framework.TestCase.assertTrue;

import io.grpc.Channel;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.net.ssl.SSLException;
import org.junit.*;
import org.starcoin.lightning.client.core.AddInvoiceResponse;
import org.starcoin.lightning.client.core.Invoice;
import org.starcoin.lightning.client.core.Invoice.InvoiceState;
import org.starcoin.lightning.client.core.InvoiceList;
import org.starcoin.lightning.client.core.PayReq;
import org.starcoin.lightning.client.core.Payment;
import org.starcoin.lightning.client.core.PaymentResponse;
import org.starcoin.lightning.client.core.SettleInvoiceRequest;


@Ignore
public class SyncClientTest {

  SyncClient aliceCli;
  SyncClient bobCli;
  SyncClient arbCli;

  @Before
  public void init() throws SSLException {
    String host= "starcoin-firstbox";
    aliceCli = gencli(host,"alice.cert", 30009,"0201036c6e6402cf01030a103f34bc89eba51787c5d7cc6f9fab8afa1201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e657261746500000620a0c4d52bd9351e0cfeba45fe9375fc5631e06dd2cf11eeb291900d2444d6bb8c");
    bobCli = gencli(host,"bob.cert", 40009,"0201036c6e6402cf01030a1062636341698aadb76c3903354d113be71201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e6572617465000006202836c12717f72e94b28b850486196f2d7b38299017fdb68d5ab11cb0bb2c7a58");
    arbCli = gencli(host,"arb.cert",20009,"0201036c6e6402cf01030a1077ee9770560ab03adeef296fd961d7551201301a160a0761646472657373120472656164120577726974651a130a04696e666f120472656164120577726974651a170a08696e766f69636573120472656164120577726974651a160a076d657373616765120472656164120577726974651a170a086f6666636861696e120472656164120577726974651a160a076f6e636861696e120472656164120577726974651a140a057065657273120472656164120577726974651a120a067369676e6572120867656e65726174650000062088232ae979d750e917d4b0131576adbdf139311eb4f27376dd396a3ea628fd29");
  }

  @Test
  public void testAddInvoice() throws SSLException, NoSuchAlgorithmException {
    SyncClient client = aliceCli;
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);
    Invoice invoice = new Invoice(HashUtils.sha256(bytes), 20);
    AddInvoiceResponse addInvoiceResponse = client.addInvoice(invoice);
    PayReq req = client.decodePayReq(addInvoiceResponse.getPaymentRequest());
    assertTrue(req.getNumSatoshis() == 20);
    Invoice invoiceLookuped = client.lookupInvoice(req.getPaymentHash());
    assertTrue(invoiceLookuped != null);
    InvoiceList invoiceList = client.listInvoices(0, 20, false, false);
    assertTrue(invoiceList.getInvoices().size() > 1);
  }

  @Test
  public void testChannel() throws SSLException {
    SyncClient client = bobCli;
    org.starcoin.lightning.client.core.Channel lightningChannel = new org.starcoin.lightning.client.core.Channel(
        true, false, true, false);
    Assert.assertTrue(client.listChannels(lightningChannel).size() != 0);
  }

  @Test
  @Ignore
  public void testSendPayment() throws SSLException, NoSuchAlgorithmException {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);

    AddInvoiceResponse invoice = aliceCli
        .addInvoice(new Invoice(HashUtils.sha256(bytes), 20));
    PaymentResponse paymentResponse = bobCli.sendPayment(new Payment(invoice.getPaymentRequest()));
    Assert.assertEquals("", paymentResponse.getPaymentError());
    Invoice findInvoice = aliceCli
        .lookupInvoice(paymentResponse.getPaymentHash());
    Assert.assertEquals(InvoiceState.SETTLED, findInvoice.getState());
  }

  @Test
  public void testSendPaymentHTLC() throws SSLException, NoSuchAlgorithmException {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);

    System.out.println(HashUtils.bytesToHex(HashUtils.sha256(bytes)));
    AddInvoiceResponse invoice = arbCli
        .addInvoice(new Invoice(HashUtils.sha256(bytes), 20));

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        arbCli.settleInvoice(new SettleInvoiceRequest(bytes));
      }
    }).start();

    PaymentResponse paymentResponse = bobCli.sendPayment(new Payment(invoice.getPaymentRequest()));
    Assert.assertEquals("", paymentResponse.getPaymentError());

    Invoice findInvoice = arbCli
        .lookupInvoice(paymentResponse.getPaymentHash());
    System.out.println(findInvoice);
    Assert.assertEquals(InvoiceState.SETTLED, findInvoice.getState());
  }

  @Test
  public void testSendPaymentHTLCCancel() throws SSLException, NoSuchAlgorithmException {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);

    System.out.println(HashUtils.bytesToHex(HashUtils.sha256(bytes)));
    AddInvoiceResponse invoice = aliceCli
        .addInvoice(new Invoice(HashUtils.sha256(bytes), 20));

    System.out.println(invoice.getPaymentRequest());
    PayReq req = aliceCli.decodePayReq(invoice.getPaymentRequest());
    assertTrue(req.getNumSatoshis() == 20);

    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        Invoice findInvoice = aliceCli
            .lookupInvoice(req.getPaymentHash());
        System.out.println(findInvoice);
        Assert.assertEquals(InvoiceState.ACCEPTED, findInvoice.getState());

        aliceCli.cancelInvoice(HashUtils.hexToBytes(req.getPaymentHash()));
      }
    }).start();

    PaymentResponse paymentResponse = bobCli.sendPayment(new Payment(invoice.getPaymentRequest()));

    Invoice findInvoice = aliceCli
        .lookupInvoice(req.getPaymentHash());
    System.out.println(findInvoice);
    Assert.assertEquals(InvoiceState.CANCELED, findInvoice.getState());
  }

  @Test
  @Ignore
  public void testGetIdentityPubkey() throws SSLException {
    String identityPubkey = bobCli.getIdentityPubkey();
    Assert.assertEquals(
        "036f43da08f0525c975ba1f83d5b93fff7d1e4e4179bcdcd6d2e3054ee3f1d572f",
        identityPubkey);
  }


  private SyncClient gencli(String certPath, int port) throws SSLException {
    String peer = "starcoin-firstbox";
    InputStream cert = getClass().getClassLoader().getResourceAsStream(certPath);
    Channel channel = Utils.buildChannel(cert, peer, port);
    SyncClient client = new SyncClient(channel);
    return client;
  }

  private SyncClient gencli(String host,String certPath, int port,String macarron) throws SSLException {
    InputStream cert = getClass().getClassLoader().getResourceAsStream(certPath);
    Channel channel = Utils.buildChannel(cert,macarron, host, port);
    SyncClient client = new SyncClient(channel);
    return client;
  }

  private SyncClient gencliInsecure(String macarron, String host, int port) throws IOException {
    Channel channel = Utils.buildInsecureChannel(new File(macarron), host, port);
    SyncClient client = new SyncClient(channel);
    return client;
  }

  @Test
  public void testAlice() throws IOException, NoSuchAlgorithmException {
    SyncClient aliceCli = gencliInsecure("/tmp/thor/lnd/lnd_alice/data/chain/bitcoin/simnet/admin.macaroon","localhost",10009);

    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);

    System.out.println(HashUtils.bytesToHex(HashUtils.sha256(bytes)));
    AddInvoiceResponse invoice = aliceCli
        .addInvoice(new Invoice(HashUtils.sha256(bytes), 20));

    byte[] preimage=Base64.getDecoder().decode("/iEmusap5UCoQlASTKBRVndgHND+z+RcmuSMueZd/ZI=");
    aliceCli.settleInvoice(new SettleInvoiceRequest(preimage));
  }

}
