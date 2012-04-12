/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2008 Pentaho Corporation.  All rights reserved.
 *
 * Created Mar 03, 2012
 * @author Ezequiel Cuellar
 */

package org.pentaho.mantle.client.admin;

import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.mantle.client.ui.xul.MantleXul;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.gwt.tags.GwtConfirmBox;
import org.pentaho.ui.xul.gwt.tags.GwtMessageBox;
import org.pentaho.ui.xul.util.XulDialogCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;

public class EmailAdminPanelController extends EmailAdminPanel implements ISysAdminPanel, UpdatePasswordController {

	private boolean isDirty = false;

	private static EmailAdminPanelController instance = new EmailAdminPanelController();
	
	public static EmailAdminPanelController getInstance() {
	  return instance;
	}
	
	private EmailAdminPanelController() {
		super();
		((Button) saveButton.getManagedObject()).addClickHandler(new SaveButtonChangeListener());
		authenticationCheckBox.addValueChangeHandler(new AuthenticateChangeHandler());
		editPasswordButton.addClickHandler(new EditPasswordListener());
		testButton.addClickHandler(new EmailTestButtonChangeListener());
		smtpHostTextBox.addKeyUpHandler(new AuthenticationHandler());
		portTextBox.addKeyUpHandler(new AuthenticationHandler());
		fromAddressTextBox.addKeyUpHandler(new AuthenticationHandler());
		userNameTextBox.addKeyUpHandler(new AuthenticationHandler());
		authenticationCheckBox.addValueChangeHandler(new AuthenticationHandler());
		debuggingCheckBox.addValueChangeHandler(new AuthenticationHandler());
		useSSLCheckBox.addValueChangeHandler(new AuthenticationHandler());
		useStartTLSCheckBox.addValueChangeHandler(new AuthenticationHandler());
		protocolsListBox.addChangeHandler(new AuthenticationHandler());
		activate();
	}

	public void updatePassword(String password) {
		passwordTextBox.setValue(password);
    if(passwordTextBox.getValue() != null && passwordTextBox.getValue().length() > 0) {
      passwordTextBox.getManagedObject().setEnabled(false);
    }
		testButton.setEnabled(isValid());
		isDirty = true;
	}

	public boolean isValid() {
		boolean smtpValid = !StringUtils.isEmpty(smtpHostTextBox.getValue());
		boolean fromAddressValid = isValidEmail(fromAddressTextBox.getValue());
		boolean portValid = isPortValid(portTextBox.getValue());
		boolean authenticationValid = true;
		if (authenticationCheckBox.getValue()) {
			boolean userNameValid = !StringUtils.isEmpty(userNameTextBox.getValue());
			boolean passwordValid = !StringUtils.isEmpty(passwordTextBox.getValue());
			authenticationValid = userNameValid && passwordValid;
		}
		if(portValid && smtpValid && fromAddressValid && authenticationValid) {
		  actionBar.expand();
		} else {
		  actionBar.collapse();
		}
		
		return portValid && smtpValid && fromAddressValid && authenticationValid;
	}

	private String appendUIParameters() {
		StringBuffer params = new StringBuffer();
		params.append("?authenticate=");
		params.append(authenticationCheckBox.getValue());
		params.append("&debug=");
		params.append(debuggingCheckBox.getValue());
		params.append("&defaultFrom=");
		params.append(fromAddressTextBox.getValue());
		params.append("&smtpHost=");
		params.append(smtpHostTextBox.getValue());
		params.append("&smtpPort=");
		params.append(portTextBox.getValue());
		params.append("&smtpProtocol=");
		params.append(protocolsListBox.getValue(protocolsListBox.getSelectedIndex()).toLowerCase());
		params.append("&userId=");
		params.append(userNameTextBox.getValue());
		params.append("&password=");
		params.append(passwordTextBox.getValue());
		params.append("&useSsl=");
		params.append(useSSLCheckBox.getValue());
		params.append("&useStartTls=");
		params.append(useStartTLSCheckBox.getValue());
		return params.toString();
	}

	// -- Remote Calls.

