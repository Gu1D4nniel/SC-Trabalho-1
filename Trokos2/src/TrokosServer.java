import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.google.zxing.WriterException;

/*Como usar 
 * java TrokosServer <port>
 * por omissao o porto eh 45678
 * 
 * 
 * java TrokosServer clearDB 
 * limpa a db (ajuda a testar)
 */
public class TrokosServer extends Thread {

	private Socket socket;
	private static int port;
	private static String cifra_pw;
	private static String keystore;
	private static String keystore_pw;

	private float startBalance = 100;

	private final String ops = "------------------------------------------------------------\nOperaçoes:\nbalance\nmakepayment <userID><amount>"
			+ "\nrequestpayment <userID> <valor>\nviewrequests\npayrequest <reqID>\nobtainQRcode <amount>\nconfirmQRcode <QRcode>"
			+ "\nnewgroup <groupID>\naddu <userID><groupID>\ngroups"
			+ "\ndividepayment <groupID><amount>\nstatuspayments <groupID>\nhistory< <groupID>"
			+ "\n------------------------------------------------------------\n";

	public TrokosServer(Socket sock) throws Exception {
		socket = sock;
		Operacoes op = new Operacoes();
		op.run();
	}

	public static void main(String[] args) throws Exception {

		// guarda porto
		if (args.length == 0) {

			port = 45678;
		}
		// clearDB
		else if (args[0].equals("clearDB")) {

			clearDB();

			return;

		} else {
			port = Integer.parseInt(args[0]);
			cifra_pw = args[1];
			keystore = args[2];
			keystore_pw = args[3];

		}

		System.out.println("servidor: main\nport:" + port);

		System.setProperty("javax.net.ssl.keyStore", "stores/" + keystore);
		System.setProperty("javax.net.ssl.keyStorePassword", keystore_pw);

		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
		SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);

