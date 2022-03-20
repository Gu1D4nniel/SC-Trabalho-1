
/***************************************************************************
*   Seguranca e Confiabilidade 2020/21
*
*
***************************************************************************/

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

/*Como usar 
 * java TrokosServer <port>
 * por omissao o porto eh 45678
 * 
 */
public class TrokosServer {

	private static int port;

	public static void main(String[] args) {

		// guarda porto
		if (args.length == 0) {

			port = 45678;
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
		// sSoc.close();
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				File fileDb = new File("db.txt");
				/*
				 * FileOutputStream fin = new FileOutputStream(fileDb, true); OutputStream ops =
				 * new BufferedOutputStream(fin);
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
							return;
						}
						if (username.equals(user.trim()) && !password.equals(passwd.trim())) {
							System.out.println("password incorreta");
							return;
						}
						// if(!username.equals(user.trim())) {

					}

					// se nao existir no ficheiro cria um novo utilizador
					pw.println(user + ":" + passwd);
					System.out.println("Utilizador:" + user + " foi criado");

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
	}
}
