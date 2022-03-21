import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/*Como usar 
 * java TrokosServer <port>
 * por omissao o porto eh 45678
 * 
 * 
 * java TrokosServer clearDB 
 * limpa a db (ajuda a testar)
 */
public class TrokosServer {

	private static int port;

	private float startBalance = 100;

	private final String ops = "------------------------------------------------------------\nOperaçoes:\nbalance\nmakepayment <userID><amount>\nviewrequests"
			+ "\npayrequest <reqID>\nobtainQRcode <amount>\nconfirmQRcode <QRcode>"
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
			File fileToDelete = new File("db.txt");
			fileToDelete.delete();

			File file = new File("dataUsers");
			File[] files = file.listFiles();
			for (File f : files) {

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

				File fileDb = new File("db.txt");
				File dataFile = new File("dataUsers");
				dataFile.mkdir();

				/*
				 * FileOutputStream fin = new FileOutputStream(fileDb, true); OutputStream ops =
				 * new BufferedOutputStream(fin);
				 * 
				 * 
				 * 
				 * FileInputStream fis = new FileInputStream(fileDb); InputStream ips = new
				 * BufferedInputStream(fis);
				 */

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
								System.out.println("BALANCE:"+balance);
								outStream.writeObject(balance);
								outStream.flush();
								// System.out.println(balance);
							} else if (respostaOp.contains(("makepayment")) || respostaOp.charAt(0) == 'm') {
								String[] split = respostaOp.split(" ");
								if (split.length < 3) {
									outStream.writeObject("Dados nao estao completos");
									return;
								}
								String destino = split[1];
								float valor = Float.parseFloat(split[2]);
								int resposta = makepayment(user, destino, valor);
								if (resposta == 0) {
									outStream.writeObject("Nao foi encontrado o destino");
									outStream.flush();
								} else if (resposta == -1) {
									outStream.writeObject("Nao tem saldo");
									outStream.flush();
								} else if (resposta == -2) {
									outStream.writeObject("O user nao pode ser igual ao destino");
									outStream.flush();
								} else {
									outStream.writeObject("O pagamento foi feito");
									outStream.flush();
								}

							} else {
								return;
							}

							return;
						}
						if (username.equals(user.trim()) && !password.equals(passwd.trim())) {
							System.out.println("password incorreta");
							outStream.writeObject("Password incorreta");
							outStream.flush();
							return;
						}

					}

					// se nao existir no ficheiro cria um novo utilizador e a sua conta
					criarConta(user, passwd, outStream, pw);

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
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
			userWriter.println("Requests from:");

			outStream.writeObject("Utilizador " + user + " foi criado com sucesso");
			outStream.flush();
			userWriter.close();

		}

		/**
		 * metodo que devolve o balanço de um dado user
		 * 
		 * @param user utilizador que fez o pedido balance
		 * @return o balanço da conta de um dado user
		 * @throws IOException 
		 */
		private float balance(String user) throws IOException {
			File file = new File("dataUsers/" + user + ".txt");
			float balance =0;
			
			FileInputStream fisUser = new FileInputStream(file);
			InputStream ipsUser = new BufferedInputStream(fisUser);	
			
			int i =0;
			String dataUser ="";
			while ((i = ipsUser.read())!= -1) {
				dataUser += (char) i;
			}
			
			
			
			String [] lines = dataUser.split("\n");
			for (String l : lines) {
				if (l.contains("Balance")) {
					String [] data = l.split(":");
					balance = Float.parseFloat(data[1]);
					
				}
			}
			ipsUser.close();
			return balance;

		}

		/**
		 * metodo que envia dinheiro de um user para outro
		 * 
		 * @param user    user que esta a enviar dinheiro
		 * @param destino user que vai receber o dinheiro
		 * @param valor   quantidade de dinheiro
		 * @throws IOException
		 * @throws FileNotFoundException
		 */
		private int makepayment(String user, String destino, float valor) throws IOException {
			Scanner sc = new Scanner(new File("db.txt"));

			// procura o destino na db
			int check = 0;
			while ((sc.hasNextLine())) {
				String input = sc.nextLine();

				if (input.contains(destino.trim())) {
					check = 1;
				}
			}//Nao existe o destino
			if (check == 0) {
				return check;
			}//User e destino sao iguais
			if (user.equals(destino)) {
				check = -2;
				return check;
			
			}

			//File do user que envia dinheiro
			File file = new File("dataUsers/" + user + ".txt");
			
			FileInputStream fisUser = new FileInputStream(file);
			InputStream ipsUser = new BufferedInputStream(fisUser);

			//Le o ficheiro e transforma em string
			int i;
			String dataUser = "";
			while ((i = ipsUser.read()) != -1) {
				dataUser += (char) i;
			}
			ipsUser.close();
			//Divide as linhas
			String[] lines = dataUser.split("\n");
			
			//procura a linha que contem o balance
			for (String l : lines) {
				if (l.contains("Balance")) {
					String[] data = l.split(":");
					//Se o valor a enviar foir maior que o saldo da erro
					if (Float.parseFloat(data[1]) - valor < 0) {
						return -1;
					} else {
						//Abre o outStream para atualizar os dados dos ficheiros
						FileOutputStream fin = new FileOutputStream(file, false);
						OutputStream ops = new BufferedOutputStream(fin);
						float valorFinal = Float.parseFloat(data[1]) - valor;
						data[1] = String.valueOf(valorFinal);
						byte[] testeB = (data[0] + ":" + data[1]).getBytes();

						ops.write(testeB);
						ops.write("\n".getBytes());
						ops.write(lines[1].getBytes());
						ops.flush();
						ops.close();

					}
				}
			}

			//Ficheiro destino
			File fileDestino = new File("dataUsers/" + destino + ".txt");

			FileInputStream fisDestino = new FileInputStream(fileDestino);
			InputStream ipsDestino = new BufferedInputStream(fisDestino);

			//Le o ficheiro destino e transforma em string
			int j;
			String dataDestino = "";
			while ((j = ipsDestino.read()) != -1) {
				dataDestino += (char) i;
			}
			ipsDestino.close();

			//Divide as linhas
			String[] linesDestino = dataUser.split("\n");

			//Procura a line com o balance
			for (String l : linesDestino) {
				if (l.contains("Balance")) {
					String[] data = l.split(":");

					//Abre um output stream para atualizar o ficheiro
					FileOutputStream fin = new FileOutputStream(fileDestino, false);
					OutputStream ops = new BufferedOutputStream(fin);
					float valorFinal = Float.parseFloat(data[1]) + valor;
					data[1] = String.valueOf(valorFinal);
					byte[] testeB = (data[0] + ":" + data[1]).getBytes();

					ops.write(testeB);
					ops.write("\n".getBytes());
					ops.write(lines[1].getBytes());
					ops.flush();
					ops.close();

				}
			}

			sc.close();
			return 1;
		}
	}
}
