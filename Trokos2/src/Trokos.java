import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.Certificate ;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/*Como usar
 * java Trokos <serverAddress> <truststore> <keystore> <password-keystore> <userID>
 * por omissao o porto eh 45678 
 */
public class Trokos {
	
	private static String nonce;

	public static void main(String[] args) throws ClassNotFoundException, NumberFormatException, UnknownHostException, IOException, SignatureException {

		String hostname = "";
		int port = 0;

	/*	if (args.length != 5) {
			System.out.println("Insira os dados corretamente");
			return;
		}*/

		// fazer aqui arranjo dos dados
		String dadosServer = args[0];
		if (dadosServer.contains((":"))) {
			String[] dados = dadosServer.split((":"));
			hostname = dados[0];
			port = Integer.parseInt(dados[1]);
		} else {
			hostname=args[0];
			port = 45678;
		}
	
		String truststore = args[1];
		String keystore = args[2];
		String keystore_pw = args[3];
		String user = args[4];

		System.setProperty("javax.net.ssl.trustStore", "../stores/"+truststore);
		SocketFactory sf = SSLSocketFactory.getDefault();
		SSLSocket s = (SSLSocket) sf.createSocket(hostname, port);
		
				

		try {
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
			
			
			Scanner sc = new Scanner(System.in);

		/*	String dados = user + ":" + passwd;

			byte[] bDados = new byte[dados.length()];

			// converte os tamanhos dos ararys em bytes
			byte sizeU = (byte) args[1].length();
			byte sizeP = (byte) args[2].length();

			// envia o tamanho do username e da password ao servidor
			out.writeObject(sizeU);
			out.writeObject(sizeP);

			// converte os dados em bytes e poe no buffer
			char[] dadosArray = dados.toCharArray();
			int i = 0;
			for (char c : dadosArray) {
				bDados[i] = (byte) c;
				i++;
			}
			
			
			*/
			
			//O cliente envia o userID ao servidor
			out.writeObject(user);
			out.flush();
			
			//Recebe o noce vindo do servidor
			String n = (String) in.readObject();
			nonce = n;
			
			//if(not Registado)
			out.writeObject(nonce);
			out.flush();
			
			//cria assinatura para enviar ao servidor
			byte[] signature = criaAssinatura(user, keystore, keystore_pw);
			out.writeObject(signature);
			out.flush();
			
			byte[] certificate = recebeCertificado(user, keystore, keystore_pw);
			out.writeObject(certificate);
			out.flush();
		
			
			
		//	String respostaLogin = (String) in.readObject();
		//	System.out.println(respostaLogin);

			// Mudar por um enum(?)
		//	if (respostaLogin.equals("Utilizador autenticado")) {
				String ops = (String) in.readObject();
				System.out.println(ops);

				System.out.println("Escreva o que deseja fazer: ");
				String respostaOp = sc.nextLine();
				System.out.println();

				out.writeObject(respostaOp);
				out.flush();

				String respostaPedido = (String) in.readObject();

				if (respostaOp.contains("balance") || respostaOp.charAt(0) == 'b') {
					System.out.println("O seu balance e: " + respostaPedido);

				} else if (respostaOp.contains("makepayment") || respostaOp.charAt(0) == 'm') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("requestpayment") || respostaOp.charAt(0) == 'r') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("viewrequests") || respostaOp.charAt(0) == 'v') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("payrequest") || respostaOp.charAt(0) == 'p') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("obtainQRcode") || respostaOp.charAt(0) == 'o') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("confirmQRcode") || respostaOp.charAt(0) == 'c') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("newgroup") || respostaOp.charAt(0) == 'n') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("addu") || respostaOp.charAt(0) == 'a') {
					System.out.println(respostaPedido);

				} else if (respostaOp.contains("groups") || respostaOp.charAt(0) == 'g') {
					System.out.println(respostaPedido);
				} else if (respostaOp.contains("dividepayment") || respostaOp.charAt(0) == 'd') {
					System.out.println(respostaPedido);
				} else if (respostaOp.contains("statuspayments") || respostaOp.charAt(0) == 's') {
					System.out.println(respostaPedido);
				}else if (respostaOp.contains("history") || respostaOp.charAt(0) == 'h') {
					System.out.println(respostaPedido);
				}else {
					System.out.println(respostaPedido);
				}
			

			out.close();
			in.close();

			s.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	
	private static byte[] recebeCertificado(String user, String keystore, String keystore_pw) throws FileNotFoundException {
		FileInputStream kfile = new FileInputStream("../stores/"+user);
		KeyStore kstore;
		
		
		try {
			kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			Certificate c =  (Certificate) kstore.getCertificate(user);
			byte[] certificate = c.getEncoded();
			return certificate;
			
		} catch (KeyStoreException e) {
			
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
		
			e.printStackTrace();
		} catch (CertificateException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		return null;
	}

	private static byte[] criaAssinatura(String user, String keystore, String keystore_pw) throws FileNotFoundException {
		FileInputStream kfile = new FileInputStream("../stores/"+user);
		KeyStore kstore;
		Signature s;
		try {
			kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			PrivateKey myPrivateKey = (PrivateKey) kstore.getKey(user, keystore_pw.toCharArray());
			s =Signature.getInstance("MD5withRSA");
			s.initSign( myPrivateKey);
			
			byte [] signature = s.sign();
			return signature;
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