		while (true) {
			new TrokosServer(ss.accept()).start();

		}

	}

	/**
	 * Este metodo apaga todos os ficheiros que existem na db
	 */
	private static void clearDB() {
		File fileToDelete = new File("db.txt");
		fileToDelete.delete();

		File folderData = new File("dataUsers");
		File folderGrupos = new File("groups");
		File folderRequests = new File("requests");
		File folderQRcodes = new File("QRcodes");
		File folderQRrequests = new File("QRrequests");
		File folderGroupPayments = new File("groupPayments");
		File folderGroupHistory = new File("groupHistory");
		File folderHistory = new File("history");

		File[] files = folderData.listFiles();
		for (File f : files) {

			if (f.isFile() && f.exists())
				f.delete();
		}
		File[] filesGrupo = folderGrupos.listFiles();
		for (File f : filesGrupo) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesRequests = folderRequests.listFiles();
		for (File f : filesRequests) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesQRrequests = folderQRrequests.listFiles();
		for (File f : filesQRrequests) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesQRcodes = folderQRcodes.listFiles();
		for (File f : filesQRcodes) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesGroupPayments = folderGroupPayments.listFiles();
		for (File f : filesGroupPayments) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesGroupHistory = folderGroupHistory.listFiles();
		for (File f : filesGroupHistory) {

			if (f.isFile() && f.exists())
				f.delete();
		}

		File[] filesHistory = folderHistory.listFiles();
		for (File f : filesHistory) {

			if (f.isFile() && f.exists())
				f.delete();
		}

	}

	class Operacoes extends OperationsTrokos {
		public void run() throws Exception {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				// cria db com users:passwords
				File fileDb = new File("db.txt");

				// cria folder com dados de cada user
				File dataFile = new File("dataUsers");
				dataFile.mkdir();

				// cria folder com dados de cada grupo
				File groupFile = new File("groups");
				groupFile.mkdir();

				// cria folder com os dados dos requests
				File requestsFile = new File("requests");
				requestsFile.mkdir();

				File qrRequests = new File("qrRequests");
				qrRequests.mkdir();

				File qrCodes = new File("QRcodes");
				qrCodes.mkdir();

				File groupPayments = new File("groupPayments");
				groupPayments.mkdir();

				File groupHistory = new File("groupHistory");
				groupHistory.mkdir();

				File userHistory = new File("history");
				userHistory.mkdir();

				// Tem o true pq senao faz sempre overwrite aos dados do ficheiro
				PrintWriter pw = new PrintWriter(new FileWriter(fileDb, true));
				Scanner sc = new Scanner(fileDb);

				try {

					String user = null;
					String passwd = null;

					// Servidor recebe o userId do cliente
					user = (String) inStream.readObject();
					System.out.println(user);

					String nonce = generateNonce();
					outStream.writeObject(nonce);
					outStream.flush();
					// recebe o tamanho do username e da passwd do client (em bytes)
					// byte sizeU = (Byte) inStream.readObject();
					// byte sizeP = (Byte) inStream.readObject();

					// transforma o tamanho de bytes para inteiros
					// int u = Byte.toUnsignedInt(sizeU);
					// int p = Byte.toUnsignedInt(sizeP);

					// char[] arrayU = new char[u + p + 1];

					// cria buffers para receber password e username
					// byte[] bufferU = new byte[arrayU.length];
					// byte[] bufferP = new byte[p];

					// recebe username
					// inStream.read(bufferU, 0, bufferU.length);

					// Verifica se o nonce que enviou ao cliente eh o que o servidor criou
					String nRecebido = (String) inStream.readObject();
					if (!nonce.equals(nRecebido)) {
						// outStream.writeObject("o nonce recebido nao corresponde ao nonce criado pelo
						// servidor");
					}

					// recebe assinatura do cliente
					byte[] assinatura = (byte[]) inStream.readObject();

					// recebe o certificado do cliente
					byte[] certificado = (byte[]) inStream.readObject();

					// verifica se a assinatura pode ser corretamente verificada com a chave publica
					// contida no certificado
					int verificacao = verificaAssinatura(assinatura, certificado);

					if (verificacao == 0) {
						// outStream.writeObject("A assinatura nao foi verificada pela chave publica
						// recebida");
					}

					int cifrarDados = cifrarDados(user, cifra_pw);

					// separa o username e a password que recebeu do buffer cliente
					// String line = new String(bufferU, StandardCharsets.UTF_8);
					// String[] arr = new String[line.length()];
					// arr = line.split(":");
					// user = arr[0].trim();
					// passwd = arr[1].trim();

					// Verificacao na db
					/*
					 * while (sc.hasNextLine()) { String input = sc.nextLine(); String username =
					 * input.substring(0, input.indexOf(':')); String password =
					 * input.substring(input.indexOf(':') + 1, input.length()); if
					 * (username.equals(user.trim()) && password.equals(passwd.trim())) {
					 * System.out.println("user autenticado");
					 * outStream.writeObject("Utilizador autenticado"); outStream.flush();
					 */ outStream.writeObject(ops);
					outStream.flush();

					String respostaOp = (String) inStream.readObject();
					System.out.println("O user escolheu:" + respostaOp);

					if (respostaOp.contains(("balance")) || respostaOp.charAt(0) == 'b') {
						String balance = String.valueOf(balance(user));

						outStream.writeObject(balance);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);

					} else if (respostaOp.contains(("makepayment")) || respostaOp.charAt(0) == 'm') {
						String[] split = respostaOp.split(" ");
						if (split.length != 3) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String destino = split[1];
						float valor = Float.parseFloat(split[2]);
						int resposta = makepayment(user, destino, valor);
						if (resposta == 0) {
							outStream.writeObject("Nao foi encontrado o destino");
							outStream.flush();
							outStream.close();
						} else if (resposta == -1) {
							outStream.writeObject("Nao tem saldo");
							outStream.flush();
							outStream.close();
						} else if (resposta == -2) {
							outStream.writeObject("O user nao pode ser igual ao destino");
							outStream.flush();
							outStream.close();
						} else {
							outStream.writeObject("O pagamento foi feito");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						}

						// REQUEST PAYMENT
					} else if (respostaOp.contains("requestpayment") || respostaOp.charAt(0) == 'r') {
						String[] split = respostaOp.split(" ");
						if (split.length != 3) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String destino = split[1];
						float valor = Float.parseFloat(split[2]);
						int check = requestpayment(user, destino, valor);

						if (check == 0) {
							outStream.writeObject("Este user nao existe");
							outStream.flush();
							outStream.close();
						}

						else if (check == 1) {
							outStream.writeObject("O pedido de pagamento foi feito com sucesso");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						}

					} else if (respostaOp.contains("viewrequests") || respostaOp.charAt(0) == 'v') {
						String resposta = viewrequests(user);

						outStream.writeObject(resposta);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);

					} else if (respostaOp.contains("payrequest") || respostaOp.charAt(0) == 'p') {
						String[] split = respostaOp.split(" ");
						if (split.length != 2) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}

						String id = split[1];
						int check = payrequest(id, user);
						if (check == 0) {
							outStream.writeObject("Utilizador nao tem saldo suficiente");
							outStream.flush();
							outStream.close();
						} else if (check == -1) {
							outStream.writeObject("Nao existe este id");
							outStream.flush();
							outStream.close();
						} else if (check == -2) {
							outStream.writeObject("Algo nao correu bem");
							outStream.flush();
							outStream.close();
						}

						else {

							outStream.writeObject("Pagamento feito");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						}
					}

					else if (respostaOp.contains("newgroup") || respostaOp.charAt(0) == 'n') {
						String[] split = respostaOp.split(" ");
						if (split.length != 2) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String nomeGrupo = split[1];
						int check = criaGrupo(user, nomeGrupo);
						if (check == 0) {
							outStream.writeObject("Este grupo ja existe");
							outStream.flush();
							outStream.close();
							return;

						}
						outStream.writeObject("Grupo criado com sucesso");
						outStream.flush();
						addToHistory(user, respostaOp);

					} else if (respostaOp.contains("dividepayment") || respostaOp.charAt(0) == 'd') {
						String[] split = respostaOp.split(" ");
						if (split.length != 3) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String nomeGrupo = split[1];
						float valor = Float.parseFloat(split[2]);
						int check = dividepayment(user, nomeGrupo, valor);
						if (check == 1) {
							outStream.writeObject("Divisao foi feito com sucesso");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						} else if (check == 0) {
							outStream.writeObject("O user que fez o pedido nao eh o dono do grupo");
							outStream.flush();
							outStream.close();
						} else if (check == -1) {
							outStream.writeObject("Este grupo nao existe");
							outStream.flush();
							outStream.close();
						} else if (check == -2) {
							outStream.writeObject("Ja existe um pagamento em curso");
							outStream.flush();
							outStream.close();
						}
					} else if (respostaOp.contains("statuspayments") || respostaOp.charAt(0) == 's') {
						String[] split = respostaOp.split(" ");
						if (split.length != 2) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String nomeGrupo = split[1];
						String membros = statuspayment(user, nomeGrupo);
						outStream.writeObject("Membros que ainda nao pagaram: " + membros);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);

					}

					else if (respostaOp.contains("addu") || respostaOp.charAt(0) == 'a') {
						String[] split = respostaOp.split(" ");
						if (split.length != 3) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String nomeMembro = split[1];
						String nomeGrupo = split[2];
						int check = adicionaAoGrupo(user, nomeMembro, nomeGrupo);
						if (check == 0) {

							outStream.writeObject("Nao existe este grupo");
							outStream.flush();
							outStream.close();

						} else if (check == -1) {
							outStream.writeObject("Apenas o owner pode adicionar membros");
							outStream.flush();
							outStream.close();
						} else if (check == -2) {
							outStream.writeObject("Este user ja faz parte do grupo");
							outStream.flush();
							outStream.close();
						} else {
							outStream.writeObject("Novo membro adicionado com sucesso");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						}
					} else if (respostaOp.contains("groups") || respostaOp.charAt(0) == 'g') {
						String grupos = groups(user);
						outStream.writeObject(grupos);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);
					} else if (respostaOp.contains("obtainQRcode") || respostaOp.charAt(0) == 'o') {
						String[] split = respostaOp.split(" ");

						if (split.length != 2) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}

						float valor = Float.parseFloat(split[1]);
						String qr = obtainQRcode(user, valor);

						outStream.writeObject(qr);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);

					} else if (respostaOp.contains("confirmQRcode") || respostaOp.charAt(0) == 'c') {
						String[] split = respostaOp.split(" ");

						if (split.length != 2) {
							outStream.writeObject("Dados nao estao completos");
							outStream.flush();
							outStream.close();
							return;
						}
						String id = split[1];
						if (confirmQRcode(user, id) == 1) {
							outStream.writeObject("Pagamento feito com sucesso");
							outStream.flush();
							outStream.close();
							addToHistory(user, respostaOp);
						}
					} else if (respostaOp.contains("history") || respostaOp.charAt(0) == 'h') {
						String history = history(user);
						outStream.writeObject(history);
						outStream.flush();
						outStream.close();
						addToHistory(user, respostaOp);
					}

					else {
						outStream.writeObject("Por favor inisira um pedido válido");
						outStream.flush();
						outStream.close();

					}

					return;
					/*
					 * if (username.equals(user.trim()) && !password.equals(passwd.trim())) {
					 * System.out.println("password incorreta");
					 * outStream.writeObject("Password incorreta"); outStream.flush();
					 * outStream.close(); return; }
					 */

					// se nao existir no ficheiro cria um novo utilizador e a sua conta
					// criarConta(user, passwd, outStream, pw);

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (WriterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalBlockSizeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				outStream.close();
				inStream.close();
				pw.close();
				sc.close();
				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private int cifrarDados(String user, String cifra_pw) throws Exception {
			String cert = user + ".cer";
			byte[] toCipher = (user + ":" + cert).getBytes();
			FileOutputStream outFile = new FileOutputStream("users.txt", true);
			byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea,
					(byte) 0xf2 };

			PBEKeySpec keySpec = new PBEKeySpec(cifra_pw.toCharArray(), salt, 20); // pass, salt, iterations
			SecretKeyFactory kf;
			try {
				kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
				SecretKey key = kf.generateSecret(keySpec);
				Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
				c.init(Cipher.ENCRYPT_MODE, key);

				byte[] output = c.doFinal(toCipher);
				if (output != null)
					outFile.write(output);

				outFile.flush();
				outFile.close();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// -------------------------------APENAS TESTE
/*
			PBEKeySpec pbeKeySpec = new PBEKeySpec(cifra_pw.toCharArray());
			SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
			SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

			FileInputStream fis = new FileInputStream("users.txt");
			salt = new byte[8];
			fis.read(salt);

			PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt, 20);

			Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, pbeParameterSpec);
			FileOutputStream fos = new FileOutputStream("plainfile_decrypted.txt");
			byte[] in = new byte[64];
			int read;
			while ((read = fis.read(in)) != -1) {
				byte[] output = cipher.update(in, 0, read);
				if (output != null)
					fos.write(output);
			}

			byte[] output = cipher.doFinal();
			if (output != null)
				fos.write(output);

			fis.close();
			fos.flush();
			fos.close();
*/
			return 0;
		}

		private int verificaAssinatura(byte[] assinatura, byte[] certificado) throws CertificateException {

			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(certificado));
			PublicKey pk = certificate.getPublicKey();
			Signature s;
			try {
				s = Signature.getInstance("MD5withRSA");
				s.initVerify(certificate);

				if (s.verify(assinatura)) {
					return 1;
				} else {
					return 0;
				}
			} catch (NoSuchAlgorithmException e) {

				e.printStackTrace();
			} catch (SignatureException e) {

				e.printStackTrace();
			} catch (InvalidKeyException e) {

				e.printStackTrace();
			}
			return 0;

		}

		private String generateNonce() {
			byte[] nonce = new byte[8];
			new SecureRandom().nextBytes(nonce);
			// long result;

			StringBuilder sb = new StringBuilder();
			for (byte b : nonce) {
				sb.append(String.format("%o", b));
			}
			String s = sb.toString();

			return s;
		}

		private void addToHistory(String user, String respostaOp) throws IOException {
			File file = new File("history/" + user + ".txt");

			PrintWriter pw = new PrintWriter(new FileWriter(file, true));

			pw.println(respostaOp);
			pw.close();

		}

		/**
		 * metodo que adiciona o user na db e cria um outro file com os seus
		 * dados(balance, requests, etc)
		 * 
		 * @param user      nome do utilizador
		 * @param passwd    password do utilizador
		 * @param outStream stream que envia mensagem ao utilizador quando eh criado a
		 *                  sua conta
		 * @param pw        printwriter para inserir os dados na db
		 * @throws IOException
		 */
		private void criarConta(String user, String passwd, ObjectOutputStream outStream, PrintWriter pw)
				throws IOException {

			pw.println(user + ":" + passwd);
			File fileUser = new File("dataUsers/" + user + ".txt");
			PrintWriter userWriter = new PrintWriter(new FileWriter(fileUser, true));

			userWriter.println("Balance:" + startBalance);
			userWriter.println("Owner of (groups):");
			userWriter.println("Member of (groups):");

			outStream.writeObject("Utilizador " + user + " foi criado com sucesso");
			outStream.flush();
			userWriter.close();

		}

	}
}
