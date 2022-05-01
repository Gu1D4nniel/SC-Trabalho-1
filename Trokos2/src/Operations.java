import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import com.google.zxing.WriterException;

public interface Operations {

	int makepayment(String user, String destino, float valor) throws IOException, NoSuchAlgorithmException, InvalidKeyException;

	float balance(String user) throws IOException, NoSuchAlgorithmException;

	//newgroup
	int criaGrupo(String user, String nomeGrupo) throws IOException;
	//addu
	int adicionaAoGrupo(String owner, String user, String nomeGrupo) throws IOException;
	
	String groups (String user) throws IOException;

	int requestpayment(String user, String destino, float valor) throws IOException;

	String viewrequests(String user) throws IOException;

	int payrequest(String id, String user) throws IOException, NoSuchAlgorithmException, InvalidKeyException;

	String obtainQRcode(String user, float valor) throws WriterException, IOException;

	int dividepayment(String user, String nomeGrupo, float valor) throws IOException;

	String statuspayment(String user, String nomeGrupo) throws IOException;

	int confirmQRcode(String user, String id) throws IOException, NoSuchAlgorithmException, InvalidKeyException;
	
	 String history(String user) throws IOException;
	 
	 void cifraData(File folder, String user );
}
