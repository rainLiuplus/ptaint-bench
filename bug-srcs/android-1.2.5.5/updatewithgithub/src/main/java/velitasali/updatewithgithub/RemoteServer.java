package velitasali.updatewithgithub;

import com.github.kevinsawicki.http.HttpRequest;

import java.io.IOException;
import java.net.URLEncoder;

public class RemoteServer
{
	private String mConnection;

	public RemoteServer(String serverUri)
	{
		mConnection = serverUri;
	}

	public String connect(String postKey, String postValue) throws IOException
	{
		HttpRequest request = HttpRequest.get(mConnection);
		StringBuilder output = new StringBuilder();

		request.readTimeout(5000);

		if (postKey != null && postValue != null)
			request.send(postKey + "=" + URLEncoder.encode(postValue, "UTF-8")).receive(output);
		else
			request.receive(output);

		return output.toString();
	}

	public String getConnectionAddress()
	{
		return mConnection;
	}

	public void setConnection(String remoteAddress)
	{
		mConnection = remoteAddress;
	}
}