	private void setEmailConfig() {
    saveButton.inProgress(true);
		String serviceUrl = GWT.getHostPageBaseURL() + "api/emailconfig/setEmailConfig" + appendUIParameters();
		RequestBuilder executableTypesRequestBuilder = new RequestBuilder(RequestBuilder.PUT, serviceUrl);
		try {
			executableTypesRequestBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
			    saveButton.inProgress(false);
				}

				public void onResponseReceived(Request request, Response response) {
          actionBar.collapse();
			    saveButton.inProgress(false);
					isDirty = false; 
					isDirty = false;
				}
			});
		} catch (RequestException e) {
		}
	}

	private void testEmail() {
		String serviceUrl = GWT.getHostPageBaseURL() + "api/emailconfig/sendEmailTest" + appendUIParameters();
		RequestBuilder executableTypesRequestBuilder = new RequestBuilder(RequestBuilder.GET, serviceUrl);
		try {
			executableTypesRequestBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
				}

				public void onResponseReceived(Request request, Response response) {
					String message = null;
					if (response.getText().equals("EmailTester.SUCESS")) {
						message = Messages.getString("connectionTest.sucess");
					} else if (response.getText().equals("EmailTester.FAIL")) {
						message = Messages.getString("connectionTest.fail");
					}
					GwtMessageBox messageBox = new GwtMessageBox();
					messageBox.setTitle(Messages.getString("connectionTest"));
					messageBox.setMessage(message);
					messageBox.setButtons(new Object[GwtMessageBox.ACCEPT]);
					messageBox.setAcceptLabel(Messages.getString("close"));
					messageBox.show();
				}
			});
		} catch (RequestException e) {
		}
	}

	private void getEmailConfig() {
		String serviceUrl = GWT.getHostPageBaseURL() + "api/emailconfig/getEmailConfig";
		RequestBuilder executableTypesRequestBuilder = new RequestBuilder(RequestBuilder.GET, serviceUrl);
		try {
			executableTypesRequestBuilder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
				}

				public void onResponseReceived(Request request, Response response) {
					JSONObject emailData = (JSONObject) JSONParser.parseLenient(response.getText());
					JSONBoolean authenticate = (JSONBoolean) emailData.get("authenticate");
					JSONBoolean debug = (JSONBoolean) emailData.get("debug");
					JSONString defaultFrom = (JSONString) emailData.get("defaultFrom");
					JSONString smtpHost = (JSONString) emailData.get("smtpHost");
					JSONNumber smtpPort = (JSONNumber) emailData.get("smtpPort");
					JSONString smtpProtocol = (JSONString) emailData.get("smtpProtocol");
					JSONString userId = (JSONString) emailData.get("userId");
					JSONString password = (JSONString) emailData.get("password");
					JSONBoolean useSsl = (JSONBoolean) emailData.get("useSsl");
					JSONBoolean useStartTls = (JSONBoolean) emailData.get("useStartTls");

					authenticationCheckBox.setValue(authenticate.booleanValue());
					smtpHostTextBox.setValue(smtpHost.stringValue());
					portTextBox.setValue(smtpPort.toString());
					useStartTLSCheckBox.setValue(useStartTls.booleanValue());
					useSSLCheckBox.setValue(useSsl.booleanValue());
					fromAddressTextBox.setValue(defaultFrom.stringValue());
					userNameTextBox.setValue(userId.stringValue());
					// If password is non-empty.. disable the text-box
					if(!StringUtils.isEmpty(password.stringValue())) {
					  passwordTextBox.getManagedObject().setEnabled(false);
					}
					 
					passwordTextBox.setValue(password.stringValue());
					
					debuggingCheckBox.setValue(debug.booleanValue());

					String protocol = smtpProtocol.stringValue();
					protocolsListBox.setSelectedIndex(protocol.equalsIgnoreCase("smtp") ? 0 : 1);

					authenticationPanel.setVisible(authenticate.booleanValue());
          testButton.setEnabled(isValid());
				}
			});
		} catch (RequestException e) {
		}
	}

	// -- ISysAdminPanel implementation.

	public void activate() {
		isDirty = false;
		getEmailConfig();
		if(passwordTextBox.getValue() != null && passwordTextBox.getValue().length() > 0) {
	    passwordTextBox.getManagedObject().setEnabled(false);
		}
	}

	public String getId() {
		return "emailAdminPanel";
	}

	public void passivate(final AsyncCallback<Boolean> callback) {
		if (isDirty) {
			GwtConfirmBox messageBox = new GwtConfirmBox();
			messageBox.setTitle(Messages.getString("confirm"));
			messageBox.setMessage(Messages.getString("dirtyStateMessage"));
			messageBox.addDialogCallback(new XulDialogCallback<String>() {

				public void onClose(XulComponent component, XulDialogCallback.Status status, String value) {
					if (status == XulDialogCallback.Status.ACCEPT) {
						callback.onSuccess(true);
					}
					if (status == XulDialogCallback.Status.CANCEL) {
						MantleXul.getInstance().selectAdminCatTreeTreeItem(Messages.getString("emailSmtpServer"));
						callback.onSuccess(false);
					}
				}

				public void onError(XulComponent e, Throwable t) {
				}
			});
			messageBox.show();
		} else {
			callback.onSuccess(true);
		}
	}

	// -- Event Listeners.

	class SaveButtonChangeListener implements ClickHandler {
		public void onClick(ClickEvent event) {
			setEmailConfig();
		}
	}

	class AuthenticateChangeHandler implements ValueChangeHandler<Boolean> {

		public void onValueChange(ValueChangeEvent<Boolean> value) {
			authenticationPanel.setVisible(value.getValue());
		}
	}

	class EditPasswordListener implements ClickHandler {
		public void onClick(ClickEvent event) {
			ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog(EmailAdminPanelController.this);
			changePasswordDialog.show();
		}
	}

	class AuthenticationHandler implements KeyUpHandler, ValueChangeHandler<Boolean>, ChangeHandler {

		public void onKeyUp(KeyUpEvent e) {
		  testButton.setEnabled(isValid());
			isDirty = true;
		}

		public void onValueChange(ValueChangeEvent<Boolean> value) {
		  testButton.setEnabled(isValid());
      isDirty = true;
		}

		public void onChange(ChangeEvent value) {
		  testButton.setEnabled(isValid());
      isDirty = true;
		}
	}

	class EmailTestButtonChangeListener implements ClickHandler {
		public void onClick(ClickEvent event) {
			testEmail();
		}
	}
}
