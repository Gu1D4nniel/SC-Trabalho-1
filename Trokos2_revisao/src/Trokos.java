import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.cert.Certificate;
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

	public static void main(String[] args) throws ClassNotFoundException, NumberFormatException, UnknownHostException,
			IOException, SignatureException {

		String hostname = "";
		int port = 0;

		/*
		 * if (args.length != 5) { System.out.println("Insira os dados corretamente");
		 * return; }
		 */

		// fazer aqui arranjo dos dados
		String dadosServer = args[0];
		if (dadosServer.contains((":"))) {
			String[] dados = dadosServer.split((":"));
			hostname = dados[0];
			port = Integer.parseInt(dados[1]);
		} else {
			hostname = args[0];
			port = 45678;
		}

		String truststore = args[1];
		String keystore = args[2];
		String keystore_pw = args[3];
		String user = args[4];

		File file = new File("../stores/" + keystore);
		if (!file.exists() || !file.isFile()) {
			System.out.println("Esta keystore nao existe");
		}

		System.setProperty("javax.net.ssl.trustStore", "../stores/" + truststore);
		SocketFactory sf = SSLSocketFactory.getDefault();
		SSLSocket s = (SSLSocket) sf.createSocket(hostname, port);

		try {
			ObjectInputStream in = new ObjectInputStream(s.getInputStream());
			ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

			Scanner sc = new Scanner(System.in);

			// O cliente envia o userID ao servidor
			out.writeObject(user);
			out.flush();

			// Recebe o noce vindo do servidor
			String n = (String) in.readObject();
			nonce = n;

			boolean respostaBool = in.readBoolean();
			System.out.println(respostaBool);

			String resposta = "";

			// User nao esta registado
			if (respostaBool == false) {

				out.writeObject(nonce);
				out.flush();
				int nRecebido = in.readInt();
				if (nRecebido == 1) {
					System.out.println("O nonce eh o mesmo que foi criado pelo server");
				} else {
					System.out.println("O nonce nao eh o mesmo criado pelo servidor");
				}
				// cria assinatura para enviar ao servidor
				byte[] signature = criaAssinatura(user, keystore, keystore_pw);
				out.writeObject(signature);
				out.flush();
				// cria certificado e envia
				byte[] certificate = recebeCertificado(user, keystore, keystore_pw);
				out.writeObject(certificate);
				out.flush();

				int checkAssinatura = in.readInt();
				if (checkAssinatura == 1) {
					System.out.println("Assinatura esta certa");
				} else {
					System.out.println("Assinatura esta errada");
				}

				resposta = (String) in.readObject();
				System.out.println(resposta);
				// User esta registado
			} else {
				// Assinar o nonce
				byte[] nonceAssinado = assinaNonce(user, nonce, keystore, keystore_pw);
				out.writeObject(nonceAssinado);
				out.flush();
				int checkNonce = in.readInt();
				if (checkNonce == 0) {
					System.out.println("Erro no nonce assinado");
				} else {
					System.out.println("Nonce assinado certo");
				}

				resposta = (String) in.readObject();
				System.out.println(resposta);
			}

			String ops = (String) in.readObject();
			System.out.println(ops);

			System.out.println("Escreva o que deseja fazer: ");
			String respostaOp = sc.nextLine();
			System.out.println();

			out.writeObject(respostaOp);
			out.flush();

			// transacoes com assinatura
			if (respostaOp.contains("makepayment") || respostaOp.charAt(0) == 'm') {
				//obter destino e valor
				String[] line = respostaOp.split(" ");
				String destino = line[1];
				String valor = line[2];

				//assinar destino e valor
				byte[] assinatura = assinaTransacao(destino, valor, user, keystore_pw);
				
				//enviar para o server
				out.writeObject(assinatura);
				out.flush();

				//resposta server
				String respostaPedido = (String) in.readObject();
				System.out.println(respostaPedido);
				
				
			} else if (respostaOp.contains("payrequest") || respostaOp.charAt(0) == 'p') {
				//guarda id do request
				String[] lineRequest = respostaOp.split(" ");
				String id = lineRequest[1];

				//verifica qual eh o request com o tal id
				String requests = (String) in.readObject();

				String destino = "";
				String valor = "";
				String[] lines = requests.split("\n");
				for (String l : lines) {
					if (l.contains(id)) {
						String[] line = l.split("/");
						destino = line[1];
						valor = line[2];
					}
				}
				//assina com o destino e o valor e envia para o server
				byte[] assinatura = assinaTransacao(destino, valor, user, keystore_pw);
				out.writeObject(assinatura);
				out.flush();
				
				//resposta server
				String respostaPedido = (String) in.readObject();
				System.out.println(respostaPedido);
			} else if (respostaOp.contains("confirmQRcode") || respostaOp.charAt(0) == 'c') {
				
				
				
				
				// RESPOSTA DE OPERACOES NORMAIS
			} else {

				String respostaPedido = (String) in.readObject();

				System.out.println(respostaPedido);
			}
			out.close();
			in.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
		s.close();
	}

	private static byte[] assinaTransacao(String destino, String valor, String user, String keystore_pw)
			throws FileNotFoundException {

		FileInputStream kfile = new FileInputStream("../stores/" + user);
		String transacao = destino + "/" + valor;

		try {
			KeyStore kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			PrivateKey pk = (PrivateKey) kstore.getKey(user, keystore_pw.toCharArray());
			Signature s = Signature.getInstance("MD5withRSA");
			s.initSign(pk);
			byte buf[] = transacao.getBytes();
			s.update(buf);

			return s.sign();

		} catch (UnrecoverableKeyException e) {

			e.printStackTrace();
		} catch (KeyStoreException e) {

			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		} catch (InvalidKeyException e) {

			e.printStackTrace();
		} catch (SignatureException e) {

			e.printStackTrace();
		} catch (CertificateException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return null;
	}

	private static byte[] assinaNonce(String user, String nonce2, String keystore, String keystore_pw)
			throws FileNotFoundException {

		FileInputStream kfile = new FileInputStream("../stores/" + user);

		try {
			KeyStore kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			PrivateKey pk = (PrivateKey) kstore.getKey(user, keystore_pw.toCharArray());
			Signature s = Signature.getInstance("MD5withRSA");
			s.initSign(pk);
			byte buf[] = nonce2.getBytes();
			s.update(buf);

			return s.sign();
		} catch (UnrecoverableKeyException e) {

			e.printStackTrace();
		} catch (KeyStoreException e) {

			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {

			e.printStackTrace();
		} catch (InvalidKeyException e) {

			e.printStackTrace();
		} catch (SignatureException e) {

			e.printStackTrace();
		} catch (CertificateException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}

		return null;
	}

	private static byte[] recebeCertificado(String user, String keystore, String keystore_pw)
			throws FileNotFoundException {
		FileInputStream kfile = new FileInputStream("../stores/" + user);
		KeyStore kstore;

		try {
			kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			Certificate c = (Certificate) kstore.getCertificate(user);
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

	private static byte[] criaAssinatura(String user, String keystore, String keystore_pw)
			throws FileNotFoundException {
		FileInputStream kfile = new FileInputStream("../stores/" + user);
		KeyStore kstore;
		Signature s;
		try {
			kstore = KeyStore.getInstance("JCEKS");
			kstore.load(kfile, keystore_pw.toCharArray());
			PrivateKey myPrivateKey = (PrivateKey) kstore.getKey(user, keystore_pw.toCharArray());
			s = Signature.getInstance("MD5withRSA");
			s.initSign(myPrivateKey);

			byte[] signature = s.sign();
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
