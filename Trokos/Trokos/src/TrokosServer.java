import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import java.util.Scanner;

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

	private static int port;

	private float startBalance = 100;

	private final String ops = "------------------------------------------------------------\nOperaçoes:\nbalance\nmakepayment <userID><amount>"
			+ "\nrequestpayment <userID> <valor>\nviewrequests\npayrequest <reqID>\nobtainQRcode <amount>\nconfirmQRcode <QRcode>"
			+ "\nnewgroup <groupID>\naddu <userID><groupID>groups"
			+ "\ndividepayment <groupID><amount>\nstatuspayments <groupID>\nhistory< <groupID>"
			+ "\n------------------------------------------------------------\n";

	public static void main(String[] args) throws FileNotFoundException {

		// guarda porto
		if (args.length == 0) {

			port = 45678;
		}
		// clearDB
		else if (args[0].equals("clearDB")) {
			File fileToDelete = new File("./db.txt");
			fileToDelete.delete();

			File folderData = new File("./dataUsers");
			File folderGrupos = new File("./groups");
			File folderRequests = new File("./requests");
			File folderQRcodes = new File("./QRcodes");
			File folderQRrequests = new File ("./QRrequests");

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

			return;
		} else {
			port = Integer.parseInt(args[0]);
		}

		System.out.println("servidor: main\nport:" + port);

		TrokosServer server = new TrokosServer();
		server.startServer();
	}

	public void startServer() {
		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			// System.out.println("thread do server para cada cliente");
		}

		public void run() {
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

				// Tem o true pq senao faz sempre overwrite aos dados do ficheiro
				PrintWriter pw = new PrintWriter(new FileWriter(fileDb, true));
				Scanner sc = new Scanner(fileDb);

				try {

					String user = null;
					String passwd = null;

					// recebe o tamanho do username e da passwd do client (em bytes)
					byte sizeU = (Byte) inStream.readObject();
					byte sizeP = (Byte) inStream.readObject();

					// transforma o tamanho de bytes para inteiros
					int u = Byte.toUnsignedInt(sizeU);
					int p = Byte.toUnsignedInt(sizeP);

					char[] arrayU = new char[u + p + 1];
					char[] arrayP = new char[p];

					// cria buffers para receber password e username
					byte[] bufferU = new byte[arrayU.length];
					// byte[] bufferP = new byte[p];

					// recebe username
					inStream.read(bufferU, 0, bufferU.length);

					// separa o username e a password que recebeu do buffer cliente
					String line = new String(bufferU, StandardCharsets.UTF_8);
					String[] arr = new String[line.length()];
					arr = line.split(":");
					user = arr[0].trim();
					passwd = arr[1].trim();

					// Verificacao na db
					while (sc.hasNextLine()) {
						String input = sc.nextLine();
						String username = input.substring(0, input.indexOf(':'));
						String password = input.substring(input.indexOf(':') + 1, input.length());
						if (username.equals(user.trim()) && password.equals(passwd.trim())) {
							System.out.println("user autenticado");
							outStream.writeObject("Utilizador autenticado");
							outStream.flush();
							outStream.writeObject(ops);
							outStream.flush();

							//
							String respostaOp = (String) inStream.readObject();
							System.out.println("O user escolheu:" + respostaOp);

							if (respostaOp.contains(("balance")) || respostaOp.charAt(0) == 'b') {
								String balance = String.valueOf(balance(user));

								outStream.writeObject(balance);
								outStream.flush();
								outStream.close();

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
								}

							} else if (respostaOp.contains("viewrequests") || respostaOp.charAt(0) == 'v') {
								String resposta = viewrequests(user);

								outStream.writeObject(resposta);
								outStream.flush();
								outStream.close();

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
								} else {

									outStream.writeObject("Pagamento feito");
									outStream.flush();
									outStream.close();
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

							} else if (respostaOp.contains("addu") || respostaOp.charAt(0) == 'a') {
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
								}
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

							}else if (respostaOp.contains("confirmQRcode") || respostaOp.charAt(0) == 'c') {
								String [] split = respostaOp.split(" ");
								
								if (split.length != 2) {
									outStream.writeObject("Dados nao estao completos");
									outStream.flush();
									outStream.close();
									return;
								}
								String id = split[1];
								if(confirmQRcode(user,id ) == 1) {
									outStream.writeObject("Pagamento feito com sucesso");
									outStream.flush();
									outStream.close();
								}
							}
							
							else {
								outStream.flush();
								outStream.close();

							}

							return;
						}
						if (username.equals(user.trim()) && !password.equals(passwd.trim())) {
							System.out.println("password incorreta");
							outStream.writeObject("Password incorreta");
							outStream.flush();
							outStream.close();
							return;
						}

					}

					// se nao existir no ficheiro cria um novo utilizador e a sua conta
					criarConta(user, passwd, outStream, pw);

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (WriterException e) {
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
