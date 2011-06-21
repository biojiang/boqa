package sonumina.b4oweb.client;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface B4OServiceAsync {

	void getTest(AsyncCallback<String> callback);

	void getNumberOfTerms(AsyncCallback<Integer> callback);

	void getNamesOfTerms(List<Integer> ids, AsyncCallback<String[]> callback);

}
