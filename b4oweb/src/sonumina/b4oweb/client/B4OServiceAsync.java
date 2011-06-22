package sonumina.b4oweb.client;

import java.util.List;

import sonumina.b4oweb.shared.SharedTerm;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface B4OServiceAsync {

	void getNumberOfTerms(AsyncCallback<Integer> callback);

	void getNamesOfTerms(List<Integer> ids, AsyncCallback<SharedTerm[]> callback);

	void getNumberOfTerms(String pattern, AsyncCallback<Integer> callback);

}
