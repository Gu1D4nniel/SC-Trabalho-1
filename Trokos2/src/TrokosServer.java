
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
public class TrokosServer extends OperationsTrokos {

	private Socket socket;
	private static int port;
	private static String cifra_pw;
	private static String keystore;
	private static String keystore_pw;

	private float startBalance = 100;

	private final String ops = "------------------------------------------------------------\nOperaçoes:\nbalance\nmakepayment <userID><amount>"
			+ "\nrequestpayment <userID> <valor>\nviewrequests\npayrequest <reqID>\nobtainQRcode <amount>\nconfirmQRcode <QRcode>"
			+ "\nnewgroup <groupID>\naddu <userID><groupID>\ngroups"
			+ "\ndividepayment <groupID><amount>\nstatuspayments <groupID>\nhistory\nquit"
			+ "\n------------------------------------------------------------\n";

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
		TrokosServer server = new TrokosServer();
		server.startServer(port);

	}

	public void startServer(int porto) throws IOException {
		ServerSocket sSoc = null;

		System.setProperty("javax.net.ssl.keyStore", "stores/" + keystore);
		System.setProperty("javax.net.ssl.keyStorePassword", keystore_pw);

		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
		SSLServerSocket ss = (SSLServerSocket) ssf.createServerSocket(port);

		while (true) {
			try {
				Socket inSoc = ss.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * Este metodo apaga todos os ficheiros que existem na db
	 */
	private static void clearDB() {
		File fileToDelete = new File("userx.txt");
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

	class ServerThread extends Thread

	{

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;

		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				// cria db com users:passwords
				// File fileDb = new File("db.txt");

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

				try {

					// Servidor recebe o userId do cliente
					String user = (String) inStream.readObject();

					// envia nonce

					String nonce = generateNonce();
					outStream.writeObject(nonce);
					outStream.flush();

					// recebe assinatura do cliente
					byte[] assinatura = (byte[]) inStream.readObject();

					// recebe o certificado do cliente
					byte[] certificado = (byte[]) inStream.readObject();

					byte[] nonceAssinado = (byte[]) inStream.readObject();

					File cif = new File ("user.cif");
					if(cif.exists())
						decifraUser("userx");
					
					File db = new File("userx");
					
				
					FileOutputStream fos = new FileOutputStream(db, true);
					if (!db.exists() || db.length() == 0 || contemUser(user) == 0) {
						outStream.writeBoolean(false);
						outStream.flush();

						// recebe nonce vidno do cliente String nRecebido = (String)
						String nRecebido = (String) inStream.readObject();
						if (nonce.equals(nRecebido)) {
							outStream.writeInt(1);
							outStream.flush();
						} else {
							outStream.writeInt(0);
							outStream.flush();
							return;
						}
						int v = verificaAssinatura(assinatura, certificado);
						if (v == 0) {
							outStream.writeInt(0);
							outStream.flush();
							return;
						} else {
							outStream.writeInt(1);
							outStream.flush();
						}

						fos.write((user + ":" + user + ".cer\n").getBytes());
						criarConta(user);
						

						outStream.writeObject("Utilizador registado e autenticado");
						outStream.flush();

						fos.close();
					} else {
						outStream.writeBoolean(true);
						outStream.flush();

						if (assinaturaNonce(nonce, nonceAssinado, certificado) == 0) {
							outStream.writeInt(0);
							outStream.flush();
						} else {
							outStream.writeInt(1);
							outStream.flush();
						}

						outStream.writeObject("user autenticado");
						outStream.flush();

					}

					
					cifraUser("userx");
					outStream.writeObject(ops);
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
						} else if (confirmQRcode(user, id) == 0) {
							outStream.writeObject("Erro no pagamento");
							outStream.flush();
							outStream.close();
						} else if (confirmQRcode(user, id) == -1) {
							outStream.writeObject("Quem fez o pedido nao pode fazer pagamento");
							outStream.flush();
							outStream.close();
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

					
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (NoSuchAlgorithmException e) {

					e.printStackTrace();
				} catch (CertificateException e) {

					e.printStackTrace();
				} catch (WriterException e) {

					e.printStackTrace();
				}
				
				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Metodo que cifra o ficheiro userx.txt
		 * @param string
		 */
		private void cifraUser(String string) {
			File file = new File(string);

			try {

				Cipher c = Cipher.getInstance("AES");
				KeyGenerator kg = KeyGenerator.getInstance("AES");
				kg.init(128);
				SecretKey key = kg.generateKey();
				c.init(Cipher.ENCRYPT_MODE, key);
				FileInputStream fis;
				FileOutputStream fos;
				CipherOutputStream cos;

				fis = new FileInputStream(file);
				fos = new FileOutputStream(string+".cif");

				cos = new CipherOutputStream(fos, c);
				byte[] b = new byte[32];
				int i = fis.read(b);
				while (i != -1) {
					cos.write(b, 0, i);
					i = fis.read(b);
				}

				cos.close();
				fis.close();
				fos.close();

				byte[] keyEncoded = key.getEncoded();
				FileOutputStream kos = new FileOutputStream(string+ ".key");

				kos.write(keyEncoded);
				file.delete();
				kos.close();
			}catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			}

		}

		
/**
 * Metodo que decifra o ficheiro userx.txt
 * @param string
 * @throws IOException 
 */
		private void decifraUser(String string) throws IOException {
			FileInputStream key = new FileInputStream(string+ ".key");
			byte[] a = key.readAllBytes();
			key.close();
			try {
				Cipher c = Cipher.getInstance("AES");
				c = Cipher.getInstance("AES");
				byte[] keyEncoded2 = a;
				SecretKeySpec keySpec2 = new SecretKeySpec(keyEncoded2, "AES");
				c.init(Cipher.DECRYPT_MODE, keySpec2); // SecretKeySpec � subclasse de secretKey

				FileInputStream fis2;
				FileOutputStream fos2;
				CipherOutputStream cos2;

				fis2 = new FileInputStream(string+ ".cif");
				fos2 = new FileOutputStream(string + ".txt");

				cos2 = new CipherOutputStream(fos2, c);
				byte[] b2 = new byte[32];
				int j = fis2.read(b2);
				while (j != -1) {
					cos2.write(b2, 0, j);
					j = fis2.read(b2);
				}

				cos2.close();
				fis2.close();
				fos2.close();
				fis2.close();

			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			
		}

		private int assinaturaNonce(String nonce, byte[] nonceAssinado, byte[] certificado)
				throws CertificateException {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(certificado));
			PublicKey pk = certificate.getPublicKey();
			Signature s;

			try {
				s = Signature.getInstance("MD5withRSA");
				s.initVerify(pk);
				// transforma o nonce criado pelo servidor num array de bytes para comparar com
				// a
				// nonce assinada recebida do cliente
				byte[] buf = nonce.getBytes();
				s.update(buf);

				if (s.verify(nonceAssinado)) {
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

		private int verificaAssinatura(byte[] assinatura, byte[] certificado) throws CertificateException {

			CertificateFactory cf = CertificateFactory.getInstance("X.509");

			Certificate certificate = cf.generateCertificate(new ByteArrayInputStream(certificado));
			PublicKey pk = certificate.getPublicKey();
			Signature s;
			try {
				s = Signature.getInstance("MD5withRSA");
				s.initVerify(pk);

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
			File folder = new File("history/");
			File file = new File("history/" + user + ".txt");

			PrintWriter pw = new PrintWriter(new FileWriter(file, true));

			pw.println(respostaOp);
			pw.close();
			cifraData(folder, user);

		}

		private void criarConta(String user) throws IOException {
			File folder = new File("dataUsers/");
			File fileUser = new File("dataUsers/" + user + ".txt");

			PrintWriter userWriter = new PrintWriter(new FileWriter(fileUser));

			userWriter.println("Balance:" + startBalance);
			userWriter.println("Owner of (groups):");
			userWriter.println("Member of (groups):");

			userWriter.close();

			cifraData(folder, user);

		}

		private int contemUser(String user) throws IOException {
			FileInputStream fis = new FileInputStream("userx.txt");
			InputStream ips = new BufferedInputStream(fis);
			String text = "";

			int i;
			while ((i = ips.read()) != -1) {
				text += (char) i;
			}

			String[] line = text.split("\n");

			for (String l : line) {

				if (l.contains(user)) {

					return 1;
				}
			}

			ips.close();
			return 0;
		}

	}

}
