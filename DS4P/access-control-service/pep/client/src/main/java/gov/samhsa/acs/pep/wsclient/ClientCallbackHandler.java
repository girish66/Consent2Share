package gov.samhsa.acs.pep.wsclient;

import gov.samhsa.acs.pep.security.CredentialProvider;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class ClientCallbackHandler implements CallbackHandler {
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback) {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[i];
                if (pc.getUsage() == WSPasswordCallback.DECRYPT ||
                        pc.getUsage() == WSPasswordCallback.SIGNATURE) {
                    // typically X.509 auth only
                    if ("myclientkey".equals(pc.getIdentifier())) {
                        pc.setPassword("ckpass");
                    }
                } else if (pc.getUsage() == WSPasswordCallback.USERNAME_TOKEN) {
                    // UsernameToken auth only
                    if (CredentialProvider.getUser().equals(pc.getIdentifier())) {
                        pc.setPassword(CredentialProvider.getPassword());
                    }
                }
            }
        }
    }

}

