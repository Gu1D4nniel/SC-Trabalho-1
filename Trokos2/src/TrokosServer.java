
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
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
import javax.crypto.IllegalBlockSizeException;
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

					File db = new File("userx.txt");
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

					/*
					 * 
					 * 
					 * 
					 * 
					 * 
					 * // primeiro ve se existe ficheiro e dados no ficheiro if (contemUser(user)
					 * ==0) { System.out.println(" nao contem"); // envia que user eh desconhecido
					 * outStream.writeObject("Este user eh desconhecido"); outStream.flush();
					 * 
					 *
					 * 
					 * 
					 * 
					 * 
					 * // verifica se a assinatura pode ser corretamente verificada com a chave
					 * publica // contida no certificado int verificacao =
					 * verificaAssinatura(assinatura, certificado);
					 * 
					 * if (verificacao == 0) { outStream.writeInt(1);
					 * 
					 * return; }
					 * 
					 * cifra(user, cifra_pw);
					 * outStream.writeObject("Utilizador foi registado e autenticado");
					 * 
					 * } else { System.out.println("contem"); int checkUsers = contemUser(user);
					 * System.out.println(user + ":" +checkUsers); /*if (checkUsers == 0) {
					 * cifra(user, cifra_pw);
					 * outStream.writeObject("userID adicionado ah database"); outStream.flush(); }
					 * else {
					 * 
					 * byte[] nonceAssinado = (byte[]) inStream.readObject(); int
					 * verificaAssinaturaNonce = assinaturaNonce(nonce, nonceAssinado, certificado);
					 * if (verificaAssinaturaNonce == 0) { System.out.println("ERRO3"); }
					 * outStream.writeObject("user autenticado"); outStream.flush();
					 * 
					 * }
					 */

					// recebe o nonce assinado e compara com o nonce criado pelo servidor

					// System.out.println(nonce);
					// CIFRAR OS DADOS

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
					 */

					outStream.writeObject(ops);
					outStream.flush();

					// criarConta(user);

					// makepayment(user, "boy", 10);
					// balance(user);
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
						}else if (confirmQRcode(user, id) == 0) {
								outStream.writeObject("Erro no pagamento");
								outStream.flush();
								outStream.close();
						}else if (confirmQRcode(user, id) == -1) {
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

					// se nao existir no ficheiro cria um novo utilizador e a sua conta
					// criarConta(user, passwd, outStream, pw);

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (WriterException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				outStream.close();
				inStream.close();

				socket.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void addToDb(String user) throws FileNotFoundException {
			File file = new File("users.txt");
			FileInputStream fis = new FileInputStream(file);

			FileInputStream fisParam = new FileInputStream("params.txt");
			FileInputStream fisKey = new FileInputStream("key.key");

			byte[] bytes;
			try {
				bytes = fisParam.readAllBytes();
				byte[] key = fisKey.readAllBytes();
				SecretKey originalKey = new SecretKeySpec(key, 0, key.length, "AES");
				AlgorithmParameters p = AlgorithmParameters.getInstance("PBEWithHmacSHA256AndAES_128");

				p.init(bytes);
				Cipher d = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
				d.init(Cipher.DECRYPT_MODE, originalKey, p);

				byte[] in = new byte[2048];
				int read;
				while ((read = fis.read(in)) != -1) {
					byte[] output = d.update(in, 0, read);

				}
				String t = "";
				byte[] output = d.doFinal();
				for (byte b : output) {
					t += (char) b;

				}
				t += "\n";

				// criaDB(t);
				fisParam.close();
				fisKey.close();
				fis.close();
				System.out.println(t);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private void criaDB(String user) {

			String cert = user + ".cer\n";
			byte[] toCipher = (user + ":" + cert).getBytes();

			byte[] salt = { (byte) 0xc9 };
			FileOutputStream fos;
			FileOutputStream fosKey;
			try {
				fos = new FileOutputStream("users.txt");
				PBEKeySpec keySpec = new PBEKeySpec(cifra_pw.toCharArray(), salt, 20); // pass, salt, iterations
				SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
				SecretKey key = kf.generateSecret(keySpec);

				fosKey = new FileOutputStream("key.key");
				fosKey.write(key.getEncoded());
				Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
				c.init(Cipher.ENCRYPT_MODE, key);

				fos.write(c.doFinal(toCipher));
				// fos.write("\n".getBytes());

				fos.close();

				byte[] params = c.getParameters().getEncoded(); // we need to get the various

				// guardar os parametros da cifra
				FileOutputStream paramFos = new FileOutputStream("params.txt");

				paramFos.write(params);

				paramFos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private void cifra(String user, String cifra_pw) {
			String cert = user + ".cer";
			byte[] toCipher = (user + ":" + cert).getBytes();
			System.out.println(user);
			System.out.println(cert);

			byte[] salt = { (byte) 0xc9, (byte) 0x36, (byte) 0x78, (byte) 0x99, (byte) 0x52, (byte) 0x3e, (byte) 0xea,
					(byte) 0xf2 };
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("users.txt", true);
				PBEKeySpec keySpec = new PBEKeySpec(cifra_pw.toCharArray(), salt, 20); // pass, salt, iterations
				SecretKeyFactory kf = SecretKeyFactory.getInstance("PBEWithHmacSHA256AndAES_128");
				SecretKey key = kf.generateSecret(keySpec);

				Cipher c = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
				c.init(Cipher.ENCRYPT_MODE, key);

				fos.write(c.doFinal(toCipher));
				// fos.write("\n".getBytes());

				fos.close();

				byte[] params = c.getParameters().getEncoded(); // we need to get the various

				// guardar os parametros da cifra
				FileOutputStream paramFos = new FileOutputStream(user + ".params.txt");

				paramFos.write(params);

				paramFos.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Generate the key based on the password
			catch (InvalidKeySpecException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BadPaddingException e) {
				// TODO Auto-generated catch block
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
