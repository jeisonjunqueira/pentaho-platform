/*!
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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.mantle.client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.mantle.client.messages.Messages;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.gwt.tags.GwtConfirmBox;
import org.pentaho.ui.xul.gwt.tags.GwtMessageBox;
import org.pentaho.ui.xul.util.XulDialogCallback;

import java.util.ArrayList;

public class UserRolesAdminPanelController extends UserRolesAdminPanel implements ISysAdminPanel,
    UpdatePasswordController {

  private static UserRolesAdminPanelController instance = new UserRolesAdminPanelController();

  public static UserRolesAdminPanelController getInstance() {
    return instance;
  }

  public UserRolesAdminPanelController() {
    super();

    usersListBox.addChangeHandler( new UsersListChangeListener() );
    rolesListBox.addChangeHandler( new RolesListChangeListener() );
    systemRolesListBox.addChangeHandler( new SystemRolesListChangeListener() );
    addRoleButton.addClickHandler( new AddRoleListener() );
    removeRoleButton.addClickHandler( new RemoveRoleListener() );
    addAllRolesButton.addClickHandler( new AddAllRolesListener() );
    removeAllRolesButton.addClickHandler( new RemoveAllRolesListener() );
    addUserButton.addClickHandler( new AddUserListener() );
    removeUserButton.addClickHandler( new RemoveUserListener() );
    addAllUsersButton.addClickHandler( new AddAllUsersListener() );
    removeAllUsersButton.addClickHandler( new RemoveAllUsersListener() );
    newUserButton.addClickHandler( new NewUserListener() );
    deleteUserButton.addClickHandler( new DeleteUserListener() );
    newRoleButton.addClickHandler( new NewRoleListener() );
    deleteRoleButton.addClickHandler( new DeleteRoleListener() );
    editPasswordButton.addClickHandler( new EditPasswordListener() );

    activate();
  }

  public void saveUser( final String name, final String password ) {
    String serviceUrl = GWT.getHostPageBaseURL() + "api/userroledao/createUser";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.setHeader( "Content-Type", "application/json" );
      String json = "{\"userName\": \"" + encodeUri( name ) + "\", \"password\": \"" + encodeUri( password ) + "\"}";
      executableTypesRequestBuilder.sendRequest( json, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          initializeAvailableUsers( name );
          initializeRoles( rolesListBox.getValue( rolesListBox.getSelectedIndex() ), "api/userroledao/roles",
              rolesListBox );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  public void saveRole( final String name ) {
    String serviceUrl = GWT.getHostPageBaseURL() + "api/userroledao/createRole?roleName=" + encodeUri( name );
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          initializeRoles( name, "api/userroledao/roles", rolesListBox );
          initializeAvailableUsers( usersListBox.getValue( usersListBox.getSelectedIndex() ) );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  public void deleteRoles() {

    String selectedRoles = "";
    for ( int i = 0; i < rolesListBox.getItemCount(); i++ ) {
      if ( rolesListBox.isItemSelected( i ) ) {
        selectedRoles = selectedRoles + encodeUri( rolesListBox.getValue( i ) ) + "|";
      }
    }

    String serviceUrl = GWT.getHostPageBaseURL() + "api/userroledao/deleteRoles?roleNames=" + selectedRoles;
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          checkForError( Messages.getString( "Error" ), response );
          availableMembersListBox.clear();
          selectedMembersListBox.clear();
          initializeRoles( null, "api/userroledao/roles", rolesListBox );
          initializeAvailableUsers( usersListBox.getValue( usersListBox.getSelectedIndex() ) );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void checkForError( String title, Response response ) {
    if ( response != null && response.getText() != null && response.getText().length() > 0 ) {
      GwtMessageBox messageBox = new GwtMessageBox();
      messageBox.setTitle( title );
      messageBox.setMessage( response.getText() );
      messageBox.setButtons( new Object[GwtMessageBox.ACCEPT] );
      messageBox.setAcceptLabel( Messages.getString( "close" ) );
      messageBox.setWidth( 300 );
      messageBox.show();
    }
  }

  private void displayErrorInMessageBox( String title, String message ) {
    GwtMessageBox messageBox = new GwtMessageBox();
    messageBox.setTitle( title );
    messageBox.setMessage( message );
    messageBox.setButtons( new Object[GwtMessageBox.ACCEPT] );
    messageBox.setAcceptLabel( Messages.getString( "close" ) );
    messageBox.setWidth( 300 );
    messageBox.show();
  }

  public void deleteUsers() {

    String selectedUsers = "";
    for ( int i = 0; i < usersListBox.getItemCount(); i++ ) {
      if ( usersListBox.isItemSelected( i ) ) {
        selectedUsers = selectedUsers + encodeUri( usersListBox.getValue( i ) ) + "|";
      }
    }

    String serviceUrl = GWT.getHostPageBaseURL() + "api/userroledao/deleteUsers?userNames=" + selectedUsers;
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          checkForError( Messages.getString( "Error" ), response );
          userPasswordTextBox.setText( "" );
          availableRolesListBox.clear();
          selectedRolesListBox.clear();
          editPasswordButton.setEnabled( false );
          initializeAvailableUsers( null );
          initializeRoles( rolesListBox.getValue( rolesListBox.getSelectedIndex() ), "api/userroledao/roles",
              rolesListBox );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  public void updatePassword( String newPassword ) {

    String userName = usersListBox.getValue( usersListBox.getSelectedIndex() );
    String serviceUrl = GWT.getHostPageBaseURL() + "api/userroledao/updatePassword";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.setHeader( "Content-Type", "application/json" );
      String json =
          "{\"userName\": \"" + encodeUri( userName ) + "\", \"password\": \"" + encodeUri( newPassword ) + "\"}";
      executableTypesRequestBuilder.sendRequest( json, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {

        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private boolean hasMultiselection( ListBox listBox ) {
    ArrayList<Integer> selectedIndices = new ArrayList<Integer>();
    for ( int i = 0; i < listBox.getItemCount(); i++ ) {
      if ( listBox.isItemSelected( i ) ) {
        selectedIndices.add( i );
      }
    }
    return selectedIndices.size() > 1;
  }

  // -- Remote Calls.

  private void initializeRoles( final String defaultValue, String serviceUrl, final ListBox listBox ) {
    final String url = GWT.getHostPageBaseURL() + serviceUrl;
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/xml" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          listBox.clear();
          NativeEvent event = com.google.gwt.dom.client.Document.get().createChangeEvent();
          String txt = response.getText();
          Document doc = XMLParser.parse( txt );
          NodeList roles = doc.getElementsByTagName( "roles" );
          for ( int i = 0; i < roles.getLength(); i++ ) {
            Node roleNode = roles.item( i );
            String role = roleNode.getFirstChild().getNodeValue();
            listBox.addItem( role );
            if ( !StringUtils.isEmpty( defaultValue ) ) {
              if ( role.equals( defaultValue ) ) {
                listBox.setSelectedIndex( i );
                DomEvent.fireNativeEvent( event, listBox );
              }
            }
          }
          if ( listBox.getSelectedIndex() == -1 && listBox.getItemCount() > 0 ) {
            listBox.setSelectedIndex( 0 );
            DomEvent.fireNativeEvent( event, listBox );
          }
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void initializeAvailableUsers( final String defaultValue ) {
    final String url = GWT.getHostPageBaseURL() + "api/userroledao/users";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/xml" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          usersListBox.clear();
          NativeEvent event = com.google.gwt.dom.client.Document.get().createChangeEvent();
          String txt = response.getText();
          Document doc = XMLParser.parse( txt );
          NodeList users = doc.getElementsByTagName( "users" );
          for ( int i = 0; i < users.getLength(); i++ ) {
            Node userNode = users.item( i );
            String user = userNode.getFirstChild().getNodeValue();
            usersListBox.addItem( user );
            if ( !StringUtils.isEmpty( defaultValue ) ) {
              if ( user.equals( defaultValue ) ) {
                usersListBox.setSelectedIndex( i );
                DomEvent.fireNativeEvent( event, usersListBox );
              }
            }
          }
          if ( usersListBox.getSelectedIndex() == -1 && usersListBox.getItemCount() > 0 ) {
            usersListBox.setSelectedIndex( 0 );
            DomEvent.fireNativeEvent( event, usersListBox );
          }
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void getRolesForUser( String user ) {
    final String url = GWT.getHostPageBaseURL() + "api/userroledao/userRoles?userName=" + encodeUri( user );
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/xml" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          selectedRolesListBox.clear();
          String txt = response.getText();
          Document doc = XMLParser.parse( txt );
          NodeList roles = doc.getElementsByTagName( "roles" );
          for ( int i = 0; i < roles.getLength(); i++ ) {
            Node roleNode = roles.item( i );
            String role = roleNode.getFirstChild().getNodeValue();
            selectedRolesListBox.addItem( role );
          }

          Timer t = new Timer() {
            public void run() {
              if ( rolesListBox.getItemCount() > 0 ) {
                cancel();
                // availableRolesListBox = rolesListBox - selectedRolesListBox
                availableRolesListBox.clear();
                for ( int i = 0; i < rolesListBox.getItemCount(); i++ ) {
                  String role = rolesListBox.getValue( i );
                  boolean isSelected = false;
                  for ( int j = 0; j < selectedRolesListBox.getItemCount(); j++ ) {
                    if ( selectedRolesListBox.getValue( j ).equals( role ) ) {
                      isSelected = true;
                    }
                  }
                  if ( !isSelected ) {
                    availableRolesListBox.addItem( role );
                  }
                }
              }
            }
          };
          t.scheduleRepeating( 100 );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void getUsersInRole( String role ) {
    final String url = GWT.getHostPageBaseURL() + "api/userroledao/roleMembers?roleName=" + encodeUri( role );
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/xml" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          selectedMembersListBox.clear();
          String txt = response.getText();
          Document doc = XMLParser.parse( txt );
          NodeList users = doc.getElementsByTagName( "users" );
          for ( int i = 0; i < users.getLength(); i++ ) {
            Node userNode = users.item( i );
            String user = userNode.getFirstChild().getNodeValue();
            selectedMembersListBox.addItem( user );
          }

          Timer t = new Timer() {
            public void run() {
              if ( usersListBox.getItemCount() > 0 ) {
                cancel();
                // availableMembersListBox = usersListBox - selectedMembersListBox
                availableMembersListBox.clear();
                for ( int i = 0; i < usersListBox.getItemCount(); i++ ) {
                  String user = usersListBox.getValue( i );
                  boolean isSelected = false;
                  for ( int j = 0; j < selectedMembersListBox.getItemCount(); j++ ) {
                    if ( selectedMembersListBox.getValue( j ).equals( user ) ) {
                      isSelected = true;
                    }
                  }
                  if ( !isSelected ) {
                    availableMembersListBox.addItem( user );
                  }
                }
              }
            }
          };
          t.scheduleRepeating( 100 );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void initializeActionBaseSecurityElements() {
    final String url = GWT.getHostPageBaseURL() + "api/userroledao/logicalRoleMap";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/json" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
        }

        public void onResponseReceived( Request request, Response response ) {
          String roleMappings = response.getText();
          rolesPermissionsPanel.initializeActionBaseSecurityElements( roleMappings );
          systemRolesPermissionsPanel.initializeActionBaseSecurityElements( roleMappings );
        }
      } );
    } catch ( RequestException e ) {
      //ignored
    }
  }

  private void processLDAPmode() {
    final String url = GWT.getHostPageBaseURL() + "api/system/authentication-provider";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/json" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
        }

        public void onResponseReceived( Request request, Response response ) {
          boolean usingLDAP = !response.getText().contains( "ldap" );
          usersLabelPanel.setVisible( usingLDAP );
          usersPanel.setVisible( usingLDAP );
          newRoleButton.setVisible( usingLDAP );
          deleteRoleButton.setVisible( usingLDAP );

          if ( !usingLDAP ) {
            mainTabPanel.getTab( 0 ).setVisible( false );
            mainTabPanel.selectTab( 1 );
          } else {
            mainTabPanel.getTab( 0 ).setVisible( true );
            mainTabPanel.selectTab( 0 );
          }
        }
      } );
    } catch ( RequestException e ) {
      //ignored
    }
  }

  private void modifyUserRoles( final String userName, String serviceUrl ) {
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          checkForError( Messages.getString( "Error" ), response );
          getRolesForUser( userName );
          initializeRoles( rolesListBox.getValue( rolesListBox.getSelectedIndex() ), "api/userroledao/roles",
              rolesListBox );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  private void modifyRoleUsers( final String roleName, String serviceUrl ) {
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.PUT, serviceUrl );
    try {
      executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {
        public void onError( Request request, Throwable exception ) {
          displayErrorInMessageBox( Messages.getString( "Error" ), exception.getLocalizedMessage() );
        }

        public void onResponseReceived( Request request, Response response ) {
          checkForError( Messages.getString( "Error" ), response );
          getUsersInRole( roleName );
          initializeAvailableUsers( usersListBox.getValue( usersListBox.getSelectedIndex() ) );
        }
      } );
    } catch ( RequestException e ) {
      displayErrorInMessageBox( Messages.getString( "Error" ), e.getLocalizedMessage() );
    }
  }

  // -- ISysAdminPanel implementation.

  public void activate() {
    processLDAPmode();
    initializeActionBaseSecurityElements();
    initializeAvailableUsers( null );

    final String url = GWT.getHostPageBaseURL() + "api/system/authentication-provider";
    RequestBuilder executableTypesRequestBuilder = new RequestBuilder( RequestBuilder.GET, url );
    executableTypesRequestBuilder.setHeader( "If-Modified-Since", "01 Jan 1970 00:00:00 GMT" );
    executableTypesRequestBuilder.setHeader( "accept", "application/json" );
    try {
      executableTypesRequestBuilder.sendRequest( null, new RequestCallback() {

        public void onError( Request request, Throwable exception ) {
        }

        public void onResponseReceived( Request request, Response response ) {
          boolean usingLDAP = response.getText().contains( "ldap" );
          if ( usingLDAP ) {
            initializeRoles( null, "api/userrolelist/roles", rolesListBox );
          } else {
            initializeRoles( null, "api/userroledao/roles", rolesListBox );
          }
          initializeRoles( null, "api/userrolelist/extraRoles", systemRolesListBox );

        }
      } );
    } catch ( RequestException e ) {
      //ignored
    }
  }

  public String getId() {
    return "userRolesAdminPanel";
  }

  public void passivate( final AsyncCallback<Boolean> callback ) {
    userPasswordTextBox.setText( "" );
    rolesListBox.clear();
    usersListBox.clear();
    selectedRolesListBox.clear();
    selectedMembersListBox.clear();
    availableMembersListBox.clear();
    availableRolesListBox.clear();
    editPasswordButton.setEnabled( false );
    callback.onSuccess( true );
  }

  // -- Event Listeners.

  class UsersListChangeListener implements ChangeHandler {
    public void onChange( ChangeEvent evt ) {
      if ( hasMultiselection( usersListBox ) ) {
        userPasswordTextBox.setText( "" );
        editPasswordButton.setEnabled( false );
        availableRolesListBox.clear();
        selectedRolesListBox.clear();
      } else {
        String user = usersListBox.getValue( usersListBox.getSelectedIndex() );
        if ( !StringUtils.isEmpty( user ) ) {
          getRolesForUser( user );
          userPasswordTextBox.setText( "fakepassword" );
          editPasswordButton.setEnabled( true );
        }
      }
    }
  }

  class RolesListChangeListener implements ChangeHandler {
    public void onChange( ChangeEvent evt ) {
      if ( hasMultiselection( rolesListBox ) ) {
        availableMembersListBox.clear();
        selectedMembersListBox.clear();
      } else {
        String role = rolesListBox.getValue( rolesListBox.getSelectedIndex() );
        if ( !StringUtils.isEmpty( role ) ) {
          getUsersInRole( role );
        }
      }
      rolesPermissionsPanel.setSelectedPermissions();
    }
  }

  class SystemRolesListChangeListener implements ChangeHandler {
    public void onChange( ChangeEvent evt ) {
      systemRolesPermissionsPanel.setSelectedPermissions();
    }
  }

  class AddRoleListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String userName = usersListBox.getValue( usersListBox.getSelectedIndex() );

      String roleNames = "";
      for ( int i = 0; i < availableRolesListBox.getItemCount(); i++ ) {
        if ( availableRolesListBox.isItemSelected( i ) ) {
          roleNames = roleNames + encodeUri( availableRolesListBox.getValue( i ) ) + "|";
        }
      }

      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/assignRoleToUser?userName=" + encodeUri( userName )
              + "&roleNames=" + roleNames;
      modifyUserRoles( userName, serviceUrl );
    }
  }

  class RemoveRoleListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String userName = usersListBox.getValue( usersListBox.getSelectedIndex() );

      String roleNames = "";
      for ( int i = 0; i < selectedRolesListBox.getItemCount(); i++ ) {
        if ( selectedRolesListBox.isItemSelected( i ) ) {
          roleNames = roleNames + encodeUri( selectedRolesListBox.getValue( i ) ) + "|";
        }
      }

      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/removeRoleFromUser?userName=" + encodeUri( userName )
              + "&roleNames=" + roleNames;
      modifyUserRoles( userName, serviceUrl );
    }
  }

  class AddAllRolesListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String userName = usersListBox.getValue( usersListBox.getSelectedIndex() );
      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/assignAllRolesToUser?userName=" + encodeUri( userName );
      modifyUserRoles( userName, serviceUrl );
    }
  }

  class RemoveAllRolesListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String userName = usersListBox.getValue( usersListBox.getSelectedIndex() );
      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/removeAllRolesFromUser?userName=" + encodeUri( userName );
      modifyUserRoles( userName, serviceUrl );
    }
  }

  class AddUserListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String roleName = rolesListBox.getValue( rolesListBox.getSelectedIndex() );

      String userNames = "";
      for ( int i = 0; i < availableMembersListBox.getItemCount(); i++ ) {
        if ( availableMembersListBox.isItemSelected( i ) ) {
          userNames = userNames + encodeUri( availableMembersListBox.getValue( i ) ) + "|";
        }
      }

      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/assignUserToRole?userNames=" + userNames + "&roleName="
              + encodeUri( roleName );
      modifyRoleUsers( roleName, serviceUrl );
    }
  }

  class RemoveUserListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String roleName = rolesListBox.getValue( rolesListBox.getSelectedIndex() );

      String userNames = "";
      for ( int i = 0; i < selectedMembersListBox.getItemCount(); i++ ) {
        if ( selectedMembersListBox.isItemSelected( i ) ) {
          userNames = userNames + encodeUri( selectedMembersListBox.getValue( i ) ) + "|";
        }
      }

      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/removeUserFromRole?userNames=" + userNames + "&roleName="
              + encodeUri( roleName );
      modifyRoleUsers( roleName, serviceUrl );
    }
  }

  class AddAllUsersListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String roleName = rolesListBox.getValue( rolesListBox.getSelectedIndex() );
      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/assignAllUsersToRole?roleName=" + encodeUri( roleName );
      modifyRoleUsers( roleName, serviceUrl );
    }
  }

  class RemoveAllUsersListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      String roleName = rolesListBox.getValue( rolesListBox.getSelectedIndex() );
      String serviceUrl =
          GWT.getHostPageBaseURL() + "api/userroledao/removeAllUsersFromRole?roleName=" + encodeUri( roleName );
      modifyRoleUsers( roleName, serviceUrl );
    }
  }

  class NewRoleListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      RoleDialog roleDialog = new RoleDialog( UserRolesAdminPanelController.this );
      roleDialog.show();
    }
  }

  class DeleteRoleListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      if ( rolesListBox.getSelectedIndex() != -1 ) {
        GwtConfirmBox warning = new GwtConfirmBox();
        warning.setMessage( Messages.getString( "deleteRoleMessage" ) );
        warning.setTitle( Messages.getString( "deleteRoleTitle" ) );
        warning.addDialogCallback( new XulDialogCallback<String>() {
          public void onClose( XulComponent sender, Status returnCode, String retVal ) {
            if ( returnCode == Status.ACCEPT ) {
              deleteRoles();
            }
          }

          public void onError( XulComponent sender, Throwable t ) {
            displayErrorInMessageBox( Messages.getString( "Error" ), t.getLocalizedMessage() );
          }
        } );
        warning.show();
      }
    }
  }

  class NewUserListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      UserDialog userDialog = new UserDialog( UserRolesAdminPanelController.this );
      userDialog.show();
    }
  }

  class DeleteUserListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      if ( usersListBox.getSelectedIndex() != -1 ) {
        GwtConfirmBox warning = new GwtConfirmBox();
        warning.setHeight( 117 );
        warning.setMessage( Messages.getString( "deleteUserMessage" ) );
        warning.setTitle( Messages.getString( "deleteUserTitle" ) );
        warning.addDialogCallback( new XulDialogCallback<String>() {
          public void onClose( XulComponent sender, Status returnCode, String retVal ) {
            if ( returnCode == Status.ACCEPT ) {
              deleteUsers();
            }
          }

          public void onError( XulComponent sender, Throwable t ) {
            displayErrorInMessageBox( Messages.getString( "Error" ), t.getLocalizedMessage() );
          }
        } );
        warning.show();
      }
    }
  }

  class EditPasswordListener implements ClickHandler {
    public void onClick( ClickEvent event ) {
      ChangePasswordDialog changePasswordDialog = new ChangePasswordDialog( UserRolesAdminPanelController.this );
      changePasswordDialog.show();
    }
  }

  private final native String encodeUri( String URI )
  /*-{
    return encodeURIComponent(URI);
  }-*/;
}
