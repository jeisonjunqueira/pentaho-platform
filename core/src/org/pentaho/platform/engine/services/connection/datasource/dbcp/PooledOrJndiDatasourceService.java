/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2013 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.platform.engine.services.connection.datasource.dbcp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.platform.api.data.DBDatasourceServiceException;
import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.repository.datasource.DatasourceMgmtServiceException;
import org.pentaho.platform.api.repository.datasource.IDatasourceMgmtService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.messages.Messages;

import javax.sql.DataSource;

public class PooledOrJndiDatasourceService extends BaseDatasourceService {

  private static final Log log = LogFactory.getLog( PooledOrJndiDatasourceService.class );

  public PooledOrJndiDatasourceService() {
  }

  DataSource retrieve( String datasource ) throws DBDatasourceServiceException {
    DataSource ds = null;
    try {
      IDatasourceMgmtService datasourceMgmtSvc =
          (IDatasourceMgmtService) PentahoSystem.get( IDatasourceMgmtService.class, PentahoSessionHolder.getSession() );
      IDatabaseConnection databaseConnection = datasourceMgmtSvc.getDatasourceByName( datasource );
      // Look in the database for the datasource
      if ( databaseConnection != null && !databaseConnection.getAccessType().equals( DatabaseAccessType.JNDI ) ) {
        ds = PooledDatasourceHelper.setupPooledDataSource( databaseConnection );
        // Database does not have the datasource, look in jndi now
      } else if ( databaseConnection == null ) {
        ds = getJndiDataSource( datasource );
      } else {
        ds = getJndiDataSource( databaseConnection.getDatabaseName() );
      }
      // if the resulting datasource is not null then store it in the cache
      if ( ds != null ) {
        cacheManager.putInRegionCache( IDBDatasourceService.JDBC_DATASOURCE, datasource, ds );
      }
    } catch ( DatasourceMgmtServiceException daoe ) {
      daoe.printStackTrace();
      log.debug( Messages.getInstance().getErrorString(
          "PooledOrJndiDatasourceService.DEBUG_0001_UNABLE_TO_FIND_DATASOURCE_IN_REPOSITORY",
          daoe.getLocalizedMessage() ), daoe );
      try {
        return getJndiDataSource( datasource );
      } catch ( DBDatasourceServiceException dse ) {
        throw new DBDatasourceServiceException( Messages.getInstance().getErrorString(
            "PooledOrJndiDatasourceService.ERROR_0003_UNABLE_TO_GET_JNDI_DATASOURCE" ), dse ); //$NON-NLS-1$
      }
    }
    return ds;
  }

  /**
   * This method clears the JNDI DS cache. The need exists because after a JNDI connection edit the old DS must be
   * removed from the cache.
   * 
   */
  public void clearCache() {
    cacheManager.removeRegionCache( IDBDatasourceService.JDBC_DATASOURCE );
  }

  /**
   * This method clears the JNDI DS cache. The need exists because after a JNDI connection edit the old DS must be
   * removed from the cache.
   * 
   */
  public void clearDataSource( String dsName ) {
    cacheManager.removeFromRegionCache( IDBDatasourceService.JDBC_DATASOURCE, dsName );
  }

  /**
   * Since JNDI is supported different ways in different app servers, it's nearly impossible to have a ubiquitous
   * way to look up a datasource. This method is intended to hide all the lookups that may be required to find a
   * jndi name.
   * 
   * @param dsName
   *          The Datasource name
   * @return DataSource if there is one bound in JNDI
   * @throws NamingException
   */
  public DataSource getDataSource( String dsName ) throws DBDatasourceServiceException {
    DataSource dataSource = null;
    if ( cacheManager != null ) {
      if ( !cacheManager.cacheEnabled( IDBDatasourceService.JDBC_DATASOURCE ) ) {
        cacheManager.addCacheRegion( IDBDatasourceService.JDBC_DATASOURCE );
      }
      Object foundDs = cacheManager.getFromRegionCache( IDBDatasourceService.JDBC_DATASOURCE, dsName );
      if ( foundDs != null ) {
        dataSource = (DataSource) foundDs;
      } else {
        dataSource = retrieve( dsName );
      }
    }
    return dataSource;
  }
}
