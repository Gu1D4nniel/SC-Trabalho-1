import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.google.zxing.WriterException;

public interface Operations {

	int makepayment(String user, String destino, float valor) throws IOException;

	float balance(String user) throws IOException, NoSuchAlgorithmException;

	//newgroup
	int criaGrupo(String user, String nomeGrupo) throws IOException;
	//addu
	int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException;
	
	String groups (String user) throws IOException;

	int requestpayment(String user, String destino, float valor) throws IOException;

	String viewrequests(String user) throws IOException;

	int payrequest(String id, String user) throws IOException, NoSuchAlgorithmException;

	String obtainQRcode(String user, float valor) throws WriterException, IOException;

	int dividepayment(String user, String nomeGrupo, float valor) throws IOException;

	String statuspayment(String user, String nomeGrupo) throws IOException;

	int confirmQRcode(String user, String id) throws IOException;
	
	 String history(String user) throws IOException;
	 
	 void cifraData(File folder, String user );
	 
	 void decifraData(File folder, String user) throws IOException ;
}
